<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

onLaunch(() => {
  const user = useUserStore()
  const app = useAppStore()
  user.restore()
  app.initNetworkWatcher()
  app.refreshGpsState()
})

onShow(() => {
  useAppStore().refreshGpsState()
})
</script>

<style>
@import './styles/legacy.css';

/* #ifdef H5 */
/* 桌面浏览器预览时，把页面限制为手机宽度并居中，模拟 App 外观 */
@media screen and (min-width: 640px) {
  body,
  uni-app {
    background: #0b1f18;
  }

  uni-page {
    max-width: 430px;
    margin: 0 auto;
    min-height: 100vh;
    position: relative;
    transform: translateZ(0);
    box-shadow: 0 0 90px rgba(0, 0, 0, 0.55);
  }
}
/* #endif */
</style>
