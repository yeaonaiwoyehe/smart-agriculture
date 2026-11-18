import axios from 'axios'
import {Notification, MessageBox, Message, Loading} from 'element-ui'
import store from '@/store'
import {getToken} from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import {tansParams, blobValidate} from "@/utils/ruoyi";
import cache from '@/plugins/cache'
import {saveAs} from 'file-saver'
import { isEncryptPath } from '@/utils/encrypt/config'
import { encrypt, decrypt, isEncryptEnabled } from '@/utils/encrypt'

let downloadLoadingInstance;
// 是否显示重新登录
export let isRelogin = {show: false};
let isRepeatSubmit = true; // 默认开启防重复提交

// axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'
// 创建axios实例
const service = axios.create({
  // axios中请求配置有baseURL选项，表示请求URL公共部分
  baseURL: process.env.VUE_APP_BASE_API,
  // 超时
  timeout: 10000
})

// request拦截器
service.interceptors.request.use(config => {
  console.log('开始处理请求拦截...')

  // 1. 设置 Content-Type
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type'];
  } else {
    config.headers['Content-Type'] = 'application/json;charset=utf-8';
  }

  // 2. 判断是否需要加密
  const needEncrypt = isEncryptPath(config.url) && isEncryptEnabled()

  if (needEncrypt) {
    switch(config.method.toLowerCase()) {
      case 'get':
        if (config.params) {
          const encryptedParams = encrypt(config.params)
          let url = config.url + '?' + tansParams({ data: encryptedParams })
          url = url.slice(0, -1)
          config.params = {}
          config.url = url
        }
        break

      case 'post':
      case 'put':
        if (config.data && !(config.data instanceof FormData)) {
          console.log('处理POST/PUT请求数据加密')
          console.log('加密前的请求体数据:', config.data)
          const encryptedData = encrypt(config.data)
          config.data = encryptedData
          console.log('加密后的请求体数据:', config.data)
        }
        break
    }
  }

  // 3. 处理 POST 请求的 URL 参数（处理 @RequestParam）
  if (config.method.toLowerCase() === 'post' && config.params) {
    let url = config.url + '?' + tansParams(config.params);
    url = url.slice(0, -1);
    config.params = {};
    config.url = url;
  }

  // 4. 处理 token
  const isToken = (config.headers || {}).isToken === false
  if (getToken() && !isToken) {
    config.headers['Authorization'] = 'Bearer ' + getToken()
  }

  // 5. 添加来源标识
  config.headers['source'] = 'Web'

  // 6. 处理防重复提交
  if (!isRepeatSubmit && (config.method === 'post' || config.method === 'put')) {
    const requestObj = {
      url: config.url,
      data: typeof config.data === 'object' ? JSON.stringify(config.data) : config.data,
      time: new Date().getTime()
    }
    const requestSize = Object.keys(JSON.stringify(requestObj)).length; // 请求数据大小
    const limitSize = 5 * 1024 * 1024; // 限制存放数据5M
    if (requestSize >= limitSize) {
      console.warn(`[${config.url}]: ` + '请求数据大小超出允许的5M限制，无法进行防重复提交验证。')
      return config;
    }
    const sessionObj = cache.session.getJSON('sessionObj')
    if (sessionObj === undefined || sessionObj === null || sessionObj === '') {
      cache.session.setJSON('sessionObj', requestObj)
    } else {
      const s_url = sessionObj.url;                  // 请求地址
      const s_data = sessionObj.data;                // 请求数据
      const s_time = sessionObj.time;                // 请求时间
      const interval = 1000;                         // 间隔时间(ms)，小于此时间视为重复提交
      if (s_data === requestObj.data && requestObj.time - s_time < interval && s_url === requestObj.url) {
        const message = '数据正在处理，请勿重复提交';
        console.warn(`[${s_url}]: ` + message)
        return Promise.reject(new Error(message))
      } else {
        cache.session.setJSON('sessionObj', requestObj)
      }
    }
  }

  // 如果是绑定手机号的请求，不需要添加token
  if (config.url.includes('/system/user/bind') || config.url.includes('/captcha')) {
    return config;
  }

  return config
}, error => {
  console.error('请求拦截器错误:', error)
  return Promise.reject(error)
})

