<template>
  <div class="my-recipe-page">
    <div class="head">
      <div>
        <h1>我的菜谱</h1>
        <p>共 {{ total }} 道 · 修改后需管理员重新审核</p>
      </div>
      <el-button type="primary" round @click="router.push('/recipe/new')">
        <el-icon style="margin-right: 4px"><Plus /></el-icon>写菜谱
      </el-button>
    </div>

    <div class="tabs">
      <span v-for="t in tabs" :key="t.value" class="tab" :class="{ active: status === t.value }"
            @click="switchStatus(t.value)">
        {{ t.label }}
      </span>
    </div>

    <div v-loading="loading">
      <el-table v-if="rows.length" :data="rows" style="width: 100%" class="table">
        <el-table-column label="菜谱" min-width="220">
          <template #default="{ row }">
            <div class="t-title">
              <div class="mini-cover"><RecipeCover :src="row.cover" :title="row.title" :seed="row.id" /></div>
              <span>{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tooltip v-if="row.status === 2" :content="row.rejectReason" placement="top">
              <el-tag type="danger" size="small" effect="light">已拒绝</el-tag>
            </el-tooltip>
            <el-tag v-else-if="row.status === 0" type="warning" size="small" effect="light">审核中</el-tag>
            <el-tag v-else type="success" size="small" effect="light">已通过</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="浏览 / 点赞" width="120">
          <template #default="{ row }">{{ row.views }} / {{ row.likesCount }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/recipe/${row.id}`)">查看</el-button>
            <el-button link type="primary" @click="router.push(`/my/recipes/${row.id}/edit`)">编辑</el-button>
            <el-button link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="还没有菜谱，快写一道吧" />
    </div>

    <el-pagination v-if="total > pageSize" class="pager" background layout="prev, pager, next" :total="total"
                   :page-size="pageSize" :current-page="page" @current-change="load" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { myRecipes, removeRecipe } from '@/api/recipe'
import RecipeCover from '@/components/RecipeCover.vue'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const status = ref(null)
const loading = ref(false)

const tabs = [
  { label: '全部', value: null },
  { label: '审核中', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已拒绝', value: 2 }
]

function switchStatus(v) {
  status.value = v
  load(1)
}

async function load(p) {
  page.value = p
  loading.value = true
  try {
    const params = { page: p, size: pageSize }
    if (status.value !== null) params.status = status.value
    const res = await myRecipes(params)
    rows.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

async function del(row) {
  await ElMessageBox.confirm(`确定删除菜谱「${row.title}」吗？删除后不可恢复。`, '删除确认', { type: 'warning' })
  await removeRecipe(row.id)
  ElMessage.success('已删除')
  load(page.value)
}

onMounted(() => load(1))
</script>

<style scoped>
.my-recipe-page {
  max-width: 900px;
  margin: 0 auto;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 18px;
}

.head h1 {
  margin: 0 0 4px;
  font-size: 28px;
}

.head p {
  margin: 0;
  color: var(--fc-neutral);
  font-size: 13px;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.tab {
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  cursor: pointer;
  color: var(--fc-neutral);
  background: var(--fc-surface);
}

.tab.active {
  background: var(--fc-ink);
  color: oklch(97% 0.012 70);
}

.t-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mini-cover {
  width: 56px;
  height: 40px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}

.pager {
  margin-top: 20px;
  justify-content: center;
}
</style>
