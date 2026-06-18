import os
from typing import Annotated, Sequence, TypedDict
from langchain_core.messages import BaseMessage, SystemMessage, ToolMessage
from langgraph.graph.message import add_messages
from langgraph.graph import StateGraph, START, END
from langchain_openai import ChatOpenAI
from langchain_chroma import Chroma

from app.core.config import settings
from app.core.embeddings import SiliconFlowEmbeddings
from app.services.tools import (
    get_my_progress,
    get_my_instructor,
    get_my_appointments,
    book_training_session,
    cancel_appointment,
    get_instructor_students,
    record_training_hours,
    get_instructor_appointments,
    handle_student_appointment,
    record_exam_result,
    get_pending_registrations,
    audit_registration,
    get_all_coaches,
    assign_coach_to_student,
    get_my_exams,
    book_exam_session,
    cancel_exam_booking,
    get_admin_exam_list,
    audit_exam_booking,
    record_exam_score,
    get_exam_sites
)

# 定义工具字典，方便按名字查找
TOOLS_MAP = {
    "get_my_progress": get_my_progress,
    "get_my_instructor": get_my_instructor,
    "get_my_appointments": get_my_appointments,
    "book_training_session": book_training_session,
    "cancel_appointment": cancel_appointment,
    "get_instructor_students": get_instructor_students,
    "record_training_hours": record_training_hours,
    "get_instructor_appointments": get_instructor_appointments,
    "handle_student_appointment": handle_student_appointment,
    "record_exam_result": record_exam_result,
    "get_pending_registrations": get_pending_registrations,
    "audit_registration": audit_registration,
    "get_all_coaches": get_all_coaches,
    "assign_coach_to_student": assign_coach_to_student,
    "get_my_exams": get_my_exams,
    "book_exam_session": book_exam_session,
    "cancel_exam_booking": cancel_exam_booking,
    "get_admin_exam_list": get_admin_exam_list,
    "audit_exam_booking": audit_exam_booking,
    "record_exam_score": record_exam_score,
    "get_exam_sites": get_exam_sites
}

# 1. 定义 Graph State (图的状态字典)
# 这是在整个 LangGraph 节点和状态流转中共享的数据对象
class AgentState(TypedDict):
    # 消息记录列表 (包含用户提问、AI 回复、Tool 返回的执行报告)，add_messages 会自动将新消息追加进列表
    messages: Annotated[Sequence[BaseMessage], add_messages]
    # 用户身份认证的 JWT Token（由前端传来，用于给 Tools 发送 API 时做鉴权）
    token: str
    # 用户的角色标识 (1-管理员, 2-教练员, 3-学员)，用于在调用 Tools 时进行严格的接口权限硬校验
    role: int
    # 存储经由 RAG 向量数据库检索出来的知识库参考上下文
    context: str

# 2. 节点 (Node)：执行 RAG 知识检索
def retrieve_context(state: AgentState):
    """
    根据用户最后一条提问，去 Chroma 向量数据库进行相似度检索，
    并将匹配出来的业务规则和参考信息追加到 state["context"] 中提供给大模型参考。
    """
    messages = state["messages"]
    last_user_message = messages[-1].content
    
    chroma_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', 'data', 'chroma_db'))
    context_str = ""
    try:
        # 如果存在编译好的向量库，则加载并搜索最相似的 2 条记录
        if os.path.exists(chroma_path):
            embeddings = SiliconFlowEmbeddings(
                model_name=settings.EMBEDDING_MODEL_NAME,
                api_key=settings.EMBEDDING_API_KEY,
                base_url=settings.EMBEDDING_BASE_URL
            )
            db = Chroma(persist_directory=chroma_path, embedding_function=embeddings)
            # 搜索匹配
            retrieved_docs = db.similarity_search(last_user_message, k=2)
            if retrieved_docs:
                context_str = "\n\n".join([doc.page_content for doc in retrieved_docs])
                print(f"[LangGraph] 成功检索到 {len(retrieved_docs)} 个上下文片段。")
    except Exception as e:
        print(f"[LangGraph Error] RAG 检索失败，忽略并转入无上下文推理：{e}")
        
    return {"context": context_str}

