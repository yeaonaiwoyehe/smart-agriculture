import { defineStore } from 'pinia'
import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { disableEncrypt } from '@/utils/encrypt'
import defaultAvatar from '@/assets/images/profile.jpg'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    name: '',
    avatar: '',
    roles: [],
    permissions: []
  }),

  actions: {
    // 登录
    login(userInfo) {
      const username = userInfo.username.trim()
      const password = userInfo.password
      const code = userInfo.code
      const uuid = userInfo.uuid
      return new Promise((resolve, reject) => {
        login(username, password, code, uuid).then(res => {
          setToken(res.token)
          this.token = res.token
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 获取用户信息
    getInfo() {
      return new Promise((resolve, reject) => {
        getInfo().then(res => {
          const user = res.user
          const avatar = user.avatar || defaultAvatar
          if (res.roles && res.roles.length > 0) {
            this.roles = res.roles
            this.permissions = res.permissions
          } else {
            this.roles = ['ROLE_DEFAULT']
          }
          this.name = user.userName
          this.avatar = avatar
          resolve(res)
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 退出系统
    logout() {
      // 先禁用加密，并且移除token
      disableEncrypt()
      removeToken()

      return new Promise((resolve, reject) => {
        logout(this.token).then(() => {
          this.token = ''
          this.roles = []
          this.permissions = []
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 前端登出
    fedLogout() {
      return new Promise(resolve => {
        this.token = ''
        removeToken()
        resolve()
      })
    }
  }
})
