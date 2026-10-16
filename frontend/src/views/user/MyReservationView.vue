<template>
  <div class="rsv-page">
    <div class="head">
      <h1>探店预约</h1>
      <div class="tabs">
        <span class="tab" :class="{ active: direction === 'initiated' }" @click="switchDir('initiated')">我发起的</span>
        <span class="tab" :class="{ active: direction === 'received' }" @click="switchDir('received')">我接收的</span>
      </div>
    </div>

    <div v-loading="loading" class="list">
      <div v-for="rv in rows" :key="rv.id" class="rsv-card" :class="`st-${rv.status}`">
        <div class="top">
          <span class="store">{{ rv.storeName }}</span>
          <el-tag :type="statusType(rv.status)" size="small" effect="light">
            {{ statusText(rv.status) }}
          </el-tag>
        </div>
        <p v-if="rv.storeIntro" class="intro">{{ rv.storeIntro }}</p>
        <ul class="info">
          <li>📍 {{ rv.storeAddress || '未填地址' }}</li>
          <li>📞 {{ rv.phone || '未填电话' }}</li>
          <li v-if="rv.remark">📝 {{ rv.remark }}</li>
        </ul>
        <div class="parties">
          <span class="who">
            <el-avatar :size="20" :src="rv.initiatorAvatar || undefined">{{ rv.initiatorNickname?.charAt(0) }}</el-avatar>
            发起：{{ rv.initiatorNickname }}
          </span>
          <span class="who">
            <el-avatar :size="20" :src="rv.expertAvatar || undefined">{{ rv.expertNickname?.charAt(0) }}</el-avatar>
            达人：{{ rv.expertNickname }}
          </span>
        </div>
        <div class="foot">
          <span class="time">{{ rv.createTime }}</span>
          <span v-if="rv.status === 2 && rv.rejectReason" class="reason">拒绝理由：{{ rv.rejectReason }}</span>
          <div v-if="direction === 'received' && rv.status === 0" class="ops">
            <el-button size="small" type="success" plain @click="agree(rv)">同意</el-button>
            <el-button size="small" type="danger" plain @click="reject(rv)">拒绝</el-button>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && !rows.length"
                :description="direction === 'initiated' ? '还没有发起过预约' : '还没有收到预约' " />
    </div>

    <el-pagination v-if="total > pageSize" class="pager" background layout="prev, pager, next" :total="total"
                   :page-size="pageSize" :current-page="page" @current-change="load" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { myReservations, agreeReservation, rejectReservation } from '@/api/storeVisit'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const direction = ref('initiated')
const loading = ref(false)

const statusType = (s) => (s === 0 ? 'warning' : s === 1 ? 'success' : 'danger')
const statusText = (s) => (s === 0 ? '待处理' : s === 1 ? '已同意' : '已拒绝')

function switchDir(d) {
  direction.value = d
  load(1)
}

async function load(p) {
  page.value = p
  loading.value = true
  try {
    const res = await myReservations({ direction: direction.value, page: p, size: pageSize })
    rows.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

async function agree(rv) {
  await ElMessageBox.confirm(`同意「${rv.initiatorNickname}」对 ${rv.storeName} 的探店预约？`, '同意预约', { type: 'success' })
  await agreeReservation(rv.id)
  ElMessage.success('已同意')
  load(page.value)
}

async function reject(rv) {
  const { value } = await ElMessageBox.prompt(`拒绝「${rv.initiatorNickname}」的预约`, '拒绝预约', {
    inputPlaceholder: '请填写拒绝理由（必填）',
    inputValidator: (v) => (v && v.trim() ? true : '拒绝必须填写理由'),
    type: 'warning'
  })
  await rejectReservation(rv.id, { reason: value.trim() })
  ElMessage.success('已拒绝')
  load(page.value)
}

onMounted(() => load(1))
</script>

<style scoped>
.rsv-page {
  max-width: var(--fc-page-narrow);
  margin: 0 auto;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fc-sp-4);
}

.head h1 {
  margin: 0;
  font-size: var(--fc-fs-2xl);
}

.tabs {
  display: flex;
  gap: var(--fc-sp-2);
}

.tab {
  padding: 7px 18px;
  border-radius: 999px;
  font-size: var(--fc-fs-base);
  cursor: pointer;
  background: var(--fc-surface);
  color: var(--fc-neutral);
}

.tab.active {
  background: var(--fc-ink);
  color: oklch(97% 0.012 70);
}

.list {
  display: flex;
  flex-direction: column;
  gap: var(--fc-sp-3-5);
}

.rsv-card {
  background: oklch(99% 0.006 70);
  border: 1px solid var(--fc-rule);
  border-left: 4px solid var(--fc-rule);
  border-radius: var(--fc-r-md);
  padding: 18px var(--fc-sp-5);
}

.rsv-card.st-1 {
  border-left-color: #67c23a;
}

.rsv-card.st-2 {
  border-left-color: var(--fc-danger);
}

.top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.store {
  font-family: var(--fc-font-display);
  font-size: var(--fc-fs-xl);
  font-weight: 700;
}

.intro {
  color: var(--fc-neutral);
  margin: var(--fc-sp-2) 0;
  font-size: var(--fc-fs-base);
}

.info {
  list-style: none;
  padding: 0;
  margin: 0 0 var(--fc-sp-2-5);
  display: flex;
  flex-direction: column;
  gap: var(--fc-sp-1);
  color: var(--fc-muted);
  font-size: var(--fc-fs-sm);
}

.parties {
  display: flex;
  gap: var(--fc-sp-5);
  padding-top: var(--fc-sp-2-5);
  border-top: 1px dashed var(--fc-rule);
}

.who {
  display: flex;
  align-items: center;
  gap: var(--fc-sp-1-5);
  font-size: var(--fc-fs-sm);
  color: var(--fc-ink);
}

.foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--fc-sp-2-5);
  flex-wrap: wrap;
  gap: var(--fc-sp-2);
}

.time {
  font-size: var(--fc-fs-xs);
  color: var(--fc-neutral);
}

.reason {
  font-size: var(--fc-fs-xs);
  color: var(--fc-danger);
}

.ops {
  display: flex;
  gap: var(--fc-sp-2);
}

.pager {
  margin-top: 18px;
  justify-content: center;
}
</style>
