<template>
  <div class="ai-copilot-container">
    <!-- Chat Window -->
    <transition name="fade-scale">
      <div v-show="isOpen" class="chat-window">
        <div class="chat-header">
          <div class="header-title">
            <img src="@/assets/robot.png" alt="AI Robot" class="robot-icon" />
            <span>AI智能助手</span>
          </div>
          <button class="close-btn" @click="toggleChat">×</button>
        </div>

        <div class="chat-messages" ref="messagesContainer">
          <div v-if="messages.length === 0" class="welcome-msg">
            <img src="@/assets/robot.png" alt="Robot" class="welcome-robot" />
            <h3>你好，{{ userStore.username }}！</h3>
            <p>我是 DMS 智能驾驶助手，有什么可以帮你的？</p>
            <div class="quick-prompts">
              <span 
                v-for="(prompt, index) in quickPrompts" 
                :key="index" 
                class="prompt-tag" 
                @click="sendQuickMessage(prompt.message)"
              >
                {{ prompt.text }}
              </span>
            </div>
          </div>
          
          <div v-for="(msg, index) in messages" :key="index" :class="['message-bubble-wrapper', msg.role]">
            <img v-if="msg.role === 'assistant'" src="@/assets/robot.png" alt="AI" class="bubble-avatar" />
            <div class="message-bubble">{{ msg.content }}</div>
          </div>
          
          <div v-if="isLoading" class="message-bubble-wrapper assistant">
            <img src="@/assets/robot.png" alt="AI" class="bubble-avatar" />
            <div class="message-bubble typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>

        <div class="chat-input-area">
          <input 
            type="text" 
            v-model="inputText" 
            @keyup.enter="sendMessage"
            placeholder="输入你的问题..." 
            class="chat-input"
            :disabled="isLoading"
          />
          <button class="send-btn" @click="sendMessage" :disabled="isLoading || !inputText.trim()">
            发送
          </button>
        </div>
      </div>
    </transition>

    <!-- Floating Button -->
    <div class="floating-btn" @click="toggleChat" :class="{ 'is-open': isOpen }">
      <img src="@/assets/robot.png" alt="AI智能助手" class="btn-icon" />
      <div class="pulse-ring"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch, computed } from 'vue'
import { useUserStore } from '@/store/user'
import axios from 'axios'
import { message } from 'ant-design-vue'

const userStore = useUserStore()
const isOpen = ref(false)
const inputText = ref('')
const isLoading = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)

// 根据不同角色，定制个性化的推荐提问列表
const quickPrompts = computed(() => {
  const role = Number(userStore.role)
  if (role === 1) {
    return [
      { text: '怎么审核学员材料？', message: '怎么审核学员材料？' },
      { text: '如何分配教练？', message: '如何给学员分配教练？' },
      { text: '查看教练及负载', message: '系统目前有哪些教练以及他们的带教负荷？' }
    ]
  } else if (role === 2) {
    return [
      { text: '怎么录入学时？', message: '怎么录入学时？' },
      { text: '如何录入考试成绩？', message: '如何录入学员考试成绩？' },
      { text: '怎么管理日程时段？', message: '怎么设置和更改我的上课日程？' }
    ]
  } else {
    // 默认学员 (role === 3)
    return [
      { text: '考驾照身体要求？', message: '考C1驾照有哪些身体要求？' },
      { text: '查一下我的教练', message: '查一下我的专属教练是谁' },
      { text: '查询我的学时进度', message: '查询我的学习进度' }
    ]
  }
})

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

const messages = ref<ChatMessage[]>([])

