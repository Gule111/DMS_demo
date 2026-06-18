import base64
import requests
from langchain_openai import ChatOpenAI
from app.core.config import settings
from app.models.schemas import AuditResult

def encode_image_from_url(url: str) -> str:
    if not url:
        return ""
    try:
        headers = {'User-Agent': 'Mozilla/5.0'}
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        encoded = base64.b64encode(response.content).decode("utf-8")
        content_type = response.headers.get('Content-Type', 'image/png')
        return f"data:{content_type};base64,{encoded}"
    except Exception as e:
        print(f"[AI Warning] 无法下载图片 {url}: {e}")
        return ""

def call_langchain_audit(task_data: dict) -> tuple[int, str]:
    """ 使用 LangChain + 大模型进行材料审核 """
    if not settings.AI_API_KEY:
        print("[AI Error] AI_API_KEY 环境变量未配置")
        return 2, "AI配置错误: 缺失 API 密钥，请联系管理员。"

    # 1. 提取材料数据
    license_type = task_data.get('licenseType', 'C1')
    id_card_front = task_data.get('idCardFront', '')
    id_card_back = task_data.get('idCardBack', '')
    health_cert = task_data.get('healthCert', '')

    try:
        print("[AI] 正在拉取并编码图片数据...")
        b64_front = encode_image_from_url(id_card_front)
        b64_back = encode_image_from_url(id_card_back)
        b64_health = encode_image_from_url(health_cert)

        # 2. 构造多模态输入格式
        messages = [
            {
                "role": "user",
                "content": [
                    {
                        "type": "text",
                        "text": (
                            f"你是一个资深的驾校报名材料审查助手。\n"
                            f"请根据报考车型要求，对学员上传的图片进行合规性初审。\n"
                            f"【当前报考车型】: {license_type}\n"
                            f"【审核要求】:\n"
                            f"1. 确认身份证正面照（图1）、身份证反面照（图2）和体检证明（图3）均已上传且内容清晰可见。\n"
                            f"2. 身份证信息需完整无遮挡。\n"
                            f"3. 体检合格证结论必须为“合格”或“符合报考条件”，且不能有不符合该车型的身体限制（如无红绿色盲）。\n"
                            f"请仔细分析图片，给出是否通过的结论。并且必须以指定的 JSON 格式返回审核结论，不要返回任何其他无关的纯文本或用 ```json 代码块包裹。格式如下：\n"
                            f'{{"is_passed": true 或 false, "reason": "审核理由（直接指出不合规的具体问题，字数严格控制在 40 字以内）"}}'
                        )
                    }
                ]
            }
        ]
        
        if b64_front: messages[0]["content"].append({"type": "image_url", "image_url": {"url": b64_front}})
        if b64_back: messages[0]["content"].append({"type": "image_url", "image_url": {"url": b64_back}})
        if b64_health: messages[0]["content"].append({"type": "image_url", "image_url": {"url": b64_health}})
        # 3. 初始化 LLM
        print(f"[AI] 初始化模型: {settings.AI_MODEL_NAME}，接口地址: {settings.AI_BASE_URL}")
        llm = ChatOpenAI(
            model=settings.AI_MODEL_NAME,
            openai_api_key=settings.AI_API_KEY,
            openai_api_base=settings.AI_BASE_URL,
            temperature=1.0,
            max_tokens=1000
        )
        
        # 4. 绑定结构化 Pydantic 输出 (指定 json_mode 契约)
        structured_llm = llm.with_structured_output(AuditResult, method="json_mode")
        
        try:
            print("[AI] 发起多模态审核请求...")
            result = structured_llm.invoke(messages)
            
            # 5. 组装返回结果
            if result.is_passed:
                return 1, f"AI建议通过: {result.reason}"
            else:
                return 2, f"AI建议驳回: {result.reason}"
        except Exception as parse_error:
            print(f"[AI Warning] 结构化解析失败: {parse_error}，尝试容错解析...")
            try:
                # 重新用普通大模型调用，直接提取纯文本进行二次匹配
                raw_response = llm.invoke(messages).content
                print(f"[AI Fallback] 原始输出: {raw_response}")
                
                is_passed = True
                for fail_word in ["不通过", "驳回", "拒绝", "未通过", "不合格", "异常", "缺失"]:
                    if fail_word in raw_response:
                        is_passed = False
                        break
                
                import re
                reason_match = re.search(r'"reason"\s*:\s*"([^"]+)"', raw_response)
                if reason_match:
                    reason = reason_match.group(1)
                else:
                    reason = raw_response.strip().replace("\n", " ")
                    if len(reason) > 40:
                        reason = reason[:40] + "..."
                
                return (1 if is_passed else 2), f"AI建议(容错): {reason}"
            except Exception as inner_error:
                print(f"[AI Error] 容错解析也失败了: {inner_error}")
                degrade_remark = "AI多模态审核异常，建议转入人工初审。原因: 无法智能识别证件图片内容"
                return 2, degrade_remark
    except Exception as e:
        error_msg = str(e)
        print(f"[AI Error] 发生外部异常: {error_msg}")
        return 2, f"AI多模态审核异常，建议转入人工初审。原因: {error_msg}"
