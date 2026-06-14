import os
from dotenv import load_dotenv

# 加载上级目录(backend)的 .env 文件
# 当前文件所在路径：ai_service/app/core/config.py
# .env 所在路径：backend/.env
env_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..', 'backend', '.env'))
load_dotenv(dotenv_path=env_path)

class Settings:
    # 数据库配置
    DB_HOST: str = os.getenv('DB_HOST', 'localhost')
    DB_PORT: int = int(os.getenv('DB_PORT', 3306))
    DB_NAME: str = os.getenv('DB_NAME', 'dms_demo')
    DB_USER: str = os.getenv('DB_USERNAME', 'root')
    DB_PASS: str = os.getenv('DB_PASSWORD', '')

    # Redis 配置
    REDIS_HOST: str = os.getenv('REDIS_HOST', 'localhost')
    REDIS_PORT: int = int(os.getenv('REDIS_PORT', 6379))
    REDIS_PASSWORD: str = os.getenv('REDIS_PASSWORD', None)

    # AI 模型配置
    AI_API_KEY: str = os.getenv('AI_API_KEY', '')
    AI_BASE_URL: str = os.getenv('AI_BASE_URL', 'https://api.moonshot.cn/v1')
    AI_MODEL_NAME: str = os.getenv('AI_MODEL_NAME', 'moonshot-v1-8k')
    
    # RAG 向量检索配置
    EMBEDDING_API_KEY: str = os.getenv('EMBEDDING_API_KEY', '')
    EMBEDDING_BASE_URL: str = os.getenv('EMBEDDING_BASE_URL', 'https://api.siliconflow.cn/v1')
    EMBEDDING_MODEL_NAME: str = os.getenv('EMBEDDING_MODEL_NAME', 'BAAI/bge-large-zh-v1.5')
    # 应用程序端口
    AI_SERVICE_PORT: int = int(os.getenv("AI_SERVICE_PORT", 8082))

# 单例配置实例
settings = Settings()
