<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索评论内容 / 评论者昵称" clearable class="search"
                @keyup.enter="load(1)" @clear="load(1)">
        <template #append><el-button @click="load(1)">搜索</el-button></template>
      </el-input>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="评论者" width="130">
        <template #default="{ row }">{{ row.authorNickname }}</template>
      </el-table-column>
      <el-table-column label="评论内容" min-width="240">
        <template #default="{ row }">
          <div class="content">
            <span v-if="row.replyUserId" class="at">回复 @{{ row.replyNickname || '?' }}</span>
            {{ row.content }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="recipeTitle" label="所属菜谱" min-width="150" />
      <el-table-column prop="createTime" label="评论时间" width="170" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="暂无评论" />

    <el-pagination v-if="total > query.size" class="pager" background layout="prev, pager, next"
                   :total="total" :page-size="query.size" :current-page="query.page" @current-change="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminCommentPage, adminRemoveComment } from '@/api/admin'

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
    const res = await adminCommentPage(params)
    rows.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

async function del(row) {
  await ElMessageBox.confirm('删除后将连同其所有回复一并删除，确定吗？', '删除评论', { type: 'warning' })
  await adminRemoveComment(row.id)
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

.content {
  white-space: pre-wrap;
}

.at {
  color: var(--fc-accent);
  font-size: 12px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
