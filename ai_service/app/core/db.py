import pymysql
from app.core.config import settings

def get_db_connection():
    return pymysql.connect(
        host=settings.DB_HOST,
        port=settings.DB_PORT,
        user=settings.DB_USER,
        password=settings.DB_PASS,
        database=settings.DB_NAME,
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor
    )

def update_audit_status(enrollment_id: int, status: int, remark: str):
    """ 更新数据库中的审核状态 (1-AI初审通过, 2-AI初审驳回/转人工) """
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            sql = "UPDATE biz_enrollments SET audit_status = %s, audit_remark = %s WHERE id = %s"
            cursor.execute(sql, (status, remark, enrollment_id))
        conn.commit()
        print(f"[MySQL] 更新成功: 报名ID={enrollment_id}, 状态={status}, 备注={remark}")
    except Exception as e:
        print(f"[MySQL Error] 更新数据失败: {e}")
    finally:
        conn.close()
