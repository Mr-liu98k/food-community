import request from './request'

// ---- 管理端：分类 ----
export const adminCategoryPage = (params) => request.get('/admin/category', { params })
export const adminCreateCategory = (data) => request.post('/admin/category', data)
export const adminUpdateCategory = (id, data) => request.put(`/admin/category/${id}`, data)
export const adminDeleteCategory = (id) => request.delete(`/admin/category/${id}`)

// ---- 管理端：菜谱 ----
export const adminRecipePage = (params) => request.get('/admin/recipe', { params })
export const adminRecipePreview = (id) => request.get(`/admin/recipe/${id}`)
export const adminAuditRecipe = (id, data) => request.put(`/admin/recipe/${id}/audit`, data)
export const adminRemoveRecipe = (id) => request.delete(`/admin/recipe/${id}`)

// ---- 管理端：评论 ----
export const adminCommentPage = (params) => request.get('/admin/comment', { params })
export const adminRemoveComment = (id) => request.delete(`/admin/comment/${id}`)

// ---- 管理端：轮播图 ----
export const adminBannerPage = (params) => request.get('/admin/banner', { params })
export const adminCreateBanner = (data) => request.post('/admin/banner', data)
export const adminUpdateBanner = (id, data) => request.put(`/admin/banner/${id}`, data)
export const adminRemoveBanner = (id) => request.delete(`/admin/banner/${id}`)

// ---- 管理端：探店信息 ----
export const adminStoreVisitPage = (params) => request.get('/admin/store-visit', { params })
export const adminAuditStoreVisit = (id, data) => request.put(`/admin/store-visit/${id}/audit`, data)
export const adminRemoveStoreVisit = (id) => request.delete(`/admin/store-visit/${id}`)

// ---- 管理端：探店预约 ----
export const adminReservationPage = (params) => request.get('/admin/reservation', { params })
export const adminRemoveReservation = (id) => request.delete(`/admin/reservation/${id}`)

// ---- 管理端：公告 ----
export const adminNoticePage = (params) => request.get('/admin/notice', { params })
export const adminCreateNotice = (data) => request.post('/admin/notice', data)
export const adminUpdateNotice = (id, data) => request.put(`/admin/notice/${id}`, data)
export const adminRemoveNotice = (id) => request.delete(`/admin/notice/${id}`)

// ---- 管理端：看板 ----
export const adminDashboard = () => request.get('/admin/dashboard')

// ---- 管理端：用户 / 管理员 ----
export const adminUserPage = (params) => request.get('/admin/users', { params })
export const adminCreateUser = (data) => request.post('/admin/users', data)
export const adminSetUserExpert = (id, expert) => request.put(`/admin/users/${id}/expert`, { expert })
export const adminUpdateUserNickname = (id, nickname) => request.put(`/admin/users/${id}`, { nickname })
export const adminResetUserPassword = (id, password) => request.put(`/admin/users/${id}/password`, { password })
export const adminRemoveUser = (id) => request.delete(`/admin/users/${id}`)

export const adminAdminPage = (params) => request.get('/admin/admins', { params })
export const adminCreateAdmin = (data) => request.post('/admin/admins', data)
export const adminUpdateAdminNickname = (id, nickname) => request.put(`/admin/admins/${id}`, { nickname })
export const adminResetAdminPassword = (id, password) => request.put(`/admin/admins/${id}/password`, { password })
export const adminRemoveAdmin = (id) => request.delete(`/admin/admins/${id}`)