// 不需要错误提示的接口列表
const noErrorMessageApis = ['/validateQR'];

// 响应拦截器
service.interceptors.response.use(res => {
    console.log('开始处理响应拦截...')

    // 判断是否是二维码检查接口
    if (noErrorMessageApis.includes(res.config.url.replace(process.env.VUE_APP_BASE_API, ''))) {
      return res.data;  // 直接返回数
    }

    // 1. 判断是否需要解密
    const needDecrypt = isEncryptPath(res.config.url) && isEncryptEnabled()
    console.log('是否需要解密:', needDecrypt)

    if (needDecrypt && res.data) {
      console.log('进入解密处理分支')
      console.log('解密前响应数据res.data:', res.data)
      try {
        // // 检查是否为二进制数据
        // if (res.request.responseType === 'blob' || res.request.responseType === 'arraybuffer') {
        //   console.log('二进制数据，跳过解密')
        //   return res.data
        // }

        // 解密响应数据
        const encryptedData = res.data
        if (encryptedData) {
          console.log('开始解密响应数据')
          const decryptedStr = decrypt(encryptedData)
          res.data = JSON.parse(decryptedStr)
          console.log('响应数据解密完成')
        }
      } catch (error) {
        console.error('响应解密失败:', error)
        return Promise.reject(new Error('响应解密失败'))
      }
    }

    // 2. 处理响应状态码
    const code = res.data.code || 200
    const msg = errorCode[code] || res.data.msg || errorCode['default']

    if (code === 401) {
      if (!isRelogin.show) {
        isRelogin.show = true;
        MessageBox.confirm('登录状态已过期，您可以继续留在该页面，或者重新登录', '系统提示', {
          confirmButtonText: '重新登录',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          isRelogin.show = false;
          store.dispatch('LogOut').then(() => {
            location.href = '/index';
          })
        }).catch(() => {
          isRelogin.show = false;
        });
      }
      return Promise.reject('无效的会话，或者会话已过期，请重新登录。')
    } else if (code === 500) {
      Message({message: msg, type: 'error'})
      return Promise.reject(new Error(msg))
    } else if (code === 601) {
      Message({message: msg, type: 'warning'})
      return res.data
    } else if (code !== 200) {
      Notification.error({title: msg})
      return Promise.reject('error')
    } else {
      return res.data
    }
  },
  error => {
    console.error('响应拦截器错误:', error)
    let {message} = error;
    if (message == "Network Error") {
      message = "后端接口连接异常";
    } else if (message.includes("timeout")) {
      message = "系统接口请求超时";
    } else if (message.includes("Request failed with status code")) {
      message = "系统接口" + message.substr(message.length - 3) + "异常";
    }
    Message({message: message, type: 'error', duration: 5 * 1000})
    return Promise.reject(error)
  })

// 通用下载方法
export function download(url, params, filename, config) {
  downloadLoadingInstance = Loading.service({
    text: "正在下载数据，请稍候",
    spinner: "el-icon-loading",
    background: "rgba(0, 0, 0, 0.7)",
  })
  return service.post(url, params, {
    transformRequest: [(params) => {
      return tansParams(params)
    }],
    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
    responseType: 'blob',
    ...config
  }).then(async (data) => {
    const isBlob = blobValidate(data);
    if (isBlob) {
      const blob = new Blob([data])
      saveAs(blob, filename)
    } else {
      const resText = await data.text();
      const rspObj = JSON.parse(resText);
      const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode['default']
      Message.error(errMsg);
    }
    downloadLoadingInstance.close();
  }).catch((r) => {
    console.error(r)
    Message.error('下载文件出现错误，请联系管理员！')
    downloadLoadingInstance.close();
  })
}

export default service