function toggleChat() {
  isOpen.value = !isOpen.value
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 自动滚动到底部
watch(() => messages.value.length, () => {
  scrollToBottom()
})

async function sendQuickMessage(text: string) {
  inputText.value = text
  await sendMessage()
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text) return
  
  // 添加用户消息
  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  isLoading.value = true
  scrollToBottom()
  
  try {
    // 构造请求历史
    const history = messages.value.slice(0, -1)
    
    // 使用 vite 代理转发至 Python 8082 端口
    const response = await axios.post('/ai-api/chat', {
      message: text,
      history: history,
      token: userStore.token,
      role: Number(userStore.role)
    })
    
    if (response.data && response.data.response) {
      messages.value.push({ role: 'assistant', content: response.data.response })
    } else {
      messages.value.push({ role: 'assistant', content: '抱歉，我不理解您的意思。' })
    }
  } catch (error) {
    console.error('AI 请求失败:', error)
    message.error('与 AI 服务通信失败，请检查网络或后端状态。')
    messages.value.push({ role: 'assistant', content: '（网络连接异常，无法获取回答）' })
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
/* 全局容器，定位于右下角 */
.ai-copilot-container {
  position: fixed;
  bottom: 40px;
  right: 40px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* 悬浮按钮 */
.floating-btn {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #40a9ff, #722ed1);
  box-shadow: 0 8px 24px rgba(114, 46, 209, 0.4);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  z-index: 2;
}

.floating-btn:hover {
  transform: scale(1.08) translateY(-4px);
  box-shadow: 0 12px 32px rgba(114, 46, 209, 0.6);
}

.floating-btn.is-open {
  transform: scale(0.9) rotate(-15deg);
  background: #666;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.btn-icon {
  width: 44px;
  height: 44px;
  object-fit: cover;
  border-radius: 50%;
  z-index: 2;
  background: #fff;
  padding: 2px;
}

.floating-btn.is-open .btn-icon {
  opacity: 0.5;
}

.pulse-ring {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  border-radius: 50%;
  border: 2px solid rgba(64, 169, 255, 0.8);
  animation: pulse 2.5s infinite;
  z-index: 1;
}

.floating-btn.is-open .pulse-ring {
  display: none;
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 1; }
  100% { transform: scale(1.6); opacity: 0; }
}

/* 聊天面板 */
.chat-window {
  width: 380px;
  height: 620px;
  max-height: 80vh;
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 24px;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transform-origin: bottom right;
}

.fade-scale-enter-active, .fade-scale-leave-active {
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.fade-scale-enter-from, .fade-scale-leave-to {
  opacity: 0;
  transform: scale(0.7) translateY(20px);
}

/* 头部 */
.chat-header {
  height: 64px;
  background: linear-gradient(135deg, #1890ff, #722ed1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  color: white;
  flex-shrink: 0;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  font-size: 16px;
  letter-spacing: 0.5px;
}

.robot-icon {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #fff;
  padding: 2px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.2);
}

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 26px;
  cursor: pointer;
  line-height: 1;
  opacity: 0.7;
  transition: opacity 0.2s, transform 0.2s;
}
.close-btn:hover {
  opacity: 1;
  transform: rotate(90deg);
}

/* 消息列表区 */
.chat-messages {
  flex: 1;
  padding: 24px 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
  background: rgba(245, 247, 250, 0.5);
}

.chat-messages::-webkit-scrollbar {
  width: 6px;
}
.chat-messages::-webkit-scrollbar-thumb {
  background: rgba(0,0,0,0.15);
  border-radius: 3px;
}

/* 欢迎引导 */
.welcome-msg {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin-top: 24px;
  color: #444;
}

.welcome-robot {
  width: 88px;
  height: 88px;
  margin-bottom: 20px;
  border-radius: 50%;
  background: #fff;
  padding: 4px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
  animation: float 4s ease-in-out infinite;
}

@keyframes float {
  0% { transform: translateY(0px); }
  50% { transform: translateY(-8px); }
  100% { transform: translateY(0px); }
}

.welcome-msg h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #1a1a1a;
}

.welcome-msg p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 24px;
}

.prompt-tag {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 8px 16px;
  font-size: 13px;
  color: #1890ff;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
}

.prompt-tag:hover {
  background: #e6f7ff;
  border-color: #1890ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(24,144,255,0.15);
}

/* 对话气泡 */
.message-bubble-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  max-width: 92%;
  animation: popIn 0.4s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
}

@keyframes popIn {
  0% { opacity: 0; transform: translateY(15px) scale(0.95); }
  100% { opacity: 1; transform: translateY(0) scale(1); }
}

.message-bubble-wrapper.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-bubble-wrapper.assistant {
  align-self: flex-start;
}

.bubble-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #fff;
  padding: 2px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.08);
  flex-shrink: 0;
}

.message-bubble {
  padding: 14px 18px;
  border-radius: 20px;
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
  white-space: pre-wrap;
  box-shadow: 0 4px 12px rgba(0,0,0,0.04);
}

.user .message-bubble {
  background: linear-gradient(135deg, #1890ff, #40a9ff);
  color: white;
  border-bottom-right-radius: 4px;
}

.assistant .message-bubble {
  background: #ffffff;
  color: #333;
  border: 1px solid rgba(0,0,0,0.06);
  border-bottom-left-radius: 4px;
}

/* 打字机效果 */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 24px;
  padding: 14px 20px;
}

.typing-indicator span {
  display: block;
  width: 6px;
  height: 6px;
  background-color: #a0aec0;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { 
    transform: scale(0); 
    opacity: 0.5;
  }
  40% { 
    transform: scale(1); 
    opacity: 1;
  }
}

/* 输入区 */
.chat-input-area {
  padding: 18px 20px;
  background: rgba(255, 255, 255, 0.9);
  border-top: 1px solid rgba(0,0,0,0.06);
  display: flex;
  gap: 12px;
  align-items: center;
  flex-shrink: 0;
  border-bottom-left-radius: 24px;
  border-bottom-right-radius: 24px;
}

.chat-input {
  flex: 1;
  padding: 12px 18px;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  outline: none;
  font-size: 14px;
  transition: all 0.3s;
  background: #f8fafc;
  color: #1e293b;
}

.chat-input:focus {
  border-color: #1890ff;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(24,144,255,0.1);
}

.send-btn {
  background: linear-gradient(135deg, #1890ff, #40a9ff);
  color: white;
  border: none;
  border-radius: 24px;
  padding: 10px 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 600;
  letter-spacing: 0.5px;
  box-shadow: 0 4px 10px rgba(24,144,255,0.3);
}

.send-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgba(24,144,255,0.4);
}

.send-btn:active {
  transform: translateY(1px);
}

.send-btn:disabled {
  background: #cbd5e1;
  box-shadow: none;
  cursor: not-allowed;
  transform: none;
}
</style>
