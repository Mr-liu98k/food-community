<template>
  <div class="expert-page">
    <div class="page-title">
      <h1>探店达人榜</h1>
      <p>热度 = 达人所有已审核探店浏览量 × 2，跟着最会吃的 TA 探店</p>
    </div>

    <div v-loading="loading">
      <div class="rank-list">
        <div v-for="(ex, i) in rows" :key="ex.id" class="rank-card">
          <span class="rank-no" :class="{ top: i < 3 }">{{ i + 1 }}</span>
          <el-avatar :size="54" :src="ex.avatar || undefined">{{ ex.nickname?.charAt(0) }}</el-avatar>
          <div class="info" @click="router.push(`/user/${ex.id}`)">
            <b class="name">{{ ex.nickname }}</b>
            <p class="intro">{{ ex.intro || '一个热爱探店的美食家' }}</p>
          </div>
          <div class="heat">
            <span class="heat-num">{{ ex.heat }}</span>
            <span class="heat-label">热度</span>
          </div>
          <div class="actions">
            <el-button type="primary" plain size="small" round @click="router.push(`/user/${ex.id}`)">主页</el-button>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && !rows.length" description="还没有探店达人，管理员可在用户管理中设置" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { expertList } from '@/api/storeVisit'

const router = useRouter()
const rows = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await expertList({})
    rows.value = res.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.expert-page {
  max-width: var(--fc-page-narrow);
  margin: 0 auto;
}

.page-title h1 {
  margin: 0 0 var(--fc-sp-1-5);
  font-size: var(--fc-fs-2xl);
}

.page-title p {
  color: var(--fc-neutral);
  margin: 0 0 var(--fc-sp-5);
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: var(--fc-sp-3);
}

.rank-card {
  display: flex;
  align-items: center;
  gap: var(--fc-sp-4);
  background: oklch(99% 0.006 70);
  border: 1px solid var(--fc-rule);
  border-radius: var(--fc-r-md-lg);
  padding: 18px var(--fc-sp-5);
}

.rank-no {
  width: 30px;
  text-align: center;
  font-family: var(--fc-font-display);
  font-size: var(--fc-fs-xl);
  font-weight: 700;
  color: var(--fc-neutral);
}

.rank-no.top {
  color: var(--fc-accent);
}

.info {
  flex: 1;
  cursor: pointer;
  min-width: 0;
}

.name {
  font-size: var(--fc-fs-lg);
}

.info:hover .name {
  color: var(--fc-accent);
}

.intro {
  margin: var(--fc-sp-1) 0 0;
  color: var(--fc-neutral);
  font-size: var(--fc-fs-sm);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.heat {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.heat-num {
  font-family: var(--fc-font-display);
  font-size: var(--fc-fs-xl);
  font-weight: 700;
  color: var(--fc-accent);
}

.heat-label {
  font-size: var(--fc-fs-2xs);
  color: var(--fc-neutral);
}
</style>
