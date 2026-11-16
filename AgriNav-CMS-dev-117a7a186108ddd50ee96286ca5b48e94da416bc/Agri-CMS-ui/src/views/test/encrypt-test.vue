<template>
  <div>
    <el-button @click="testEncryption">测试加密</el-button>
    <el-divider></el-divider>
    <el-form :model="decryptForm" label-width="120px">
      <el-form-item label="待解密内容">
        <el-input
          type="textarea"
          v-model="decryptForm.encryptedText"
          :rows="4"
          placeholder="请输入需要解密的内容"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="testDecryption">测试解密</el-button>
      </el-form-item>
    </el-form>
    <div v-if="results.length">
      <h3>测试结果：</h3>
      <pre>{{ results.join('\n') }}</pre>
    </div>
  </div>
</template>

<script>
import { Encryptor, enableEncrypt } from '@/utils/encrypt'

export default {
  name: 'EncryptTest',
  data() {
    return {
      results: [],
      encryptor: null,
      decryptForm: {
        encryptedText: ''
      }
    }
  },
  created() {
    // 在组件创建时初始化加密器
    this.encryptor = new Encryptor()
    // 启用加密
    enableEncrypt()
    // 设置AES密钥
    this.encryptor.aesKey = "kLUxwiyyBpy5z5YUsY5xGVfXBFRYvs5PHrFp2IFxqm4="
  },
  methods: {
    testEncryption() {
      // 测试GET请求数据
      const testGetData = {
        current: 1,
        size: 10,
        startTime: "2024-10-20",
        endTime: "2024-10-31"
      }

      // 测试POST请求数据
      const testPostData = {
        address: "河北省石家庄市",
        boundary: "POLYGON((38.75888 115.263046,38.757946 115.364547,38.843135 115.357742,38.842666 115.269248,38.75888 115.263046))",
        centerPoint: "(38.800657 115.313646)",
        landArea: "116269.875",
        landName: "好好好试验田123",
        landType: "圈地",
        measurementMethod: "测量",
        ownerName: "小明sa",
        ownerPhone: "123456789"
      }

      try {
        // 清空之前的结果
        this.results = []

        // 测试GET请求数据加密
        this.results.push('=== 测试GET请求数据 ===')
        this.results.push('原始数据: ' + JSON.stringify(testGetData, null, 2))
        const encryptedGet = this.encryptor.encrypt(testGetData)
        this.results.push('加密结果: ' + encryptedGet)
        const decryptedGet = this.encryptor.decrypt(encryptedGet)
        this.results.push('解密结果: ' + decryptedGet)
        this.results.push('')

        // 测试POST请求数据加密
        this.results.push('=== 测试POST请求数据 ===')
        this.results.push('原始数据: ' + JSON.stringify(testPostData, null, 2))
        const encryptedPost = this.encryptor.encrypt(testPostData)
        this.results.push('加密结果: ' + encryptedPost)
        const decryptedPost = this.encryptor.decrypt(encryptedPost)
        this.results.push('解密结果: ' + decryptedPost)

        // 将加密结果复制到剪贴板
        navigator.clipboard.writeText(encryptedPost)
          .then(() => this.results.push('\n加密结果已复制到剪贴板'))
          .catch(err => console.error('复制失败:', err))

      } catch (error) {
        this.results.push('测试失败: ' + error.message)
        console.error('测试失败:', error)
      }
    },

    testDecryption() {
      try {
        // 清空之前的结果
        this.results = []

        if (!this.decryptForm.encryptedText) {
          this.$message.warning('请输入需要解密的内容')
          return
        }

        this.results.push('=== 测试解密 ===')
        this.results.push('加密内容: ' + this.decryptForm.encryptedText)

        // 解密
        const decrypted = this.encryptor.decrypt(this.decryptForm.encryptedText)
        this.results.push('解密结果: ' + decrypted)

        // 尝试解析JSON
        try {
          const jsonData = JSON.parse(decrypted)
          this.results.push('\n解析后的JSON数据:')
          this.results.push(JSON.stringify(jsonData, null, 2))
        } catch (e) {
          this.results.push('\n解密结果不是有效的JSON格式')
        }

      } catch (error) {
        this.results.push('解密失败: ' + error.message)
        console.error('解密失败:', error)
      }
    }
  }
}
</script>

<style scoped>
pre {
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
