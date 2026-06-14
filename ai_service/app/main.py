from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.routes import router
from app.core.config import settings

app = FastAPI(
    title="DMS AI Service",
    description="驾校管理系统 AI 基础服务 (FastAPI) - 基于微服务架构解耦",
    version="1.0.0"
)

# 允许跨域
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 挂载路由 (带有 /api 前缀)
app.include_router(router, prefix="/api")

if __name__ == "__main__":
    import uvicorn
    # 获取端口配置，默认使用 8082
    uvicorn.run("app.main:app", host="0.0.0.0", port=settings.AI_SERVICE_PORT, reload=True)
