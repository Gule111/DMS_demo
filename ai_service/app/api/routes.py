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
    AI 聊天服务接口
    使用 LangGraph Agent 处理对话，集成 RAG 与业务 Tool 调用
    """
    if not settings.AI_API_KEY:
        raise HTTPException(
            status_code=500,
            detail="AI Service is not configured. Missing API Key."
        )

    try:
        # 1. 构建对话上下文消息列表
        initial_messages = []
        
        # 拼接历史消息 (忽略 system 消息，因为 agent 内部会自动构建 SystemMessage)
        for h in request.history:
            if h.role == "user":
                initial_messages.append(HumanMessage(content=h.content))
            elif h.role == "assistant":
                initial_messages.append(AIMessage(content=h.content))

        # 添加当前用户发送的消息
        initial_messages.append(HumanMessage(content=request.message))

        print(f"[FastAPI] 收到聊天请求: '{request.message}'")
        print(f"[FastAPI] 包含历史对话数: {len(request.history)}，JWT Token: {'已提供' if request.token else '未提供'}")
        
        # 2. 调用 LangGraph agent 执行工作流
        result_state = dms_agent.invoke({
            "messages": initial_messages,
            "token": request.token,
            "role": request.role,
            "context": ""
        })
        
        # 3. 提取最终的回复内容
        final_message = result_state["messages"][-1]
        print(f"[FastAPI] 返回大模型回复: {final_message.content[:50]}...")
        
        return {"response": final_message.content}

    except Exception as e:
        print(f"[FastAPI Error] Agent 执行失败: {e}")
        raise HTTPException(
            status_code=500,
            detail=f"AI 服务交互异常: {str(e)}"
        )
