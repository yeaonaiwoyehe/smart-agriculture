<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import { useUserStore } from '@/stores/user'
import { maskPhone } from '@/utils/format'
import { go, goBack } from '@/utils/nav'
import { errorMessage, hideLoading, showLoading, toast } from '@/utils/ui'

const user = useUserStore()
const nickName = ref('')
const phone = ref('')
const deptName = ref('')
const saving = ref(false)

const avatarText = computed(() => (nickName.value || user.displayName || '机').slice(0, 1))

onLoad(() => {
  bind()
})

watch(
  () => user.profile,
  () => bind()
)

function bind() {
  nickName.value = user.profile?.nickName || ''
  phone.value = user.profile?.phoneNumber || ''
  deptName.value = user.profile?.deptName || ''
}

async function handleSave() {
  if (!nickName.value.trim()) {
    toast('请输入姓名')
    return
  }
  saving.value = true
  showLoading('保存中')
  try {
    await user.saveProfile({ nickName: nickName.value.trim() })
    hideLoading()
    toast('已保存', 'success')
  } catch (e) {
    hideLoading()
    toast(errorMessage(e, '保存失败'))
  } finally {
    saving.value = false
  }
}

function changePhoneTip() {
  go('/pages/mine/security')
}
</script>

<template>
  <view class="page-root">
    <view class="page-head">
      <view class="icon-btn" @tap="goBack('/pages/mine/index')">
        <SfIcon name="back" :size="40" color="#10231b" />
      </view>
      <text class="page-title grow">个人资料</text>
      <view class="icon-btn" @tap="handleSave">
        <SfIcon name="check" :size="38" color="#14513d" />
      </view>
    </view>

    <view class="pad" style="margin-top: 24rpx">
      <view class="card" style="padding: 30rpx">
        <view class="row gap-10">
          <view class="profile-avatar mini">{{ avatarText }}</view>
          <view class="grow">
            <text class="profile-name-text">{{ nickName || '未设置姓名' }}</text>
            <text class="profile-sub-text">
              {{ maskPhone(phone) }} · {{ deptName || '未分配作业队' }}
            </text>
          </view>
          <text class="status status--ok">农机手</text>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">基本信息</text>
        <text class="tiny muted">用于作业单与结算归属</text>
      </view>

      <view class="card" style="padding: 8rpx 30rpx 30rpx">
        <view class="field" style="margin-top: 22rpx">
          <view class="field-label"><text>姓名</text><text class="tiny muted">{{ nickName.length }}/12</text></view>
          <input
            v-model="nickName"
            class="input"
            maxlength="12"
            placeholder="请输入姓名"
            placeholder-class="ph"
          />
        </view>

        <view class="field" @tap="changePhoneTip">
          <view class="field-label">
            <text>手机号</text>
            <text class="tiny field-action">换绑</text>
          </view>
          <view class="readonly">
            <text>{{ maskPhone(phone) }}</text>
            <SfIcon name="chevron" :size="30" color="#8d9c91" />
          </view>
        </view>

        <view class="field">
          <view class="field-label"><text>所属作业队</text></view>
          <view class="readonly"><text>{{ deptName || '未分配' }}</text></view>
        </view>

        <view class="field">
          <view class="field-label"><text>角色</text></view>
          <view class="readonly"><text>农机手</text></view>
        </view>
      </view>
    </view>

    <view class="pad" style="margin-top: 32rpx">
      <view class="btn btn--primary btn--block" :class="{ 'btn--disabled': saving }" @tap="handleSave">
        <SfIcon name="check" :size="34" color="#ffffff" />
        <text>{{ saving ? '保存中...' : '保存资料' }}</text>
      </view>
    </view>

    <view class="notice" style="margin: 28rpx 36rpx 48rpx">
      <SfIcon name="shield" :size="28" color="#6a7b70" />
      <text>姓名与手机号用于作业单与结算归属，请保持真实有效。</text>
    </view>
  </view>
</template>

<style scoped>
.profile-avatar.mini {
  width: 100rpx;
  height: 100rpx;
  border-radius: 30rpx;
  font-size: 42rpx;
}

.profile-name-text {
  display: block;
  font-size: 36rpx;
  font-weight: 800;
  line-height: 1.2;
  color: var(--ink);
}

.profile-sub-text {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: var(--muted);
}

.field-action {
  color: var(--green-700);
  font-weight: 700;
}

.readonly {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  height: 88rpx;
  padding: 0 26rpx;
  border-radius: 26rpx;
  border: 2rpx solid var(--line);
  background: var(--surface-2);
  font-size: 30rpx;
  color: var(--ink-2);
}

.btn--disabled {
  opacity: 0.6;
}

.ph {
  color: #8d9c91;
}
</style>
