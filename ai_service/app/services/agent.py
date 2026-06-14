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
    assign_coach_to_student
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
    "assign_coach_to_student": assign_coach_to_student
}

# 1. 定义 Graph State
class AgentState(TypedDict):
    messages: Annotated[Sequence[BaseMessage], add_messages]
    token: str
    role: int
    context: str

# 2. 节点：执行 RAG 检索
def retrieve_context(state: AgentState):
    """
    根据用户最后一条消息去 Chroma 检索，把结果追加到 state["context"]
    """
    messages = state["messages"]
    last_user_message = messages[-1].content
    
    chroma_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', 'data', 'chroma_db'))
    context_str = ""
    try:
        if os.path.exists(chroma_path):
            embeddings = SiliconFlowEmbeddings(
                model_name=settings.EMBEDDING_MODEL_NAME,
                api_key=settings.EMBEDDING_API_KEY,
                base_url=settings.EMBEDDING_BASE_URL
            )
            db = Chroma(persist_directory=chroma_path, embedding_function=embeddings)
            # 搜索最相关的 2 条知识段落
            retrieved_docs = db.similarity_search(last_user_message, k=2)
            if retrieved_docs:
                context_str = "\n\n".join([doc.page_content for doc in retrieved_docs])
                print(f"[LangGraph] 成功检索到 {len(retrieved_docs)} 个上下文片段。")
    except Exception as e:
        print(f"[LangGraph Error] RAG 检索失败，忽略：{e}")
        
    return {"context": context_str}

# 3. 节点：调用 LLM 大模型
def call_model(state: AgentState):
    """
    组装 System Prompt（含 context），绑定 tools，并调用大模型
    """
    messages = state["messages"]
    context_str = state.get("context", "")
    
    from datetime import datetime
    current_time_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    role = state.get("role", 3)
    role_name = "学员"
    if role == 1:
        role_name = "管理员"
    elif role == 2:
        role_name = "教练员"
        
    # 构造系统提示词
    system_prompt = (
        "你是一个专业、友好的驾校管理系统 AI 助手 (DMS Copilot)。\n"
        f"当前系统时间是：{current_time_str}。\n"
        f"当前登录用户的角色是：{role_name}（1代表管理员，2代表教练员，3代表学员）。\n"
        "【重要角色与权限指令】：\n"
        "1. 学员专属功能：get_my_progress、get_my_instructor、get_my_appointments、book_training_session、cancel_appointment。如果当前用户不是学员，必须拒绝他们访问这些功能。\n"
        "2. 教练专属功能：get_instructor_students、record_training_hours、get_instructor_appointments、handle_student_appointment、record_exam_result。如果当前用户不是教练，必须拒绝访问。\n"
        "3. 管理员专属功能：get_pending_registrations、audit_registration、get_all_coaches、assign_coach_to_student。如果当前用户不是管理员，必须拒绝访问。\n"
        "【基本指令】：\n"
        "1. 如果用户的问题可以通过参考下方【参考知识库】解答，请优先基于知识库内容给出回答。\n"
        "2. 在调用工具时，系统会自动为你注入 token，你无需询问用户 token 也不必尝试伪造。只需将你识别出的所需参数填入即可。\n"
        "3. 当用户询问“你能做什么”或介绍功能时，请**仅针对当前用户的角色**，直接用文字全面总结你可以为该角色提供的专属服务（例如：除了查询，还要强调能直接发起的预约或审批等操作），**绝对不要**列出属于其他角色的功能，也不要擅自执行工具去拉取真实数据作答。\n\n"
    )
    if context_str:
        system_prompt += f"【参考知识库】:\n{context_str}\n"

    # 初始化模型并绑定所有定义的 Tools
    llm = ChatOpenAI(
        model=settings.AI_MODEL_NAME,
        openai_api_key=settings.AI_API_KEY,
        openai_api_base=settings.AI_BASE_URL,
        temperature=0.7
    )
    llm_with_tools = llm.bind_tools(list(TOOLS_MAP.values()))
    
    # 将 SystemMessage 放在最前面
    invoke_messages = [SystemMessage(content=system_prompt)] + list(messages)
    
    print(f"[LangGraph] 调用 LLM 进行推理...")
    response = llm_with_tools.invoke(invoke_messages)
    
    return {"messages": [response]}

# 4. 节点：执行 Tool
def execute_tools(state: AgentState):
    """
    解析 LLM 返回的 tool_calls，注入 token 并执行，返回结果消息。
    """
    messages = state["messages"]
    last_message = messages[-1]
    token = state.get("token", "")
    
    tool_outputs = []
    if hasattr(last_message, "tool_calls"):
        for tool_call in last_message.tool_calls:
            tool_name = tool_call["name"]
            tool_args = dict(tool_call["args"])
            
            # 【核心逻辑】：拦截工具参数，强制注入当前的真实用户 Token
            tool_args["token"] = token
            
            # 【安全检查】：防范越权调用，严格的角色隔离
            role = state.get("role", 3)
            student_only_tools = ["get_my_progress", "get_my_instructor", "get_my_appointments", "book_training_session", "cancel_appointment"]
            instructor_only_tools = ["get_instructor_students", "record_training_hours", "get_instructor_appointments", "handle_student_appointment", "record_exam_result"]
            admin_only_tools = ["get_pending_registrations", "audit_registration", "get_all_coaches", "assign_coach_to_student"]
            
            if tool_name in student_only_tools and role != 3:
                output = "错误: 您当前的角色不是学员，无权访问或操作学员专属功能。"
            elif tool_name in instructor_only_tools and role != 2:
                output = "错误: 您当前的角色不是教练员，无权访问或操作教练专属功能。"
            elif tool_name in admin_only_tools and role != 1:
                output = "错误: 您当前的角色不是管理员，无权访问或操作管理员专属功能。"
            else:
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
            
            tool_outputs.append(
                ToolMessage(
                    content=str(output),
                    tool_call_id=tool_call["id"],
                    name=tool_name
                )
            )
            
    return {"messages": tool_outputs}

# 5. 路由函数：决定走向
def should_continue(state: AgentState) -> str:
    """
    检查最后一个消息是否有 tool_calls。有则执行工具，无则结束图。
    """
    last_message = state["messages"][-1]
    if hasattr(last_message, "tool_calls") and len(last_message.tool_calls) > 0:
        return "tools"
    return END

# ================================
# 构建并编译 LangGraph StateMachine
# ================================
builder = StateGraph(AgentState)

# 注册节点
builder.add_node("retrieve", retrieve_context)
builder.add_node("agent", call_model)
builder.add_node("tools", execute_tools)

# 连线
builder.add_edge(START, "retrieve")
builder.add_edge("retrieve", "agent")

# 添加条件路由
builder.add_conditional_edges(
    "agent",
    should_continue,
    {
        "tools": "tools",
        END: END
    }
)

# 工具执行完毕后，回传给大模型汇总
builder.add_edge("tools", "agent")

# 编译成可直接 invoke 的应用
dms_agent = builder.compile()
