<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import AppTabBar from '@/components/AppTabBar.vue'
import type { OverviewStats } from '@/api/user'
import { fetchOverviewStats } from '@/api/user'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { formatArea, formatMoney, maskPhone } from '@/utils/format'
import { confirmAction, toast } from '@/utils/ui'
import { go, goBack } from '@/utils/nav'

const user = useUserStore()
const app = useAppStore()
const stats = ref<OverviewStats | null>(null)

const avatarText = computed(() => (user.profile?.nickName || '机').slice(0, 1))

const services = [
  { icon: 'user', title: '个人资料', desc: '姓名、手机号与所属队组', url: '/pages/mine/profile' },
  { icon: 'truck', title: '我的农机', desc: '农机档案与作业幅宽', url: '/pages/mine/machinery' },
  { icon: 'message', title: '消息中心', desc: '任务、作业与结算通知', url: '/pages/message/index' },
  { icon: 'alert', title: '异常记录', desc: '异常上报与处理进度', url: '/pages/exception/index' },
  { icon: 'shield', title: '账号安全', desc: '登录状态与密码设置', url: '/pages/mine/security' },
  { icon: 'help', title: '使用帮助', desc: '接单、开工、结算操作说明', url: '/pages/mine/help' },
  { icon: 'edit', title: '设置', desc: '演示数据与偏好设置', url: '/pages/mine/settings' }
] as const

onShow(() => {
  void load()
})

async function load() {
  if (!user.profile) {
    try {
      await user.loadProfile()
    } catch (e) {
      // 使用缓存资料
    }
  }
  try {
    stats.value = await fetchOverviewStats()
  } catch (e) {
    stats.value = null
  }
  await app.refreshUnread()
}

function openProfile() {
  go('/pages/mine/profile')
}

async function handleLogout() {
  const ok = await confirmAction({
    title: '确认退出登录？',
    content: '退出后需要重新使用手机号登录，本地演示数据不会丢失。',
    confirmText: '退出',
    danger: true
  })
  if (!ok) return
  user.logout()
  toast('已退出登录')
  setTimeout(() => uni.reLaunch({ url: '/pages/login/index' }), 300)
}
</script>

<template>
  <view class="page-root has-tabbar">
    <view class="profile-hero">
      <view class="icon-btn icon-btn--dark profile-back" @tap="goBack('/pages/home/index')">
        <SfIcon name="back" :size="38" color="#ffffff" />
      </view>
      <view class="profile-avatar" @tap="openProfile">{{ avatarText }}</view>
      <view class="grow" @tap="openProfile">
        <view class="profile-name">{{ user.displayName }}</view>
        <view class="profile-sub">
          {{ maskPhone(user.phone) }} · {{ user.profile?.deptName || '未分配作业队' }}
        </view>
      </view>
      <SfIcon name="chevron" :size="34" color="rgba(255,255,255,.5)" />
    </view>

    <view class="pad" style="margin-top: 28rpx">
      <view class="card" style="padding: 28rpx 30rpx">
        <view class="stat-mini-grid" style="margin-top: 0">
          <view class="stat-mini">
            <text class="stat-mini-num">{{ formatArea(stats?.totalArea) }}<text> 亩</text></text>
            <text>累计作业面积</text>
          </view>
          <view class="stat-mini">
            <text class="stat-mini-num">¥{{ formatMoney(stats?.totalIncome) }}</text>
            <text>累计收入</text>
          </view>
          <view class="stat-mini">
            <text class="stat-mini-num">{{ formatArea(stats?.monthArea) }}<text> 亩</text></text>
            <text>本月核算面积</text>
          </view>
          <view class="stat-mini">
            <text class="stat-mini-num">{{ stats?.doneTasks ?? 0 }}</text>
            <text>完成作业单</text>
          </view>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">服务与支持</text>
        <text v-if="app.mockEnabled" class="status status--review">演示数据</text>
      </view>
      <view class="card list-card">
        <view v-for="s in services" :key="s.url" class="list-item" @tap="go(s.url)">
          <view class="ico-box">
            <SfIcon :name="s.icon" :size="38" color="#14513d" />
          </view>
          <view class="grow">
            <text class="list-title">{{ s.title }}</text>
            <text class="list-desc">{{ s.desc }}</text>
          </view>
          <text
            v-if="s.url === '/pages/message/index' && app.unreadCount"
            class="unread-badge"
          >
            {{ app.unreadCount }}
          </text>
          <SfIcon name="chevron" :size="30" color="#8d9c91" />
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-head"><text class="section-title">账号与安全</text></view>
      <view class="card" style="padding: 8rpx 30rpx">
        <view class="info-row"><text>登录手机号</text><text>{{ maskPhone(user.phone) }}</text></view>
        <view class="info-row"><text>所属队组</text><text>{{ user.profile?.deptName || '—' }}</text></view>
        <view class="info-row"><text>数据权限</text><text>仅本人任务 / 作业单 / 轨迹 / 结算</text></view>
      </view>
      <view class="notice" style="margin-top: 20rpx">
        <SfIcon name="shield" :size="28" color="#6a7b70" />
        <text>所有请求均经后端身份认证；接单、开完工、提交复核与异常工单均需二次确认并生成回执。</text>
      </view>
    </view>

    <view class="pad" style="margin-top: 36rpx; margin-bottom: 24rpx">
      <view class="btn btn--danger btn--block" @tap="handleLogout">
        <SfIcon name="logout" :size="36" color="#a92f24" />
        <text>退出登录</text>
      </view>
      <view class="tiny muted" style="text-align: center; margin-top: 20rpx">
        农机作业管控 · 移动端 v1.0.0
      </view>
    </view>

    <AppTabBar active="profile" />
  </view>
</template>

<style scoped>
.profile-hero {
  position: relative;
}

.profile-back {
  position: absolute;
  top: calc(20rpx + var(--status-bar-height, 0px));
  left: 28rpx;
  width: 68rpx;
  height: 68rpx;
  border-radius: 22rpx;
}

.profile-hero {
  padding-top: calc(120rpx + var(--status-bar-height, 0px));
}

.stat-mini-num {
  display: block;
  font-size: 42rpx;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.stat-mini-num text {
  font-size: 22rpx;
  color: var(--muted);
  font-weight: 700;
}

.list-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
}

.list-desc {
  display: block;
  margin-top: 2rpx;
  font-size: 24rpx;
  color: var(--muted);
}

.unread-badge {
  min-width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  text-align: center;
  border-radius: 1998rpx;
  background: var(--red);
  color: #fff;
  font-size: 20rpx;
  padding: 0 8rpx;
}
</style>
