<template>
  <div class="notice-page">
    <div class="head">
      <h1>系统公告</h1>
      <p>平台动态、活动与使用说明都在这里</p>
    </div>

    <div v-loading="loading" class="list">
      <div v-for="n in rows" :key="n.id" class="item" @click="openDetail(n)">
        <span class="dot">📢</span>
        <div class="mid">
          <b>{{ n.title }}</b>
          <p class="summary">{{ strip(n.content) }}</p>
        </div>
        <span class="time">{{ n.createTime }}</span>
      </div>
      <el-empty v-if="!loading && !rows.length" description="暂无公告" />
    </div>

    <el-pagination v-if="total > pageSize" class="pager" background layout="prev, pager, next" :total="total"
                   :page-size="pageSize" :current-page="page" @current-change="load" />

    <el-dialog v-model="visible" :title="current?.title" width="600px">
      <div class="meta-line">{{ current?.createTime }}</div>
      <MarkdownContent :content="current?.content || ''" />
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { noticePage, noticeDetail } from '@/api/notice'
import MarkdownContent from '@/components/MarkdownContent.vue'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const visible = ref(false)
const current = ref(null)

function strip(html) {
  const text = (html || '').replace(/[#*`>\-\[\]()]/g, ' ').replace(/\s+/g, ' ').trim()
  return text.length > 90 ? text.slice(0, 90) + '…' : text
}

async function load(p) {
  page.value = p
  loading.value = true
  try {
    const res = await noticePage({ page: p, size: pageSize })
    rows.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

async function openDetail(n) {
  const res = await noticeDetail(n.id)
  current.value = res.data
  visible.value = true
}

onMounted(() => load(1))
</script>

<style scoped>
.notice-page {
  max-width: 760px;
  margin: 0 auto;
}

.head h1 {
  margin: 0 0 4px;
  font-size: 28px;
}

.head p {
  margin: 0 0 16px;
  color: var(--fc-neutral);
  font-size: 13px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  background: oklch(99% 0.006 70);
  border: 1px solid var(--fc-rule);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.item:hover {
  border-color: var(--fc-accent);
}

.dot {
  font-size: 20px;
}

.mid {
  flex: 1;
  min-width: 0;
}

.mid b {
  font-size: 15px;
}

.summary {
  margin: 4px 0 0;
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

.pager {
  margin-top: 18px;
  justify-content: center;
}

.meta-line {
  color: var(--fc-neutral);
  font-size: 12px;
  margin-bottom: 10px;
}
</style>
