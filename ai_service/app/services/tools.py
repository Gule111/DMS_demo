from langchain_core.tools import tool
from app.core.backend_client import BackendClient
import json

@tool
def get_my_progress(token: str) -> str:
    """
    获取当前登录学员的各个科目学习进度和总览。
    需要传入前端的 jwt token 作为认证。
    返回的 JSON 字符串包含各科目的已学时和状态。
    """
    client = BackendClient(token)
    res = client.get("/progress/my")
    if res.get("code") == 200:
        return json.dumps(res.get("data", []), ensure_ascii=False)
    else:
        return f"查询进度失败: {res.get('msg', res.get('message', '未知错误'))}"

@tool
def get_my_instructor(token: str) -> str:
    """
    获取系统为当前学员分配的专属教练信息（包括教练姓名、电话、评分、教龄等）。
    需要传入前端的 jwt token 作为认证。
    """
    client = BackendClient(token)
    res = client.get("/instructor/my")
    if res.get("code") == 200:
        data = res.get("data")
        if data:
            return json.dumps(data, ensure_ascii=False)
        else:
            return "您暂未被分配专属教练，后台正在为您匹配中。"
    else:
        return f"查询专属教练失败: {res.get('msg', res.get('message', '未知错误'))}"

@tool
def get_my_appointments(token: str) -> str:
    """
    获取当前登录学员的所有约课日程（预约记录）。
    包含预约日期、时间段（timeSlot）、练习科目（subject）以及状态（status: 1-待审核/已预约, 2-已完成, 3-已取消）。
    需要传入前端的 jwt token 作为认证。
    """
    client = BackendClient(token)
    res = client.get("/appointment/my")
    if res.get("code") == 200:
        return json.dumps(res.get("data", []), ensure_ascii=False)
    else:
        return f"查询预约记录失败: {res.get('msg', res.get('message', '未知错误'))}"

@tool
def book_training_session(token: str, appointment_date: str, time_slot: str, subject: int) -> str:
    """
    为当前学员预约教练练车。
    参数:
    - token: 前端的 jwt token。
    - appointment_date: 预约日期，格式必须为 YYYY-MM-DD，只能预约明天及以后的日期。
    - time_slot: 预约时段，必须是以下四个之一："08:00-10:00", "10:00-12:00", "14:00-16:00", "16:00-18:00"。
    - subject: 练习科目，只能为 2 (代表科目二场内技训) 或 3 (代表科目三道路驾驶)。
    """
    client = BackendClient(token)
    data = {
        "appointmentDate": appointment_date,
        "timeSlot": time_slot,
        "subject": subject
    }
    res = client.post("/appointment/book", data=data)
    if res.get("code") == 200:
        return "预约成功！请按时前往驾校练车。"
    else:
        return f"预约失败: {res.get('msg', res.get('message', '未知错误'))}"

@tool
def cancel_appointment(token: str, appointment_id: int) -> str:
    """
    取消指定的预约记录。
    参数:
    - token: 前端的 jwt token。
    - appointment_id: 预约记录的唯一 ID。
    """
    client = BackendClient(token)
    res = client.post(f"/appointment/cancel/{appointment_id}")
    if res.get("code") == 200:
        return f"预约 ID {appointment_id} 已成功取消。"
    else:
        return f"取消失败: {res.get('msg', res.get('message', '未知错误'))}"

# ==========================================
# 教练员专属工具 (Role = 2)
# ==========================================

@tool
def get_instructor_students(token: str) -> str:
    """
    教练专用：获取当前教练名下分配的学员名单。
    """
    client = BackendClient(token)
    res1 = client.get("/instructor/current")
    if res1.get("code") != 200 or not res1.get("data"):
        return "获取教练信息失败，请确保当前登录账号为教练员。"
    instructor_id = res1["data"]["id"]
    res2 = client.get(f"/instructor/students/{instructor_id}")
    if res2.get("code") == 200:
        return json.dumps(res2.get("data", []), ensure_ascii=False)
    else:
        return f"获取学员名单失败: {res2.get('msg', '未知错误')}"

