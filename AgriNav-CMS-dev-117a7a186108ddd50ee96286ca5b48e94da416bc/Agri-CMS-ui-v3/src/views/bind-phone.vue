<template>
  <div class="bind-phone">
    <div class="bind-container">
      <h3 class="title">绑定手机号</h3>
      <div class="wechat-info" v-if="wechatUser">
        <img :src="wechatUser.avatar" class="avatar" />
        <span class="nickname">{{ wechatUser.nickname }}</span>
      </div>

      <el-form ref="bindFormRef" :model="bindForm" :rules="bindRules" class="bind-form">
        <el-form-item prop="phoneNumber">
          <el-input
            v-model="bindForm.phoneNumber"
            placeholder="请输入手机号"
            clearable>
            <template #prefix>
              <i class="el-icon-mobile-phone"></i>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="captcha">
          <el-input
            v-model="bindForm.captcha"
            placeholder="请输入验证码"
            style="width: 63%"
            clearable>
            <template #prefix>
              <i class="el-icon-message"></i>
            </template>
          </el-input>
          <el-button
            class="captcha-button"
            type="primary"
            @click="getBindCaptcha"
            :disabled="captchaTimer > 0">
            {{ captchaTimer > 0 ? `${captchaTimer}秒后重试` : '获取验证码' }}
          </el-button>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="bind-button"
            @click="confirmBind"
            :loading="loading">
            确认绑定
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { bindWechat, getCaptcha } from '@/api/login'
import { setToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

defineOptions({ name: 'BindPhone' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const bindFormRef = ref(null)
const wechatUser = ref(null)
const bindForm = reactive({
  phoneNumber: '',
  captcha: ''
})
const bindRules = {
  phoneNumber: [
    { required: true, trigger: 'blur', message: '请输入手机号' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  captcha: [
    { required: true, trigger: 'blur', message: '请输入验证码' }
  ]
}
const loading = ref(false)
const captchaTimer = ref(0)

onMounted(() => {
  const userDetailsStr = route.query.userDetails
  if (!userDetailsStr) {
    console.error('未获取到用户信息')
    ElMessage.error('未获取到用户信息')
    router.push('/login')
    return
  }

  try {
    wechatUser.value = JSON.parse(decodeURIComponent(userDetailsStr))
    console.log('解析到的微信用户信息:', wechatUser.value)
  } catch (e) {
    console.error('解析微信用户数据失败:', e)
    ElMessage.error('获取用户信息失败')
    router.push('/login')
  }
})

function getBindCaptcha() {
  if (!bindForm.phoneNumber) {
    ElMessage.warning('请先输入手机号')
    return
  }

  getCaptcha(bindForm.phoneNumber).then(response => {
    if (response.code === 200) {
      ElMessage.success('验证码发送成功')
      captchaTimer.value = 60
      const timer = setInterval(() => {
        captchaTimer.value--
        if (captchaTimer.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    }
  })
}

function confirmBind() {
  bindFormRef.value.validate(valid => {
    if (valid) {
      loading.value = true
      bindWechat(wechatUser.value, bindForm.phoneNumber, bindForm.captcha)
        .then(res => {
          if (res.code === 200) {
            ElMessage.success('绑定成功')

            // 存储token
            setToken(res.token)

            // 获取用户信息
            userStore.getInfo()
              .then(() => {
                // 生成路由
                permissionStore.generateRoutes()
                  .then(accessRoutes => {
                    // 添加路由
                    accessRoutes.forEach(accessRoute => {
                      router.addRoute(accessRoute)
                    })
                    // 跳转到首页
                    router.push({ path: '/' })
                      .catch(err => {
                        console.error('路由跳转失败:', err)
                      })
                  })
              })
          } else {
            ElMessage.error(res.msg || '绑定失败，请重试')
          }
        })
        .catch(error => {
          console.error('绑定请求失败:', error)
          ElMessage.error('绑定失败，请重试')
        })
        .finally(() => {
          loading.value = false
        })
    }
  })
}
</script>

<style lang="scss" scoped>
.bind-phone {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-image: url("../assets/images/login-background.jpg");
  background-size: cover;
}

.bind-container {
  width: 400px;
  padding: 30px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.title {
  text-align: center;
  margin-bottom: 30px;
  color: #303133;
}

.wechat-info {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;

  .avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    margin-right: 10px;
  }

  .nickname {
    font-size: 16px;
    font-weight: bold;
  }
}

.bind-form {
  margin-top: 20px;
}

.captcha-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.captcha-button {
  width: 30%;
  height: 40px;
  background-color: #409EFF;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;

  &:hover {
    background-color: #66b1ff;
  }

  &:disabled {
    background-color: #a0cfff;
    cursor: not-allowed;
  }
}

.bind-button {
  width: 100%;
  height: 40px;
  background-color: #409EFF;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;

  &:hover {
    background-color: #66b1ff;
  }

  &:disabled {
    background-color: #a0cfff;
    cursor: not-allowed;
  }
}
</style>
