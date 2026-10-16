<template>
  <div class="img-uploader">
    <div v-if="modelValue" class="preview">
      <img :src="modelValue" alt="预览图" />
      <div class="mask">
        <el-icon @click="remove"><Delete /></el-icon>
      </div>
    </div>
    <el-upload
      v-else
      :show-file-list="false"
      :http-request="doUpload"
      accept="image/jpeg,image/png,image/gif,image/webp"
    >
      <div class="placeholder">
        <el-icon :size="20"><Plus /></el-icon>
        <span>上传图片</span>
      </div>
    </el-upload>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const props = defineProps({
  modelValue: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])

async function doUpload({ file }) {
  const fd = new FormData()
  fd.append('file', file)
  try {
    const res = await request.post('/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    emit('update:modelValue', res.data)
    ElMessage.success('上传成功')
  } catch (e) {
    // 错误提示已由 request 拦截器统一处理
  }
}

function remove() {
  emit('update:modelValue', '')
}
</script>

<style scoped>
.img-uploader {
  display: inline-block;
}

.preview {
  position: relative;
  width: 160px;
  height: 120px;
  border-radius: var(--fc-r-sm);
  overflow: hidden;
  border: 1px solid var(--fc-rule);
}

.preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  opacity: 0;
  transition: opacity 0.2s;
  color: #fff;
  font-size: var(--fc-fs-xl);
  cursor: pointer;
}

.preview:hover .mask {
  opacity: 1;
}

.placeholder {
  width: 160px;
  height: 120px;
  border: 1px dashed var(--fc-rule);
  border-radius: var(--fc-r-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--fc-sp-1-5);
  color: var(--fc-neutral);
  font-size: var(--fc-fs-sm);
  background: var(--fc-paper);
  cursor: pointer;
}

.placeholder:hover {
  border-color: var(--fc-accent);
  color: var(--fc-accent);
}
</style>
