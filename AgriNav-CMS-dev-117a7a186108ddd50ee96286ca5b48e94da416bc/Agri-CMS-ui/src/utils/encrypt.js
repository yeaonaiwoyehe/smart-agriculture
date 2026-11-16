import JSEncrypt from 'jsencrypt/bin/jsencrypt.min'
import CryptoJS from 'crypto-js'
import request from '@/utils/request'

// 加密开关状态
let encryptEnabled = false

// 定义加密器类
class Encryptor {
  constructor() {
    this.publicKey = null
    this.aesKey = null
  }

  // 初始化RSA密钥
  async initRSAKey(rsaPublicKey) {
    if (!rsaPublicKey) {
      throw new Error('RSA公钥不能为空')
    }

    try {
      console.log('开始初始化RSA')
      this.publicKey = rsaPublicKey
      this.aesKey = this.generateAESKey()
      const encryptedAesKey = this.rsaEncrypt(this.aesKey)
      await this.exchangeKey(encryptedAesKey)
      console.log('密钥交换完成')
      return true
    } catch (error) {
      console.error('加密初始化失败:', error)
      this.publicKey = null
      this.aesKey = null
      throw error
    }
  }

  // RSA加密
  rsaEncrypt(data) {
    const encrypt = new JSEncrypt()
    encrypt.setPublicKey(this.publicKey)
    return encrypt.encrypt(data)
  }

  // 密钥交换
  async exchangeKey(encryptedAesKey) {
    return request({
      url: '/system/key/exchange',
      method: 'post',
      headers: {
        encrypt: false,
        source: 'Web'
      },
      data: encryptedAesKey
    })
  }

  // AES加密
  encrypt(data) {
    if (!this.aesKey) {
      throw new Error('AES密钥未初始化')
    }

    const content = typeof data === 'string' ? data : JSON.stringify(data)
    const iv = CryptoJS.lib.WordArray.random(16)
    const keyBytes = CryptoJS.enc.Base64.parse(this.aesKey)

    const encrypted = CryptoJS.AES.encrypt(content, keyBytes, {
      iv: iv,
      mode: CryptoJS.mode.CBC,
      padding: CryptoJS.pad.Pkcs7
    })

    const combined = CryptoJS.lib.WordArray.create()
    combined.concat(iv)
    combined.concat(encrypted.ciphertext)

    return this.base64URLEncode(CryptoJS.enc.Base64.stringify(combined))
  }

  // AES解密
  decrypt(data) {
    if (!this.aesKey) {
      throw new Error('AES密钥未初始化')
    }

    try {
      const base64 = this.base64URLDecode(data)
      const combined = CryptoJS.enc.Base64.parse(base64)

      const iv = combined.clone()
      iv.sigBytes = 16
      iv.clamp()

      const ciphertext = combined.clone()
      ciphertext.words = ciphertext.words.slice(4)
      ciphertext.sigBytes = combined.sigBytes - 16

      const keyBytes = CryptoJS.enc.Base64.parse(this.aesKey)

      const decrypted = CryptoJS.AES.decrypt(
        { ciphertext: ciphertext },
        keyBytes,
        {
          iv: iv,
          mode: CryptoJS.mode.CBC,
          padding: CryptoJS.pad.Pkcs7
        }
      )

      return CryptoJS.enc.Utf8.stringify(decrypted)
    } catch (error) {
      console.error('解密失败:', error)
      throw error
    }
  }

  // Base64URL编码
  base64URLEncode(base64) {
    return base64.replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '')
  }

  // Base64URL解码
  base64URLDecode(base64url) {
    let base64 = base64url.replace(/-/g, '+').replace(/_/g, '/')
    while (base64.length % 4) {
      base64 += '='
    }
    return base64
  }

  // 生成AES密钥
  generateAESKey() {
    const randomBytes = CryptoJS.lib.WordArray.random(32)
    return CryptoJS.enc.Base64.stringify(randomBytes)
  }

  //获取RSA公钥
  getPublicKey() {
    return request({
      url: '/system/key/public',
      method: 'get',
      headers: {
        encrypt: false,
        source: 'Web'
      }
    })
  }
}

// 创建单例
const encryptor = new Encryptor()

// 导出加密开关相关方法
export const enableEncrypt = () => { encryptEnabled = true }
export const disableEncrypt = () => { encryptEnabled = false }
export const isEncryptEnabled = () => encryptEnabled

// 导出加密相关方法
export const initRSAKey = (rsaPublicKey) => encryptor.initRSAKey(rsaPublicKey)
export const encrypt = (data) => encryptor.encrypt(data)
export const decrypt = (data) => encryptor.decrypt(data)

// 导出类和实例
export { Encryptor, encryptor}
