import { defineStore } from 'pinia'
import { chatUnreadCount } from '@/api/chat'

/**
 * 未读消息红点 store：导航栏轮询刷新；打开会话/收到消息时主动更新。
 */
export const useChatStore = defineStore('chat', {
  state: () => ({
    unreadTotal: 0
  }),
  actions: {
    async refreshUnread() {
      try {
        const res = await chatUnreadCount()
        this.unreadTotal = res.data || 0
      } catch (e) {
        // 未登录等场景忽略
      }
    }
  }
})
