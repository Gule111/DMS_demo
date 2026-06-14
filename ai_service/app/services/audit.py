from langchain_openai import ChatOpenAI
from app.core.config import settings
from app.models.schemas import AuditResult

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
                        f"请仔细分析图片，给出是否通过的结论及理由。"
                    )
                },
                {"type": "image_url", "image_url": {"url": id_card_front}},
                {"type": "image_url", "image_url": {"url": id_card_back}},
                {"type": "image_url", "image_url": {"url": health_cert}}
            ]
        }
    ]

    try:
        # 3. 初始化 LLM
        print(f"[AI] 初始化模型: {settings.AI_MODEL_NAME}，接口地址: {settings.AI_BASE_URL}")
        llm = ChatOpenAI(
            model=settings.AI_MODEL_NAME,
            openai_api_key=settings.AI_API_KEY,
            openai_api_base=settings.AI_BASE_URL,
            temperature=0.0,
            max_tokens=1000
        )
        
        # 4. 绑定结构化 Pydantic 输出
        structured_llm = llm.with_structured_output(AuditResult)
        
        print("[AI] 发起多模态审核请求...")
        result = structured_llm.invoke(messages)
        
        # 5. 组装返回结果
        if result.is_passed:
            return 1, f"AI建议通过: {result.reason}"
        else:
            return 2, f"AI建议驳回: {result.reason}"

    except Exception as e:
        error_msg = str(e)
        print(f"[AI Error] 多模态审核发生异常: {error_msg}")
        degrade_remark = f"AI多模态审核异常，建议转入人工初审。原因: 无法智能识别证件图片内容（{error_msg}）"
        print(f"[AI Degrading] 采用降级策略: {degrade_remark}")
        return 2, degrade_remark
