import os
import json
import time
import redis
import pymysql
from dotenv import load_dotenv

# 加载环境变量
env_path = os.path.join(os.path.dirname(__file__), '..', '..', 'backend', '.env')
load_dotenv(dotenv_path=env_path)

DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = int(os.getenv('DB_PORT', 3306))
DB_NAME = os.getenv('DB_NAME', 'dms_demo')
DB_USER = os.getenv('DB_USERNAME', 'root')
DB_PASS = os.getenv('DB_PASSWORD', '')

REDIS_HOST = os.getenv('REDIS_HOST', 'localhost')
REDIS_PORT = int(os.getenv('REDIS_PORT', 6379))
REDIS_PASSWORD = os.getenv('REDIS_PASSWORD', None)

QUEUE_NAME = 'dms:enrollment:audit:queue'

def get_db_connection():
    return pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASS,
        database=DB_NAME,
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor
    )

def setup_test_enrollment():
    """ 在数据库中创建一条临时的待审核报名数据，以便进行端到端测试 """
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            # 1. 查找或插入一个测试学员 (如果不存在，可以用 student_id=1)
            # 在这里，为了测试安全，我们查找 biz_enrollments 里最大的 id 或者直接建一个空的/假的数据
            # 即使 student_id 不存在，我们只管 biz_enrollments 表的更新
            sql_insert = """
            INSERT INTO biz_enrollments (student_id, id_card_front, id_card_back, health_cert, audit_status, audit_remark)
            VALUES (%s, %s, %s, %s, %s, %s)
            """
            # 使用临时虚拟图片 URL
            cursor.execute(sql_insert, (
                9999,  # 虚拟学员 ID
                "http://tf15hvn1e.hn-bkt.clouddn.com/uploads/id_cards/front/test_front.jpg",
                "http://tf15hvn1e.hn-bkt.clouddn.com/uploads/id_cards/back/test_back.jpg",
                "http://tf15hvn1e.hn-bkt.clouddn.com/uploads/health_certs/test_health.jpg",
                0,     # 0-待审核
                "等待测试"
            ))
            enrollment_id = conn.insert_id()
        conn.commit()
        print(f"[Test] 数据库插入成功，生成临时报名记录 ID: {enrollment_id}")
        return enrollment_id
    except Exception as e:
        print(f"[Test Error] 插入数据库记录失败: {e}")
        return None
    finally:
        conn.close()

def check_enrollment_status(enrollment_id):
    """ 查询数据库中的最新审核状态 """
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            sql = "SELECT audit_status, audit_remark FROM biz_enrollments WHERE id = %s"
            cursor.execute(sql, (enrollment_id,))
            res = cursor.fetchone()
            return res
    except Exception as e:
        print(f"[Test Error] 查询数据库记录失败: {e}")
        return None
    finally:
        conn.close()

def cleanup_test_enrollment(enrollment_id):
    """ 清理测试记录 """
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            sql = "DELETE FROM biz_enrollments WHERE id = %s"
            cursor.execute(sql, (enrollment_id,))
        conn.commit()
        print(f"[Test] 成功删除临时测试报名记录 ID: {enrollment_id}")
    except Exception as e:
        print(f"[Test Error] 清除数据库记录失败: {e}")
    finally:
        conn.close()

def main():
    print("[*] 开始进行 AI 报名审核端到端测试准备...")
    
    # 1. 建立测试记录
    enrollment_id = setup_test_enrollment()
    if not enrollment_id:
        print("[Error] 无法创建测试数据，退出。")
        return
        
    # 2. 组装 Redis 任务
    task = {
        "enrollmentId": enrollment_id,
        "studentId": 9999,
        "idCardFront": "http://tf15hvn1e.hn-bkt.clouddn.com/uploads/id_cards/front/test_front.jpg",
        "idCardBack": "http://tf15hvn1e.hn-bkt.clouddn.com/uploads/id_cards/back/test_back.jpg",
        "healthCert": "http://tf15hvn1e.hn-bkt.clouddn.com/uploads/health_certs/test_health.jpg",
        "licenseType": "C2" # 测试报考 C2 车型
    }
    
    # 3. 推送至 Redis
    r = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, password=REDIS_PASSWORD, decode_responses=True, protocol=2)
    r.lpush(QUEUE_NAME, json.dumps(task))
    print(f"[Test] 任务成功推送到 Redis 队列 '{QUEUE_NAME}'")
    print(f"[Test] 待消费的数据: {json.dumps(task, ensure_ascii=False)}")
    
    print("\n-----------------------------------------------------")
    print("【操作指引】请现在另外开启一个终端，运行 worker 服务以进行消费:")
    print("命令: .venv\\Scripts\\python -m app.worker.main")
    print("-----------------------------------------------------\n")
    
    print("[*] 等待 15 秒以检测数据库是否被 worker 成功更新 (请确保已运行 worker.py)...")
    for i in range(15):
        time.sleep(1)
        res = check_enrollment_status(enrollment_id)
        if res and res['audit_status'] != 0:
            print(f"\n[Success] 检测到数据库已更新！")
            print(f"-> 审核状态(audit_status): {res['audit_status']} (1-通过, 2-驳回)")
            print(f"-> 审核备注(audit_remark): {res['audit_remark']}")
            break
        print(".", end="", flush=True)
    else:
        print("\n[Timeout] 15秒内数据库未被更新，可能是 worker 未运行，或者 API 交互超时。")

    # 4. 清理数据
    cleanup_test_enrollment(enrollment_id)
    print("[*] 端到端测试完毕。")

if __name__ == '__main__':
    main()
