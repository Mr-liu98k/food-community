import request from './request'

/** 当前用户信息 */
export const getMe = () => request.get('/user/me')

/** 修改资料 { nickname, avatar, intro } */
export const updateProfile = (data) => request.put('/user/me', data)

/** 修改密码 { oldPassword, newPassword } */
export const changePassword = (data) => request.put('/user/me/password', data)
