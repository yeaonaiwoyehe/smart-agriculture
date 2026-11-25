<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import AppTabBar from '@/components/AppTabBar.vue'
import type { Settlement } from '@/types'
import { fetchSettlementList, fetchSettlementSummary } from '@/api/settlement'
import type { SettlementSummary } from '@/mock/settlement'
import { SETTLEMENT_STATUS_TEXT } from '@/utils/constants'
import { formatArea, formatMoney } from '@/utils/format'
import { errorMessage } from '@/utils/ui'
import { go, goBack } from '@/utils/nav'

type Filter = 'all' | Settlement['status']

const list = ref<Settlement[]>([])
const summary = ref<SettlementSummary | null>(null)
const filter = ref<Filter>('all')
const loading = ref(false)
const error = ref('')

const filters: Array<{ key: Filter; label: string }> = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待结算' },
  { key: 'paid', label: '已结算' },
  { key: 'reviewing', label: '复核中' }
]

const filtered = computed(() =>
  filter.value === 'all' ? list.value : list.value.filter((s) => s.status === filter.value)
)

const current = computed(() => list.value.find((s) => s.status === 'pending') || list.value[0] || null)

function statusClass(status: Settlement['status']): string {
  if (status === 'paid') return 'status--ok'
  if (status === 'reviewing') return 'status--review'
  return 'status--todo'
}

onShow(() => {
  void load()
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [rows, sum] = await Promise.all([fetchSettlementList(), fetchSettlementSummary()])
    list.value = rows
    summary.value = sum
  } catch (e) {
    error.value = errorMessage(e, '结算数据加载失败')
  } finally {
    loading.value = false
  }
}

function openDetail(item: Settlement) {
  go(`/pages/settlement/detail?id=${item.settlementId}`)
}
</script>

