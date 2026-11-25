import { defineStore } from 'pinia'
import { isMockEnabled } from '@/config'
import { fetchUnreadCount } from '@/api/message'

export type GpsPermission = 'unknown' | 'granted' | 'denied' | 'unavailable'

interface AppState {
  online: boolean
  networkType: string
  gpsPermission: GpsPermission
  unreadCount: number
  mockEnabled: boolean
  lastSyncAt: number
}

const GPS_KEY = 'app.gpsPermission'

export const useAppStore = defineStore('app', {
  state: (): AppState => ({
    online: true,
    networkType: 'unknown',
    gpsPermission: 'unknown',
    unreadCount: 0,
    mockEnabled: isMockEnabled(),
    lastSyncAt: 0
  }),
  getters: {
    networkLabel: (state) => (state.online ? '网络正常' : '网络已断开'),
    gpsLabel: (state) => {
      if (state.gpsPermission === 'granted') return '定位正常'
      if (state.gpsPermission === 'denied') return '定位未授权'
      if (state.gpsPermission === 'unavailable') return '定位不可用'
      return '定位未开启'
    }
  },
  actions: {
    initNetworkWatcher() {
      uni.getNetworkType({
        success: (res) => {
          this.networkType = res.networkType
          this.online = res.networkType !== 'none'
        }
      })
      uni.onNetworkStatusChange((res) => {
        this.online = res.isConnected
        this.networkType = res.networkType
      })
    },
    refreshGpsState() {
      const saved = uni.getStorageSync(GPS_KEY) as GpsPermission
      if (saved === 'granted' || saved === 'denied') {
        this.gpsPermission = saved
      } else {
        this.gpsPermission = 'unknown'
      }
    },
    setGpsPermission(value: GpsPermission) {
      this.gpsPermission = value
      uni.setStorageSync(GPS_KEY, value)
    },
    /** 申请定位权限并获取一次当前位置 */
    requestGps(): Promise<{ latitude: number; longitude: number } | null> {
      return new Promise((resolve) => {
        if (typeof uni.getLocation !== 'function') {
          this.setGpsPermission('unavailable')
          resolve(null)
          return
        }
        uni.getLocation({
          type: 'gcj02',
          success: (res) => {
            this.setGpsPermission('granted')
            resolve({ latitude: res.latitude, longitude: res.longitude })
          },
          fail: () => {
            this.setGpsPermission('denied')
            resolve(null)
          }
        })
      })
    },
    async refreshUnread() {
      try {
        this.unreadCount = await fetchUnreadCount()
      } catch (e) {
        this.unreadCount = 0
      }
    },
    markSynced() {
      this.lastSyncAt = Date.now()
    },
    switchMock(enabled: boolean) {
      this.mockEnabled = enabled
    }
  }
})
