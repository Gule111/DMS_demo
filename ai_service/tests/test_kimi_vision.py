import os
import base64
import requests
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI

# 加载 .env
env_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', 'backend', '.env'))
load_dotenv(dotenv_path=env_path)

api_key = os.getenv("AI_API_KEY")
base_url = os.getenv("AI_BASE_URL", "https://api.moonshot.cn/v1")
model_name = os.getenv("AI_MODEL_NAME", "kimi-k2.5")

def encode_image_from_url(url: str) -> str:
    """下载图片并转换为 base64 编码的 data URI"""
    print(f"正在下载图片: {url}")
    headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36'}
    response = requests.get(url, headers=headers)
    response.raise_for_status()
    encoded = base64.b64encode(response.content).decode("utf-8")
    content_type = response.headers.get('Content-Type', 'image/png')
    return f"data:{content_type};base64,{encoded}"

def test_vision():
    print(f"初始化模型: {model_name}")
    llm = ChatOpenAI(
        model=model_name,
        openai_api_key=api_key,
        openai_api_base=base_url,
        temperature=1.0,
        max_tokens=1000
    )

    image_url = "https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png"
    
    print("\n--- 测试一: 直接传递 URL ---")
    messages_url = [
        {
            "role": "user",
            "content": [
                {"type": "text", "text": "描述这张图片"},
                {"type": "image_url", "image_url": {"url": image_url}}
            ]
        }
    ]
    try:
        res = llm.invoke(messages_url)
        print("URL 测试成功:", res.content)
    except Exception as e:
        print("URL 测试失败:", str(e))

    print("\n--- 测试二: 传递 Base64 编码数据 ---")
    try:
        base64_url = encode_image_from_url(image_url)
        messages_b64 = [
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": "描述这张图片"},
                    {"type": "image_url", "image_url": {"url": base64_url}}
                ]
            }
        ]
        res = llm.invoke(messages_b64)
        print("Base64 测试成功:", res.content)
    except Exception as e:
        print("Base64 测试失败:", str(e))

if __name__ == "__main__":
    test_vision()
