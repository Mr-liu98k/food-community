<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="按标题搜索" clearable class="search"
                @keyup.enter="load(1)" @clear="load(1)">
        <template #append><el-button @click="load(1)">搜索</el-button></template>
      </el-input>
      <el-select v-model="query.status" placeholder="审核状态" clearable class="w140" @change="load(1)">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已拒绝" :value="2" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column label="探店" min-width="220">
        <template #default="{ row }">
          <div class="t-title">
            <div class="mini-cover"><RecipeCover :src="row.cover" :title="row.title" :seed="row.id" /></div>
            <span>{{ row.title }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="作者" width="130">
        <template #default="{ row }">{{ row.authorNickname }}</template>
      </el-table-column>
      <el-table-column label="浏览" width="80" prop="views" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.status === 0" type="warning" size="small">待审核</el-tag>
          <el-tag v-else-if="row.status === 1" type="success" size="small">已通过</el-tag>
          <el-tag v-else type="danger" size="small">已拒绝</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提交时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="preview(row)">预览</el-button>
          <el-button v-if="row.status === 0" link type="success" @click="doAudit(row, 1)">通过</el-button>
          <el-button v-if="row.status === 0" link type="danger" @click="doAudit(row, 2)">拒绝</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="暂无探店信息" />

    <el-pagination v-if="total > query.size" class="pager" background layout="prev, pager, next"
                   :total="total" :page-size="query.size" :current-page="query.page" @current-change="load" />

    <!-- 预览（审核在同页完成） -->
    <el-dialog v-model="previewVisible" :title="current?.title || '探店预览'" width="640px">
      <div v-if="current">
        <div class="cover"><RecipeCover :src="current.cover" :title="current.title" :seed="current.id" /></div>
        <div class="meta">
          <span>作者：{{ current.authorNickname }}</span>
          <span>📍 {{ current.address || '未填地址' }}</span>
          <span>浏览量：{{ current.views }}</span>
        </div>
        <el-alert v-if="current.status === 2" type="error" :closable="false" class="mb"
                  :title="`拒绝理由：${current.rejectReason}`" />
        <MarkdownContent :content="current.content" />
      </div>
      <template #footer>
        <template v-if="current && current.status === 0">
          <el-button type="success" @click="doAudit(current, 1, true)">审核通过</el-button>
          <el-button type="danger" @click="doAudit(current, 2, true)">审核拒绝</el-button>
        </template>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminStoreVisitPage, adminAuditStoreVisit, adminRemoveStoreVisit } from '@/api/admin'
import RecipeCover from '@/components/RecipeCover.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const previewVisible = ref(false)
const current = ref(null)
const query = reactive({ page: 1, size: 10, keyword: '', status: null })

async function load(p) {
  query.page = p
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== null && query.status !== undefined && query.status !== '') params.status = query.status
    const res = await adminStoreVisitPage(params)
    rows.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

function preview(row) {
  current.value = row
  previewVisible.value = true
}

async function doAudit(row, status, fromDialog = false) {
  if (status === 2) {
    const { value } = await ElMessageBox.prompt('拒绝理由将展示给发布者', '审核拒绝', {
      inputPlaceholder: '请填写拒绝理由（必填）',
      inputValidator: (v) => (v && v.trim() ? true : '拒绝必须填写理由'),
      type: 'warning'
    })
    await adminAuditStoreVisit(row.id, { status: 2, reason: value.trim() })
    ElMessage.success('已拒绝')
  } else {
    await ElMessageBox.confirm(`确定通过「${row.title}」的审核吗？`, '审核通过', { type: 'success' })
    await adminAuditStoreVisit(row.id, { status: 1 })
    ElMessage.success('已通过审核')
  }
  if (fromDialog) previewVisible.value = false
  load(query.page)
}

async function del(row) {
  await ElMessageBox.confirm(`确定删除探店「${row.title}」吗？`, '删除确认', { type: 'warning' })
  await adminRemoveStoreVisit(row.id)
  ElMessage.success('已删除')
  load(query.page)
}

onMounted(() => load(1))
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: var(--fc-sp-2-5);
  margin-bottom: var(--fc-sp-3-5);
}

.search {
  width: 240px;
}

.w140 {
  width: 140px;
}

.t-title {
  display: flex;
  align-items: center;
  gap: var(--fc-sp-2-5);
}

.mini-cover {
  width: 52px;
  height: 38px;
  border-radius: var(--fc-r-xs);
  overflow: hidden;
  flex-shrink: 0;
}

.pager {
  margin-top: var(--fc-sp-4);
  justify-content: flex-end;
}

.cover {
  height: 220px;
  border-radius: var(--fc-r-sm-md);
  overflow: hidden;
  margin-bottom: var(--fc-sp-3);
}

.meta {
  display: flex;
  gap: var(--fc-sp-4);
  color: var(--fc-neutral);
  font-size: var(--fc-fs-sm);
  margin-bottom: var(--fc-sp-3);
}

.mb {
  margin-bottom: var(--fc-sp-3);
}
</style>
