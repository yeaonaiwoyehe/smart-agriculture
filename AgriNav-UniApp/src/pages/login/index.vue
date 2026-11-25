<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import { sendCaptcha } from '@/api/auth'
import { isMockEnabled } from '@/config'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { errorMessage, toast } from '@/utils/ui'
import { getToken } from '@/utils/storage'

const user = useUserStore()
const app = useAppStore()

const phone = ref('')
const captcha = ref('')
const sending = ref(false)
const logging = ref(false)
const countdown = ref(0)
const error = ref('')
const mock = isMockEnabled()
let timer: ReturnType<typeof setInterval> | null = null

const phoneValid = computed(() => /^1[3-9]\d{9}$/.test(phone.value))

onLoad(() => {
  // 以本地存储为准：登录失效时请求层会清除存储，避免用内存中的旧 token 反复跳首页造成死循环
  if (getToken()) {
    uni.reLaunch({ url: '/pages/home/index' })
  }
})

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
    error.value = '请输入正确的手机号'
    return
  }
  sending.value = true
  error.value = ''
  try {
    const res = await sendCaptcha(phone.value)
    startCountdown()
    toast(res.mockCode ? `验证码已发送（演示：${res.mockCode}）` : '验证码已发送')
  } catch (e) {
    error.value = errorMessage(e, '验证码发送失败')
  } finally {
    sending.value = false
  }
}

async function handleLogin() {
  if (!phoneValid.value) {
    error.value = '请输入正确的手机号'
    return
  }
  if (captcha.value && !/^\d{6}$/.test(captcha.value)) {
    error.value = '验证码为 6 位数字'
    return
  }
  logging.value = true
  error.value = ''
  try {
    await user.loginByPhone(phone.value, captcha.value || undefined)
    await app.refreshUnread()
    toast('登录成功')
    uni.reLaunch({ url: '/pages/home/index' })
  } catch (e) {
    error.value = errorMessage(e, '登录失败')
  } finally {
    logging.value = false
  }
}

function fillDemo() {
  phone.value = '13800001234'
  captcha.value = '123456'
}
</script>

<template>
  <view class="login">
    <view class="login-brand">
      <view class="login-logo">
        <SfIcon name="leaf" :size="46" color="#082a21" />
      </view>
      <view>
        <text class="login-brand-name">田野通</text>
        <text class="login-brand-en">AGRI PILOT</text>
      </view>
    </view>

    <text class="login-title">一句话，<text class="login-title-em">把地干明白。</text></text>
    <text class="login-sub">
      农机手的智能作业助手。查任务、接单、开工、报异常、看结算，说一句就行。
    </text>

    <view class="login-status">
      <view class="sync-dot" :class="{ 'sync-dot--off': !app.online }" />
      <text>{{ app.online ? '服务在线 · Agent 工具已连接' : '网络不可用 · 仅本地指令可用' }}</text>
    </view>

    <text class="login-label">手机号登录</text>

    <view class="login-form">
      <view class="login-field" :class="{ 'login-field--error': !!error && !phoneValid }">
        <SfIcon name="user" :size="36" color="rgba(255,255,255,0.6)" />
        <input
          v-model="phone"
          class="login-input"
          type="number"
          maxlength="11"
          placeholder="请输入手机号"
          placeholder-class="login-ph"
        />
      </view>

      <view class="login-field">
        <SfIcon name="lock" :size="36" color="rgba(255,255,255,0.6)" />
        <input
          v-model="captcha"
          class="login-input"
          type="number"
          maxlength="6"
          placeholder="请输入验证码"
          placeholder-class="login-ph"
          @confirm="handleLogin"
        />
        <text class="pwd-eye" @tap="handleSend">
          {{ countdown > 0 ? countdown + 's' : sending ? '发送中' : '获取验证码' }}
        </text>
      </view>

      <text v-if="error" class="login-err">{{ error }}</text>

      <button class="btn btn--lime login-submit" :disabled="logging" @tap="handleLogin">
        {{ logging ? '登录中…' : '登 录' }}
      </button>

      <text v-if="mock" class="login-demo" @tap="fillDemo">
        演示模式：任意手机号 + 验证码 123456<br />
        点击这里一键填入演示账号
      </text>
      <text v-else class="login-demo">
        验证码由平台短信服务下发，有效期 3 分钟
      </text>
    </view>

    <view class="login-foot">
      <text class="login-foot-line">每个账号只能查看和操作本人的任务、作业单、轨迹与结算数据。</text>
      <text class="login-foot-line">所有关键操作均记录操作人、定位、指令与回执编号。</text>
    </view>
  </view>
</template>

<style scoped>
.login-input {
  flex: 1;
  min-width: 0;
  height: 100%;
  background: transparent;
  border: none;
  color: #fff;
  font-size: 30rpx;
}

.login-ph {
  color: rgba(255, 255, 255, 0.38);
}
</style>
