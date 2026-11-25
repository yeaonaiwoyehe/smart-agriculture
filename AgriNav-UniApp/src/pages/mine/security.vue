<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import SfIcon from '@/components/SfIcon.vue'
import { changePassword, changePhone, sendCaptcha } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { maskPhone } from '@/utils/format'
import { goBack } from '@/utils/nav'
import { confirmAction, errorMessage, hideLoading, showLoading, toast } from '@/utils/ui'

const user = useUserStore()
const newPhone = ref('')
const captcha = ref('')
const countdown = ref(0)
const sending = ref(false)
const changing = ref(false)

const oldPwd = ref('')
const newPwd = ref('')
const confirmPwd = ref('')
const pwdChanging = ref(false)

let timer: ReturnType<typeof setInterval> | null = null

const phoneValid = computed(() => /^1[3-9]\d{9}$/.test(newPhone.value))

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

function startCountdown() {
  countdown.value = 60
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0 && timer) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

async function handleSend() {
  if (countdown.value > 0 || sending.value) return
  if (!phoneValid.value) {
    toast('请输入正确的新手机号')
    return
  }
  sending.value = true
  try {
    const res = await sendCaptcha(newPhone.value)
    startCountdown()
    toast(res.mockCode ? `验证码已发送（演示：${res.mockCode}）` : '验证码已发送')
  } catch (e) {
    toast(errorMessage(e, '发送失败'))
  } finally {
    sending.value = false
  }
}

async function handleChangePhone() {
  if (!phoneValid.value) {
    toast('请输入正确的新手机号')
    return
  }
  if (newPhone.value === user.profile?.phoneNumber) {
    toast('新手机号与当前手机号相同')
    return
  }
  if (!/^\d{6}$/.test(captcha.value)) {
    toast('请输入 6 位验证码')
    return
  }
  const ok = await confirmAction({
    title: '确认换绑手机号',
    content: `将从 ${maskPhone(user.profile?.phoneNumber)} 换绑为 ${maskPhone(
      newPhone.value
    )}，换绑后需使用新手机号登录。`,
    confirmText: '确认换绑'
  })
  if (!ok) return
  changing.value = true
  showLoading('提交中')
  try {
    await changePhone(newPhone.value, captcha.value)
    await user.loadProfile()
    hideLoading()
    uni.showModal({
      title: '换绑成功',
      content: '手机号已更新，请使用新手机号登录。',
      showCancel: false,
      success: () => {
        user.logout()
        uni.reLaunch({ url: '/pages/login/index' })
      }
    })
  } catch (e) {
    hideLoading()
    toast(errorMessage(e, '换绑失败'))
  } finally {
    changing.value = false
  }
}

async function handleChangePassword() {
  if (oldPwd.value.length < 6 || newPwd.value.length < 6) {
    toast('密码长度不少于 6 位')
    return
  }
  if (newPwd.value !== confirmPwd.value) {
    toast('两次输入的新密码不一致')
    return
  }
  const ok = await confirmAction({
    title: '确认修改密码',
    content: '修改成功后需要用新密码登录，是否继续？',
    confirmText: '确认修改'
  })
  if (!ok) return
  pwdChanging.value = true
  showLoading('提交中')
  try {
    await changePassword(oldPwd.value, newPwd.value)
    hideLoading()
    oldPwd.value = ''
    newPwd.value = ''
    confirmPwd.value = ''
    uni.showModal({
      title: '修改成功',
      content: '密码已更新，请使用手机号 + 新密码登录。',
      showCancel: false
    })
  } catch (e) {
    hideLoading()
    toast(errorMessage(e, '修改失败'))
  } finally {
    pwdChanging.value = false
  }
}
</script>

