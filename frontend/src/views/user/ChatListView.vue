<template>
  <div class="chat-list-page">
    <div class="head">
      <h1>消息</h1>
      <span class="sub">一对一私聊 · 对方在线时实时送达</span>
    </div>

    <div v-loading="loading">
      <div class="list">
        <div v-for="c in items" :key="c.peerId" class="conv" @click="open(c)">
          <el-badge :value="c.unread" :hidden="!c.unread" :max="99" class="avatar-badge">
            <el-avatar :size="46" :src="c.peerAvatar || undefined">{{ c.peerNickname?.charAt(0) }}</el-avatar>
          </el-badge>
          <div class="mid">
            <b>{{ c.peerNickname }}</b>
            <p class="last">{{ c.lastContent }}</p>
          </div>
          <span class="time">{{ shortTime(c.lastTime) }}</span>
        </div>
      </div>
      <el-empty v-if="!loading && !items.length" description="还没有聊天记录，去达人主页点「私聊」发起对话吧" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { chatConversations } from '@/api/chat'
import { useChatStore } from '@/stores/chat'

const router = useRouter()
const chatStore = useChatStore()
const items = ref([])
const loading = ref(false)

function shortTime(t) {
  if (!t) return ''
  const d = new Date(t.replace(/-/g, '/'))
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  const pad = (n) => String(n).padStart(2, '0')
  const hm = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  return sameDay ? hm : `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${hm}`
}

async function load() {
  loading.value = true
  try {
    const res = await chatConversations()
    items.value = res.data
  } finally {
    loading.value = false
  }
}

function open(c) {
  router.push(`/chat/${c.peerId}?name=${encodeURIComponent(c.peerNickname || '')}`)
}

let timer = null
onMounted(async () => {
  await load()
  timer = setInterval(() => chatStore.refreshUnread(), 20000)
})
onUnmounted(() => timer && clearInterval(timer))
</script>

<style scoped>
.chat-list-page {
  max-width: 640px;
  margin: 0 auto;
}

.head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 14px;
}

.head h1 {
  margin: 0;
  font-size: 28px;
}

.sub {
  color: var(--fc-neutral);
  font-size: 13px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.conv {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: oklch(99% 0.006 70);
  border: 1px solid var(--fc-rule);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.conv:hover {
  border-color: var(--fc-accent);
  transform: translateX(2px);
}

.mid {
  flex: 1;
  min-width: 0;
}

.mid b {
  font-size: 15px;
}

.last {
  margin: 3px 0 0;
  color: var(--fc-neutral);
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.time {
  font-size: 12px;
  color: var(--fc-neutral);
  flex-shrink: 0;
}
</style>