# 3. 节点 (Node)：调用大模型进行推理
def call_model(state: AgentState):
    """
    根据当前的用户角色组装系统提示词 (System Prompt)，绑定可用 Tools 列表，
    并把 RAG 知识库上下文合并传入大模型，驱动模型做出思考、对话或调用工具的选择。
    """
    messages = state["messages"]
    context_str = state.get("context", "")
    
    from datetime import datetime
    current_time_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    # 获取经过安全验证的用户真实角色，转换成对应的中文描述
    role = state.get("role", 3)
    role_name = "学员"
    if role == 1:
        role_name = "管理员"
    elif role == 2:
        role_name = "教练员"
        
    # 构造极度严密的安全系统提示词 (第一防线)
    system_prompt = (
        "你是一个专业、友好的驾校管理系统 AI 助手 (AI智能助手)。\n"
        f"当前系统时间是：{current_time_str}。\n"
        f"当前登录用户的角色是：{role_name}（1代表管理员，2代表教练员，3代表学员）。\n"
        "【重要角色与权限指令】：\n"
        "1. 学员专属功能：get_my_progress、get_my_instructor、get_my_appointments、book_training_session、cancel_appointment、get_my_exams、book_exam_session、cancel_exam_booking。如果当前用户不是学员，必须拒绝他们访问这些功能。\n"
        "2. 教练专属功能：get_instructor_students、record_training_hours、get_instructor_appointments、handle_student_appointment、record_exam_result。如果当前用户不是教练，必须拒绝访问。\n"
        "3. 管理员专属功能：get_pending_registrations、audit_registration、get_all_coaches、assign_coach_to_student、get_admin_exam_list、audit_exam_booking、record_exam_score。如果当前用户不是管理员，必须拒绝访问。\n"
        "4. 学员与管理员共用功能：get_exam_sites 用于查询可用考场列表。如果当前用户是教练，必须拒绝访问。\n"
        "【基本指令】：\n"
        "1. 如果用户的问题可以通过参考下方【参考知识库】解答，请优先基于知识库内容给出回答。\n"
        "2. 在调用工具时，系统会自动为你注入 token，你无需询问用户 token 也不必尝试伪造。只需将你识别出的所需参数填入即可。\n"
        "3. 当用户询问“你能做什么”或介绍功能时，请**仅针对当前用户的角色**，直接用文字全面总结你可以为该角色提供的专属服务（例如：除了查询，还要强调能直接发起的预约或审批等操作），**绝对不要**列出属于其他角色的功能，防范越权行为。\n\n"
    )
    # 如果通过 RAG 召回了相关文档，则将其作为背景知识拼装给模型
    if context_str:
        system_prompt += f"【参考知识库】:\n{context_str}\n"

    # 初始化大语言模型连接
    llm = ChatOpenAI(
        model=settings.AI_MODEL_NAME,
        openai_api_key=settings.AI_API_KEY,
        openai_api_base=settings.AI_BASE_URL,
        temperature=1.0
    )
    # 将定义的工具函数绑定到大模型上，让模型具备通过 Function Calling 触发工具调用的能力
    llm_with_tools = llm.bind_tools(list(TOOLS_MAP.values()))
    
    # 组装 System Prompt 与所有的历史会话消息
    invoke_messages = [SystemMessage(content=system_prompt)] + list(messages)
    
    print(f"[LangGraph] 调用 LLM 进行推理...")
    response = llm_with_tools.invoke(invoke_messages)
    
    return {"messages": [response]}

