import request from './request'

// ---- 前台探店（公开） ----
export const storeVisitPage = (params) => request.get('/store-visit', { params })
export const storeVisitHot = (limit = 8) => request.get('/store-visit/hot', { params: { limit } })
export const storeVisitDetail = (id) => request.get(`/store-visit/${id}`)

// ---- 我的探店 ----
export const myStoreVisits = (params) => request.get('/my/store-visit', { params })
export const myStoreVisitDetail = (id) => request.get(`/my/store-visit/${id}`)
export const createStoreVisit = (data) => request.post('/my/store-visit', data)
export const updateStoreVisit = (id, data) => request.put(`/my/store-visit/${id}`, data)
export const removeStoreVisit = (id) => request.delete(`/my/store-visit/${id}`)

// ---- 达人榜 / 预约 ----
export const expertList = (params) => request.get('/expert/list', { params })
export const myReservations = (params) => request.get('/my/reservations', { params })
export const createReservation = (data) => request.post('/reservation', data)
export const agreeReservation = (id) => request.post(`/my/reservations/${id}/agree`)
export const rejectReservation = (id, data) => request.post(`/my/reservations/${id}/reject`, data)

// ---- 作者主页 ----
export const authorStoreVisits = (id, params) => request.get(`/user/${id}/store-visits`, { params })
