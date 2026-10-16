<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <div class="admin-brand">
        <el-icon :size="22"><Food /></el-icon>
        <span>美食社区 · 管理端</span>
      </div>
      <!-- 注意：default-openeds 必须用 : 绑定，否则传入的是字符串 "['info']"
           而非数组，Element Plus 无法解析，导致「信息管理」默认不展开。 -->
      <el-menu
        :default-active="activeMenu"
        :default-openeds="['info']"
        router
        class="admin-menu"
      >
        <el-menu-item index="/admin/dashboard">
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

/* 侧栏：深色壳 + 暖调偏移（与全局暖色系同源，避免冷硬割裂） */
.admin-aside {
  background: oklch(26% 0.014 55);
  display: flex;
  flex-direction: column;
  box-shadow: var(--fc-sh-sm);
}

.admin-brand {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fc-sp-2);
  color: var(--fc-ink-invert);
  font-size: var(--fc-fs-md);
  font-weight: 600;
  letter-spacing: -0.01em;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.admin-menu {
  flex: 1;
  border-right: none;
  background: transparent;
  padding: var(--fc-sp-3) var(--fc-sp-2);
  overflow-y: auto;
  --el-menu-text-color: rgba(255, 255, 255, 0.7);
  --el-menu-active-color: #ffffff;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
  --el-menu-bg-color: transparent;
}

.admin-menu .el-menu-item {
  color: rgba(255, 255, 255, 0.7);
  height: 42px;
  line-height: 42px;
  margin-bottom: var(--fc-sp-1);
  border-radius: var(--fc-r-sm);
  transition: background var(--fc-dur-fast) var(--fc-ease-standard),
              color var(--fc-dur-fast) var(--fc-ease-standard);
}

.admin-menu .el-menu-item:hover {
  color: #ffffff;
}

/* 激活态：此前引用的 --fc-primary 未定义 → 背景透明，高亮失效。
   现改用 accent，并加左侧指示条强化当前位置感。 */
.admin-menu .el-menu-item.is-active {
  background: var(--fc-accent);
  color: #ffffff;
  font-weight: 600;
  position: relative;
}

.admin-menu .el-menu-item.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  border-radius: var(--fc-r-full);
  background: #ffffff;
}

.admin-menu :deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.7);
  height: 42px;
  line-height: 42px;
  border-radius: var(--fc-r-sm);
}

.admin-menu :deep(.el-sub-menu__title:hover) {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.06);
}

.admin-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 var(--fc-sp-6);
  background: var(--fc-surface-2);
  border-bottom: 1px solid var(--fc-rule);
  flex-shrink: 0;
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

.user-entry .nickname {
  font-size: var(--fc-fs-base);
  color: var(--fc-ink);
}

/* 内容区：暖调底，与用户端同源 */
.admin-main {
  background: var(--fc-paper);
  padding: var(--fc-sp-5);
  overflow: auto;
}

/* 管理端表格统一呼吸感 */
.admin-main :deep(.el-table) {
  border-radius: var(--fc-r-md);
  overflow: hidden;
}

@media (max-width: 960px) {
  .admin-aside {
    width: 168px !important;
  }
}
</style>
