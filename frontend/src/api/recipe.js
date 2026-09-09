import request from './request'

// ---- 公开浏览 ----
export const pageRecipes = (params) => request.get('/recipe', { params })
export const hotRecipes = (limit = 8) => request.get('/recipe/hot', { params: { limit } })
export const recipeDetail = (id) => request.get(`/recipe/${id}`)

// ---- 我的菜谱 ----
export const myRecipes = (params) => request.get('/my/recipe', { params })
export const createRecipe = (data) => request.post('/my/recipe', data)
export const updateRecipe = (id, data) => request.put(`/my/recipe/${id}`, data)
export const removeRecipe = (id) => request.delete(`/my/recipe/${id}`)
