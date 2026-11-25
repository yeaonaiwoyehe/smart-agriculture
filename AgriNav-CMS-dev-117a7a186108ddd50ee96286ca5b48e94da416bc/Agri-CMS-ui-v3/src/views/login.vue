<template>
  <div class="login-page">
    <div class="login-bg" />
    <div class="login-overlay" />

    <div class="login-wrap">
      <div class="login-card">
        <div class="login-title">
          <div class="brand-mark">
            <span class="brand-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" width="22" height="22">
                <path d="M3 21h18" />
                <path d="M5 21V11l7-5 7 5v10" />
                <path d="M9 21v-6h6v6" />
              </svg>
            </span>
            <span class="brand-name">田界智航</span>
          </div>
          <h1 class="title">农机作业管控智能管理平台</h1>
          <p class="subtitle">AgriNav · 基于轨迹数据与 AI 智能体的作业全流程管理</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" class="login-form" @keyup.enter="onSubmit">
          <el-form-item prop="phone">
            <el-input
              v-model="form.phone"
              placeholder="请输入手机号"
              size="large"
              maxlength="11"
              clearable
            >
              <template #prefix>
                <el-icon class="input-icon"><Iphone /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="code">
            <div class="code-row">
              <el-input
                v-model="form.code"
                placeholder="请输入验证码"
                size="large"
                maxlength="6"
              >
                <template #prefix>
                  <el-icon class="input-icon"><Lock /></el-icon>
                </template>
              </el-input>
              <el-button
                class="code-btn"
                size="large"
                :disabled="countdown > 0"
                @click="sendCode"
              >
                {{ countdown > 0 ? `${countdown}s 后重试` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>

          <el-button
            class="login-btn"
            type="primary"
            size="large"
            :loading="loading"
            @click="onSubmit"
          >
            登 录
          </el-button>
        </el-form>

        <div class="divider">
          <span>其他登录方式</span>
        </div>

        <div class="social-row">
          <button class="social-btn social-wechat" aria-label="微信登录" title="微信登录">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
              <path d="M8.69 11.55c-.29 0-.53-.24-.53-.53s.24-.53.53-.53.53.24.53.53-.24.53-.53.53zm5.83 0c-.29 0-.53-.24-.53-.53s.24-.53.53-.53.53.24.53.53-.24.53-.53.53zM9.5 4C5.36 4 2 6.87 2 10.42c0 1.95 1.07 3.7 2.73 4.85l-.68 2.05 2.38-1.22c.75.22 1.56.34 2.4.34.27 0 .53-.01.79-.04-.17-.54-.27-1.11-.27-1.71 0-3.02 2.9-5.46 6.48-5.46.22 0 .43.01.64.02C15.78 6.21 12.9 4 9.5 4zm6.15 7.92c-3.16 0-5.71 2.14-5.71 4.78 0 2.63 2.55 4.77 5.71 4.77.67 0 1.32-.09 1.92-.27l1.94 1.03-.53-1.64c1.42-.94 2.34-2.37 2.34-3.89 0-2.64-2.55-4.78-5.67-4.78zm-1.9 3.2c-.24 0-.44-.2-.44-.44s.2-.44.44-.44.44.2.44.44-.2.44-.44.44zm3.83 0c-.24 0-.44-.2-.44-.44s.2-.44.44-.44.44.2.44.44-.2.44-.44.44z"/>
            </svg>
          </button>
          <button class="social-btn social-sms" aria-label="短信登录" title="短信登录">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="5" width="18" height="14" rx="2" />
              <path d="M7 10h.01M11 10h.01M15 10h.01M7 14h10" />
            </svg>
          </button>
        </div>

        <div class="login-footer">
          <span>© {{ year }} 田界智航 · 智慧农业作业管控平台</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Iphone, Lock } from '@element-plus/icons-vue'
import { getCaptcha } from '@/api/login'
import { authErrorMessage, initializeAuthState } from '@/permission'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const formRef = ref(null)
const loading = ref(false)
const countdown = ref(0)

const form = reactive({ phone: '', code: '' })
const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为 6 位数字', trigger: 'blur' }
  ]
}

const year = computed(() => new Date().getFullYear())

let timer = null
function startCountdown() {
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}

