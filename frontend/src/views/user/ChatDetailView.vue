<template>
  <div class="chat-detail">
    <div class="d-header">
      <el-button text @click="router.push('/chat')">
        <el-icon><ArrowLeft /></el-icon>返回
      </el-button>
      <el-avatar :size="32" :src="peer.avatar || undefined">{{ peer.name?.charAt(0) }}</el-avatar>
      <b>{{ peer.name || '…' }}</b>
      <span class="online" :class="{ on: online }">{{ online ? '在线' : '离线' }}</span>
    </div>

    <div ref="scrollBox" class="msgs">
      <div class="load-more-wrap">
        <el-button v-if="hasMore" text size="small" :loading="loadingMore" @click="loadOlder">加载更早消息</el-button>
      </div>
      <div v-for="m in messages" :key="m.key" class="row" :class="m.fromUserId === myId ? 'mine' : 'peer'">
        <div class="bubble">
          <pre>{{ m.content }}</pre>
          <span class="tm">{{ shortTime(m.createTime) }}</span>
        </div>
      </div>
      <el-empty v-if="!messages.length && !loadingFirst" description="打个招呼开始聊天吧" :image-size="80" />
    </div>

    <div class="composer">
      <el-input v-model="draft" :rows="2" type="textarea" resize="none" maxlength="1000"
                placeholder="输入消息…（Enter 发送）" @keydown.enter.exact.prevent="send" />
      <el-button type="primary" :disabled="!draft.trim() || !connected" @click="send">发送</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { chatHistory, chatMarkRead, openChatSocket } from '@/api/chat'
import { authorProfile } from '@/api/interaction'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const chatStore = useChatStore()

const peerId = computed(() => Number(route.params.id))
const myId = computed(() => store.user?.id)
const peer = ref({ name: route.query.name || '' })

const messages = ref([]) // 显示顺序：时间正序（旧→新）
const draft = ref('')
const page = ref(1)
const hasMore = ref(false)
const loadingMore = ref(false)
const loadingFirst = ref(false)
const scrollBox = ref(null)

let socket = null
let reconnectTimer = null
const connected = ref(false)
const online = ref(false)

function shortTime(t) {
  if (!t) return ''
  const d = new Date(t.replace(/-/g, '/'))
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function scrollBottom() {
  requestAnimationFrame(() => {
    if (scrollBox.value) scrollBox.value.scrollTop = scrollBox.value.scrollHeight
  })
}

async function loadHistory(p) {
  const res = await chatHistory(peerId.value, { page: p, size: 20 })
  return res.data
}

async function loadFirst() {
  loadingFirst.value = true
  try {
    const data = await loadHistory(1)
    page.value = 1
    hasMore.value = data.total > data.records.length
    messages.value = [...data.records].reverse().map((m, i) => ({ ...m, key: `${m.id}-${i}` }))
    await markRead()
    scrollBottom()
  } finally {
    loadingFirst.value = false
  }
}

async function loadOlder() {
  loadingMore.value = true
  try {
    const next = page.value + 1
    const data = await loadHistory(next)
    page.value = next
    hasMore.value = data.records.length >= 20 // 是否还有更早
    const older = [...data.records].reverse()
    messages.value = [...older, ...messages.value].map((m, i) => ({ ...m, key: `${m.id}-${i}-${Date.now()}` }))
    scrollBottom()
  } finally {
    loadingMore.value = false
  }
}

async function markRead() {
  await chatMarkRead(peerId.value)
  chatStore.refreshUnread()
}

function connect() {
  if (!store.token) return
  socket = openChatSocket(store.token)

  socket.onopen = () => {
    connected.value = true
  }
  socket.onmessage = (ev) => {
    let data
    try {
      data = JSON.parse(ev.data)
    } catch {
      return
    }
    if (data.type === 'chat' && data.message) {
      const m = data.message
      if (m.toUserId === myId.value && m.fromUserId === peerId.value) {
        messages.value.push({ ...m, key: `ws-${m.id}` })
        scrollBottom()
        markRead()
      }
    }
    if (data.type === 'chat' && data.message && data.message.fromUserId === myId.value) {
      online.value = true
    }
  }
  socket.onclose = () => {
    connected.value = false
    reconnectTimer = setTimeout(connect, 3000)
  }
  socket.onerror = () => {
    socket && socket.close()
  }
}

function send() {
  const text = draft.value.trim()
  if (!text || !socket || socket.readyState !== WebSocket.OPEN) {
    return ElMessage.warning(connected.value ? '' : '连接未就绪，请稍候')
  }
  socket.send(JSON.stringify({ toUserId: peerId.value, content: text }))
  messages.value.push({
    key: `tmp-${Date.now()}`,
    fromUserId: myId.value,
    content: text,
    createTime: formatNow()
  })
  draft.value = ''
  scrollBottom()
}

function formatNow() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

onMounted(async () => {
  // 对方昵称/头像：会话列表里查，查不到用作者主页
  try {
    const res = await authorProfile(peerId.value)
    peer.value = { name: res.data.user.nickname, avatar: res.data.user.avatar }
  } catch (e) {
    // 保持 query 里的名字
  }
  await loadFirst()
  connect()
})

onBeforeUnmount(() => {
  if (socket) {
    socket.onclose = null
    socket.close()
  }
  clearTimeout(reconnectTimer)
})
</script>

<style scoped>
.chat-detail {
  max-width: 720px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 150px);
  min-height: 480px;
  background: oklch(99% 0.006 70);
  border: 1px solid var(--fc-rule);
  border-radius: 14px;
  overflow: hidden;
}

.d-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  border-bottom: 1px solid var(--fc-rule);
  background: var(--fc-surface);
}

.d-header b {
  font-size: 15px;
}

.online {
  font-size: 12px;
  color: var(--fc-neutral);
}

.online.on {
  color: #67c23a;
}

.msgs {
  flex: 1;
  overflow-y: auto;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: var(--fc-paper);
}

.load-more-wrap {
  text-align: center;
}

.row {
  display: flex;
}

.row.mine {
  justify-content: flex-end;
}

.bubble {
  max-width: 68%;
  padding: 9px 13px;
  border-radius: 14px;
  position: relative;
}

.row.mine .bubble {
  background: var(--fc-accent);
  color: #fff;
  border-top-right-radius: 4px;
}

.row.peer .bubble {
  background: #fff;
  border: 1px solid var(--fc-rule);
  border-top-left-radius: 4px;
}

.bubble pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
}

.tm {
  display: block;
  text-align: right;
  font-size: 11px;
  margin-top: 4px;
  opacity: 0.65;
}

.composer {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding: 10px 14px 12px;
  border-top: 1px solid var(--fc-rule);
  background: oklch(99% 0.006 70);
}
</style>
