import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiBase = (env.VITE_API_BASE || '/dev-api').trim()
  // 只有相对路径才需要代理；填了完整 URL 时前端直连云服务器，无需代理
  const proxy = apiBase.startsWith('http')
    ? {}
    : {
        [apiBase]: {
          target: env.VITE_API_TARGET || 'http://localhost:8088',
          changeOrigin: true,
          rewrite: (path) => path
        }
      }

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      host: '0.0.0.0',
      port: 5180,
      strictPort: true,
      open: false,
      proxy
    },
    build: {
      outDir: 'dist',
      chunkSizeWarningLimit: 1600
    }
  }
})
