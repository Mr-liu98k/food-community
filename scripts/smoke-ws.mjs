// M5 WebSocket 冒烟：双用户 A/B 实时互发 + 未读 + 已读（Node 22 内置 fetch/WebSocket）
// 端口解析：优先 BACKEND_PORT → 读 backend/.env 的 SERVER_PORT → 8080
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

function resolvePort() {
  if (process.env.BACKEND_PORT) return process.env.BACKEND_PORT
  try {
    const envPath = join(dirname(fileURLToPath(import.meta.url)), '..', 'backend', '.env')
    for (const line of readFileSync(envPath, 'utf8').split(/\r?\n/)) {
      const m = line.match(/^\s*SERVER_PORT\s*=\s*(.+?)\s*$/)
      if (m) return m[1]
    }
  } catch { /* .env 不存在则走默认值 */ }
  return '8080'
}

const PORT = resolvePort()
const BASE = `http://localhost:${PORT}/api`
const WS = `ws://localhost:${PORT}/api/ws/chat`
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

const stamp = Date.now()
const mkUser = (tag) => ({ username: `m5_${tag}_${stamp}`, password: `pw${Math.random().toString(36).slice(2, 10)}`, nickname: `M5${tag}` })

async function api(path, opts = {}, token) {
  const res = await fetch(BASE + path, {
    ...opts,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(opts.headers || {})
    }
  })
  return res.json()
}

async function login(u) {
  await api('/auth/register', { method: 'POST', body: JSON.stringify(u) })
  const res = await api('/auth/login', { method: 'POST', body: JSON.stringify(u) })
  return res.data
}

function openWs(token) {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(`${WS}?token=${encodeURIComponent(token)}`)
    ws.onopen = () => resolve(ws)
    ws.onerror = () => reject(new Error('ws error'))
  })
}

function nextMessage(ws, timeout = 5000) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('等待消息超时')), timeout)
    ws.onmessage = (ev) => {
      clearTimeout(timer)
      resolve(JSON.parse(ev.data))
    }
  })
}

let pass = 0
const ok = (name, cond) => {
  console.log(`${cond ? '✅' : '❌'} ${name}`)
  if (!cond) process.exitCode = 1
  if (cond) pass++
}

const A = await login(mkUser('A'))
const B = await login(mkUser('B'))
ok('A/B 注册登录', A.token && B.token)

const wsA = await openWs(A.token)
const wsB = await openWs(B.token)
ok('A/B WebSocket 握手建立', wsA.readyState === WebSocket.OPEN && wsB.readyState === WebSocket.OPEN)

// A 发给 B
wsA.send(JSON.stringify({ toUserId: B.user.id, content: '你好呀，看到你发的可乐鸡翅了' }))
const recv = await nextMessage(wsB)
ok('B 实时收到 A 的消息', recv.type === 'chat' && recv.message.content.includes('可乐鸡翅'))

// B 未读数 = 1
await sleep(200)
let unread = (await api('/chat/unread-count', {}, B.token)).data
ok('B 未读数 = 1', unread === 1)

// B 回复 A
wsB.send(JSON.stringify({ toUserId: A.user.id, content: '谢谢！欢迎来主页逛逛' }))
const recv2 = await nextMessage(wsA)
ok('A 实时收到 B 的回复', recv2.message.content === '谢谢！欢迎来主页逛逛')

// B 打开会话标记已读 → 未读清零
await api('/chat/read', { method: 'POST', body: JSON.stringify({ peerId: A.user.id }) }, B.token)
await sleep(200)
unread = (await api('/chat/unread-count', {}, B.token)).data
ok('B 标记已读后未读 = 0', unread === 0)

// A 视角会话列表含 B，最近一条是 B 的回复
const convs = (await api('/chat/conversations', {}, A.token)).data
const convB = convs.find((c) => c.peerId === B.user.id)
ok('A 会话列表包含 B 且最近消息正确', convB && convB.lastContent === '谢谢！欢迎来主页逛逛')

// 历史接口
const hist = (await api(`/chat/history?peerId=${B.user.id}&page=1&size=20`, {}, A.token)).data
ok('A↔B 历史共 2 条', hist.total === 2)

wsA.close(); wsB.close()
console.log(`\nWebSocket 冒烟结束：通过 ${pass} 项`)
