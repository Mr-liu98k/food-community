<template>
  <el-container class="user-layout">
    <el-header class="user-header">
      <div class="header-inner">
        <router-link to="/home" class="brand">
          <el-icon :size="26"><Food /></el-icon>
          <span class="brand-name">美食社区</span>
        </router-link>

        <nav class="nav">
          <router-link to="/home" class="nav-item">首页</router-link>
          <router-link to="/recipes" class="nav-item">菜谱</router-link>
          <router-link to="/store-visits" class="nav-item">探店</router-link>
          <router-link to="/experts" class="nav-item">探店达人</router-link>
          <router-link v-if="store.isLogin" to="/my/recipes" class="nav-item">我的菜谱</router-link>
          <router-link v-if="store.isLogin" to="/ai" class="nav-item">AI 美食专家</router-link>
          <!-- M5 起接入：聊天 -->
        </nav>

        <el-button v-if="store.isLogin" type="primary" round class="write-btn" @click="router.push('/recipe/new')">
          ✍ 写菜谱
        </el-button>

        <el-dropdown v-if="store.isLogin" @command="onCommand">
          <span class="user-entry">
            <el-avatar :size="30" :src="store.user?.avatar || undefined">
              {{ store.user?.nickname?.charAt(0) }}
            </el-avatar>
            <span class="nickname">{{ store.user?.nickname }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="my-recipes">我的菜谱</el-dropdown-item>
              <el-dropdown-item command="my-store-visits">我的探店</el-dropdown-item>
              <el-dropdown-item command="reservations">探店预约</el-dropdown-item>
              <el-dropdown-item command="favorites">我的收藏</el-dropdown-item>
              <el-dropdown-item command="comments">我的评论</el-dropdown-item>
              <el-dropdown-item command="notices">系统公告</el-dropdown-item>
              <el-dropdown-item command="chat">
                <div class="chat-entry">
                  <span>聊天</span>
                  <el-badge :value="chatStore.unreadTotal" :hidden="!chatStore.unreadTotal" :max="99" />
                </div>
              </el-dropdown-item>
              <el-dropdown-item divided command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item v-if="store.isAdmin" command="admin">管理后台</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button v-else type="primary" round @click="router.push('/login')">登录 / 注册</el-button>
      </div>
    </el-header>

    <el-main class="user-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'

const router = useRouter()
const store = useUserStore()
const chatStore = useChatStore()

function onCommand(cmd) {
  if (cmd === 'my-recipes') router.push('/my/recipes')
  if (cmd === 'my-store-visits') router.push('/my/store-visits')
  if (cmd === 'reservations') router.push('/my/reservations')
  if (cmd === 'favorites') router.push('/my/favorites')
  if (cmd === 'comments') router.push('/my/comments')
  if (cmd === 'chat') router.push('/chat')
  if (cmd === 'notices') router.push('/notices')
  if (cmd === 'profile') router.push('/profile')
  if (cmd === 'admin') router.push('/admin/dashboard')
  if (cmd === 'logout') {
    store.logout()
    router.push('/login')
  }
}

// 未读红点：登录后每 20s 轮询一次
let unreadTimer = null
onMounted(() => {
  if (store.isLogin) {
    chatStore.refreshUnread()
    unreadTimer = setInterval(() => chatStore.refreshUnread(), 20000)
  }
})
onUnmounted(() => unreadTimer && clearInterval(unreadTimer))
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
}

.user-header {
  height: 60px;
  padding: 0;
  background: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-inner {
  max-width: 1100px;
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 0 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--fc-primary);
  font-size: 20px;
  font-weight: 700;
}

.nav {
  flex: 1;
  display: flex;
  gap: 20px;
}

.nav-item {
  color: #606266;
  font-size: 15px;
}

.nav-item.router-link-active {
  color: var(--fc-primary);
  font-weight: 600;
}

.write-btn {
  --el-button-bg-color: var(--fc-accent);
  --el-button-border-color: var(--fc-accent);
  --el-button-hover-bg-color: var(--fc-accent);
  --el-button-hover-border-color: var(--fc-accent);
  font-weight: 600;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.nickname {
  font-size: 14px;
  color: #303133;
}

.user-main {
  max-width: 1100px;
  width: 100%;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.chat-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-width: 120px;
}
</style>
