import os
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, SystemMessage

# 加载上级目录(backend)的 .env 文件
env_path = os.path.join(os.path.dirname(__file__), '..', '..', 'backend', '.env')
load_dotenv(dotenv_path=env_path)

print("[*] 开始测试本地 LangChain 与月之暗面 API 连通性...")
print(f"当前模型: {os.getenv('AI_MODEL_NAME')}")
print(f"API Base URL: {os.getenv('AI_BASE_URL')}")

# 检查环境变量
api_key = os.getenv("AI_API_KEY")
if not api_key:
    print("[Error] 环境变量中缺少 AI_API_KEY，请检查 backend/.env 文件配置。")
    exit(1)

# 初始化大模型客户端
llm = ChatOpenAI(
    model=os.getenv("AI_MODEL_NAME", "moonshot-v1-8k"),
    openai_api_key=api_key,
    openai_api_base=os.getenv("AI_BASE_URL", "https://api.moonshot.cn/v1"),
    temperature=0.7
)

# 准备测试消息
messages = [
    SystemMessage(content="你是一个专业的驾校管理助手。"),
    HumanMessage(content="你好！请问考C1驾照有哪些基本身体要求？")
]

try:
    print("[*] 发起请求，正在等待 AI 回复...")
    response = llm.invoke(messages)
    print("\n[AI 响应内容]:")
    print(response.content)
    print("\n[Success] 测试成功！全新虚拟环境中的 LangChain 可以与月之暗面 API 正常工作！")
except Exception as e:
    print(f"\n[Error] 测试失败: {e}")
