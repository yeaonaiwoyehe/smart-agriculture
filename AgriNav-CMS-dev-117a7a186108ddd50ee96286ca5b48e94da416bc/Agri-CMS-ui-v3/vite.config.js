import { defineConfig, loadEnv } from 'vite'
import path from 'path'
import vue from '@vitejs/plugin-vue'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())

  return {
    plugins: [
      vue(),
      createSvgIconsPlugin({
        iconDirs: [path.resolve(process.cwd(), 'src/assets/icons/svg')],
        symbolId: 'icon-[name]'
      })
    ],
    resolve: {
      alias: {
        '@': path.resolve(process.cwd(), 'src')
      },
      extensions: ['.mjs', '.js', '.json', '.vue']
    },
    css: {
      preprocessorOptions: {
        scss: {
          // 兼容原有 scss 写法，关闭新版本弃用告警
          silenceDeprecations: ['legacy-js-api', 'import', 'global-builtin', 'color-functions'],
          sassOptions: {
            outputStyle: 'expanded'
          }
        }
      }
    },
    server: {
      host: '0.0.0.0',
      port: Number(env.VITE_PORT) || 81,
      open: true,
      proxy: {
        [env.VITE_APP_BASE_API || '/dev-api']: {
          target: 'http://localhost:8088',
          changeOrigin: true,
          rewrite: (p) => p.replace(new RegExp('^' + (env.VITE_APP_BASE_API || '/dev-api')), '')
        }
      }
    },
    build: {
      outDir: 'dist',
      assetsDir: 'static',
      chunkSizeWarningLimit: 3000,
      rollupOptions: {
        output: {
          manualChunks: {
            'chunk-element-plus': ['element-plus'],
            'chunk-echarts': ['echarts']
          }
        }
      }
    }
  }
})
