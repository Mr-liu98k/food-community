import request from './request'

/** 注册：成功返回 void */
export const register = (data) => request.post('/auth/register', data)

/** 登录：返回 { token, user } */
export const login = (data) => request.post('/auth/login', data)
