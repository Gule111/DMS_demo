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
    """
    def __init__(self, token: str):
        self.token = token
        # Spring Boot API endpoint URL
        self.base_url = BACKEND_BASE_URL
        
    def _get_headers(self) -> Dict[str, str]:
        headers = {"Content-Type": "application/json"}
        if self.token:
            # 如果 token 不是以 Bearer 开头，则添加 Bearer 前缀
            auth_token = self.token if self.token.lower().startswith("bearer ") else f"Bearer {self.token}"
            headers["Authorization"] = auth_token
        return headers

    def get(self, endpoint: str, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        url = f"{self.base_url}{endpoint}"
        try:
            response = requests.get(url, headers=self._get_headers(), params=params, timeout=10)
            response.raise_for_status()
            return response.json()
        except requests.RequestException as e:
            print(f"[BackendClient] GET {endpoint} failed: {e}")
            if e.response is not None:
                try:
                    return e.response.json()
                except Exception:
                    return {"code": 500, "message": str(e)}
            return {"code": 500, "message": str(e)}

    def post(self, endpoint: str, data: Optional[Dict[str, Any]] = None, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        url = f"{self.base_url}{endpoint}"
        try:
            response = requests.post(url, headers=self._get_headers(), json=data, params=params, timeout=10)
            response.raise_for_status()
            return response.json()
        except requests.RequestException as e:
            print(f"[BackendClient] POST {endpoint} failed: {e}")
            if e.response is not None:
                try:
                    return e.response.json()
                except Exception:
                    return {"code": 500, "message": str(e)}
            return {"code": 500, "message": str(e)}
