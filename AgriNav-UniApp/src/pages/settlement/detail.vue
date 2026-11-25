<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import type { Settlement, SettlementItem } from '@/types'
import { fetchSettlementDetail, submitReview } from '@/api/settlement'
import { SETTLEMENT_STATUS_TEXT } from '@/utils/constants'
import { formatArea, formatMoney } from '@/utils/format'
import { goBack } from '@/utils/nav'
import { confirmAction, errorMessage, hideLoading, showLoading, toast } from '@/utils/ui'

const settlement = ref<Settlement | null>(null)
const settlementId = ref('')
const loading = ref(true)
const error = ref('')
const activeItem = ref<SettlementItem | null>(null)
const reason = ref('')
const images = ref<string[]>([])
const submitting = ref(false)

const totalDeduction = computed(() =>
  (settlement.value?.items || []).reduce((sum, i) => sum + (i.deduction || 0), 0)
)

onLoad(async (query) => {
  // 结算单 ID 为雪花字符串，保持字符串原样传递
  const id = String(query?.id || '')
  settlementId.value = id
  if (!id) {
    error.value = '缺少结算单号'
    loading.value = false
    return
  }
  await load(id)
})

async function load(id: string) {
  loading.value = true
  error.value = ''
  try {
    settlement.value = await fetchSettlementDetail(id)
  } catch (e) {
    error.value = errorMessage(e, '结算单加载失败')
  } finally {
    loading.value = false
  }
}

function statusClass(status: Settlement['status']): string {
  if (status === 'paid') return 'status--ok'
  if (status === 'reviewing') return 'status--review'
  return 'status--todo'
}

function openReview(item: SettlementItem) {
  activeItem.value = item
  reason.value = ''
  images.value = []
}

function closeReview() {
  activeItem.value = null
}

function chooseImage() {
  uni.chooseImage({
    count: 3 - images.value.length,
    sizeType: ['compressed'],
    success: (res) => {
      images.value = [...images.value, ...(res.tempFilePaths as string[])].slice(0, 3)
    }
  })
}

