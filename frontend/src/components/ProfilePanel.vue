<template>
  <div class="profile-page">
    <el-card shadow="never" class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人中心</span>
          <el-tag v-if="store.isAdmin" type="warning" size="small">管理员</el-tag>
          <el-tag v-else-if="store.user?.isExpert" type="success" size="small">探店达人</el-tag>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 基本资料 -->
        <el-tab-pane label="基本资料" name="profile">
          <el-form :model="profileForm" :rules="profileRules" ref="profileFormRef" label-width="90px" class="form-box">
            <el-form-item label="用户名">
              <el-input :model-value="store.user?.username" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="profileForm.nickname" maxlength="20" show-word-limit />
            </el-form-item>
            <el-form-item label="头像">
              <div class="avatar-row">
                <el-avatar :size="64" :src="profileForm.avatar || undefined">
                  {{ profileForm.nickname?.charAt(0) || '?' }}
                </el-avatar>
                <el-input v-model="profileForm.avatar" placeholder="头像图片 URL（M1 支持本地图片上传）" class="avatar-input" />
              </div>
            </el-form-item>
            <el-form-item label="个人简介">
              <el-input v-model="profileForm.intro" type="textarea" :rows="3" maxlength="255" show-word-limit
                        placeholder="介绍一下自己，会展示在你的主页上" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 修改密码 -->
        <el-tab-pane label="修改密码" name="password">
          <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="90px" class="form-box">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="savePassword">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMe, updateProfile, changePassword } from '@/api/user'
import { useUserStore } from '@/stores/user'

// 用户端 /profile 与管理端 /admin/profile 复用本组件（同一份资料接口）
const store = useUserStore()
const activeTab = ref('profile')
const saving = ref(false)
const profileFormRef = ref()
const pwdFormRef = ref()

const profileForm = reactive({ nickname: '', avatar: '', intro: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

const profileRules = {
  nickname: [{ required: true, message: '昵称不能为空', trigger: 'blur' }]
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需为 6-32 位', trigger: 'blur' }
  ]
}

onMounted(async () => {
  const { data } = await getMe()
  store.setUser(data)
  profileForm.nickname = data.nickname
  profileForm.avatar = data.avatar || ''
  profileForm.intro = data.intro || ''
})

async function saveProfile() {
  await profileFormRef.value.validate()
  saving.value = true
  try {
    const { data } = await updateProfile(profileForm)
    store.setUser(data)
    ElMessage.success('资料已保存')
  } finally {
    saving.value = false
  }
}

async function savePassword() {
  await pwdFormRef.value.validate()
  saving.value = true
  try {
    await changePassword(pwdForm)
    ElMessage.success('密码已修改，请重新登录')
    store.logout()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 640px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.form-box {
  max-width: 480px;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}
</style>
