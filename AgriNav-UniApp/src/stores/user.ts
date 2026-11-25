import { defineStore } from 'pinia'
import type { UserProfile } from '@/types'
import { isMockEnabled } from '@/config'
import * as authApi from '@/api/auth'
import * as userApi from '@/api/user'
import {
  clearAuth,
  getCachedUser,
  getRsaPublicKey,
  getToken,
  setCachedUser,
  setReadyAt,
  setRsaPublicKey,
  setToken
} from '@/utils/storage'

interface UserState {
  token: string
  rsaPublicKey: string
  profile: UserProfile | null
  loading: boolean
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: '',
    rsaPublicKey: '',
    profile: null,
    loading: false
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    displayName: (state) => state.profile?.nickName || '机手',
    phone: (state) => state.profile?.phoneNumber || ''
  },
  actions: {
    restore() {
      const token = getToken()
      // 演示模式遗留的 token 在真实后端下一定 401，启动时直接清除，避免反复跳登录
      if (token.startsWith('mock-token-') && !isMockEnabled()) {
        clearAuth()
        this.token = ''
        this.rsaPublicKey = ''
        this.profile = null
        return
      }
      this.token = token
      this.rsaPublicKey = getRsaPublicKey()
      this.profile = getCachedUser<UserProfile>()
    },
    async loginByPhone(phoneNumber: string, captcha?: string) {
      this.loading = true
      try {
        const res = await authApi.login(phoneNumber, captcha)
        this.token = res.token
        setToken(res.token)
        if (res.rsa_public_key) {
          this.rsaPublicKey = res.rsa_public_key
          setRsaPublicKey(res.rsa_public_key)
        }
        await this.loadProfile()
        setReadyAt(Date.now())
      } finally {
        this.loading = false
      }
    },
    async loadProfile() {
      const profile = await authApi.getInfo()
      this.profile = profile
      setCachedUser(profile)
      return profile
    },
    async saveProfile(patch: Partial<UserProfile>) {
      const profile = await authApi.updateProfile(patch)
      this.profile = profile
      setCachedUser(profile)
      return profile
    },
    async loadMachinery() {
      return userApi.fetchMachineryList()
    },
    logout() {
      void authApi.logout()
      this.token = ''
      this.rsaPublicKey = ''
      this.profile = null
      clearAuth()
    }
  }
})
