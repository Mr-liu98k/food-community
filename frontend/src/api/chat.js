import request from './request'

export const chatConversations = () => request.get('/chat/conversations')
export const chatHistory = (peerId, params) => request.get('/chat/history', { params: { peerId, ...params } })
export const chatUnreadCount = () => request.get('/chat/unread-count')
export const chatMarkRead = (peerId) => request.post('/chat/read', { peerId })

/** 建立聊天 WebSocket（token 经 query 传入，握手由后端 JWT 校验） */
export function openChatSocket(token) {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  return new WebSocket(`${proto}://${location.host}/api/ws/chat?token=${encodeURIComponent(token)}`)
}
