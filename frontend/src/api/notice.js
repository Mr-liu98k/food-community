import request from './request'

// ---- 前台公告（公开） ----
export const noticePage = (params) => request.get('/notice', { params })
export const noticeLatest = (limit = 1) => request.get('/notice/latest', { params: { limit } })
export const noticeDetail = (id) => request.get(`/notice/${id}`)
