<template>
  <div class="auth-container">
    <!-- 注册卡片，带动态阻尼倾斜效果 -->
    <div 
      class="auth-card" 
      ref="cardRef"
      @mousemove="handleMouseMove"
      @mouseleave="handleMouseLeave"
      :style="cardStyle"
    >
      <h2>注册新账号</h2>

      <a-form :model="form" @finish="handleRegister" layout="vertical">
        <a-form-item label="用户名" name="username"
          :rules="[{ required: true, message: '请输入用户名' }]">
          <a-input v-model:value="form.username" placeholder="请输入用户名" size="large" />
        </a-form-item>

        <a-form-item label="手机号" name="phone"
          :rules="[{ required: true, message: '请输入手机号' }]">
          <a-input v-model:value="form.phone" placeholder="请输入手机号" size="large" />
        </a-form-item>

        <a-form-item label="密码" name="password"
          :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password v-model:value="form.password" placeholder="6-20位，须包含字母和数字" size="large" />
        </a-form-item>

        <a-form-item label="确认密码" name="confirmPassword"
          :rules="[{ required: true, message: '请确认密码' }]">
          <a-input-password v-model:value="form.confirmPassword" placeholder="请再次输入密码" size="large" />
        </a-form-item>

        <a-form-item label="验证码" name="code"
          :rules="[{ required: true, message: '请输入验证码' }]">
          <div style="display: flex; gap: 8px;">
            <a-input v-model:value="form.code" placeholder="请输入验证码" size="large" />
            <a-button size="large" :disabled="countdown > 0" @click="handleSendCode"
              :loading="sendingCode">
              {{ countdown > 0 ? `${countdown}秒后重发` : '获取验证码' }}
            </a-button>
          </div>
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" block :loading="loading">
            注 册
          </a-button>
        </a-form-item>
      </a-form>

      <div class="switch-link">
        已有账号？<a @click="$router.push('/login')">返回登录</a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { sendCode, register } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const form = ref({
  username: '',
  phone: '',
  password: '',
  confirmPassword: '',
  code: '',
})

const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const phoneRegex = /^1[3-9]\d{9}$/
const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{6,20}$/

// --- 动态阻尼倾斜效果逻辑 ---
const cardRef = ref<HTMLElement | null>(null)
const mouseX = ref(0)
const mouseY = ref(0)
const isHovering = ref(false)

const handleMouseMove = (e: MouseEvent) => {
  if (!cardRef.value) return
  isHovering.value = true
  const rect = cardRef.value.getBoundingClientRect()
  const x = (e.clientX - rect.left) / rect.width - 0.5
  const y = (e.clientY - rect.top) / rect.height - 0.5
  mouseX.value = x
  mouseY.value = y
}

const handleMouseLeave = () => {
  isHovering.value = false
  mouseX.value = 0
  mouseY.value = 0
}

const cardStyle = computed(() => {
  const rotateX = isHovering.value ? mouseY.value * -15 : 0
  const rotateY = isHovering.value ? mouseX.value * 15 : 0
  return {
    transform: `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg)`,
    transition: isHovering.value ? 'transform 0.1s cubic-bezier(0.25, 0.46, 0.45, 0.94)' : 'transform 0.5s ease-out'
  }
})
// ----------------------------

async function handleSendCode() {
  if (!form.value.phone) {
    message.warning('请先输入手机号')
    return
  }
  if (!phoneRegex.test(form.value.phone)) {
    message.warning('手机号格式不正确')
    return
  }
  if (!form.value.password) {
    message.warning('请先输入密码')
    return
  }
  if (!passwordRegex.test(form.value.password)) {
    message.warning('密码须为6-20位，且包含字母和数字')
    return
  }
  if (form.value.password !== form.value.confirmPassword) {
    message.warning('两次输入的密码不一致')
    return
  }

  sendingCode.value = true
  try {
    const res: any = await sendCode(form.value.phone)
    message.success(res.data || '验证码已发送')

    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0 && timer) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch {
    // 错误已在拦截器中处理
  } finally {
    sendingCode.value = false
  }
}

async function handleRegister() {
  if (!phoneRegex.test(form.value.phone)) {
    message.warning('手机号格式不正确')
    return
  }
  if (!passwordRegex.test(form.value.password)) {
    message.warning('密码须为6-20位，且包含字母和数字')
    return
  }
  if (form.value.password !== form.value.confirmPassword) {
    message.warning('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    const res: any = await register(form.value.username, form.value.phone, form.value.password, form.value.code)
    userStore.setUser(res.data)
    message.success('注册成功，已自动登录')
    router.push('/app/dashboard')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 灰白色高级质感背景 */
  background: radial-gradient(circle at center, #fdfdfd 0%, #e6e8eb 100%);
  position: relative;
  overflow: hidden;
}

.auth-container::before {
  content: '';
  position: absolute;
  top: -20%;
  left: -10%;
  width: 60%;
  height: 60%;
  background: radial-gradient(circle, rgba(255,255,255,0.8) 0%, transparent 70%);
  filter: blur(40px);
  z-index: 0;
}

.auth-container::after {
  content: '';
  position: absolute;
  bottom: -20%;
  right: -10%;
  width: 50%;
  height: 50%;
  background: radial-gradient(circle, rgba(220,225,235,0.6) 0%, transparent 70%);
  filter: blur(50px);
  z-index: 0;
}

.auth-card {
  position: relative;
  z-index: 1;
  width: 420px;
  padding: 40px;
  /* 毛玻璃质感 */
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: 
    0 8px 32px 0 rgba(31, 38, 135, 0.07),
    inset 0 0 0 1px rgba(255, 255, 255, 0.5);
  will-change: transform;
  transform-style: preserve-3d;
}

.auth-card h2 {
  text-align: center;
  margin-bottom: 30px;
  font-size: 24px;
  color: #333;
  font-weight: 600;
  letter-spacing: 1px;
  text-shadow: 0 1px 2px rgba(255,255,255,0.8);
}

:deep(.ant-input), :deep(.ant-input-password) {
  background: rgba(255, 255, 255, 0.6) !important;
  border-color: rgba(200, 200, 200, 0.4) !important;
  backdrop-filter: blur(4px);
}
:deep(.ant-input:focus), :deep(.ant-input-password:focus) {
  background: rgba(255, 255, 255, 0.9) !important;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2) !important;
}

:deep(.ant-btn) {
  border-radius: 8px;
  backdrop-filter: blur(4px);
}

.switch-link {
  text-align: center;
  margin-top: 20px;
  color: #666;
  font-size: 14px;
}

.switch-link a {
  color: #1890ff;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s;
}

.switch-link a:hover {
  color: #40a9ff;
  text-decoration: underline;
}
</style>
