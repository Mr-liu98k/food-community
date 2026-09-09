<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="按店铺名 / 电话 / 昵称搜索" clearable class="search"
                @keyup.enter="load(1)" @clear="load(1)">
        <template #append><el-button @click="load(1)">搜索</el-button></template>
      </el-input>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="storeName" label="店铺名称" min-width="140" />
      <el-table-column label="发起人" width="120">
        <template #default="{ row }">{{ row.initiatorNickname }}</template>
      </el-table-column>
      <el-table-column label="探店达人" width="120">
        <template #default="{ row }">{{ row.expertNickname }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column prop="storeAddress" label="地址" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.status === 0" type="warning" size="small">待处理</el-tag>
          <el-tag v-else-if="row.status === 1" type="success" size="small">已同意</el-tag>
          <el-tag v-else type="danger" size="small" :title="row.rejectReason">已拒绝</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="发起时间" width="170" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="暂无预约记录" />

    <el-pagination v-if="total > query.size" class="pager" background layout="prev, pager, next"
                   :total="total" :page-size="query.size" :current-page="query.page" @current-change="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminReservationPage, adminRemoveReservation } from '@/api/admin'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '' })

async function load(p) {
  query.page = p
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    const res = await adminReservationPage(params)
    rows.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

async function del(row) {
  await ElMessageBox.confirm(`确定删除「${row.initiatorNickname}」对 ${row.storeName} 的预约记录吗？`, '删除确认', { type: 'warning' })
  await adminRemoveReservation(row.id)
  ElMessage.success('已删除')
  load(query.page)
}

onMounted(() => load(1))
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 14px;
}

.search {
  width: 280px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
