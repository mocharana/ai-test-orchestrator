import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// dev 跨域：前端 5173 的 /api 请求转发到后端 8081（浏览器同源，无 CORS 问题）
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
