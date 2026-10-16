import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'

// 后端端口解析：优先环境变量 BACKEND_PORT → 读 backend/.env 的 SERVER_PORT → 8080
// 这样前端代理与后端启动端口共用同一份配置（backend/.env），
// 改端口只需改 .env 一处，前端无需再手工传 BACKEND_PORT。
function resolveBackendPort() {
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

const BACKEND_PORT = resolveBackendPort()
const backendTarget = `http://localhost:${BACKEND_PORT}`

// 开发代理：/api 与 /uploads 转发到后端，前端不关心跨域
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    // host: true 让 vite 同时监听 IPv4/IPv6，避免只绑 [::1] 时
    // http://localhost:5173 / http://127.0.0.1:5173 连不上的问题。
    host: true,
    port: 5173,
    proxy: {
      '/api': {
        target: backendTarget,
        changeOrigin: true,
        ws: true // WebSocket 升级（/api/ws/chat）
      },
      '/uploads': {
        target: backendTarget,
        changeOrigin: true
      }
    }
  }
})
