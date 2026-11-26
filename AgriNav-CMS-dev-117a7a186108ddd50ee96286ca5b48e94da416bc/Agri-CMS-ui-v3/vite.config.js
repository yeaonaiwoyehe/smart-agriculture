import { fileURLToPath, URL } from 'node:url'
import path from 'path'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiBase = (env.VITE_APP_BASE_API || '/dev-api').trim()
  const apiTarget = (env.VITE_APP_API_TARGET || 'http://82.156.224.217').trim()

  // 相对前缀（如 /prod-api）走本地代理；完整 URL 则前端直连，无需代理。
  // 云端 Nginx 本身以 /prod-api 作为路径前缀，因此不能 strip 该前缀，
  // 否则会转发到 http://82.156.224.217/login 导致 404。
  const proxy = apiBase.startsWith('http')
    ? {}
    : {
        [apiBase]: {
          target: apiTarget,
          changeOrigin: true,
          rewrite: (path) => path
        }
      }

  return {
    plugins: [
      vue(),
      // 同事加的 SVG 图标插件（必须保留，因为 main.js 引用了）
      createSvgIconsPlugin({
        iconDirs: [path.resolve(process.cwd(), 'src/assets/icons/svg')],
        symbolId: 'icon-[name]'
      })
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
      extensions: ['.mjs', '.js', '.json', '.vue']
    },
    css: {
      preprocessorOptions: {
        scss: {
          // 同事加的兼容原有 scss 写法的配置
          silenceDeprecations: ['legacy-js-api', 'import', 'global-builtin', 'color-functions'],
          sassOptions: {
            outputStyle: 'expanded'
          }
        }
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
      assetsDir: 'static',
      chunkSizeWarningLimit: 1600,
      rollupOptions: {
        output: {
          // 同事加的打包优化
          manualChunks: {
            'chunk-element-plus': ['element-plus'],
            'chunk-echarts': ['echarts']
          }
        }
      }
    }
  }
})