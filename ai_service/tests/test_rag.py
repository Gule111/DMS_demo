import os
import sys
from dotenv import load_dotenv
from langchain_chroma import Chroma

# 将根目录添加到 sys.path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from app.core.embeddings import SiliconFlowEmbeddings

# Load backend/.env for credentials
env_path = os.path.join(os.path.dirname(__file__), '..', '..', 'backend', '.env')
load_dotenv(dotenv_path=env_path)

CHROMA_DB_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'data', 'chroma_db'))

def test_query():
    api_key = os.getenv('EMBEDDING_API_KEY')
    base_url = os.getenv('EMBEDDING_BASE_URL', 'https://api.siliconflow.cn/v1')
    model_name = os.getenv('EMBEDDING_MODEL_NAME', 'BAAI/bge-large-zh-v1.5')
    
    embeddings = SiliconFlowEmbeddings(
        model_name=model_name,
        api_key=api_key,
        base_url=base_url
    )
    
    print(f"[*] 加载 Chroma 本地数据库: {CHROMA_DB_PATH}")
    db = Chroma(persist_directory=CHROMA_DB_PATH, embedding_function=embeddings)
    
    query = "考C1驾照有哪些身体要求"
    print(f"[*] 查询语义: '{query}'")
    results = db.similarity_search(query, k=2)
    
    print("\n[检索结果展示]:")
    for idx, doc in enumerate(results):
        print(f"\n--- 结果 {idx + 1} (来源: {doc.metadata.get('source')}) ---")
        print(doc.page_content)
    
    assert len(results) > 0, "检索结果不应为空"
    print("\n[Success] RAG 检索测试成功！")

if __name__ == '__main__':
    test_query()
