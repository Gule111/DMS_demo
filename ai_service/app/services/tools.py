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
    - subject: 科目(1、2或3)。
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
def get_pending_registrations(token: str, status: int = 1) -> str:
    """
    管理员专用：查询待审核的在线报名记录。
    参数:
    - token: 前端的 jwt token。
    - status: 过滤状态。1 代表“AI初审通过”的数据（默认，待管理员终审）；2 代表“AI初审驳回/未通过”的数据。
    """
    client = BackendClient(token)
    res = client.get(f"/enrollment/admin/list?status={status}")
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

# ==========================================
# 考场预约与成绩录入工具 (学员/管理员)
# ==========================================

@tool
def get_exam_sites(token: str) -> str:
    """
    获取系统配置的可用考试场地列表（考场列表）。
    需要传入前端的 jwt token 作为认证。
    返回的 JSON 字符串包含每个考场的唯一ID、键值等信息。
    """
    client = BackendClient(token)
    res = client.get("/dict/type/EXAM_SITE")
    if res.get("code") == 200:
        return json.dumps(res.get("data", []), ensure_ascii=False)
    else:
        return f"查询考场列表失败: {res.get('msg', res.get('message', '未知错误'))}"

@tool
def get_my_exams(token: str) -> str:
    """
    学员专用：获取当前学员的考试预约记录和历史考试记录（包含模拟和正式考试）。
    需要传入前端的 jwt token 作为认证。
    返回的 JSON 包含预约科目、考试类型（examType: 1-正式考试, 2-模拟考试）、考试日期、考试地点、状态（0-待审核, 1-预约成功, 2-考试完成, 3-已拒绝）以及分数。
    """
    client = BackendClient(token)
    res = client.get("/exam/my")
    if res.get("code") == 200:
        return json.dumps(res.get("data", []), ensure_ascii=False)
    else:
        return f"查询考试记录失败: {res.get('msg', res.get('message', '未知错误'))}"

@tool
def book_exam_session(token: str, subject: int, exam_date: str, exam_site: str, exam_type: int = 1) -> str:
    """
    学员专用：发起一个新的考场预约申请（支持正式考试或模拟考试）。
    参数:
    - token: 前端的 jwt token。
    - subject: 预约科目。1 (代表科目一), 2 (代表科目二), 3 (代表科目三), 4 (代表科目四)。
    - exam_date: 期望考试日期，格式必须为 YYYY-MM-DD。
    - exam_site: 期望考场地点名称（例如“城东第一考场”、“北郊考场”）。
    - exam_type: 考试类型。1 代表正式考试（直接成功，无需审核），2 代表模拟考试（需要管理员审核）。默认为 1。
    """
    import datetime
    try:
        dt = datetime.datetime.strptime(exam_date, "%Y-%m-%d")
        timestamp = int(dt.timestamp() * 1000)
    except Exception:
        return "日期格式错误，必须为 YYYY-MM-DD"
        
    client = BackendClient(token)
    data = {
        "subject": subject,
        "examSite": exam_site,
        "examDate": timestamp,
        "examType": exam_type
    }
    res = client.post("/exam/book", data=data)
    if res.get("code") == 200:
        return res.get("data", "预约申请处理成功。")
    else:
        return f"预约失败: {res.get('msg', res.get('message', '未知错误'))}"

@tool
def cancel_exam_booking(token: str, exam_id: int) -> str:
    """
    学员专用：取消待审核状态的考试预约申请。
    参数:
    - token: 前端的 jwt token。
    - exam_id: 考试预约记录唯一 ID。
    """
    client = BackendClient(token)
    res = client.post(f"/exam/cancel/{exam_id}")
    if res.get("code") == 200:
        return f"考试预约记录 ID {exam_id} 已成功取消。"
    else:
        return f"取消考试预约失败: {res.get('msg', '未知错误')}"

@tool
def get_admin_exam_list(token: str, status: int = None) -> str:
    """
    管理员专用：获取全校所有学员的考试预约和审核记录。
    参数:
    - token: 前端的 jwt token。
    - status: 过滤状态。0 (待审核), 1 (预约成功), 2 (考试完成), 3 (已拒绝)。不传则获取全部。
    """
    client = BackendClient(token)
    url = "/exam/admin/list"
    if status is not None:
        url += f"?status={status}"
    res = client.get(url)
    if res.get("code") == 200:
        return json.dumps(res.get("data", []), ensure_ascii=False)
    else:
        return f"查询考试预约列表失败: {res.get('msg', '未知错误')}"

@tool
def audit_exam_booking(token: str, exam_id: int, status: int, exam_site: str = None, exam_date: str = None) -> str:
    """
    管理员专用：审核考试预约申请并进行考场正式分配与排位（主要适用于模拟考试，正式考试无需审批）。
    参数:
    - token: 前端的 jwt token。
    - exam_id: 考试预约记录ID。
    - status: 审核结果。1 代表同意并批准，3 代表拒绝。
    - exam_site: 分配的正式考试考场名称，若批准则必填。
    - exam_date: 确认的考试日期，格式为 YYYY-MM-DD，若批准则必填。
    """
    data = {
        "id": exam_id,
        "status": status
    }
    
    if exam_site:
        data["examSite"] = exam_site
        
    if exam_date:
        import datetime
        try:
            dt = datetime.datetime.strptime(exam_date, "%Y-%m-%d")
            data["examDate"] = int(dt.timestamp() * 1000)
        except Exception:
            return "日期格式错误，必须为 YYYY-MM-DD"
            
    client = BackendClient(token)
    res = client.post("/exam/admin/audit", data=data)
    if res.get("code") == 200:
        return f"考场分配与预约审核操作成功，审核结果更新为 {status}。"
    else:
        return f"预约审核操作失败: {res.get('msg', res.get('message', '未知错误'))}"

@tool
def record_exam_score(token: str, exam_id: int, score: int) -> str:
    """
    管理员专用：录入学员考试的正式成绩或模拟考试成绩（0-100分）。
    参数:
    - token: 前端的 jwt token。
    - exam_id: 考试预约记录ID。
    - score: 考试分数。
    """
    client = BackendClient(token)
    data = {
        "id": exam_id,
        "score": score
    }
    res = client.post("/exam/admin/score", data=data)
    if res.get("code") == 200:
        return f"学员考试成绩 {score} 录入完毕，已同步至进度管理中。"
    else:
        return f"成绩录入失败: {res.get('msg', res.get('message', '未知错误'))}"
