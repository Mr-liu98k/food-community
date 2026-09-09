import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

/**
 * 路由设计（单工程双布局）：
 *  - 用户端：/（UserLayout）：home 首页 / recipes 菜谱 / recipe/:id 详情 / recipe/new 发布
 *            / my/recipes 我的菜谱 / my/recipes/:id/edit 编辑 / profile 个人中心
 *  - 管理端：/admin（AdminLayout，仅管理员）：dashboard 看板 / categories / recipes / profile
 *  - /login 公开；其余需登录；adminOnly 额外校验角色
 */
const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/UserLayout.vue'),
    redirect: '/home',
    children: [
      { path: 'home', name: 'home', component: () => import('@/views/user/HomeView.vue') },
      { path: 'recipes', name: 'recipe-list', component: () => import('@/views/user/RecipeListView.vue') },
      { path: 'recipe/new', name: 'recipe-new', component: () => import('@/views/user/RecipeEditView.vue') },
      { path: 'recipe/:id', name: 'recipe-detail', component: () => import('@/views/user/RecipeDetailView.vue') },
      { path: 'user/:id', name: 'user-profile', component: () => import('@/views/user/UserProfileView.vue') },
      { path: 'my/recipes', name: 'my-recipes', component: () => import('@/views/user/MyRecipeView.vue') },
      {
        path: 'my/recipes/:id/edit',
        name: 'recipe-edit',
        component: () => import('@/views/user/RecipeEditView.vue')
      },
      { path: 'my/favorites', name: 'my-favorites', component: () => import('@/views/user/MyFavoriteView.vue') },
      { path: 'my/comments', name: 'my-comments', component: () => import('@/views/user/MyCommentView.vue') },
      { path: 'my/store-visits', name: 'my-store-visits', component: () => import('@/views/user/MyStoreVisitView.vue') },
      {
        path: 'my/store-visits/:id/edit',
        name: 'store-visit-edit',
        component: () => import('@/views/user/StoreVisitEditView.vue')
      },
      { path: 'my/reservations', name: 'my-reservations', component: () => import('@/views/user/MyReservationView.vue') },
      { path: 'store-visits', name: 'store-visit-list', component: () => import('@/views/user/StoreVisitListView.vue') },
      { path: 'store-visit/new', name: 'store-visit-new', component: () => import('@/views/user/StoreVisitEditView.vue') },
      { path: 'experts', name: 'expert-list', component: () => import('@/views/user/ExpertListView.vue') },
      { path: 'ai', name: 'ai-chat', component: () => import('@/views/user/AiChatView.vue') },
      { path: 'notices', name: 'notice-list', component: () => import('@/views/user/NoticeListView.vue') },
      { path: 'chat', name: 'chat-list', component: () => import('@/views/user/ChatListView.vue') },
      { path: 'chat/:peerId', name: 'chat-detail', component: () => import('@/views/user/ChatDetailView.vue') },
      { path: 'profile', name: 'account-profile', component: () => import('@/components/ProfilePanel.vue') }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { adminOnly: true },
    children: [
      { path: 'dashboard', name: 'admin-dashboard', component: () => import('@/views/admin/DashboardView.vue') },
      { path: 'categories', name: 'admin-categories', component: () => import('@/views/admin/CategoryManageView.vue') },
      { path: 'recipes', name: 'admin-recipes', component: () => import('@/views/admin/RecipeManageView.vue') },
      { path: 'comments', name: 'admin-comments', component: () => import('@/views/admin/CommentManageView.vue') },
      { path: 'banners', name: 'admin-banners', component: () => import('@/views/admin/BannerManageView.vue') },
      {
        path: 'store-visits',
        name: 'admin-store-visits',
        component: () => import('@/views/admin/StoreVisitManageView.vue')
      },
      {
        path: 'reservations',
        name: 'admin-reservations',
        component: () => import('@/views/admin/ReservationManageView.vue')
      },
      { path: 'notices', name: 'admin-notices', component: () => import('@/views/admin/NoticeManageView.vue') },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/AdminAccountManage.vue'),
        props: { mode: 'users' }
      },
      {
        path: 'admins',
        name: 'admin-admins',
        component: () => import('@/views/admin/AdminAccountManage.vue'),
        props: { mode: 'admins' }
      },
      { path: 'profile', name: 'admin-profile', component: () => import('@/components/ProfilePanel.vue') }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局守卫：登录态 + 管理员角色
router.beforeEach((to) => {
  const store = useUserStore()

  if (to.path === '/login' && store.isLogin) {
    return store.isAdmin ? '/admin/dashboard' : '/home'
  }
  if (to.meta.public) {
    return true
  }
  if (!store.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.adminOnly && !store.isAdmin) {
    ElMessage.warning('无访问权限')
    return '/home'
  }
  return true
})

export default router
