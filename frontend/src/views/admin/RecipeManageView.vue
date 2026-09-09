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
      <el-select v-model="query.categoryId" placeholder="分类" clearable class="w140" @change="load(1)">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column label="菜谱" min-width="200">
        <template #default="{ row }">
          <div class="t-title">
            <div class="mini-cover"><RecipeCover :src="row.cover" :title="row.title" :seed="row.id" /></div>
            <span>{{ row.title }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="110" />
      <el-table-column label="作者" width="140">
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
      <el-table-column prop="createTime" label="提交时间" width="165" />
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="preview(row)">预览</el-button>
          <el-button v-if="row.status === 0" link type="success" @click="doAudit(row, 1)">通过</el-button>
          <el-button v-if="row.status === 0" link type="danger" @click="doAudit(row, 2)">拒绝</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-if="total > query.size" class="pager" background layout="prev, pager, next"
                   :total="total" :page-size="query.size" :current-page="query.page" @current-change="load" />

    <!-- 预览弹窗 -->
    <el-dialog v-model="previewVisible" title="菜谱完整预览" width="720px" top="4vh">
      <div v-loading="previewLoading" class="pv" v-if="current">
        <div class="pv-cover"><RecipeCover :src="current.recipe.cover" :title="current.recipe.title" :seed="current.recipe.id" /></div>
        <h2 class="pv-title">{{ current.recipe.title }}</h2>
        <div class="pv-meta">
          <span>作者：{{ current.recipe.authorNickname }}</span>
          <span>分类：{{ current.recipe.categoryName }}</span>
          <span>浏览量：{{ current.recipe.views }}</span>
          <span>发布于：{{ current.recipe.createTime }}</span>
        </div>
        <el-alert v-if="current.recipe.status === 2" type="error" :closable="false" class="mb"
                  :title="`拒绝理由：${current.recipe.rejectReason}`" />
        <div v-if="current.recipe.description" class="pv-sec">
          <h3>关于这道菜</h3>
          <MarkdownContent :content="current.recipe.description" />
        </div>
        <div class="pv-sec">
          <h3>食材清单</h3>
          <ul class="pv-ings">
            <li v-for="i in current.ingredients" :key="i.id">{{ i.name }} <b>{{ i.amount }}</b></li>
          </ul>
        </div>
        <div class="pv-sec">
          <h3>制作步骤</h3>
          <ol class="pv-steps">
            <li v-for="s in current.steps" :key="s.id"><MarkdownContent :content="s.content" /></li>
          </ol>
        </div>
      </div>
      <template #footer>
        <template v-if="current && current.recipe.status === 0">
          <el-button type="success" @click="doAudit(current.recipe, 1, true)">审核通过</el-button>
          <el-button type="danger" @click="promptReject(current.recipe)">审核拒绝</el-button>
        </template>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategories } from '@/api/category'
import {
  adminRecipePage, adminRecipePreview, adminAuditRecipe, adminRemoveRecipe
} from '@/api/admin'
import RecipeCover from '@/components/RecipeCover.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'

const rows = ref([])
const categories = ref([])
const total = ref(0)
const loading = ref(false)
const previewVisible = ref(false)
const previewLoading = ref(false)
const current = ref(null)

const query = reactive({ page: 1, size: 10, keyword: '', status: null, categoryId: null })

async function load(p) {
  query.page = p
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== null && query.status !== undefined && query.status !== '') params.status = query.status
    if (query.categoryId) params.categoryId = query.categoryId
    const res = await adminRecipePage(params)
    rows.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

async function preview(row) {
  current.value = null
  previewVisible.value = true
  previewLoading.value = true
  try {
    const res = await adminRecipePreview(row.id)
    current.value = res.data
  } finally {
    previewLoading.value = false
  }
}

async function doAudit(row, status, closePreview = false) {
  if (status === 2) {
    return promptReject(row, closePreview)
  }
  await ElMessageBox.confirm(`确定通过菜谱「${row.title}」的审核吗？`, '审核通过', { type: 'success' })
  await adminAuditRecipe(row.id, { status: 1 })
  ElMessage.success('已通过审核')
  afterAudit(closePreview)
}

async function promptReject(row, closePreview = false) {
  const { value } = await ElMessageBox.prompt('拒绝理由将展示给发布者', '审核拒绝', {
    inputPlaceholder: '请填写拒绝理由（必填）',
    inputValidator: (v) => (v && v.trim() ? true : '拒绝必须填写理由'),
    type: 'warning'
  })
  await adminAuditRecipe(row.id, { status: 2, reason: value.trim() })
  ElMessage.success('已拒绝')
  afterAudit(closePreview)
}

function afterAudit(closePreview) {
  if (closePreview) previewVisible.value = false
  load(query.page)
}

async function del(row) {
  await ElMessageBox.confirm(`确定删除菜谱「${row.title}」吗？将同时删除其食材、步骤及互动数据。`, '删除确认', { type: 'warning' })
  await adminRemoveRecipe(row.id)
  ElMessage.success('已删除')
  load(query.page)
}

onMounted(async () => {
  const res = await getCategories()
  categories.value = res.data
  load(1)
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.search { width: 240px; }
.w140 { width: 140px; }

.t-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mini-cover {
  width: 52px;
  height: 38px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.pv-cover {
  height: 200px;
  border-radius: 10px;
  overflow: hidden;
}

.pv-title {
  margin: 14px 0 6px;
}

.pv-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: #777;
  font-size: 13px;
  margin-bottom: 12px;
}

.mb {
  margin-bottom: 12px;
}

.pv-sec h3 {
  font-size: 16px;
  margin: 14px 0 8px;
}

.pv-ings {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px 24px;
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 14px;
}

.pv-ings b {
  color: var(--fc-accent);
}

.pv-steps {
  padding-left: 1.3em;
  line-height: 1.8;
  margin: 0;
}
</style>
