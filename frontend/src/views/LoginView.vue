<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-title">
        <el-icon :size="34" color="#e8734a"><Food /></el-icon>
        <h1>美食社区</h1>
        <p>分享菜谱 · 探店打卡 · AI 美食问答</p>
      </div>

      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="rules" label-position="top" @keyup.enter="doLogin">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" size="large" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" size="large" show-password :prefix-icon="Lock" />
            </el-form-item>
            <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="doLogin">
              登 录
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top" @keyup.enter="doRegister">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="用户名（4-20 位字母/数字/下划线）" size="large" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="昵称（选填）" size="large" :prefix-icon="Postcard" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码（6-32 位）" size="large" show-password :prefix-icon="Lock" />
            </el-form-item>
            <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="doRegister">
              注 册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Postcard } from '@element-plus/icons-vue'
import { login, register } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const activeTab = ref('login')
const loading = ref(false)
const loginFormRef = ref()
const registerFormRef = ref()

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', nickname: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,20}$/, message: '4-20 位字母、数字或下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需为 6-32 位', trigger: 'blur' }
  ]
}

function goHome(user) {
  const redirect = route.query.redirect
  if (redirect && redirect.startsWith('/')) {
    router.push(redirect)
  } else {
    router.push(user.role === 1 ? '/admin/dashboard' : '/home')
  }
}

async function doLogin() {
  await loginFormRef.value.validate()
  loading.value = true
  try {
    const { data } = await login(loginForm)
    store.setLogin(data.token, data.user)
    ElMessage.success('登录成功')
    goHome(data.user)
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  await registerFormRef.value.validate()
  loading.value = true
  try {
    await register(registerForm)
    ElMessage.success('注册成功，正在登录…')
    const { data } = await login({ username: registerForm.username, password: registerForm.password })
    store.setLogin(data.token, data.user)
    goHome(data.user)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 20% 20%, rgba(232, 115, 74, 0.15), transparent 40%),
    radial-gradient(circle at 80% 80%, rgba(240, 180, 90, 0.18), transparent 40%),
    #f7f6f3;
}

.login-card {
  width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 36px 40px 28px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
}

.login-title {
  text-align: center;
  margin-bottom: 16px;
}

.login-title h1 {
  margin: 8px 0 4px;
  font-size: 26px;
  color: #2b2f36;
}

.login-title p {
  margin: 0 0 12px;
  color: #909399;
  font-size: 13px;
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
}
</style>