async function handleSubmitReview() {
  if (!settlement.value || !activeItem.value) return
  if (reason.value.trim().length < 5) {
    toast('请至少填写 5 个字的复核原因')
    return
  }
  const ok = await confirmAction({
    title: '提交复核申请',
    content: `结算单：${settlement.value.settlementNo}\n地块：${activeItem.value.plotName}\n原因：${reason.value}\n\n提交后结算单将进入复核状态。`,
    confirmText: '提交'
  })
  if (!ok) return
  submitting.value = true
  showLoading('提交中')
  try {
    const res = await submitReview({
      settlementId: settlement.value.settlementId,
      itemId: activeItem.value.itemId,
      reason: reason.value.trim(),
      images: images.value
    })
    hideLoading()
    activeItem.value = null
    uni.showModal({
      title: '提交成功',
      content: `回执编号：${res.receiptNo}\n${res.message}`,
      showCancel: false
    })
    await load(settlement.value.settlementId)
  } catch (e) {
    hideLoading()
    toast(errorMessage(e, '复核提交失败'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <view class="page-root">
    <view class="page-head">
      <view class="icon-btn" @tap="goBack('/pages/settlement/index')">
        <SfIcon name="back" :size="40" color="#10231b" />
      </view>
      <text class="page-title grow">结算明细</text>
      <view class="icon-btn" @tap="load(settlementId)">
        <SfIcon name="refresh" :size="38" color="#10231b" />
      </view>
    </view>

    <view v-if="loading" class="pad" style="margin-top: 24rpx">
      <view class="card" style="padding: 32rpx">
        <view class="skeleton" style="height: 40rpx; width: 46%" />
        <view class="skeleton" style="height: 24rpx; width: 66%; margin-top: 18rpx" />
        <view class="skeleton" style="height: 76rpx; width: 42%; margin-top: 28rpx" />
        <view class="skeleton" style="height: 108rpx; margin-top: 28rpx" />
      </view>
    </view>

    <view v-else-if="error" class="pad" style="margin-top: 24rpx">
      <view class="alert-banner">
        <view class="icon-wrap">
          <SfIcon name="alert" :size="36" color="#c47a0a" />
        </view>
        <text class="grow">{{ error }}</text>
        <view class="btn btn--soft btn--sm" @tap="load(settlementId)">重试</view>
      </view>
    </view>

    <template v-else-if="settlement">
      <view class="settle-hero">
        <view class="row-between">
          <view class="grow">
            <text class="settle-label">结算单号</text>
            <view class="settle-no">{{ settlement.settlementNo }}</view>
            <text class="settle-period">{{ settlement.periodStart }} 至 {{ settlement.periodEnd }}</text>
          </view>
          <text class="status" :class="statusClass(settlement.status)">
            {{ SETTLEMENT_STATUS_TEXT[settlement.status] }}
          </text>
        </view>

        <view class="settle-amount">
          <text class="settle-currency">¥</text>
          <text class="settle-num">{{ formatMoney(settlement.totalAmount) }}</text>
        </view>

        <view class="settle-metrics">
          <view class="settle-metric">
            <text class="metric-num">{{ formatArea(settlement.totalArea) }}</text>
            <text class="metric-label">结算面积(亩)</text>
          </view>
          <view class="settle-metric">
            <text class="metric-num">{{ formatMoney(settlement.paidAmount) }}</text>
            <text class="metric-label">已付(元)</text>
          </view>
          <view class="settle-metric">
            <text class="metric-num metric-num--warn">{{ formatMoney(totalDeduction) }}</text>
            <text class="metric-label">扣减(元)</text>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-head">
          <text class="section-title">结算明细</text>
          <text class="tiny muted">{{ (settlement.items || []).length }} 条</text>
        </view>

        <view v-if="!(settlement.items || []).length" class="card empty" style="padding: 56rpx 40rpx">
          <view class="empty-ico">
            <SfIcon name="wallet" :size="60" color="#1c6a4f" />
          </view>
          <text class="empty-title">暂无明细</text>
          <text class="tiny" style="display: block; margin-top: 8rpx">完成作业并结束上传后会自动核算</text>
        </view>

        <view v-for="item in settlement.items || []" :key="item.itemId" class="card order-card">
          <view class="row-between">
            <text class="plot-name">{{ item.plotName }}</text>
            <text class="item-amount">¥{{ formatMoney(item.amount) }}</text>
          </view>

          <view class="row gap-8" style="margin-top: 12rpx">
            <text class="plot-code">{{ item.workTypeName }}</text>
            <text class="tiny muted">{{ formatArea(item.area) }} 亩 × {{ item.unitPrice }} 元/亩</text>
          </view>

          <text class="item-no">作业单 {{ item.trajectoryInfoId }}</text>

          <view v-if="item.deductionReason" class="deduction">
            <view class="row-between">
              <text class="deduction-title">扣减 ¥{{ formatMoney(item.deduction) }}</text>
              <text
                v-if="settlement.status !== 'paid'"
                class="deduction-action"
                @tap="openReview(item)"
              >
                申请复核
              </text>
            </view>
            <text class="deduction-reason">{{ item.deductionReason }}</text>
          </view>
        </view>
      </view>

      <view class="notice" style="margin: 28rpx 36rpx 48rpx">
        <SfIcon name="shield" :size="28" color="#6a7b70" />
        <text>面积与金额均由业务系统核算，助手仅做展示，不参与计算。</text>
      </view>
    </template>

    <!-- 复核表单 -->
    <view v-if="activeItem" class="backdrop" @tap="closeReview" />
    <view v-if="activeItem" class="sheet">
      <view class="sheet-grip" />
      <view class="row-between">
        <view class="grow">
          <text class="sheet-title">申请复核</text>
          <text class="sheet-sub">{{ activeItem.plotName }} · {{ activeItem.workTypeName }}</text>
        </view>
        <view class="icon-btn" @tap="closeReview">
          <SfIcon name="close" :size="36" color="#10231b" />
        </view>
      </view>

      <view class="field">
        <view class="field-label">
          <text>复核原因</text>
          <text class="tiny muted">{{ reason.length }}/200</text>
        </view>
        <textarea
          v-model="reason"
          class="textarea"
          maxlength="200"
          placeholder="请说明复核原因，例如：地块东南角积水未作业，扣减不合理"
          placeholder-class="ph"
        />
      </view>

      <view class="field">
        <view class="field-label">
          <text>现场照片（可选）</text>
          <text class="tiny muted">{{ images.length }}/3</text>
        </view>
        <view class="photo-grid">
          <view v-for="(img, idx) in images" :key="idx" class="thumb-wrap">
            <image :src="img" class="thumb" mode="aspectFill" />
            <view class="photo-del" @tap="images.splice(idx, 1)">
              <SfIcon name="close" :size="18" color="#ffffff" />
            </view>
          </view>
          <view v-if="images.length < 3" class="photo-add" @tap="chooseImage">
            <SfIcon name="camera" :size="40" color="#8d9c91" />
            <text>添加照片</text>
          </view>
        </view>
      </view>

      <view class="sheet-actions">
        <view class="btn btn--ghost" @tap="closeReview">取消</view>
        <view
          class="btn btn--primary"
          :class="{ 'btn--disabled': submitting }"
          @tap="handleSubmitReview"
        >
          <SfIcon v-if="submitting" name="refresh" :size="32" color="#ffffff" />
          <text>{{ submitting ? '提交中...' : '提交复核' }}</text>
        </view>
      </view>
      <text class="sheet-note">提交后结算单将进入复核状态，回执编号由业务系统生成。</text>
    </view>
  </view>
</template>

<style scoped>
.settle-label {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.58);
  letter-spacing: 0.05em;
}

.settle-no {
  margin-top: 6rpx;
  font-size: 36rpx;
  font-weight: 800;
  letter-spacing: 0.02em;
  font-variant-numeric: tabular-nums;
}

.settle-period {
  display: block;
  margin-top: 6rpx;
  font-size: 23rpx;
  color: rgba(255, 255, 255, 0.55);
}

.settle-amount {
  display: flex;
  align-items: baseline;
  margin-top: 24rpx;
  color: var(--lime);
}

.settle-currency {
  font-size: 32rpx;
  font-weight: 700;
  margin-right: 6rpx;
}

.settle-num {
  font-size: 76rpx;
  font-weight: 800;
  line-height: 1.05;
  font-variant-numeric: tabular-nums;
}

.settle-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin-top: 26rpx;
}

.settle-metric {
  background: rgba(255, 255, 255, 0.08);
  border: 2rpx solid rgba(255, 255, 255, 0.1);
  border-radius: 24rpx;
  padding: 16rpx 18rpx;
}

.metric-num {
  display: block;
  font-size: 32rpx;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.metric-num--warn {
  color: #ffd08a;
}

.metric-label {
  display: block;
  margin-top: 2rpx;
  font-size: 21rpx;
  color: rgba(255, 255, 255, 0.6);
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

.item-amount {
  font-size: 32rpx;
  font-weight: 800;
  color: var(--green-700);
  font-variant-numeric: tabular-nums;
}

.item-no {
  display: block;
  margin-top: 12rpx;
  font-size: 21rpx;
  color: var(--muted);
  word-break: break-all;
}

.deduction-title {
  font-size: 27rpx;
  font-weight: 700;
  color: #a1330a;
}

.deduction-action {
  font-size: 24rpx;
  font-weight: 700;
  color: var(--green-700);
  padding: 6rpx 0 6rpx 16rpx;
}

.deduction-reason {
  display: block;
  margin-top: 6rpx;
  font-size: 23rpx;
  color: #7b5300;
  line-height: 1.6;
}

.backdrop {
  position: fixed;
}

.sheet {
  position: fixed;
}

.photo-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.thumb-wrap {
  position: relative;
}

.thumb {
  width: 150rpx;
  height: 150rpx;
  border-radius: 20rpx;
}

.photo-del {
  position: absolute;
  top: -8rpx;
  right: -8rpx;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: rgba(6, 32, 25, 0.86);
  display: grid;
  place-items: center;
}

.photo-add {
  width: 150rpx;
  height: 150rpx;
  border-radius: 20rpx;
  border: 2rpx dashed var(--line);
  background: var(--surface-2);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  font-size: 22rpx;
  color: var(--muted);
}

.btn--disabled {
  opacity: 0.6;
}

.ph {
  color: #8d9c91;
}
</style>
