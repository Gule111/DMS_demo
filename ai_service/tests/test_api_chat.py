import requests
import json

url = "http://127.0.0.1:8082/api/chat"
payload = {
    "message": "我要在哪里提交我的体检表？",
    "history": []
}

try:
    print(f"[*] 准备向本地 FastAPI 发起 POST 请求: {url}")
    print(f"[*] 提问: {payload['message']}")
    response = requests.post(url, json=payload, timeout=20)
    print(f"[*] HTTP 状态码: {response.status_code}")
    print("[*] 响应 JSON 内容:")
    print(json.dumps(response.json(), indent=2, ensure_ascii=False))
except Exception as e:
    print(f"[Error] API 交互测试失败: {e}")
