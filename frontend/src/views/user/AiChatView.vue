<template>
  <div class="ai-page">
    <div class="ai-shell">
      <!-- 会话区 -->
      <div ref="scrollBox" class="chat-box">
        <div v-if="!messages.length" class="welcome">
          <div class="logo">🍳</div>
          <h1>AI 美食专家</h1>
          <p>做菜、挑食材、控火候、选店……不懂就问，答案逐字生成中。</p>
          <div class="chips">
            <span v-for="q in quickQuestions" :key="q" class="chip" @click="ask(q)">{{ q }}</span>
          </div>
        </div>

        <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
          <div class="avatar" v-if="m.role === 'assistant'">🍳</div>
          <div class="bubble">
            <MarkdownContent v-if="m.role === 'assistant'" :content="m.content" />
            <pre v-else class="user-text">{{ m.content }}</pre>
            <span v-if="streaming && i === messages.length - 1" class="cursor">▍</span>
          </div>
        </div>

        <div v-if="errorMsg" class="error">{{ errorMsg }}</div>
      </div>

      <!-- 输入区 -->
      <div class="composer">
        <el-input v-model="input" type="textarea" :rows="2" resize="none" :disabled="streaming"
                  placeholder="问问 AI：红烧肉怎么炖才软烂？冰箱里有鸡胸肉能做什么？" maxlength="2000"
                  @keydown.enter.exact.prevent="send" />
        <div class="composer-bar">
          <span class="tip">Enter 发送 · Shift+Enter 换行 · AI 回答可能有误请自行判断</span>
          <div class="actions">
            <el-button v-if="streaming" type="danger" plain round :disabled="!streaming" @click="stop">
              ⏹ 停止
            </el-button>
            <el-button v-else type="primary" round :loading="starting" @click="send">
              {{ messages.length ? '发送' : '开始对话' }}
            </el-button>
            <el-button v-if="messages.length && !streaming" text @click="clear">清空</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { streamChat } from '@/api/ai'
import MarkdownContent from '@/components/MarkdownContent.vue'

const messages = ref([])
const input = ref('')
const streaming = ref(false)
const starting = ref(false)
const errorMsg = ref('')
const scrollBox = ref(null)
let controller = null

const quickQuestions = [
  '红烧肉怎么炖才软烂不腻？',
  '厨房新手买什么锅合适？',
  '鸡胸肉怎么做好吃不柴？',
  '周末朋友来家里，推荐三道好做的菜'
]

function scrollToBottom() {
  nextTick(() => {
    if (scrollBox.value) {
      scrollBox.value.scrollTop = scrollBox.value.scrollHeight
    }
  })
}

async function ask(q) {
  input.value = q
  await send()
}

async function send() {
  const text = input.value.trim()
  if (!text) return
  if (streaming.value) return

  input.value = ''
  errorMsg.value = ''
  messages.value.push({ role: 'user', content: text })

  const aiIndex = messages.value.length
  messages.value.push({ role: 'assistant', content: '' })
  streaming.value = true
  scrollToBottom()

  controller = new AbortController()
  try {
    // 发送最近 10 轮（含本次提问），保证多轮上下文
    const history = messages.value.slice(0, aiIndex + 1).slice(-10)
    await streamChat(history, (delta) => {
      messages.value[aiIndex] = { ...messages.value[aiIndex], content: messages.value[aiIndex].content + delta }
      scrollToBottom()
    }, controller.signal)
    if (!messages.value[aiIndex].content) {
      messages.value[aiIndex].content = '（没有返回内容，请换一种问法试试）'
    }
  } catch (e) {
    if (e.name === 'AbortError') {
      // 用户主动停止：保留已生成内容
    } else {
      errorMsg.value = `⚠️ ${e.message || 'AI 服务暂时不可用'}`
    }
  } finally {
    streaming.value = false
    controller = null
    scrollToBottom()
  }
}

function stop() {
  if (controller) {
    controller.abort()
  }
}

function clear() {
  messages.value = []
  input.value = ''
  errorMsg.value = ''
}

// 若请求返回业务错误（如未配置 Key），fetch 走不到这里；需要兼容非流式错误响应
</script>

<style scoped>
.ai-page {
  max-width: 860px;
  margin: 0 auto;
}

.ai-shell {
  background: oklch(99% 0.006 70);
  border: 1px solid var(--fc-rule);
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 180px);
  min-height: 460px;
}

.chat-box {
  flex: 1;
  overflow-y: auto;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: var(--fc-paper);
}

.welcome {
  margin: auto;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.logo {
  font-size: 52px;
}

.welcome h1 {
  margin: 4px 0 0;
  font-size: 26px;
}

.welcome p {
  color: var(--fc-neutral);
  margin: 0;
  font-size: 14px;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-top: 10px;
  max-width: 560px;
}

.chip {
  padding: 7px 14px;
  background: var(--fc-surface);
  border: 1px solid var(--fc-rule);
  border-radius: 999px;
  font-size: 13px;
  cursor: pointer;
  color: var(--fc-ink);
  transition: all 0.15s;
}

.chip:hover {
  border-color: var(--fc-accent);
  color: var(--fc-accent);
}

.msg {
  display: flex;
  gap: 10px;
  max-width: 78%;
}

.msg.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--fc-accent-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  flex-shrink: 0;
}

.bubble {
  padding: 10px 14px;
  border-radius: 14px;
  line-height: 1.7;
  font-size: 14px;
  word-break: break-word;
  min-width: 40px;
}

.msg.assistant .bubble {
  background: oklch(99% 0.006 70);
  border: 1px solid var(--fc-rule);
  border-top-left-radius: 4px;
}

.msg.user .bubble {
  background: var(--fc-accent);
  color: #fff;
  border-top-right-radius: 4px;
}

.user-text {
  margin: 0;
  white-space: pre-wrap;
  font-family: inherit;
  font-size: 14px;
}

.cursor {
  animation: blink 1s steps(2) infinite;
  color: var(--fc-accent);
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.error {
  color: #d43d2a;
  font-size: 13px;
  text-align: center;
}

.composer {
  border-top: 1px solid var(--fc-rule);
  padding: 12px 16px 14px;
  background: oklch(99% 0.006 70);
}

.composer-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.tip {
  font-size: 12px;
  color: var(--fc-neutral);
}

.actions {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
