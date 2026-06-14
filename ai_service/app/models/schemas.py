from pydantic import BaseModel, Field

class ChatMessage(BaseModel):
    role: str  # 'user' 或 'assistant' 或 'system'
    content: str

class ChatRequest(BaseModel):
    message: str
    history: list[ChatMessage] = Field(default_factory=list)
    token: str = Field(default="", description="JWT Token from frontend")
    role: int = Field(default=3, description="User role: 1-Admin, 2-Instructor, 3-Student")

class AuditResult(BaseModel):
    is_passed: bool = Field(description="是否通过初审。如果所有证件齐全、报考车型无误且体检合格，则为 True；若有缺失、结论不合格或发现明显问题则为 False")
    reason: str = Field(description="给出判定通过或驳回的详细审查原因。")
