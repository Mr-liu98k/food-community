<template>
  <div class="sv-edit">
    <div class="page-title">
      <h1>{{ isEdit ? '编辑探店' : '发布探店' }}</h1>
      <p>{{ isEdit ? '修改后需重新提交审核' : '发布后由管理员审核，通过后展示给全社区' }}</p>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" v-loading="loading" class="card">
      <el-form-item label="探店封面">
        <ImageUploader v-model="form.cover" />
      </el-form-item>
      <el-form-item label="探店标题" prop="title">
        <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="如：巷口的宝藏面馆，人均 20 吃到扶墙" />
      </el-form-item>
      <el-form-item label="店铺地址" prop="address">
        <el-input v-model="form.address" maxlength="255" placeholder="方便想去打卡的人找到（选填）" />
      </el-form-item>
      <el-form-item label="探店内容" prop="content">
        <el-input v-model="form.content" type="textarea" :rows="10" placeholder="写写店铺体验、推荐菜、人均消费、避坑提示…（支持 Markdown）" />
      </el-form-item>
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
import { createStoreVisit, updateStoreVisit } from '@/api/storeVisit'
import ImageUploader from '@/components/ImageUploader.vue'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const saving = ref(false)

const id = computed(() => route.params.id || null)
const isEdit = computed(() => !!id.value)

const form = reactive({ title: '', cover: '', content: '', address: '' })

const rules = {
  title: [{ required: true, message: '请输入探店标题', trigger: 'blur' }],
  content: [{ required: true, message: '请填写探店内容', trigger: 'blur' }]
}

onMounted(async () => {
  if (!isEdit.value) return
  loading.value = true
  try {
    const res = await request.get(`/my/store-visit/${id.value}`)
    Object.assign(form, {
      title: res.data.title,
      cover: res.data.cover || '',
      content: res.data.content || '',
      address: res.data.address || ''
    })
  } finally {
    loading.value = false
  }
})

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { title: form.title, cover: form.cover, content: form.content, address: form.address }
    if (isEdit.value) {
      await updateStoreVisit(id.value, payload)
      ElMessage.success('修改已提交，等待重新审核')
    } else {
      await createStoreVisit(payload)
      ElMessage.success('发布成功，等待管理员审核')
    }
    router.push('/my/store-visits')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.sv-edit {
  max-width: var(--fc-page-narrow);
  margin: 0 auto;
}

.page-title h1 {
  margin: 0 0 var(--fc-sp-1-5);
  font-size: var(--fc-fs-2xl);
}

.page-title p {
  color: var(--fc-neutral);
  margin: 0 0 18px;
}

.card {
  background: oklch(99% 0.006 70);
  border: 1px solid var(--fc-rule);
  border-radius: var(--fc-r-md);
  padding: 22px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--fc-sp-3);
}
</style>
