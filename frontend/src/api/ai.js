import { useUserStore } from '@/stores/user'

/**
 * AI 美食专家 SSE 流式请求。
 * 返回 Promise：正常结束时 resolve；abort 时 reject(AbortError)。
 * onDelta 每收到一段文本回调一次（逐字渲染）。
 */
export async function streamChat(messages, onDelta, signal) {
  const store = useUserStore()
  const res = await fetch('/api/ai/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${store.token}`
    },
    body: JSON.stringify({ messages }),
    signal
  })
  if (!res.ok || !res.body) {
    throw new Error(`AI 服务请求失败（${res.status}）`)
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    let idx
    while ((idx = buffer.indexOf('\n\n')) >= 0) {
      const frame = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 2)
      for (const line of frame.split('\n')) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (data && data !== '[DONE]') {
            onDelta(data)
          }
        }
      }
    }
  }
}
