<script setup lang="ts">
import { ref } from 'vue'
import SfIcon from '@/components/SfIcon.vue'
import { goAgent, goBack } from '@/utils/nav'

const openIndex = ref<number | null>(0)

const faqs = [
  {
    q: '如何接收并开始一个任务？',
    a: '进入「任务」页，在「待接收」中选择任务查看详情，确认后点击「接收任务」；返回「作业」页选择该任务并点击「开始作业」，系统开始计时并记录轨迹。'
  },
  {
    q: '作业过程中断网怎么办？',
    a: '演示模式下数据保存在本机；接入真实后端后，轨迹会先写入本地队列，网络恢复后按顺序补传。断网期间仍可继续作业，但请保持定位开启。'
  },
  {
    q: '作业面积和金额是怎么算出来的？',
    a: '面积与金额由业务系统（结算核算）根据有效轨迹与作业幅宽计算，App 只展示结果。智能助手不会自行估算或修改任何数字。'
  },
  {
    q: '为什么结算金额被扣减了？',
    a: '常见原因包括轨迹覆盖不足、地块部分区域未作业、重复作业等。可在结算详情查看扣减说明并提交复核，复核申请会生成回执编号。'
  },
  {
    q: '如何上报异常？',
    a: '进入「异常上报」选择问题类型，填写地点、时间与现象，建议附现场照片，确认提交后会生成工单编号，可在工单列表跟踪处理进度。'
  },
  {
    q: 'AI 助手能做什么？',
    a: '可以查询今日任务、作业状态、结算与扣减原因，也可以发起开工、完工与异常上报。写操作都会先二次确认，执行后返回回执编号。'
  },
  {
    q: '语音输入是真实的语音识别吗？',
    a: '当前演示版本的语音入口为模拟识别，会随机返回示例问句。接入原生语音识别插件后即可按住说话。'
  }
]

function toggle(index: number) {
  openIndex.value = openIndex.value === index ? null : index
}
</script>

<template>
  <view class="page-root">
    <view class="page-head">
      <view class="icon-btn" @tap="goBack('/pages/mine/index')">
        <SfIcon name="back" :size="40" color="#10231b" />
      </view>
      <text class="page-title grow">使用帮助</text>
      <view class="icon-btn" @tap="goAgent('怎么接收任务？')">
        <SfIcon name="spark" :size="36" color="#14513d" />
      </view>
    </view>

    <view class="pad" style="margin-top: 24rpx">
      <view class="help-hero">
        <view class="help-hero-ico">
          <SfIcon name="help" :size="46" color="#c8f45a" />
        </view>
        <view class="grow">
          <text class="help-hero-title">不会操作？直接问助手</text>
          <text class="help-hero-sub">支持语音提问，回答只使用已审核话术</text>
        </view>
        <view class="btn btn--lime btn--sm" @tap="goAgent()">去问</view>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">常见问题</text>
        <text class="tiny muted">{{ faqs.length }} 条</text>
      </view>
      <view class="card list-card">
        <view v-for="(item, index) in faqs" :key="index" class="faq-item">
          <view class="faq-q" @tap="toggle(index)">
            <text class="faq-index">{{ index + 1 }}</text>
            <text class="faq-question grow">{{ item.q }}</text>
            <SfIcon
              name="chevron"
              :size="28"
              :color="openIndex === index ? '#14513d' : '#8d9c91'"
            />
          </view>
          <text v-if="openIndex === index" class="faq-a">{{ item.a }}</text>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">联系人工</text>
      </view>
      <view class="card list-card">
        <view class="list-item">
          <view class="ico-box">
            <SfIcon name="user" :size="36" color="#14513d" />
          </view>
          <view class="grow">
            <text class="list-title">作业调度</text>
            <text class="list-desc">0455-8888123 · 现场问题优先联系</text>
          </view>
          <SfIcon name="phone" :size="32" color="#1c6a4f" />
        </view>
        <view class="list-item">
          <view class="ico-box">
            <SfIcon name="phone" :size="36" color="#14513d" />
          </view>
          <view class="grow">
            <text class="list-title">技术支持</text>
            <text class="list-desc">0455-8888900 · 工作日 8:00-18:00</text>
          </view>
          <SfIcon name="phone" :size="32" color="#1c6a4f" />
        </view>
      </view>
    </view>

    <view class="notice" style="margin: 28rpx 36rpx 48rpx">
      <SfIcon name="shield" :size="28" color="#6a7b70" />
      <text>农机手端只开放经过审核的固定话术与常见问题，不提供管理制度的自由问答。</text>
    </view>
  </view>
</template>

<style scoped>
.help-hero {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 30rpx;
  border-radius: 40rpx;
  background: linear-gradient(120deg, #0e3b2e, #1b6449);
  color: #ffffff;
  box-shadow: 0 16rpx 40rpx rgba(8, 40, 30, 0.18);
}

.help-hero-ico {
  width: 84rpx;
  height: 84rpx;
  border-radius: 26rpx;
  background: rgba(200, 244, 90, 0.16);
  display: grid;
  place-items: center;
  flex: none;
}

.help-hero-title {
  display: block;
  font-size: 31rpx;
  font-weight: 800;
}

.help-hero-sub {
  display: block;
  margin-top: 4rpx;
  font-size: 23rpx;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.5;
}

.faq-item {
  border-bottom: 2rpx solid var(--line);
}

.faq-item:last-child {
  border-bottom: none;
}

.faq-q {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 28rpx 30rpx;
}

.faq-q:active {
  background: var(--surface-2);
}

.faq-index {
  width: 44rpx;
  height: 44rpx;
  border-radius: 14rpx;
  background: var(--surface-2);
  border: 2rpx solid var(--line);
  color: var(--green-700);
  font-size: 22rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
}

.faq-question {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--ink);
  line-height: 1.45;
}

.faq-a {
  display: block;
  padding: 0 30rpx 28rpx 92rpx;
  font-size: 25rpx;
  color: var(--muted);
  line-height: 1.75;
}

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
</style>