<template>
  <view class="page-root has-tabbar">
    <view class="page-head">
      <view class="icon-btn" @tap="goBack('/pages/home/index')">
        <SfIcon name="back" :size="40" color="#10231b" />
      </view>
      <text class="page-title grow">结算中心</text>
      <view class="icon-btn" @tap="load">
        <SfIcon name="refresh" :size="38" color="#10231b" />
      </view>
    </view>

    <view class="settle-hero">
      <view class="row-between">
        <text class="settle-label">
          本月待结算 · {{ current ? `${current.periodStart} 至 ${current.periodEnd}` : '—' }}
        </text>
        <text class="status" style="background: rgba(200, 244, 90, 0.18); color: var(--lime)">
          {{ current ? SETTLEMENT_STATUS_TEXT[current.status] : '暂无' }}
        </text>
      </view>
      <view class="settle-hero-amount">¥{{ formatMoney(summary?.pendingAmount || 0) }}</view>
      <view class="settle-hero-meta">
        <text>核算面积 <text class="lime">{{ formatArea(summary?.totalArea || 0) }} 亩</text></text>
        <text>结算单 <text class="white">{{ list.length }} 单</text></text>
        <text v-if="summary?.reviewingAmount" class="amber">
          复核中 ¥{{ formatMoney(summary.reviewingAmount) }}
        </text>
      </view>
      <view class="settle-hero-note">
        <SfIcon name="shield" :size="24" color="rgba(255,255,255,.45)" />
        <text>面积与金额均由平台作业单核算，不由大模型估算</text>
      </view>
    </view>

    <scroll-view class="tabs-scroll" scroll-x :show-scrollbar="false">
      <view class="tabs">
        <view
          v-for="f in filters"
          :key="f.key"
          class="tab"
          :class="{ 'tab--active': filter === f.key }"
          @tap="filter = f.key"
        >
          {{ f.label }}
        </view>
      </view>
    </scroll-view>

    <view v-if="error" class="card" style="margin: 0 32rpx 20rpx; padding: 24rpx; color: var(--red); font-size: 26rpx">
      {{ error }}
    </view>

    <view class="section" style="margin-top: 12rpx">
      <view class="section-head">
        <text class="section-title">结算周期</text>
        <text class="tiny muted">每月 1 日核算上月</text>
      </view>

      <view v-if="loading && !filtered.length" class="card" style="padding: 32rpx">
        <view class="skeleton" style="height: 36rpx; width: 50%" />
        <view class="skeleton" style="height: 24rpx; width: 75%; margin-top: 16rpx" />
      </view>

      <view
        v-for="item in filtered"
        :key="item.settlementId"
        class="card order-card"
        @tap="openDetail(item)"
      >
        <view class="row-between">
          <view class="grow">
            <view class="row gap-6">
              <text class="settle-no">{{ item.settlementNo }}</text>
            </view>
            <text class="tiny muted" style="margin-top: 6rpx; display: block">
              {{ item.periodStart }} 至 {{ item.periodEnd }} · {{ item.itemsCount }} 单明细
            </text>
          </view>
          <text class="status" :class="statusClass(item.status)">
            {{ SETTLEMENT_STATUS_TEXT[item.status] }}
          </text>
        </view>

        <view class="row-between" style="margin-top: 20rpx">
          <view>
            <text class="tiny muted">核算面积</text>
            <view class="settle-area">
              {{ formatArea(item.totalArea) }}
              <text>亩</text>
            </view>
          </view>
          <view style="text-align: right">
            <text class="tiny muted">结算金额</text>
            <view class="settle-money">¥{{ formatMoney(item.totalAmount) }}</view>
          </view>
        </view>

        <view class="settle-foot">
          <text>{{ item.status === 'paid' ? '已打款' : item.status === 'reviewing' ? '复核处理中' : '待平台打款' }}</text>
          <view class="plot-link">
            <text>查看明细</text>
            <SfIcon name="chevron" :size="26" color="#1c6a4f" />
          </view>
        </view>
      </view>

      <view v-if="!loading && !filtered.length" class="card empty" style="padding: 56rpx 40rpx">
        <view class="empty-ico">
          <SfIcon name="wallet" :size="60" color="#1c6a4f" />
        </view>
        <text class="empty-title">还没有结算单</text>
        <text class="tiny" style="display: block; margin-top: 8rpx">
          完成作业并结束上传后，平台会自动核算
        </text>
      </view>
    </view>

    <view class="tiny muted footnote">
      结算金额 = 系统核算面积 × 作业单价。扣减原因可在结算明细中查看并提交复核。
    </view>

    <AppTabBar active="settlement" />
  </view>
</template>

<style scoped>
.tabs-scroll {
  width: 100%;
  white-space: nowrap;
  margin-top: 24rpx;
}

.tabs-scroll .tabs {
  display: inline-flex;
  position: static;
  background: transparent;
  padding: 0 32rpx 12rpx;
}

.tabs-scroll .tab {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-right: 12rpx;
  flex: none;
}

.settle-hero-amount {
  margin-top: 16rpx;
  font-size: 80rpx;
  font-weight: 800;
  color: #ffffff;
  line-height: 1.1;
}

.settle-hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx 40rpx;
  margin-top: 16rpx;
  font-size: 25rpx;
  color: rgba(255, 255, 255, 0.62);
}

.settle-hero-meta .lime {
  color: var(--lime);
  font-weight: 700;
}

.settle-hero-meta .white {
  color: #ffffff;
  font-weight: 700;
}

.settle-hero-meta .amber {
  color: #ffd08a;
}

.settle-hero-note {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-top: 20rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.45);
}

.settle-no {
  font-size: 32rpx;
  font-weight: 800;
}

.settle-area {
  font-size: 40rpx;
  font-weight: 800;
  color: var(--ink);
}

.settle-area text {
  font-size: 22rpx;
  color: var(--muted);
  font-weight: 700;
  margin-left: 4rpx;
}

.settle-money {
  font-size: 40rpx;
  font-weight: 800;
  color: var(--green-700);
}

.settle-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 2rpx dashed var(--line);
  font-size: 22rpx;
  color: var(--muted);
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

.footnote {
  text-align: center;
  margin: 36rpx 48rpx 12rpx;
  line-height: 1.7;
}
</style>
