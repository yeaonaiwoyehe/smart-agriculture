import { createApp } from 'vue'
import Cookies from 'js-cookie'

import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/display.css'

// 兼容原有 element-ui 图标字体（页面中大量使用 <i class="el-icon-xxx">）
import '@/assets/styles/element-icons.css'

import '@/assets/styles/index.scss' // global css
import '@/assets/styles/ruoyi.scss' // ruoyi css
import '@/assets/styles/element-plus-override.scss' // Element Plus 视觉对齐

import App from './App.vue'
import router from './router'
import pinia from './stores'
import directive from './directive' // directive
import plugins from './plugins' // plugins
import { download } from '@/utils/request'

import 'virtual:svg-icons-register' // icon
import './permission' // permission control
import { getDicts } from '@/api/system/dict/data'
import { getConfigKey } from '@/api/system/config'
import { parseTime, resetForm, addDateRange, selectDictLabel, selectDictLabels, handleTree } from '@/utils/ruoyi'
// 分页组件
import Pagination from '@/components/Pagination/index.vue'
// 自定义表格工具组件
import RightToolbar from '@/components/RightToolbar/index.vue'
// 富文本组件
import Editor from '@/components/Editor/index.vue'
// 文件上传组件
import FileUpload from '@/components/FileUpload/index.vue'
// 图片上传组件
import ImageUpload from '@/components/ImageUpload/index.vue'
// 图片预览组件
import ImagePreview from '@/components/ImagePreview/index.vue'
// 字典标签组件
import DictTag from '@/components/DictTag/index.vue'
// 字典数据组件
import DictData from '@/components/DictData'
// element-ui 图标类名兼容组件
import ElementIcons from '@/components/ElementIcons'
// 图标组件
import SvgIcon from '@/components/SvgIcon/index.vue'

const app = createApp(App)

// 全局方法挂载
app.config.globalProperties.getDicts = getDicts
app.config.globalProperties.getConfigKey = getConfigKey
app.config.globalProperties.parseTime = parseTime
app.config.globalProperties.resetForm = resetForm
app.config.globalProperties.addDateRange = addDateRange
app.config.globalProperties.selectDictLabel = selectDictLabel
app.config.globalProperties.selectDictLabels = selectDictLabels
app.config.globalProperties.download = download
app.config.globalProperties.handleTree = handleTree

// 全局组件挂载
app.component('DictTag', DictTag)
app.component('SvgIcon', SvgIcon)
app.component('svg-icon', SvgIcon)
app.component('Pagination', Pagination)
app.component('RightToolbar', RightToolbar)
app.component('Editor', Editor)
app.component('FileUpload', FileUpload)
app.component('ImageUpload', ImageUpload)
app.component('ImagePreview', ImagePreview)

// Element Plus 图标全局注册
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(directive)
app.use(plugins)
app.use(ElementIcons)
DictData.install(app)

// element-ui 的 size 与 element-plus 不完全一致
const sizeMap = { medium: 'default', large: 'large', small: 'small', default: 'default' }
const size = sizeMap[Cookies.get('size')] || 'default'
app.use(ElementPlus, { locale: zhCn, size })

app.config.errorHandler = (err) => {
  console.error('[Vue Error]', err)
}

app.mount('#app')