# 4. 节点 (Node)：执行大模型选择的工具函数 (Tool execution)
def execute_tools(state: AgentState):
    """
    核心安全过滤节点 (第二防线)
    解析 LLM 返回的 tool_calls，强行拦截并进行硬编码层面的越权防范校验。
    如果校验通过，自动将用户真实的 Jwt Token 注入工具中并调用 Java 接口，最后收集工具返回的结果。
    """
    messages = state["messages"]
    last_message = messages[-1]
    token = state.get("token", "")
    
    tool_outputs = []
    if hasattr(last_message, "tool_calls"):
        for tool_call in last_message.tool_calls:
            tool_name = tool_call["name"]
            tool_args = dict(tool_call["args"])
            
            # 【安全防护 1】：拦截并注入真实用户 Token，不允许大模型自行伪造
            tool_args["token"] = token
            
            # 【安全防护 2】：硬编码做角色隔离拦截，即使黑客通过提示词注入诱导了大模型生成调用，也会在此被强行阻断！
            role = state.get("role", 3)
            student_only_tools = ["get_my_progress", "get_my_instructor", "get_my_appointments", "book_training_session", "cancel_appointment", "get_my_exams", "book_exam_session", "cancel_exam_booking"]
            instructor_only_tools = ["get_instructor_students", "record_training_hours", "get_instructor_appointments", "handle_student_appointment", "record_exam_result"]
            admin_only_tools = ["get_pending_registrations", "audit_registration", "get_all_coaches", "assign_coach_to_student", "get_admin_exam_list", "audit_exam_booking", "record_exam_score"]
            
            # 严格权限验证
            if tool_name in student_only_tools and role != 3:
                output = "错误: 您当前的角色不是学员，无权访问或操作学员专属功能。"
            elif tool_name in instructor_only_tools and role != 2:
                output = "错误: 您当前的角色不是教练员，无权访问或操作教练专属功能。"
            elif tool_name in admin_only_tools and role != 1:
                output = "错误: 您当前的角色不是管理员，无权访问或操作管理员专属功能。"
            elif tool_name == "get_exam_sites" and role not in [1, 3]:
                output = "错误: 您当前的角色无权查询考场。"
            else:
                # 校验通过，开始调用具体的 Python Tool 逻辑并访问 Java 后端接口
                print(f"[LangGraph Tool] 准备执行工具 {tool_name}，参数: {tool_args}")
                tool = TOOLS_MAP.get(tool_name)
                if not tool:
                    output = f"错误: 找不到工具 {tool_name}"
                else:
                    try:
                        output = tool.invoke(tool_args)
                        print(f"[LangGraph Tool] 工具 {tool_name} 执行完毕，输出: {output}")
                    except Exception as e:
                        output = f"工具 {tool_name} 执行异常: {str(e)}"
            
            # 将工具执行结果作为 ToolMessage 返回，以便状态机流转回大模型进行下一步汇总回答
            tool_outputs.append(
                ToolMessage(
                    content=str(output),
                    tool_call_id=tool_call["id"],
                    name=tool_name
                )
            )
            
            
    return {"messages": tool_outputs}

# 5. 条件路由函数 (Conditional Router)
def should_continue(state: AgentState) -> str:
    """
    检查最后一个消息是否包含大模型的 tool_calls 意图。
    - 如果有，则路由分发到 "tools" 节点去执行具体的接口调用；
    - 如果没有，说明大模型不需要调用接口，直接回答即可，路由分发到 END 结束工作流。
    """
    last_message = state["messages"][-1]
    if hasattr(last_message, "tool_calls") and len(last_message.tool_calls) > 0:
        return "tools"
    return END

# ================================================
# 构建并编译 LangGraph 工作流状态机引擎 (StateMachine)
# ================================================
builder = StateGraph(AgentState)

# 注册各个工作节点
builder.add_node("retrieve", retrieve_context)
builder.add_node("agent", call_model)
builder.add_node("tools", execute_tools)

# 定义默认的静态执行连线：START -> 检索上下文 -> 大模型思考
builder.add_edge(START, "retrieve")
builder.add_edge("retrieve", "agent")

# 在大模型节点之后绑定条件路由线：根据思考结果（是否要用工具）动态决定走向
builder.add_conditional_edges(
    "agent",
    should_continue,
    {
        "tools": "tools",
        END: END
    }
)

# 工具节点执行完后，必须连线回到大模型，让大模型阅读工具返回的业务数据并进行最终的自然语言汇总回答
builder.add_edge("tools", "agent")

# 编译编译图状态机引擎，生成可直接在业务代码中 invoke() 的 dms_agent 实例
dms_agent = builder.compile()
