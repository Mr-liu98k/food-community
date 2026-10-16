<template>
  <div class="my-comment-page">
    <div class="head">
      <div>
        <h1>我的评论</h1>
        <p>共 {{ total }} 条</p>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" style="width: 100%">
      <el-table-column label="评论内容" min-width="260">
        <template #default="{ row }">
          <div class="content" :title="row.content">{{ row.content }}</div>
          <div class="reply-flag" v-if="row.replyUserId">回复了该菜谱下的评论</div>
        </template>
      </el-table-column>
      <el-table-column label="关联菜谱" min-width="160">
        <template #default="{ row }">
          <router-link :to="`/recipe/${row.recipeId}`" class="r-link">{{ row.recipeTitle }}</router-link>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="170" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="还没有评论过" />

    <el-pagination v-if="total > pageSize" class="pager" background layout="prev, pager, next" :total="total"
                   :page-size="pageSize" :current-page="page" @current-change="load" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { myComments, removeComment } from '@/api/interaction'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)

async function load(p) {
  page.value = p
  loading.value = true
  try {
    const res = await myComments({ page: p, size: pageSize })
    rows.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

async function del(row) {
  await ElMessageBox.confirm('删除后将连同其下所有回复一并删除，确定吗？', '删除评论', { type: 'warning' })
  await removeComment(row.id)
  ElMessage.success('已删除')
  load(page.value)
}

onMounted(() => load(1))
</script>

<style scoped>
.my-comment-page {
  max-width: var(--fc-page);
  margin: 0 auto;
}

.head {
  margin-bottom: var(--fc-sp-4);
}

.head h1 {
  margin: 0 0 var(--fc-sp-1);
  font-size: var(--fc-fs-2xl);
}

.head p {
  margin: 0;
  color: var(--fc-neutral);
  font-size: var(--fc-fs-sm);
}

.content {
  white-space: pre-wrap;
  word-break: break-word;
}

.reply-flag {
  font-size: var(--fc-fs-xs);
  color: var(--fc-neutral);
  margin-top: var(--fc-sp-0-5);
}

.r-link {
  color: var(--fc-accent);
}

.r-link:hover {
  text-decoration: underline;
}

.pager {
  margin-top: 18px;
  justify-content: center;
}
</style>
