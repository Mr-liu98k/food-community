<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">+ 新增轮播图</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column label="轮播图" width="200">
        <template #default="{ row }">
          <div class="thumb"><RecipeCover :src="row.image" :title="row.recipeTitle" :seed="row.id" /></div>
        </template>
      </el-table-column>
      <el-table-column label="关联菜谱" min-width="200">
        <template #default="{ row }">{{ row.recipeTitle }}</template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="暂无轮播图" />

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑轮播图' : '新增轮播图'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="轮播图片">
          <ImageUploader v-model="form.image" />
        </el-form-item>
        <el-form-item label="关联菜谱" required>
          <el-select v-model="form.recipeId" filterable placeholder="选择已审核通过的菜谱（支持搜索）" style="width: 100%">
            <el-option v-for="r in recipeOptions" :key="r.id" :label="`${r.title}（${r.authorNickname}）`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
          <span class="hint">数字越小越靠前，首页会跳到关联菜谱详情</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!form.recipeId" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminRecipePage } from '@/api/admin'
import {
  adminBannerPage, adminCreateBanner, adminUpdateBanner, adminRemoveBanner
} from '@/api/admin'
import RecipeCover from '@/components/RecipeCover.vue'
import ImageUploader from '@/components/ImageUploader.vue'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const recipeOptions = ref([])
const query = reactive({ page: 1, size: 10 })
const form = reactive({ id: null, recipeId: null, image: '', sort: 0 })

async function load(p) {
  query.page = p
  loading.value = true
  try {
    const res = await adminBannerPage({ page: query.page, size: query.size })
    rows.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

async function loadRecipeOptions() {
  const res = await adminRecipePage({ page: 1, size: 200, status: 1 })
  recipeOptions.value = res.data.records
}

function openDialog(row) {
  if (row) {
    Object.assign(form, { id: row.id, recipeId: row.recipeId, image: row.image || '', sort: row.sort })
  } else {
    Object.assign(form, { id: null, recipeId: null, image: '', sort: 0 })
  }
  loadRecipeOptions()
  dialogVisible.value = true
}

async function save() {
  if (!form.recipeId) return ElMessage.warning('请选择要关联的菜谱')
  saving.value = true
  try {
    if (form.id) {
      await adminUpdateBanner(form.id, { recipeId: form.recipeId, image: form.image, sort: form.sort })
      ElMessage.success('已更新')
    } else {
      await adminCreateBanner({ recipeId: form.recipeId, image: form.image, sort: form.sort })
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    load(query.page)
  } finally {
    saving.value = false
  }
}

async function del(row) {
  await ElMessageBox.confirm('确定删除这张轮播图吗？', '删除确认', { type: 'warning' })
  await adminRemoveBanner(row.id)
  ElMessage.success('已删除')
  load(query.page)
}

onMounted(() => load(1))
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: var(--fc-sp-3-5);
}

.thumb {
  width: 120px;
  height: 52px;
  border-radius: var(--fc-r-xs);
  overflow: hidden;
}

.pager {
  margin-top: var(--fc-sp-4);
  justify-content: flex-end;
}

.hint {
  margin-left: var(--fc-sp-2-5);
  color: var(--fc-neutral);
  font-size: var(--fc-fs-xs);
}
</style>
