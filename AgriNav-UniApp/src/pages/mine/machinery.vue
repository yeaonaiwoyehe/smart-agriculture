<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import type { Machinery } from '@/types'
import { fetchMachineryList } from '@/api/user'
import { goBack } from '@/utils/nav'
import { errorMessage } from '@/utils/ui'

const list = ref<Machinery[]>([])
const loading = ref(false)
const error = ref('')

const statusText: Record<Machinery['status'], string> = {
  idle: '闲置',
  working: '作业中',
  maintenance: '维修中'
}

onShow(() => {
  void load()
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    list.value = await fetchMachineryList()
  } catch (e) {
    error.value = errorMessage(e, '农机信息加载失败')
  } finally {
    loading.value = false
  }
}

function statusClass(status: Machinery['status']): string {
  if (status === 'working') return 'status--ok'
  if (status === 'maintenance') return 'status--review'
  return 'status--todo'
}
</script>

<template>
  <view class="page-root">
    <view class="page-head">
      <view class="icon-btn" @tap="goBack('/pages/mine/index')">
        <SfIcon name="back" :size="40" color="#10231b" />
      </view>
      <text class="page-title grow">我的农机</text>
      <view class="icon-btn" @tap="load">
        <SfIcon name="refresh" :size="38" color="#10231b" />
      </view>
    </view>

    <view class="section" style="margin-top: 24rpx">
      <view class="section-head">
        <text class="section-title">农机档案</text>
        <text class="tiny muted">{{ list.length }} 台</text>
      </view>

      <view v-if="error" class="alert-banner">
        <view class="icon-wrap">
          <SfIcon name="alert" :size="36" color="#c47a0a" />
        </view>
        <text class="grow">{{ error }}</text>
        <view class="btn btn--soft btn--sm" @tap="load">重试</view>
      </view>

      <view v-if="loading && !list.length" class="card" style="padding: 32rpx">
        <view class="skeleton" style="height: 38rpx; width: 46%" />
        <view class="skeleton" style="height: 24rpx; width: 32%; margin-top: 16rpx" />
        <view class="skeleton" style="height: 120rpx; margin-top: 26rpx" />
      </view>

      <view v-else-if="!list.length" class="card empty" style="padding: 56rpx 40rpx">
        <view class="empty-ico">
          <SfIcon name="truck" :size="60" color="#1c6a4f" />
        </view>
        <text class="empty-title">暂无绑定农机</text>
        <text class="tiny" style="display: block; margin-top: 8rpx">
          绑定农机后，作业时自动关联机型与作业幅宽
        </text>
      </view>

      <view v-for="item in list" :key="item.id" class="card machine">
        <view class="row-between">
          <view class="row gap-10 grow">
            <view class="machine-ico">
              <SfIcon name="truck" :size="40" color="#14513d" />
            </view>
            <view class="grow">
              <text class="plot-name">{{ item.machineName }}</text>
              <text class="plot-code machine-no">{{ item.machineNo }}</text>
            </view>
          </view>
          <text class="status" :class="statusClass(item.status)">{{ statusText[item.status] }}</text>
        </view>

        <view class="divider" style="margin: 22rpx 0 8rpx" />

        <view class="machine-grid">
          <view class="machine-cell">
            <text class="cell-label">类型</text>
            <text class="cell-value">{{ item.machineType }}</text>
          </view>
          <view class="machine-cell">
            <text class="cell-label">品牌</text>
            <text class="cell-value">{{ item.brand }}</text>
          </view>
          <view class="machine-cell">
            <text class="cell-label">动力</text>
            <text class="cell-value">{{ item.power }}</text>
          </view>
          <view class="machine-cell">
            <text class="cell-label">作业幅宽</text>
            <text class="cell-value">{{ item.workWidth }} m</text>
          </view>
        </view>
      </view>
    </view>

    <view class="notice" style="margin: 28rpx 36rpx 48rpx">
      <SfIcon name="info" :size="28" color="#6a7b70" />
      <text>农机档案来自后端农机管理模块，作业时自动关联当前农机与作业幅宽。</text>
    </view>
  </view>
</template>

<style scoped>
.machine {
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-sizing: border-box;
}

.machine-ico {
  width: 76rpx;
  height: 76rpx;
  border-radius: 22rpx;
  background: var(--surface-2);
  border: 2rpx solid var(--line);
  display: grid;
  place-items: center;
  flex: none;
}

.machine-no {
  display: inline-block;
  margin-top: 8rpx;
}

.machine-grid {
  display: flex;
  flex-wrap: wrap;
}

.machine-cell {
  width: 50%;
  padding: 10rpx 0;
}

.cell-label {
  display: block;
  font-size: 21rpx;
  color: var(--muted);
}

.cell-value {
  display: block;
  margin-top: 4rpx;
  font-size: 27rpx;
  font-weight: 600;
  color: var(--ink-2);
}

.empty-ico {
  display: flex;
  justify-content: center;
  margin-bottom: 20rpx;
}

.empty-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--ink-2);
}
</style>
