import { API_BASE_URL } from '@/config'
import { getToken } from '@/utils/storage'

/** 图片上传：POST /common/upload（multipart，字段名 file） */
export function uploadFile(filePath: string): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${API_BASE_URL}/common/upload`,
      filePath,
      name: 'file',
      header: {
        Authorization: getToken(),
        source: 'App'
      },
      timeout: 60000,
      success: (res) => {
        try {
          const body = JSON.parse(res.data || '{}')
          if (body.code !== 200) {
            reject(new Error(body.msg || '上传失败'))
            return
          }
          const url = body.url || body.fileName || body.data?.url
          resolve(url || '')
        } catch (e) {
          reject(new Error('上传返回解析失败'))
        }
      },
      fail: (err) => reject(new Error(err.errMsg || '上传失败'))
    })
  })
}
