import { fileURLToPath, URL } from 'node:url'
import path from 'path'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiBase = (env.VITE_API_BASE || '/dev-api').trim()
  
  // 你的代理逻辑：只有相对路径才需要代理；填了完整 URL 时前端直连云服务器，无需代理
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