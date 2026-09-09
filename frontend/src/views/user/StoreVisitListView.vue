<template>
  <div class="sv-page">
    <div class="page-title">
      <h1>探店情报</h1>
      <p>跟着社区食客去打卡，每篇都经过管理员审核</p>
    </div>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索探店标题…" clearable class="search" @keyup.enter="load(1)" @clear="load(1)">
        <template #append><el-button @click="load(1)">搜索</el-button></template>
      </el-input>
    </div>

    <div v-loading="loading" class="grid">
      <div v-for="sv in rows" :key="sv.id" class="sv-card card-link" @click="open(sv)">
        <div class="thumb"><RecipeCover :src="sv.cover" :title="sv.title" :seed="sv.id" /></div>
        <div class="body">
          <h3>{{ sv.title }}</h3>
          <div class="meta">
            <span class="author">
              <el-avatar :size="20" :src="sv.authorAvatar || undefined">{{ sv.authorNickname?.charAt(0) }}</el-avatar>
              {{ sv.authorNickname }}
            </span>
            <span class="views">👁 {{ sv.views }}</span>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && !rows.length" description="还没有探店情报" />

    <el-pagination v-if="total > pageSize" class="pager" background layout="prev, pager, next" :total="total"
                   :page-size="pageSize" :current-page="page" @current-change="load" />

    <!-- 详情弹窗（打开即浏览量+1） -->
    <el-dialog v-model="visible" :title="current?.title" width="620px" @closed="current = null">
      <div v-if="current" v-loading="detailLoading">
        <div class="cover"><RecipeCover :src="current.cover" :title="current.title" :seed="current.id" /></div>
        <div class="byline">
          <router-link :to="`/user/${current.userId}`" class="author-link">
            <el-avatar :size="26" :src="current.authorAvatar || undefined">{{ current.authorNickname?.charAt(0) }}</el-avatar>
            <span>{{ current.authorNickname }}</span>
          </router-link>
          <span class="addr">📍 {{ current.address || '未标注地址' }}</span>
        </div>
        <MarkdownContent :content="current.content" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { storeVisitPage, storeVisitDetail } from '@/api/storeVisit'
import RecipeCover from '@/components/RecipeCover.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 12
const keyword = ref('')
const loading = ref(false)
const visible = ref(false)
const current = ref(null)
const detailLoading = ref(false)

async function load(p) {
  page.value = p
  loading.value = true
  try {
    const params = { page: p, size: pageSize }
    if (keyword.value) params.keyword = keyword.value
    const res = await storeVisitPage(params)
    rows.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

async function open(sv) {
  visible.value = true
  detailLoading.value = true
  try {
    const res = await storeVisitDetail(sv.id)
    current.value = res.data
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.sv-page {
  max-width: 1000px;
  margin: 0 auto;
}

.page-title h1 {
  margin: 0 0 6px;
  font-size: 30px;
}

.page-title p {
  color: var(--fc-neutral);
  margin: 0 0 18px;
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.search {
  width: 260px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.sv-card {
  background: oklch(99% 0.006 70);
  border: 1px solid var(--fc-rule);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
}

.thumb {
  height: 150px;
}

.body {
  padding: 12px 14px;
}

.body h3 {
  margin: 0 0 8px;
  font-size: 16px;
  line-height: 1.4;
}

.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.author {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--fc-neutral);
}

.views {
  font-size: 12px;
  color: var(--fc-muted);
}

.pager {
  margin-top: 22px;
  justify-content: center;
}

.cover {
  height: 230px;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 12px;
}

.byline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.author-link {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--fc-ink);
}

.author-link:hover {
  color: var(--fc-accent);
}

.addr {
  color: var(--fc-neutral);
  font-size: 13px;
}

@media (max-width: 800px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
