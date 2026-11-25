import { createApp } from 'vue'
import Cookies from 'js-cookie'

import App from './App.vue'
import router from './router'
import './permission' // permission control

import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// --- 样式引入 ---
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/display.css'
import 'virtual:svg-icons-register' // icon

// 保留你重构的新 UI 样式
import './styles/tokens.css'
import './styles/layout.css'
import './styles/components.css'
import './styles/ai.css'
import './styles/element.css'

// 兼容旧版图标字体 (同事加的)
import '@/assets/styles/element-icons.css' 
import '@/assets/styles/element-plus-override.scss' 

// --- 插件与全局方法 ---
import directive from './directive' 
import plugins from './plugins' 
import { download } from '@/utils/request'
import { getDicts } from '@/api/system/dict/data'
import { getConfigKey } from '@/api/system/config'
import { parseTime, resetForm, addDateRange, selectDictLabel, selectDictLabels, handleTree } from '@/utils/ruoyi'

// --- 全局组件引入 ---
import Pagination from '@/components/Pagination/index.vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Editor from '@/components/Editor/index.vue'
import FileUpload from '@/components/FileUpload/index.vue'
import ImageUpload from '@/components/ImageUpload/index.vue'
import ImagePreview from '@/components/ImagePreview/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import DictData from '@/components/DictData'
import ElementIcons from '@/components/ElementIcons'
import SvgIcon from '@/components/SvgIcon/index.vue'

const app = createApp(App)

// --- 全局方法挂载 ---
app.config.globalProperties.getDicts = getDicts
app.config.globalProperties.getConfigKey = getConfigKey
app.config.globalProperties.parseTime = parseTime
app.config.globalProperties.resetForm = resetForm
app.config.globalProperties.addDateRange = addDateRange
app.config.globalProperties.selectDictLabel = selectDictLabel
app.config.globalProperties.selectDictLabels = selectDictLabels
app.config.globalProperties.download = download
app.config.globalProperties.handleTree = handleTree

// --- 全局组件挂载 ---
app.component('DictTag', DictTag)
app.component('SvgIcon', SvgIcon)
app.component('svg-icon', SvgIcon)
app.component('Pagination', Pagination)
app.component('RightToolbar', RightToolbar)
app.component('Editor', Editor)
app.component('FileUpload', FileUpload)
app.component('ImageUpload', ImageUpload)
app.component('ImagePreview', ImagePreview)

// --- 图标注册 ---
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// --- 插件注册 ---
app.use(createPinia())
app.use(router)
app.use(directive)
app.use(plugins)
app.use(ElementIcons)
DictData.install(app)

// element-plus 的 size 配置
const sizeMap = { medium: 'default', large: 'large', small: 'small', default: 'default' }
const size = sizeMap[Cookies.get('size')] || 'default'
app.use(ElementPlus, { locale: zhCn, size })

// 错误处理
app.config.errorHandler = (err) => {
  console.error('[Vue Error]', err)
}

app.mount('#app')