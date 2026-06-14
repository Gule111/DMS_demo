import json
import time
import redis
from app.core.config import settings
from app.core.db import update_audit_status
from app.services.audit import call_langchain_audit

QUEUE_NAME = 'dms:enrollment:audit:queue'

def main():
    print(f"[*] 启动 Python Worker 异步队列监听进程 (基于 LangChain, DDD 解耦架构)...")
    print(f"[*] 监听 Redis 队列: {QUEUE_NAME}")
    
    # 连接 Redis
    r = redis.Redis(
        host=settings.REDIS_HOST, 
        port=settings.REDIS_PORT, 
        password=settings.REDIS_PASSWORD, 
        decode_responses=True,
        protocol=2
    )
    
    while True:
        try:
            # 阻塞式监听队列
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
                
                # 调用业务逻辑层：LangChain 审核
                status, remark = call_langchain_audit(task_data)
                
                # 调用数据层：回写数据库
                update_audit_status(enrollment_id, status, remark)
                
        except Exception as e:
            print(f"[Worker Error] 循环发生异常: {e}")
            time.sleep(3)

if __name__ == '__main__':
    main()
