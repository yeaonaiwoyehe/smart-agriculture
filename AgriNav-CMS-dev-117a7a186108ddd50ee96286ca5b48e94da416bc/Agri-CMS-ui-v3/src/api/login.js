import request from '@/utils/request'

// 登录方法

export function login(phoneNumber, captcha) {
  return request({
    url: '/login',
    method: 'post',
    params: {
      phoneNumber,
      captcha
    }
  })
}

// 注册方法
export function register(data) {
  return request({
    url: '/register',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 获取用户详细信息
export function getInfo() {
  return request({
    url: '/getInfo',
    method: 'get'
  })
}

// 退出方法
export function logout() {
  return request({
    url: '/logout',
    method: 'post'
  })
}

// 获取验证码
export function getCodeImg() {
  return request({
    url: '/captchaImage',
    headers: {
      isToken: false
    },
    method: 'get',
    timeout: 20000
  })
}

export function getCaptcha(phoneNumber) {
  return request({
    url: '/captcha',
    method: 'get',
    params: {
      phoneNumber
    }
  })
}

// 微信登录绑定手机号
export function bindWechat(details, phoneNumber, captcha) {
  return request({
    url: '/system/user/bind',
    method: 'post',
    data: details,
    params: {
      phoneNumber,
      captcha
    }
  })
}

// 处理OAuth回调
export function loginOAuth(code) {
  return request({
    url: '/loginOAuth',
    method: 'get',
    params: {code}
  })
}

export function getCaptchaQR() {
  return request({
    url: '/captchaQR',
    method: 'get'
  })
}

// 验证二维码状态
export function validateQR(uuid) {
  return request({
    url: '/validateQR',
    method: 'get',
    params: { uuid }
  })
}
