from fastapi import APIRouter, HTTPException
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, SystemMessage, AIMessage
from app.core.config import settings
from app.models.schemas import ChatRequest
import os
from langchain_chroma import Chroma
from app.core.embeddings import SiliconFlowEmbeddings

router = APIRouter()

@router.get("/health")
def health_check():
    return {
        "status": "ok",
        "service": "dms-ai-service",
        "model": settings.AI_MODEL_NAME
    }

from app.services.agent import dms_agent

@router.post("/chat")
async def chat_endpoint(request: ChatRequest):
    """
    AI 聊天服务 HTTP POST 接口，暴露给前端悬浮窗使用。
    - 接收当前的最新提问 (message)、历史会话列表 (history)、用户 Token 与角色角色 (role)。
    - 调用 LangGraph Agent 进行智能决策、Chroma 知识检索与 Tool 业务调用。
    """
    if not settings.AI_API_KEY:
        raise HTTPException(
            status_code=500,
            detail="AI Service is not configured. Missing API Key."
        )

    try:
        # 1. 重构并拼装对话历史上下文消息列表
        # 由于大模型是无状态的，必须在每次请求时重新传回历史聊天记录
        initial_messages = []
        
        # 拼接以往对话，区分为人类用户的 HumanMessage 和大模型的 AIMessage
        for h in request.history:
            if h.role == "user":
                initial_messages.append(HumanMessage(content=h.content))
            elif h.role == "assistant":
                initial_messages.append(AIMessage(content=h.content))

        # 将当前用户正在发送的提问追加到列表末尾
        initial_messages.append(HumanMessage(content=request.message))

        print(f"[FastAPI] 收到聊天请求: '{request.message}'")
        print(f"[FastAPI] 包含历史对话数: {len(request.history)}，JWT Token: {'已提供' if request.token else '未提供'}")
        
        # 2. 调用 LangGraph 工作流引擎开始运行
        # 传入对话消息序列，以及用于身份标识的真实 Token 和 role id。
        # 状态机中的拦截器和提示词生成器将直接读取这部分数据。
        result_state = dms_agent.invoke({
            "messages": initial_messages,
            "token": request.token,
            "role": request.role,
            "context": ""
        })
        
        # 3. 提取最终的运行结果并返回
        # 状态图运行完毕后，最末尾的消息即为最终的大模型最终汇总回复
        final_message = result_state["messages"][-1]
        print(f"[FastAPI] 返回大模型回复: {final_message.content[:50]}...")
        
        return {"response": final_message.content}

    except Exception as e:
        print(f"[FastAPI Error] Agent 执行失败: {e}")
        raise HTTPException(
            status_code=500,
            detail=f"AI 服务交互异常: {str(e)}"
        )