@tool
def record_training_hours(token: str, student_id: int, subject: int, hours: float, content: str) -> str:
    """
    教练专用：给指定学员录入练车学时。
    参数:
    - token: 前端的 jwt token。
    - student_id: 学员的用户ID。
    - subject: 科目(2或3)。
    - hours: 本次练车学时（如2.0）。
    - content: 训练内容说明。
    """
    client = BackendClient(token)
    url = f"/progress/record?studentId={student_id}&subject={subject}&hours={hours}&content={content}"
    res = client.post(url)
    if res.get("code") == 200:
        return "学时录入成功。"
    else:
        return f"学时录入失败: {res.get('msg', '未知错误')}"

@tool
def get_instructor_appointments(token: str) -> str:
    """
    教练专用：查询学员向该教练发起的预约列表。
    """
    client = BackendClient(token)
    res = client.get("/appointment/instructor/list")
    if res.get("code") == 200:
        return json.dumps(res.get("data", []), ensure_ascii=False)
    else:
        return f"查询预约列表失败: {res.get('msg', '未知错误')}"

@tool
def handle_student_appointment(token: str, appointment_id: int, status: int) -> str:
    """
    教练专用：审批学员的约课申请。
    参数:
    - token: 前端的 jwt token。
    - appointment_id: 预约记录ID。
    - status: 2代表接受预约，3代表拒绝预约。
    """
    client = BackendClient(token)
    res = client.post(f"/appointment/handle?id={appointment_id}&status={status}")
    if res.get("code") == 200:
        return f"预约审批操作成功，状态已更新为 {status}。"
    else:
        return f"审批失败: {res.get('msg', '未知错误')}"

@tool
def record_exam_result(token: str, student_id: int, subject: int, score: int, remark: str) -> str:
    """
    教练专用：录入学员的考试成绩和评语。
    参数:
    - token: 前端的 jwt token。
    - student_id: 学员的用户ID。
    - subject: 科目(2或3)。
    - score: 考试分数。
    - remark: 评语或备注。
    """
    client = BackendClient(token)
    url = f"/progress/exam-result?studentId={student_id}&subject={subject}&score={score}&remark={remark}"
    res = client.post(url)
    if res.get("code") == 200:
        return "成绩录入成功。"
    else:
        return f"成绩录入失败: {res.get('msg', '未知错误')}"

# ==========================================
# 管理员专属工具 (Role = 1)
# ==========================================

@tool
def get_pending_registrations(token: str) -> str:
    """
    管理员专用：查询待审核的在线报名记录。
    """
    client = BackendClient(token)
    res = client.get("/enrollment/admin/list?status=1")
    if res.get("code") == 200:
        return json.dumps(res.get("data", []), ensure_ascii=False)
    else:
        return f"查询待审核报名失败: {res.get('msg', '未知错误')}"

@tool
def audit_registration(token: str, enrollment_id: int, status: int, remark: str) -> str:
    """
    管理员专用：审核在线报名申请。
    参数:
    - token: 前端的 jwt token。
    - enrollment_id: 报名记录ID。
    - status: 2代表通过，3代表驳回。
    - remark: 审核备注。
    """
    client = BackendClient(token)
    data = {
        "enrollmentId": enrollment_id,
        "status": status,
        "remark": remark
    }
    res = client.post("/enrollment/admin/audit", data=data)
    if res.get("code") == 200:
        return f"报名审核成功，已将状态修改为 {status}。"
    else:
        return f"审核失败: {res.get('msg', '未知错误')}"

@tool
def get_all_coaches(token: str) -> str:
    """
    管理员专用：获取全校所有教练的列表及详情。
    """
    client = BackendClient(token)
    res = client.get("/instructor/list")
    if res.get("code") == 200:
        return json.dumps(res.get("data", []), ensure_ascii=False)
    else:
        return f"获取教练列表失败: {res.get('msg', '未知错误')}"

@tool
def assign_coach_to_student(token: str, student_id: int, instructor_id: int) -> str:
    """
    管理员专用：为学员手动分配或更换教练。
    参数:
    - token: 前端的 jwt token。
    - student_id: 学员的用户ID。
    - instructor_id: 教练的ID。
    """
    client = BackendClient(token)
    res = client.post(f"/instructor/assign/manual?studentId={student_id}&instructorId={instructor_id}")
    if res.get("code") == 200:
        return "分配教练成功！"
    else:
        return f"分配教练失败: {res.get('msg', '未知错误')}"
