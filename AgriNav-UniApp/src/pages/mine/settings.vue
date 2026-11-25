<script setup lang="ts">
import { computed, ref } from 'vue'
import SfIcon from '@/components/SfIcon.vue'
import { setMockEnabled } from '@/config'
import { getApiBaseUrl } from '@/api/request'
import { resetDb } from '@/mock/db'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { goBack } from '@/utils/nav'
import { confirmAction, toast } from '@/utils/ui'

const app = useAppStore()
const user = useUserStore()
const switching = ref(false)

const baseUrl = computed(() => getApiBaseUrl() || '未配置')
const gpsText = computed(() => app.gpsLabel)

async function toggleMock(e: any) {
  const next = !!e.detail.value
  if (next === app.mockEnabled) return
  if (switching.value) return
  switching.value = true
  const ok = await confirmAction({
    title: next ? '切换到演示数据' : '切换到真实后端',
    content: next
      ? '演示数据保存在本机，不会上传服务器。切换后将退出登录，避免演示 token 与真实 token 混用。'
      : `将直接调用后端 ${baseUrl.value}。任务/开工/完工接口后端暂未提供，相关页面会提示不可用。切换后将退出登录。`,
    confirmText: '切换'
  })
  if (!ok) {
    switching.value = false
    return
  }
  setMockEnabled(next)
  app.switchMock(next)
  user.logout()
  toast('已切换，请重新登录')
  setTimeout(() => uni.reLaunch({ url: '/pages/login/index' }), 400)
}

async function handleReset() {
  if (!app.mockEnabled) {
    toast('仅演示模式支持重置数据')
    return
  }
  const ok = await confirmAction({
    title: '重置演示数据',
    content: '将清空本机所有任务、作业单、结算、消息与助手会话，并恢复初始演示数据。此操作不可撤销。',
    confirmText: '确认重置',
    danger: true
  })
  if (!ok) return
  resetDb()
  toast('已重置演示数据', 'success')
}

async function handleClear() {
  const ok = await confirmAction({
    title: '清理本地缓存',
    content: '将清理登录态与本地缓存（不影响演示数据），之后需要重新登录。',
    confirmText: '清理'
  })
  if (!ok) return
  user.logout()
  toast('已清理')
  setTimeout(() => uni.reLaunch({ url: '/pages/login/index' }), 300)
}

async function checkGps() {
  const res = await app.requestGps()
  if (res) {
    toast('定位正常', 'success')
  } else {
    toast('未获取到定位，请检查系统权限')
  }
}
</script>

<template>
  <view class="page-root">
    <view class="page-head">
      <view class="icon-btn" @tap="goBack('/pages/mine/index')">
        <SfIcon name="back" :size="40" color="#10231b" />
      </view>
      <text class="page-title grow">设置</text>
      <view class="icon-btn">
        <SfIcon name="edit" :size="36" color="#8d9c91" />
      </view>
    </view>

    <view class="section" style="margin-top: 24rpx">
      <view class="section-head">
        <text class="section-title">数据与网络</text>
      </view>
      <view class="card list-card">
        <view class="list-item">
          <view class="ico-box">
            <SfIcon name="spark" :size="36" color="#14513d" />
          </view>
          <view class="grow">
            <text class="list-title">使用演示数据</text>
            <text class="list-desc">关闭后将请求真实后端接口</text>
          </view>
          <switch :checked="app.mockEnabled" color="#1c6a4f" @change="toggleMock" />
        </view>

        <view class="list-item">
          <view class="ico-box">
            <SfIcon name="wifi" :size="36" color="#14513d" />
          </view>
          <view class="grow">
            <text class="list-title">后端地址</text>
            <text class="list-desc break">{{ baseUrl }}</text>
          </view>
        </view>

        <view class="list-item">
          <view class="ico-box">
            <SfIcon name="satellite" :size="36" color="#14513d" />
          </view>
          <view class="grow">
            <text class="list-title">网络状态</text>
            <text class="list-desc">{{ app.networkLabel }}（{{ app.networkType }}）</text>
          </view>
          <text class="status" :class="app.online ? 'status--ok' : 'status--timeout'">
            {{ app.online ? '在线' : '离线' }}
          </text>
        </view>

        <view class="list-item" @tap="checkGps">
          <view class="ico-box">
            <SfIcon name="pin" :size="36" color="#14513d" />
          </view>
          <view class="grow">
            <text class="list-title">定位状态</text>
            <text class="list-desc">{{ gpsText }}</text>
          </view>
          <view class="btn btn--soft btn--sm">检测</view>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">数据维护</text>
      </view>
      <view class="card list-card">
        <view class="list-item" @tap="handleReset">
          <view class="ico-box">
            <SfIcon name="trash" :size="36" color="#14513d" />
          </view>
          <view class="grow">
            <text class="list-title">重置演示数据</text>
            <text class="list-desc">恢复任务、结算、消息初始状态</text>
          </view>
          <SfIcon name="chevron" :size="30" color="#8d9c91" />
        </view>
        <view class="list-item" @tap="handleClear">
          <view class="ico-box">
            <SfIcon name="logout" :size="36" color="#14513d" />
          </view>
          <view class="grow">
            <text class="list-title">清理登录态与缓存</text>
            <text class="list-desc">退出登录并清除本地缓存</text>
          </view>
          <SfIcon name="chevron" :size="30" color="#8d9c91" />
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">关于</text>
      </view>
      <view class="card" style="padding: 8rpx 30rpx">
        <view class="info-row"><text>版本</text><text>v1.0.0</text></view>
        <view class="info-row"><text>技术栈</text><text>uni-app + Vue3 + TS + Pinia</text></view>
        <view class="info-row"><text>当前模式</text><text>{{ app.mockEnabled ? '演示数据' : '真实后端' }}</text></view>
      </view>
    </view>

    <view class="notice" style="margin: 28rpx 36rpx 48rpx">
      <SfIcon name="shield" :size="28" color="#6a7b70" />
      <text>演示模式与真实模式严格隔离：演示数据不会上传服务器，真实请求也不会回落到演示数据。</text>
    </view>
  </view>
</template>

<style scoped>
.list-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: var(--ink);
}

.list-desc {
  display: block;
  margin-top: 4rpx;
  font-size: 23rpx;
  color: var(--muted);
}

.break {
  word-break: break-all;
}
</style>
