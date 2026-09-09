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
  max-width: 760px;
  margin: 0 auto;
}

.page-title h1 {
  margin: 0 0 6px;
  font-size: 30px;
}

.page-title p {
  color: var(--fc-neutral);
  margin: 0 0 20px;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rank-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: oklch(99% 0.006 70);
  border: 1px solid var(--fc-rule);
  border-radius: 14px;
  padding: 18px 20px;
}

.rank-no {
  width: 30px;
  text-align: center;
  font-family: var(--fc-font-display);
  font-size: 20px;
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
  font-size: 17px;
}

.info:hover .name {
  color: var(--fc-accent);
}

.intro {
  margin: 4px 0 0;
  color: var(--fc-neutral);
  font-size: 13px;
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
  font-size: 22px;
  font-weight: 700;
  color: var(--fc-accent);
}

.heat-label {
  font-size: 11px;
  color: var(--fc-neutral);
}
</style>