<template>
  <view class="page-root">
    <view class="page-head">
      <view class="icon-btn" @tap="goBack('/pages/mine/index')">
        <SfIcon name="back" :size="40" color="#10231b" />
      </view>
      <text class="page-title grow">账号安全</text>
    </view>

    <view class="pad" style="margin-top: 24rpx">
      <view class="card" style="padding: 30rpx">
        <view class="row gap-10">
          <view class="security-ico">
            <SfIcon name="lock" :size="40" color="#14513d" />
          </view>
          <view class="grow">
            <text class="tiny muted">当前手机号</text>
            <text class="security-phone">{{ maskPhone(user.profile?.phoneNumber) }}</text>
          </view>
        </view>
        <text class="tiny muted" style="display: block; margin-top: 18rpx; line-height: 1.6">
          手机号是登录凭证，也是作业单与结算的归属依据。
        </text>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">换绑手机号</text>
      </view>
      <view class="card" style="padding: 8rpx 30rpx 30rpx">
        <view class="field" style="margin-top: 22rpx">
          <view class="field-label"><text>新手机号</text></view>
          <input
            v-model="newPhone"
            class="input"
            type="number"
            maxlength="11"
            placeholder="请输入新手机号"
            placeholder-class="ph"
          />
        </view>
        <view class="field">
          <view class="field-label"><text>验证码</text></view>
          <view class="code-row">
            <input
              v-model="captcha"
              class="input grow"
              type="number"
              maxlength="6"
              placeholder="6 位短信验证码"
              placeholder-class="ph"
            />
            <view
              class="code-btn"
              :class="{ 'code-btn--off': countdown > 0 }"
              @tap="handleSend"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </view>
          </view>
        </view>
        <view
          class="btn btn--primary btn--block"
          :class="{ 'btn--disabled': changing }"
          style="margin-top: 12rpx"
          @tap="handleChangePhone"
        >
          <SfIcon name="phone" :size="32" color="#ffffff" />
          <text>{{ changing ? '提交中...' : '确认换绑' }}</text>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">修改密码</text>
      </view>
      <view class="card" style="padding: 8rpx 30rpx 30rpx">
        <text class="tiny muted" style="display: block; margin-top: 22rpx; line-height: 1.6">
          后端当前以手机号后 8 位作为默认密码，修改后请牢记新密码。
        </text>
        <view class="field">
          <view class="field-label"><text>原密码</text></view>
          <input v-model="oldPwd" class="input" password placeholder="请输入原密码" placeholder-class="ph" />
        </view>
        <view class="field">
          <view class="field-label"><text>新密码</text></view>
          <input v-model="newPwd" class="input" password placeholder="不少于 6 位" placeholder-class="ph" />
        </view>
        <view class="field">
          <view class="field-label"><text>确认新密码</text></view>
          <input
            v-model="confirmPwd"
            class="input"
            password
            placeholder="再次输入新密码"
            placeholder-class="ph"
          />
        </view>
        <view
          class="btn btn--ghost btn--block"
          :class="{ 'btn--disabled': pwdChanging }"
          style="margin-top: 12rpx"
          @tap="handleChangePassword"
        >
          <SfIcon name="edit" :size="32" color="#10231b" />
          <text>{{ pwdChanging ? '提交中...' : '修改密码' }}</text>
        </view>
      </view>
    </view>

    <view class="notice" style="margin: 28rpx 36rpx 48rpx">
      <SfIcon name="shield" :size="28" color="#6a7b70" />
      <text>换绑手机号或修改密码后需要重新登录，敏感操作均有审计记录。</text>
    </view>
  </view>
</template>

<style scoped>
.security-ico {
  width: 76rpx;
  height: 76rpx;
  border-radius: 22rpx;
  background: var(--surface-2);
  border: 2rpx solid var(--line);
  display: grid;
  place-items: center;
  flex: none;
}

.security-phone {
  display: block;
  margin-top: 2rpx;
  font-size: 32rpx;
  font-weight: 800;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.code-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.code-row .input {
  width: auto;
  min-width: 0;
  flex: 1;
}

.code-btn {
  flex: none;
  width: 210rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 26rpx;
  border: 2rpx solid var(--green-700);
  background: var(--surface-2);
  color: var(--green-700);
  font-size: 25rpx;
  font-weight: 700;
}

.code-btn--off {
  border-color: var(--line);
  color: var(--muted-2);
}

.btn--disabled {
  opacity: 0.6;
}

.ph {
  color: #8d9c91;
}
</style>
