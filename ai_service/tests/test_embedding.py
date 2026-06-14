import os
import sys
from dotenv import load_dotenv

# 将当前根目录添加到 sys.path 以便导入 app
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from app.core.embeddings import SiliconFlowEmbeddings

# Load backend/.env for credentials
env_path = os.path.join(os.path.dirname(__file__), '..', '..', 'backend', '.env')
load_dotenv(dotenv_path=env_path)

def test_embedding():
    print("[*] 正在测试 SiliconFlow Embedding 连通性...")
    api_key = os.getenv('EMBEDDING_API_KEY')
    base_url = os.getenv('EMBEDDING_BASE_URL', 'https://api.siliconflow.cn/v1')
    model_name = os.getenv('EMBEDDING_MODEL_NAME', 'BAAI/bge-large-zh-v1.5')

    if not api_key:
        print("[Error] 未配置 EMBEDDING_API_KEY，请检查 backend/.env 文件。")
        return

    print(f"Embedding Model: {model_name}")
    print(f"Embedding Base URL: {base_url}")

    try:
        embeddings = SiliconFlowEmbeddings(
            model_name=model_name,
            api_key=api_key,
            base_url=base_url
        )
        res = embeddings.embed_query("驾校怎么报名")
        print(f"[Success] 向量生成成功！维度: {len(res)}，前 5 维数据: {res[:5]}")
    except Exception as e:
        print(f"[Error] 向量计算失败: {e}")

if __name__ == '__main__':
    test_embedding()
