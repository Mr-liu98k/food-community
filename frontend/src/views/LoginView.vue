<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-title">
        <div class="brand-mark">
          <el-icon :size="30"><Food /></el-icon>
        </div>
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
  padding: var(--fc-sp-6) var(--fc-sp-4);
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 15% 15%, oklch(88% 0.07 62 / 0.5), transparent 45%),
    radial-gradient(circle at 85% 85%, oklch(90% 0.06 80 / 0.55), transparent 45%),
    var(--fc-paper);
}

/* 背景装饰：大号食物意象，低透明度做氛围，不干扰内容 */
.login-page::before,
.login-page::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  pointer-events: none;
}

.login-page::before {
  width: 420px;
  height: 420px;
  top: -140px;
  right: -120px;
  background: oklch(80% 0.1 55 / 0.22);
}

.login-page::after {
  width: 360px;
  height: 360px;
  bottom: -130px;
  left: -110px;
  background: oklch(84% 0.09 75 / 0.22);
}

.login-card {
  position: relative;
  z-index: 1;
  width: min(420px, 100%);
  background: var(--fc-surface-2);
  border: 1px solid var(--fc-rule);
  border-radius: var(--fc-r-lg);
  padding: var(--fc-sp-10) var(--fc-sp-10) var(--fc-sp-6);
  box-shadow: var(--fc-sh-lg);
}

.login-title {
  text-align: center;
  margin-bottom: var(--fc-sp-6);
}

/* 品牌标识：暖色渐变块，比裸图标更有识别度 */
.brand-mark {
  width: 56px;
  height: 56px;
  margin: 0 auto var(--fc-sp-3);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--fc-r-md);
  color: #fff;
  background: linear-gradient(135deg, oklch(72% 0.15 62), var(--fc-accent));
  box-shadow: var(--fc-sh-accent);
}

.login-title h1 {
  margin: 0 0 var(--fc-sp-1);
  font-size: var(--fc-fs-2xl);
  color: var(--fc-ink);
}

.login-title p {
  margin: 0;
  color: var(--fc-neutral);
  font-size: var(--fc-fs-sm);
  letter-spacing: 0.02em;
}

.submit-btn {
  width: 100%;
  margin-top: var(--fc-sp-1);
  height: 44px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

/* 表单与 tab 的间距节奏 */
.login-card :deep(.el-tabs__header) {
  margin-bottom: var(--fc-sp-6);
}

.login-card :deep(.el-form-item) {
  margin-bottom: var(--fc-sp-5);
}

@media (max-width: 480px) {
  .login-card {
    padding: var(--fc-sp-6) var(--fc-sp-5) var(--fc-sp-4);
  }
}
</style>
