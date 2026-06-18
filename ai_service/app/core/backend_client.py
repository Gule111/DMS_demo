import requests
import json
from typing import Dict, Any, Optional
# Since backend_client is in app/core, we import settings from app.core.config
from app.core.config import settings

# Default SpringBoot Backend URL
BACKEND_BASE_URL = "http://localhost:8080"

class BackendClient:
    """
    HTTP 客户端用于与 SpringBoot 后端进行交互，自动附带授权 JWT Token。
    这架桥梁使得 Python AI 服务能以当前登录的学员/教练/管理员的身份调用 Java 的业务接口。
    """
    def __init__(self, token: str):
        self.token = token
        # Spring Boot 后端默认接口服务地址
        self.base_url = BACKEND_BASE_URL
        
    def _get_headers(self) -> Dict[str, str]:
        """ 构造带有一致 JSON 格式和 JWT Token 身份认证信息的请求头 """
        headers = {"Content-Type": "application/json"}
        if self.token:
            # 统一加上 Bearer 认证前缀。Java 后端的过滤器会提取此 Header 并进行解密验签。
            auth_token = self.token if self.token.lower().startswith("bearer ") else f"Bearer {self.token}"
            headers["Authorization"] = auth_token
        return headers

    def get(self, endpoint: str, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """
        封装统一的 HTTP GET 请求方法
        - endpoint: 接口子路径（例如 '/progress/my'）
        - params: 请求查询参数
        """
        url = f"{self.base_url}{endpoint}"
        try:
            response = requests.get(url, headers=self._get_headers(), params=params, timeout=10)
            # 若状态码非 2xx 则抛出 HTTPError 异常
            response.raise_for_status()
            return response.json()
        except requests.RequestException as e:
            print(f"[BackendClient Warning] GET {endpoint} 失败: {e}")
            if e.response is not None:
                try:
                    return e.response.json()
                except Exception:
                    return {"code": 500, "message": str(e)}
            return {"code": 500, "message": str(e)}

    def post(self, endpoint: str, data: Optional[Dict[str, Any]] = None, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """
        封装统一的 HTTP POST 请求方法
        - endpoint: 接口子路径（例如 '/appointment/book'）
        - data: 以 JSON Body 发送的请求体参数
        - params: Query 传参
        """
        url = f"{self.base_url}{endpoint}"
        try:
            response = requests.post(url, headers=self._get_headers(), json=data, params=params, timeout=10)
            response.raise_for_status()
            return response.json()
        except requests.RequestException as e:
            print(f"[BackendClient Warning] POST {endpoint} 失败: {e}")
            if e.response is not None:
                try:
                    return e.response.json()
                except Exception:
                    return {"code": 500, "message": str(e)}
            return {"code": 500, "message": str(e)}
