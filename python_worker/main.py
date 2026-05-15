import os
import json
import time
import redis
import pymysql
import requests
from dotenv import load_dotenv

# 加载上级目录(backend)的 .env 文件
env_path = os.path.join(os.path.dirname(__file__), '..', 'backend', '.env')
load_dotenv(dotenv_path=env_path)

# 获取环境变量
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = int(os.getenv('DB_PORT', 3306))
DB_NAME = os.getenv('DB_NAME', 'dms_demo')
DB_USER = os.getenv('DB_USERNAME', 'root')
DB_PASS = os.getenv('DB_PASSWORD', '')

REDIS_HOST = os.getenv('REDIS_HOST', 'localhost')
REDIS_PORT = int(os.getenv('REDIS_PORT', 6379))
REDIS_PASSWORD = os.getenv('REDIS_PASSWORD', None)

DIFY_API_KEY = os.getenv('DIFY_API_KEY')
DIFY_API_URL = os.getenv('DIFY_API_URL', 'http://localhost/v1/workflows/run')

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

def update_audit_status(enrollment_id, status, remark):
    """ 更新数据库中的审核状态 """
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            sql = "UPDATE biz_enrollments SET audit_status = %s, audit_remark = %s WHERE id = %s"
            cursor.execute(sql, (status, remark, enrollment_id))
        conn.commit()
        print(f"[MySQL] 更新成功: 报名ID={enrollment_id}, 状态={status}, 备注={remark}")
    except Exception as e:
        print(f"[MySQL Error] 更新失败: {e}")
    finally:
        conn.close()

def call_dify_workflow(task_data):
    """ 调用 Dify Workflow 进行审核 """
    headers = {
        'Authorization': f'Bearer {DIFY_API_KEY}',
        'Content-Type': 'application/json'
    }
    
    # 收集所有的图片 URL
    image_urls = []
    if task_data.get('idCardFront'): image_urls.append(task_data.get('idCardFront'))
    if task_data.get('idCardBack'): image_urls.append(task_data.get('idCardBack'))
    if task_data.get('healthCert'): image_urls.append(task_data.get('healthCert'))

    # 构建 Dify 规定的文件对象数组
    dify_files = [
        {
            "type": "image",
            "transfer_method": "remote_url",
            "url": url
        } for url in image_urls
    ]

    # 构建 Dify 工作流的输入参数
    payload = {
        "inputs": {
            "target_license_type": task_data.get('licenseType', 'C1'),
            # 根据截图，材料变量名叫 materials，这里传入文件数组
            "materials": dify_files 
        },
        "response_mode": "blocking",
        "user": f"student_{task_data.get('studentId')}",
        # 为了兼容 Dify 的 legacy 文件上传方式，也在最外层放一份 files
        "files": dify_files
    }

    try:
        print(f"[Dify] 正在请求 Dify API: {DIFY_API_URL} ...")
        response = requests.post(DIFY_API_URL, headers=headers, json=payload, timeout=60)
        response.raise_for_status()
        result_json = response.json()
        print(f"[Dify] 收到 API 响应内容: {json.dumps(result_json, ensure_ascii=False)}")
        
        # 解析 Dify 的返回结果
        outputs = result_json.get('data', {}).get('outputs', {})
        
        # 默认值
        audit_result = 'fail'
        reason = 'AI未给出具体原因'

        # 核心修复：处理 Dify 常见的 result 嵌套字符串情况
        if 'result' in outputs:
            raw_result = outputs['result']
            if isinstance(raw_result, str) and raw_result.strip().startswith('{'):
                try:
                    # 尝试二次解析 JSON 字符串
                    nested_data = json.loads(raw_result)
                    # 兼容不同的字段名：isPassed, status, passed 等
                    is_passed = nested_data.get('isPassed') or nested_data.get('status')
                    if is_passed in [True, 'true', 'pass', '通过']:
                        audit_result = 'pass'
                    
                    reason = nested_data.get('reason') or nested_data.get('message') or raw_result
                except Exception as e:
                    print(f"[Worker] 二次解析 result 失败: {e}")
                    reason = raw_result
            else:
                # 如果不是 JSON 字符串，直接当作理由
                reason = str(raw_result)
        else:
            # 兜底逻辑：尝试直接从 outputs 获取
            audit_result = outputs.get('status', 'fail')
            reason = outputs.get('reason', reason)
        
        if audit_result == 'pass' or audit_result == '通过':
            return 1, f"AI建议通过: {reason}" # 1-AI初审通过
        else:
            return 2, f"AI建议驳回: {reason}" # 2-AI初审驳回

    except requests.exceptions.HTTPError as e:
        error_msg = e.response.text if e.response else str(e)
        print(f"[Dify Error] HTTP 状态码异常: {e.response.status_code if e.response else ''}")
        print(f"[Dify Error] 详细错误信息: {error_msg}")
        return 2, f"AI接口请求失败(视为初审驳回): {error_msg}"
    except Exception as e:
        print(f"[Dify Error] 调用 Dify API 失败: {e}")
        return 2, f"AI初审异常(视为初审驳回): {str(e)}"

def main():
    print(f"[*] 启动 Python Worker 异步队列监听进程...")
    print(f"[*] 监听 Redis 队列: {QUEUE_NAME}")
    
    # 连接 Redis
    r = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, password=REDIS_PASSWORD, decode_responses=True)
    
    while True:
        try:
            # 阻塞式监听队列，没有数据时会一直卡在这里等待
            item = r.blpop(QUEUE_NAME, timeout=0)
            if item:
                queue_name, data_str = item
                print(f"\n[Queue] 收到新任务: {data_str}")
                
                # 解析 JSON 任务
                task_data = json.loads(data_str)
                enrollment_id = task_data.get('enrollmentId')
                
                if not enrollment_id:
                    print("[Error] 任务缺少 enrollmentId")
                    continue
                
                # 调用 Dify
                status, remark = call_dify_workflow(task_data)
                
                # 回写数据库
                update_audit_status(enrollment_id, status, remark)
                
        except Exception as e:
            print(f"[Worker Error] 发生异常: {e}")
            time.sleep(3) # 出错后停顿3秒再试，防止死循环刷屏

if __name__ == '__main__':
    main()