async function sendCode() {
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    ElMessage.warning('请先输入正确的手机号')
    return
  }
  try {
    const res = await getCaptcha(form.phone)
    startCountdown()
    if (res?.mockCode !== undefined && res?.mockCode !== null) {
      ElMessage.success(`演示验证码：${res.mockCode}`)
    } else {
      ElMessage.success('验证码已发送')
    }
  } catch (err) {
    ElMessage.error(err?.msg || err?.message || '验证码发送失败，请稍后重试')
  }
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await userStore.login(form.phone, form.code)
    await initializeAuthState({ force: true })

    ElMessage.success('登录成功，欢迎回来')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.push(redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/dashboard')
  } catch (err) {
    console.error('[login] 登录链路失败', err)
    ElMessage.error(authErrorMessage(err, '登录失败，请稍后重试'))
    await userStore.fedLogout()
    permissionStore.resetRoutes()
    appStore.resetAuth()
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.login-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: #0d3a2a;
  font-family: var(--font-sans);
}

.login-bg {
  position: absolute;
  inset: 0;
  background: url('/images/login-bg.jpg') center/cover no-repeat;
}

.login-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(6, 32, 23, 0.55) 0%, rgba(14, 58, 41, 0.25) 45%, rgba(20, 107, 69, 0.18) 100%),
    linear-gradient(90deg, rgba(6, 32, 23, 0.35) 0%, rgba(6, 32, 23, 0) 55%);
}

.login-wrap {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 clamp(24px, 6vw, 10vw);
}

.login-card {
  flex-shrink: 0;
  width: min(440px, 92vw);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(18px) saturate(160%);
  -webkit-backdrop-filter: blur(18px) saturate(160%);
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: var(--radius-lg);
  box-shadow:
    0 24px 48px rgba(6, 32, 23, 0.22),
    0 8px 16px rgba(6, 32, 23, 0.08);
  padding: 40px 44px 32px;
  color: var(--text-1);
}

.login-title {
  margin-bottom: 28px;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(27, 134, 85, 0.1);
  border-radius: 999px;
  color: var(--brand-700);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  margin-bottom: 16px;
}
.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--brand-600);
}

.title {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.35;
  color: var(--brand-950);
  letter-spacing: 0.3px;
}

.subtitle {
  margin-top: 8px;
  font-size: 13px;
  color: var(--text-2);
  line-height: 1.6;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.input-icon {
  color: var(--text-3);
  font-size: 16px;
}

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;
}
.code-row :deep(.el-input) {
  flex: 1;
}
.code-btn {
  flex-shrink: 0;
  min-width: 120px;
  color: var(--brand-600);
  border-color: var(--brand-400);
  background: rgba(31, 174, 107, 0.08);
}
.code-btn:hover {
  color: var(--brand-700);
  border-color: var(--brand-500);
  background: rgba(31, 174, 107, 0.16);
}
.code-btn:disabled {
  color: var(--text-3);
  border-color: var(--border);
  background: var(--surface-3);
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--ai-gradient);
  box-shadow: var(--ai-glow);
  color: #fff;
}
.login-btn:hover {
  filter: brightness(1.06);
  box-shadow: 0 14px 32px rgba(31, 174, 107, 0.34);
}
.login-btn:active { filter: brightness(0.98); }

.divider {
  display: flex;
  align-items: center;
  margin: 20px 0 14px;
  font-size: 12px;
  color: var(--text-3);
}
.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(20, 32, 26, 0.16), transparent);
}
.divider span { padding: 0 12px; }

.social-row {
  display: flex;
  justify-content: center;
  gap: 28px;
}
.social-btn {
  width: 42px;
  height: 42px;
  border: 1px solid rgba(20, 32, 26, 0.12);
  background: rgba(255, 255, 255, 0.6);
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}
.social-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
.social-wechat { color: #1aad19; }
.social-wechat:hover { border-color: #1aad19; background: rgba(26, 173, 25, 0.08); }
.social-sms { color: var(--brand-600); }
.social-sms:hover { border-color: var(--brand-500); background: rgba(27, 134, 85, 0.08); }

.login-footer {
  margin-top: 22px;
  text-align: center;
  font-size: 12px;
  color: var(--text-3);
}

@media (max-width: 480px) {
  .login-card { padding: 28px 22px 24px; }
  .title { font-size: 18px; }
}
</style>