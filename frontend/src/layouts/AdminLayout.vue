<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <div class="admin-brand">
        <el-icon :size="22"><Food /></el-icon>
        <span>美食社区 · 管理端</span>
      </div>
      <el-menu :default-active="activeMenu" default-openeds="['info']" router class="admin-menu">        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>系统首页</span>
        </el-menu-item>

        <el-sub-menu index="info">
          <template #title>
            <el-icon><Management /></el-icon>
            <span>信息管理</span>
          </template>
          <el-menu-item index="/admin/categories">菜谱分类信息</el-menu-item>
          <el-menu-item index="/admin/recipes">菜谱笔记</el-menu-item>
          <el-menu-item index="/admin/comments">评论信息</el-menu-item>
          <el-menu-item index="/admin/store-visits">探店信息</el-menu-item>
          <el-menu-item index="/admin/reservations">探店预约信息</el-menu-item>
          <el-menu-item index="/admin/banners">轮播图信息</el-menu-item>
          <el-menu-item index="/admin/notices">公告信息</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="users">
          <template #title>
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </template>
          <el-menu-item index="/admin/admins">管理员信息</el-menu-item>
          <el-menu-item index="/admin/users">用户信息</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <el-dropdown @command="onCommand">
          <span class="user-entry">
            <el-avatar :size="30" :src="store.user?.avatar || undefined">
              {{ store.user?.nickname?.charAt(0) }}
            </el-avatar>
            <span class="nickname">{{ store.user?.nickname }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="home">回到用户端</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const activeMenu = computed(() => route.path)

function onCommand(cmd) {
  if (cmd === 'profile') router.push('/admin/profile')
  if (cmd === 'home') router.push('/home')
  if (cmd === 'logout') {
    store.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}

.admin-aside {
  background: #2b2f36;
  display: flex;
  flex-direction: column;
}

.admin-brand {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.admin-menu {
  flex: 1;
  border-right: none;
  background: transparent;
  --el-menu-text-color: #c9cdd4;
  --el-menu-active-color: #ffffff;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
}

.admin-menu .el-menu-item {
  color: #c9cdd4;
}

.admin-menu .el-menu-item.is-active {
  background: var(--fc-primary);
}

.admin-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.admin-main {
  background: #f5f6f8;
  padding: 20px;
  overflow: auto;
}
</style>
