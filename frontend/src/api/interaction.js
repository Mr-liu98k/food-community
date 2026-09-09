import request from './request'

// ---- 点赞 / 收藏 / 我的收藏 ----
export const likeRecipe = (recipeId) => request.post(`/recipe/${recipeId}/like`)
export const unlikeRecipe = (recipeId) => request.delete(`/recipe/${recipeId}/like`)
export const favoriteRecipe = (recipeId) => request.post(`/recipe/${recipeId}/favorite`)
export const unfavoriteRecipe = (recipeId) => request.delete(`/recipe/${recipeId}/favorite`)
export const myFavorites = (params) => request.get('/my/favorites', { params })

// ---- 评论 ----
export const recipeComments = (recipeId, params) => request.get(`/recipe/${recipeId}/comments`, { params })
export const postComment = (recipeId, data) => request.post(`/recipe/${recipeId}/comment`, data)
export const removeComment = (id) => request.delete(`/comment/${id}`)
export const myComments = (params) => request.get('/my/comments', { params })

// ---- 轮播 ----
export const banners = () => request.get('/banner/list')

// ---- 作者主页 ----
export const authorProfile = (id) => request.get(`/user/${id}/profile`)
export const authorRecipes = (id, params) => request.get(`/user/${id}/recipes`, { params })
