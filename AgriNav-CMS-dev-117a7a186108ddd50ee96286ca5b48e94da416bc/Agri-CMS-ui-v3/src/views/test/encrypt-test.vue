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

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Encryptor, enableEncrypt } from '@/utils/encrypt'

defineOptions({ name: 'EncryptTest' })

const results = ref([])
const encryptor = new Encryptor()
const decryptForm = reactive({
  encryptedText: ''
})

onMounted(() => {
  // 启用加密
  enableEncrypt()
  // 设置AES密钥
  encryptor.aesKey = 'kLUxwiyyBpy5z5YUsY5xGVfXBFRYvs5PHrFp2IFxqm4='
})

function testEncryption() {
  // 测试GET请求数据
  const testGetData = {
    current: 1,
    size: 10,
    startTime: '2024-10-20',
    endTime: '2024-10-31'
  }

  // 测试POST请求数据
  const testPostData = {
    address: '河北省石家庄市',
    boundary: 'POLYGON((38.75888 115.263046,38.757946 115.364547,38.843135 115.357742,38.842666 115.269248,38.75888 115.263046))',
    centerPoint: '(38.800657 115.313646)',
    landArea: '116269.875',
    landName: '好好好试验田123',
    landType: '圈地',
    measurementMethod: '测量',
    ownerName: '小明sa',
    ownerPhone: '123456789'
  }

  try {
    // 清空之前的结果
    results.value = []

    // 测试GET请求数据加密
    results.value.push('=== 测试GET请求数据 ===')
    results.value.push('原始数据: ' + JSON.stringify(testGetData, null, 2))
    const encryptedGet = encryptor.encrypt(testGetData)
    results.value.push('加密结果: ' + encryptedGet)
    const decryptedGet = encryptor.decrypt(encryptedGet)
    results.value.push('解密结果: ' + decryptedGet)
    results.value.push('')

    // 测试POST请求数据加密
    results.value.push('=== 测试POST请求数据 ===')
    results.value.push('原始数据: ' + JSON.stringify(testPostData, null, 2))
    const encryptedPost = encryptor.encrypt(testPostData)
    results.value.push('加密结果: ' + encryptedPost)
    const decryptedPost = encryptor.decrypt(encryptedPost)
    results.value.push('解密结果: ' + decryptedPost)

    // 将加密结果复制到剪贴板
    navigator.clipboard.writeText(encryptedPost)
      .then(() => results.value.push('\n加密结果已复制到剪贴板'))
      .catch(err => console.error('复制失败:', err))

  } catch (error) {
    results.value.push('测试失败: ' + error.message)
    console.error('测试失败:', error)
  }
}

function testDecryption() {
  try {
    // 清空之前的结果
    results.value = []

    if (!decryptForm.encryptedText) {
      ElMessage.warning('请输入需要解密的内容')
      return
    }

    results.value.push('=== 测试解密 ===')
    results.value.push('加密内容: ' + decryptForm.encryptedText)

    // 解密
    const decrypted = encryptor.decrypt(decryptForm.encryptedText)
    results.value.push('解密结果: ' + decrypted)

    // 尝试解析JSON
    try {
      const jsonData = JSON.parse(decrypted)
      results.value.push('\n解析后的JSON数据:')
      results.value.push(JSON.stringify(jsonData, null, 2))
    } catch (e) {
      results.value.push('\n解密结果不是有效的JSON格式')
    }

  } catch (error) {
    results.value.push('解密失败: ' + error.message)
    console.error('解密失败:', error)
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
