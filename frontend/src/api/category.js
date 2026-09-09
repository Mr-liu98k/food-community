import request from './request'

/** 前台分类列表（全量，公开） */
export const getCategories = () => request.get('/category/list')
