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
  height: 64px;
  padding: 0;
  background: color-mix(in oklab, var(--fc-surface-2) 88%, transparent);
  backdrop-filter: saturate(1.6) blur(12px);
  border-bottom: 1px solid var(--fc-rule);
  position: sticky;
  top: 0;
  z-index: var(--fc-z-header);
}

.header-inner {
  max-width: var(--fc-container);
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: var(--fc-sp-8);
  padding: 0 var(--fc-sp-4);
}

.brand {
  display: flex;
  align-items: center;
  gap: var(--fc-sp-2);
  color: var(--fc-accent);
  font-size: var(--fc-fs-xl);
  font-weight: 700;
  letter-spacing: -0.01em;
  white-space: nowrap;
}

.nav {
  flex: 1;
  display: flex;
  gap: var(--fc-sp-5);
}

/* 导航项：底部指示条，比单纯变色更有层级感 */
.nav-item {
  position: relative;
  padding: var(--fc-sp-1) 0;
  color: var(--fc-muted);
  font-size: var(--fc-fs-md);
  transition: color var(--fc-dur-fast) var(--fc-ease-standard);
}

.nav-item::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -6px;
  height: 2px;
  border-radius: var(--fc-r-full);
  background: var(--fc-accent);
  transform: scaleX(0);
  transition: transform var(--fc-dur-base) var(--fc-ease-standard);
}

.nav-item:hover {
  color: var(--fc-ink);
}

.nav-item.router-link-active {
  color: var(--fc-accent);
  font-weight: 600;
}

.nav-item.router-link-active::after {
  transform: scaleX(1);
}

.write-btn {
  --el-button-bg-color: var(--fc-accent);
  --el-button-border-color: var(--fc-accent);
  --el-button-hover-bg-color: var(--fc-accent-hover);
  --el-button-hover-border-color: var(--fc-accent-hover);
  --el-button-active-bg-color: var(--fc-accent-hover);
  --el-button-active-border-color: var(--fc-accent-hover);
  font-weight: 600;
  box-shadow: var(--fc-sh-xs);
}

.user-entry {
  display: flex;
  align-items: center;
  gap: var(--fc-sp-2);
  cursor: pointer;
  outline: none;
  padding: var(--fc-sp-1) var(--fc-sp-2);
  border-radius: var(--fc-r-full);
  transition: background var(--fc-dur-fast) var(--fc-ease-standard);
}

.user-entry:hover {
  background: var(--fc-surface);
}

.nickname {
  font-size: var(--fc-fs-base);
  color: var(--fc-ink);
  max-width: 8em;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.user-main {
  max-width: var(--fc-container);
  width: 100%;
  margin: 0 auto;
  padding: var(--fc-sp-6) var(--fc-sp-4) var(--fc-sp-12);
}

.chat-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fc-sp-4);
  min-width: 120px;
}

/* 窄屏：导航可横向滚动，避免换行挤压 */
@media (max-width: 860px) {
  .header-inner {
    gap: var(--fc-sp-4);
  }

  .nav {
    overflow-x: auto;
    scrollbar-width: none;
  }

  .nav::-webkit-scrollbar {
    display: none;
  }

  .nickname {
    display: none;
  }
}
</style>
