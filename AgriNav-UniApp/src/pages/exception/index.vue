<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import type { FeedbackItem } from '@/types'
import type { ExceptionLabel } from '@/mock/exception'
import {
  fetchExceptionLabels,
  fetchFeedbackList,
  submitFeedback
} from '@/api/exception'
import { isMockEnabled } from '@/config'
import { FEEDBACK_STATUS_TEXT } from '@/utils/constants'
import { relativeTime } from '@/utils/format'
import { go, goBack } from '@/utils/nav'
import { confirmAction, errorMessage, hideLoading, showLoading, toast } from '@/utils/ui'

const labels = ref<ExceptionLabel[]>([])
const activeType = ref('machine')
const content = ref('')
const images = ref<string[]>([])
const tickets = ref<FeedbackItem[]>([])
const submitting = ref(false)
const loading = ref(false)
const error = ref('')
const realMode = !isMockEnabled()

const activeLabel = computed(
  () => labels.value.find((l) => l.code === activeType.value)?.name || '其他问题'
)

onShow(() => {
  void load()
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [labelList, list] = await Promise.all([
      fetchExceptionLabels().catch(() => [] as ExceptionLabel[]),
      fetchFeedbackList().catch(() => [] as FeedbackItem[])
    ])
    labels.value = labelList.length
      ? labelList
      : [
          { code: 'machine', name: '农机故障' },
          { code: 'plot', name: '地块问题' },
          { code: 'road', name: '道路受阻' },
          { code: 'gps', name: 'GPS/设备异常' },
          { code: 'weather', name: '天气环境' },
          { code: 'other', name: '其他问题' }
        ]
    // 真实模式下标签来自后端字典，默认选中第一个有效标签
    if (labels.value.length && !labels.value.some((l) => l.code === activeType.value)) {
      activeType.value = labels.value[0].code
    }
    tickets.value = list
  } catch (e) {
    error.value = errorMessage(e, '数据加载失败')
  } finally {
    loading.value = false
  }
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

async function handleSubmit() {
  if (content.value.trim().length < 5) {
    toast('请至少填写 5 个字的问题描述')
    return
  }
  const ok = await confirmAction({
    title: '确认提交异常上报',
    content: `类型：${activeLabel.value}\n描述：${content.value.trim()}\n照片：${images.value.length} 张\n\n提交后将生成工单，工作人员会尽快处理。`,
    confirmText: '提交上报'
  })
  if (!ok) return
  submitting.value = true
  showLoading('提交中')
  try {
    let uploaded: string[] = []
    if (realMode) {
      // 后端 Feedback 实体没有图片字段，照片无法保存，避免无意义上传
      if (images.value.length) toast('后端暂不支持保存反馈图片，本次仅提交文字描述')
    } else if (images.value.length) {
      uploaded = images.value
    }
    const item = await submitFeedback({
      type: activeType.value,
      content: content.value.trim(),
      images: uploaded
    })
    hideLoading()
    content.value = ''
    images.value = []
    uni.showModal({
      title: '提交成功',
      content: `工单编号：${item.receiptNo}\n类型：${item.typeName}\n\n可点击「查看工单」跟踪处理进度。`,
      showCancel: false,
      success: () => {
        void load()
        go(`/pages/exception/detail?id=${item.id}`)
      }
    })
  } catch (e) {
    hideLoading()
    toast(errorMessage(e, '提交失败'))
  } finally {
    submitting.value = false
  }
}

function openTicket(id: string) {
  go(`/pages/exception/detail?id=${id}`)
}

function feedbackStatusClass(status: FeedbackItem['status']): string {
  if (status === 'resolved') return 'status--ok'
  if (status === 'processing') return 'status--review'
  return 'status--todo'
}

function scrollToForm() {
  uni.pageScrollTo({ scrollTop: 0, duration: 250 })
}
</script>

<template>
  <view class="page-root">
    <view class="page-head">
      <view class="icon-btn" @tap="goBack('/pages/mine/index')">
        <SfIcon name="back" :size="40" color="#10231b" />
      </view>
      <text class="page-title grow">异常上报</text>
      <view class="icon-btn icon-btn--dark" @tap="scrollToForm">
        <SfIcon name="plus" :size="40" color="#c8f45a" />
      </view>
    </view>

    <view class="pad" style="margin-top: 24rpx">
      <view class="report-cta" @tap="scrollToForm">
        <view class="report-cta-icon">
          <SfIcon name="alert" :size="46" color="#c47a0a" />
        </view>
        <view class="grow">
          <text class="report-cta-title">上报作业异常</text>
          <text class="report-cta-desc">机器故障、地块障碍、道路问题，描述清楚即可生成工单</text>
        </view>
        <SfIcon name="chevron" :size="34" color="#8a5a12" />
      </view>
    </view>

    <view id="exception-form" class="section">
      <view class="section-head">
        <text class="section-title">提交上报</text>
        <text class="status status--review">{{ activeLabel }}</text>
      </view>

      <view class="card" style="padding: 30rpx">
        <view class="field" style="margin-top: 0">
          <view class="field-label"><text>问题类型</text></view>
          <view class="type-chips">
            <view
              v-for="label in labels"
              :key="label.code"
              class="type-chip"
              :class="{ 'type-chip--on': activeType === label.code }"
              @tap="activeType = label.code"
            >
              {{ label.name }}
            </view>
          </view>
        </view>

        <view class="field">
          <view class="field-label">
            <text>问题描述</text>
            <text class="tiny muted">{{ content.length }}/300</text>
          </view>
          <textarea
            v-model="content"
            class="textarea"
            maxlength="300"
            placeholder="请描述发生的位置、时间和现象，例如：永安村5号地块东侧，深松机左铲开裂，作业暂停"
            placeholder-class="ph"
          />
          <text class="tiny muted" style="display: block; margin-top: 10rpx">
            建议包含地点、时间、现象，便于队长定位处理。
          </text>
        </view>

        <view class="field">
          <view class="field-label">
            <text>现场照片</text>
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
              <text>现场照片</text>
            </view>
          </view>
        </view>

        <view
          class="btn btn--primary btn--block"
          :class="{ 'btn--disabled': submitting }"
          style="margin-top: 32rpx"
          @tap="handleSubmit"
        >
          <SfIcon name="upload" :size="34" color="#ffffff" />
          <text>{{ submitting ? '提交中...' : '提交上报' }}</text>
        </view>
        <text class="appendix-note" style="display: block; margin-top: 18rpx; text-align: center">
          提交前会二次确认；工单编号由业务系统生成，可在下方工单列表查看进度。
        </text>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">我的工单</text>
        <text class="tiny muted">{{ tickets.length }} 条</text>
      </view>

      <view v-if="error" class="alert-banner">
        <view class="icon-wrap">
          <SfIcon name="alert" :size="36" color="#c47a0a" />
        </view>
        <text class="grow">{{ error }}</text>
      </view>

      <view v-if="loading && !tickets.length" class="card" style="padding: 32rpx">
        <view class="skeleton" style="height: 34rpx; width: 40%" />
        <view class="skeleton" style="height: 24rpx; width: 82%; margin-top: 18rpx" />
        <view class="skeleton" style="height: 24rpx; width: 60%; margin-top: 12rpx" />
      </view>

      <view v-else-if="!tickets.length" class="card empty" style="padding: 56rpx 40rpx">
        <view class="empty-ico">
          <SfIcon name="alert" :size="60" color="#1c6a4f" />
        </view>
        <text class="empty-title">暂无异常工单</text>
        <text class="tiny" style="display: block; margin-top: 8rpx">
          遇到问题随时上报，队长会尽快联系您
        </text>
      </view>

      <view
        v-for="ticket in tickets"
        :key="ticket.id"
        class="card ticket"
        @tap="openTicket(ticket.id)"
      >
        <view class="row-between">
          <view class="row gap-8">
            <text class="status" :class="feedbackStatusClass(ticket.status)">
              {{ FEEDBACK_STATUS_TEXT[ticket.status] }}
            </text>
            <text class="plot-code">{{ ticket.typeName }}</text>
          </view>
          <SfIcon name="chevron" :size="30" color="#8d9c91" />
        </view>
        <text class="ticket-content">{{ ticket.content }}</text>
        <view class="ticket-foot">
          <text>{{ ticket.receiptNo }}</text>
          <text>{{ relativeTime(ticket.createTime) }}</text>
        </view>
      </view>
    </view>

    <view class="tiny muted" style="text-align: center; margin: 36rpx 48rpx 48rpx; line-height: 1.7">
      工单提交后会通知机耕队队长，处理进度可在工单详情中查看。
    </view>
  </view>
</template>

<style scoped>
.icon-btn--dark {
  background: var(--green-800);
  border-color: var(--green-800);
}

.report-cta {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 28rpx;
  border-radius: 40rpx;
  background: linear-gradient(115deg, #fff3df 0%, #ffe7c4 100%);
  border: 2rpx solid #f5d49c;
  color: #6f4300;
  box-shadow: 0 8rpx 24rpx rgba(122, 75, 5, 0.08);
}

.report-cta:active {
  transform: scale(0.99);
}

.report-cta-icon {
  width: 92rpx;
  height: 92rpx;
  border-radius: 28rpx;
  background: #ffffff;
  display: grid;
  place-items: center;
  flex: none;
}

.report-cta-title {
  display: block;
  font-size: 32rpx;
  font-weight: 800;
}

.report-cta-desc {
  display: block;
  margin-top: 4rpx;
  font-size: 23rpx;
  opacity: 0.8;
  line-height: 1.5;
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

.photo-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.thumb-wrap {
  position: relative;
}

.thumb {
  width: 160rpx;
  height: 160rpx;
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
  width: 160rpx;
  height: 160rpx;
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

.ticket:active {
  transform: scale(0.995);
}

.ticket-content {
  display: block;
  margin-top: 16rpx;
  font-size: 27rpx;
  color: var(--ink-2);
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.ticket-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-top: 18rpx;
  padding-top: 16rpx;
  border-top: 2rpx dashed var(--line);
  font-size: 21rpx;
  color: var(--muted);
}

.btn--disabled {
  opacity: 0.6;
}

.ph {
  color: #8d9c91;
}
</style>
