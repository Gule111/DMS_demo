from typing import List
from langchain_core.embeddings import Embeddings
import requests

class SiliconFlowEmbeddings(Embeddings):
    """
    自定义硅基流动 (SiliconFlow) 向量生成客户端，继承自 LangChain Embeddings。
    通过直接调用 HTTP 接口，避开 langchain-openai 的本地 token 预处理 (tiktoken) 及多余参数，
    完美兼容 SiliconFlow 的 API。
    """
    def __init__(self, api_key: str, base_url: str, model_name: str):
        self.api_key = api_key
        self.base_url = base_url.rstrip("/")
        self.model_name = model_name
        self.headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }

    def embed_documents(self, texts: List[str]) -> List[List[float]]:
        # 硅基流动推荐每批最多 64 条
        batch_size = 32
        embeddings = []
        url = f"{self.base_url}/embeddings"
        
        for i in range(0, len(texts), batch_size):
            batch = texts[i:i+batch_size]
            payload = {
                "model": self.model_name,
                "input": batch
            }
            response = requests.post(url, headers=self.headers, json=payload)
            response.raise_for_status()
            res_json = response.json()
            
            # 保证顺序提取
            sorted_data = sorted(res_json["data"], key=lambda x: x.get("index", 0))
            batch_embeddings = [item["embedding"] for item in sorted_data]
            embeddings.extend(batch_embeddings)
            
        return embeddings

    def embed_query(self, text: str) -> List[float]:
        url = f"{self.base_url}/embeddings"
        payload = {
            "model": self.model_name,
            "input": [text]
        }
        response = requests.post(url, headers=self.headers, json=payload)
        response.raise_for_status()
        res_json = response.json()
        return res_json["data"][0]["embedding"]
