<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="按标题搜索" clearable class="search"
                @keyup.enter="load(1)" @clear="load(1)">
        <template #append><el-button @click="load(1)">搜索</el-button></template>
      </el-input>
      <el-button type="primary" @click="openDialog()">+ 发布公告</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="createTime" label="发布时间" width="170" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="preview(row)">预览</el-button>
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="暂无公告" />

    <el-pagination v-if="total > query.size" class="pager" background layout="prev, pager, next"
                   :total="total" :page-size="query.size" :current-page="query.page" @current-change="load" />

    <!-- 预览 -->
    <el-dialog v-model="previewVisible" :title="previewRow?.title" width="600px">
      <div class="meta-line">{{ previewRow?.createTime }}</div>
      <MarkdownContent :content="previewRow?.content || ''" />
    </el-dialog>

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '发布公告'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="支持 Markdown" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminNoticePage, adminCreateNotice, adminUpdateNotice, adminRemoveNotice
} from '@/api/admin'
import MarkdownContent from '@/components/MarkdownContent.vue'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const previewVisible = ref(false)
const previewRow = ref(null)
const formRef = ref()

const query = reactive({ page: 1, size: 10, keyword: '' })
const form = reactive({ id: null, title: '', content: '' })

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

async function load(p) {
  query.page = p
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    const res = await adminNoticePage(params)
    rows.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

function preview(row) {
  previewRow.value = row
  previewVisible.value = true
}

function openDialog(row) {
  if (row) {
    Object.assign(form, { id: row.id, title: row.title, content: row.content })
  } else {
    Object.assign(form, { id: null, title: '', content: '' })
  }
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await adminUpdateNotice(form.id, { title: form.title, content: form.content })
      ElMessage.success('公告已更新')
    } else {
      await adminCreateNotice({ title: form.title, content: form.content })
      ElMessage.success('公告已发布')
    }
    dialogVisible.value = false
    load(query.page)
  } finally {
    saving.value = false
  }
}

async function del(row) {
  await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '删除确认', { type: 'warning' })
  await adminRemoveNotice(row.id)
  ElMessage.success('已删除')
  load(query.page)
}

onMounted(() => load(1))
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 14px;
}

.search {
  width: 260px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.meta-line {
  color: #999;
  font-size: 12px;
  margin-bottom: 10px;
}
</style>
