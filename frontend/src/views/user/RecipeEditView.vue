<template>
  <div class="edit-page">
    <div class="page-title">
      <h1>{{ isEdit ? '编辑菜谱' : '写一道菜谱' }}</h1>
      <p>{{ isEdit ? '修改后需重新提交审核' : '发布后由管理员审核，通过后展示给全社区' }}</p>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" v-loading="loading">
      <!-- 基本信息 -->
      <section class="card">
        <h2>基本信息</h2>
        <div class="basic-row">
          <el-form-item label="菜谱封面" prop="cover" class="cover-item">
            <ImageUploader v-model="form.cover" />
          </el-form-item>
          <div class="basic-fields">
            <el-form-item label="菜谱标题" prop="title">
              <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="给这道菜起个名字" />
            </el-form-item>
            <el-form-item label="所属分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </div>
        </div>
        <el-form-item label="关于这道菜（可选，支持 Markdown）" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="讲讲故事、口味特点、小贴士…" />
        </el-form-item>
      </section>

      <!-- 食材清单 -->
      <section class="card">
        <div class="card-head">
          <h2>食材清单</h2>
          <el-button type="primary" plain size="small" @click="addIngredient">+ 添加食材</el-button>
        </div>
        <div v-for="(ing, i) in form.ingredients" :key="i" class="row-line">
          <el-input v-model="ing.name" placeholder="食材名称，如：鸡蛋" class="w-name" />
          <el-input v-model="ing.amount" placeholder="用量，如：2 个" class="w-amount" />
          <el-button text type="danger" @click="form.ingredients.splice(i, 1)">删除</el-button>
        </div>
        <el-empty v-if="!form.ingredients.length" description="还没有食材" :image-size="60" />
      </section>

      <!-- 制作步骤 -->
      <section class="card">
        <div class="card-head">
          <h2>制作步骤</h2>
          <el-button type="primary" plain size="small" @click="addStep">+ 添加步骤</el-button>
        </div>
        <div v-for="(step, i) in form.steps" :key="i" class="step-line">
          <span class="step-no">{{ i + 1 }}</span>
          <el-input v-model="step.content" type="textarea" :rows="2" placeholder="这一步要做什么？" />
          <el-button text type="danger" @click="form.steps.splice(i, 1)">删除</el-button>
        </div>
        <el-empty v-if="!form.steps.length" description="还没有步骤" :image-size="60" />
      </section>

      <div class="actions">
        <el-button size="large" @click="router.back()">取消</el-button>
        <el-button size="large" type="primary" :loading="saving" @click="submit">提交{{ isEdit ? '修改' : '审核' }}</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { getCategories } from '@/api/category'
import { createRecipe, updateRecipe } from '@/api/recipe'
import ImageUploader from '@/components/ImageUploader.vue'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const categories = ref([])
const loading = ref(false)
const saving = ref(false)

const recipeId = computed(() => route.params.id || null)
const isEdit = computed(() => !!recipeId.value)

const form = reactive({
  title: '',
  cover: '',
  categoryId: null,
  description: '',
  ingredients: [{ name: '', amount: '' }],
  steps: [{ content: '' }]
})

const rules = {
  title: [{ required: true, message: '请输入菜谱标题', trigger: 'blur' }],
  cover: [{ required: true, message: '请上传封面图', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

function addIngredient() { form.ingredients.push({ name: '', amount: '' }) }
function addStep() { form.steps.push({ content: '' }) }

async function loadDetail() {
  loading.value = true
  try {
    const res = await request.get(`/my/recipe/${recipeId.value}`)
    const r = res.data.recipe
    form.title = r.title
    form.cover = r.cover
    form.categoryId = r.categoryId
    form.description = r.description || ''
    form.ingredients = res.data.ingredients.map((x) => ({ name: x.name, amount: x.amount }))
    form.steps = res.data.steps.map((x) => ({ content: x.content }))
  } finally {
    loading.value = false
  }
}

async function submit() {
  await formRef.value.validate()
  const payload = {
    categoryId: form.categoryId,
    title: form.title,
    cover: form.cover,
    description: form.description,
    ingredients: form.ingredients.filter((x) => x.name.trim() && x.amount.trim()),
    steps: form.steps.filter((x) => x.content.trim())
  }
  if (!payload.ingredients.length) return ElMessage.warning('请至少填写一种食材')
  if (!payload.steps.length) return ElMessage.warning('请至少填写一个制作步骤')

  saving.value = true
  try {
    if (isEdit.value) {
      await updateRecipe(recipeId.value, payload)
      ElMessage.success('修改已提交，等待管理员重新审核')
    } else {
      await createRecipe(payload)
      ElMessage.success('发布成功，等待管理员审核')
    }
    router.push('/my/recipes')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const res = await getCategories()
  categories.value = res.data
  if (isEdit.value) {
    loadDetail()
  }
})
</script>

<style scoped>
.edit-page {
  max-width: 760px;
  margin: 0 auto;
}

.page-title h1 {
  margin: 0 0 6px;
  font-size: 30px;
}

.page-title p {
  color: var(--fc-neutral);
  margin: 0 0 18px;
}

.card {
  background: oklch(99% 0.006 70);
  border: 1px solid var(--fc-rule);
  border-radius: 12px;
  padding: 20px 22px;
  margin-bottom: 16px;
}

.card h2 {
  margin: 0 0 14px;
  font-size: 18px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.card-head h2 {
  margin: 0;
}

.basic-row {
  display: flex;
  gap: 22px;
}

.basic-fields {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.row-line {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.w-name { flex: 1.4; }
.w-amount { flex: 1; }

.step-line {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 10px;
}

.step-no {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--fc-ink);
  color: oklch(97% 0.012 70);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  margin-top: 2px;
}

.step-line .el-textarea { flex: 1; }

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}
</style>
