<template>
  <div class="app-container">
    <!-- 将整个内容包裹在一个flex容器中 -->
    <div class="content-wrapper">
      <!-- 主要内容区域 -->
      <div class="main-content">
        <!-- 原有的表单、表格等内容 -->
        <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="名称" prop="name">
            <el-input
              v-model="queryParams.name"
              placeholder="请输入农机找活名称"
              clearable
              style="width: 240px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="农机类型" prop="typeId">
            <el-select v-model="queryParams.typeId" placeholder="请选择农机类型" clearable style="width: 240px">
              <el-option
                v-for="item in typeOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="农机找活状态" clearable style="width: 240px">
              <el-option
                v-for="dict in dict.type.agricultural_machinery_status"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="作物类型" prop="cropId">
            <el-select
              v-model="queryParams.cropId"
              multiple
              placeholder="请选择作物类"
              clearable
              style="width: 240px"
            >
              <el-option
                v-for="item in cropOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="作业区域" prop="location">
            <el-input
              v-model="locationInput"
              placeholder="请输入作业区域，按回车键添加"
              style="width: 240px"
              @keyup.enter="handleLocationInputConfirm"
            >
              <template #append>
                <el-button icon="el-icon-plus" @click="handleLocationInputConfirm"></el-button>
              </template>
            </el-input>
            <div style="margin-top: 10px">
              <el-tag
                v-for="(loc, index) in queryParams.location"
                :key="index"
                closable
                :disable-transitions="false"
                @close="handleLocationClose(index)"
                style="margin-right: 10px; margin-bottom: 5px"
              >
                {{ loc }}
              </el-tag>
            </div>
          </el-form-item>
          <el-form-item label="排序" prop="orderByCreateTimeDesc">
            <el-switch
              v-model="queryParams.orderByCreateTimeDesc"
              active-text="最新发布"
              inactive-text="最早发布"
              @change="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="small" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="small" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button
              type="primary"
              plain
              icon="el-icon-plus"
              size="small"
              @click="handleAdd"
              v-hasPermi="['home:agriculturalMachinery:add']"
            >新增
            </el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table
          v-loading="loading"
          :data="agriculturalMachineryList"
        >
          <el-table-column label="名称" align="center" prop="name"/>
          <el-table-column label="农机类型" align="center" prop="typeName"/>
          <el-table-column label="联系方式" align="center" prop="phoneNumber"/>
          <el-table-column label="运粮车" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.havingGrainCarrier ? 'success' : 'info'">
                {{ scope.row.havingGrainCarrier ? '有' : '无' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center">
            <template #default="scope">
              <dict-tag :options="dict.type.agricultural_machinery_status" :value="scope.row.status"/>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template #default="scope">
              <span>{{ formatDate(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="截止时间" align="center" prop="endTime" width="180">
            <template #default="scope">
              <span>{{ formatDate(scope.row.endTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="200">
            <template #default="scope">
              <el-button
                size="small"
                link
                icon="el-icon-view"
                @click="handleView(scope.row)"
              >查看详情
              </el-button>
              <el-button
                size="small"
                link
                icon="el-icon-edit"
                @click="handleUpdate(scope.row)"
              >修改
              </el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                @click="handleDelete(scope.row)"
              >下架
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页区域 -->
      <div class="pagination-container">
        <pagination
          v-show="total>0"
          :total="total"
          v-model:page="pageNum"
          v-model:limit="pageSize"
          @pagination="getList"
        />
      </div>
    </div>

    <!-- 添加或修改农机找活对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入农机找活名称"/>
        </el-form-item>
        <el-form-item label="农机类型" prop="typeId">
          <el-select v-model="form.typeId" placeholder="请选择农机类型">
            <el-option
              v-for="item in typeOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="联系方式" prop="phoneNumber">
          <el-input v-model="form.phoneNumber" placeholder="请输联系方式"/>
        </el-form-item>
        <el-form-item label="作物类型" prop="cropId">
          <el-select v-model="form.cropId" multiple placeholder="请选择作物类型">
            <el-option
              v-for="item in cropOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="作业区域" prop="locations">
          <div v-for="(location, index) in form.locations" :key="index" style="margin-bottom: 10px">
            <el-cascader
              v-model="location.codes"
              :options="addressOptions"
              :props="addressProps"
              @change="(value) => handleAddressChange(value, index)"
              placeholder="请选择作业区域"
              style="width: 85%"
            ></el-cascader>
            <el-button link @click="removeLocation(index)" style="margin-left: 10px">删除</el-button>
          </div>
          <el-button link @click="addLocation" icon="el-icon-plus">添加作业区域</el-button>
        </el-form-item>
        <el-form-item label="运粮车" prop="havingGrainCarrier">
          <el-switch v-model="form.havingGrainCarrier"></el-switch>
        </el-form-item>
        <el-form-item label="图片" prop="image">
          <el-upload
            class="upload-demo"
            action="#"
            :http-request="uploadImage"
            :on-preview="handlePreview"
            :on-remove="handleRemove"
            v-model:file-list="fileList"
            list-type="picture">
            <el-button size="small" type="primary">点击上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="截止日期" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="date"
            placeholder="选择截止日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 添加查看详情的对话框 -->
    <el-dialog title="查看详情" v-model="viewDialogVisible" width="700px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="名称">{{ viewForm.name }}</el-descriptions-item>
        <el-descriptions-item label="农机类型">{{ viewForm.typeName }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ viewForm.phoneNumber }}</el-descriptions-item>
        <el-descriptions-item label="运粮车">
          <el-tag :type="viewForm.havingGrainCarrier ? 'success' : 'info'">
            {{ viewForm.havingGrainCarrier ? '有' : '无' }}
          </el-tag>
        </el-descriptions-item>
        <!-- 添加创建时间和截止时间的显示 -->
        <el-descriptions-item label="创建时间">{{ formatDate(viewForm.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="截止时间">{{ formatDate(viewForm.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="作物类型" :span="2">
          <el-tag
            v-for="(crop, index) in viewForm.crop"
            :key="index"
            size="small"
            type="success"
            style="margin-right: 5px"
          >{{ crop }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="作业区域" :span="2">
          <el-tag
            v-for="(location, index) in viewForm.location"
            :key="index"
            size="small"
            type="info"
            style="margin-right: 5px"
          >{{ location }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 图片轮播 -->
      <div class="carousel-container" v-if="viewForm.image && viewForm.image.length > 0">
        <h4 class="carousel-title">图片展示</h4>
        <el-carousel :height="carouselHeight" trigger="click" indicator-position="outside">
          <el-carousel-item v-for="(url, index) in viewForm.image" :key="index">
            <div class="carousel-image-container">
              <el-image
                :src="url"
                fit="contain"
                :preview-src-list="viewForm.image"
              >
                <template #error>
                  <div class="image-slot">
                    <i class="el-icon-picture-outline"></i>
                  </div>
                </template>
              </el-image>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="viewDialogVisible = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'AgriculturalMachinery',
  // 声明要使用的字典
  dicts: ['agricultural_machinery_status']
}
</script>

<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listAgriculturalMachinery,
  delAgriculturalMachinery,
  addAgriculturalMachinery,
  updateAgriculturalMachinery
} from '@/api/agriculturalMachinery/agriculturalMachinery'
import { listAgriculturalMachineryTypes } from '@/api/agriculturalMachinery/agriculturalMachineryType'
import { getCrops } from '@/api/field/crop'
import { getProvinces, getCities, getDistricts, getVillages, getHamlets } from '@/api/field/landAddress'

const { proxy } = getCurrentInstance()

const formRef = ref(null)
const queryFormRef = ref(null)
// 遮罩层
const loading = ref(true)
// 显示搜索
const showSearch = ref(true)
// 总条数
const total = ref(0)
// 农机找活表格数据
const agriculturalMachineryList = ref([])
// 弹出层标
const title = ref('')
// 是否显示弹出层
const open = ref(false)
// 状态数据字典
const statusOptions = ref([])
// 查询参数
const queryParams = ref({
  name: null,
  typeId: null,
  status: null,
  cropId: [],
  location: [],
  myAgriculturalMachinery: true,
  orderByCreateTimeDesc: true // 默认按最新发布排序
})
const pageNum = ref(1)
const pageSize = ref(10)
// 表单参数
const form = ref({
  id: null,
  name: null,
  typeId: null,
  phoneNumber: null,
  cropId: [],
  locations: [{ codes: [], address: '' }], // 修改为数组形式存储多个地址
  havingGrainCarrier: false,
  image: [],
  endTime: ''
})

// 自定义验证规则
const validatePhone = (rule, value, callback) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的11位手机号码'))
  } else {
    callback()
  }
}

// 表单校验
const rules = {
  name: [
    { required: true, message: '农活名称不能为', trigger: 'blur' }
  ],
  typeId: [
    { required: true, message: '农机类型不能为空', trigger: 'change' }
  ],
  phoneNumber: [
    { required: true, message: '联系方式不能为空', trigger: 'blur' },
    { validator: validatePhone, trigger: 'blur' }
  ],
  cropId: [
    { required: true, message: '作物类型不能为空', trigger: 'change' }
  ],
  locations: [
    { required: true, message: '至少选择一个作业区域', trigger: 'change' }
  ],
  endTime: [
    { required: true, message: '截止日期不能为空', trigger: 'change' }
  ]
}
const typeOptions = ref([])
const cropOptions = ref([])
const addressOptions = ref([])
const addressProps = {
  value: 'code',
  label: 'name',
  children: 'children',
  lazy: true,
  checkStrictly: true,
  lazyLoad: lazyLoadAddress
}
const fileList = ref([])
// 存储地址数据的缓存
const addressCache = new Map()
const viewDialogVisible = ref(false)
const viewForm = ref({
  name: '',
  typeName: '',
  phoneNumber: '',
  havingGrainCarrier: false,
  crop: [],
  location: [],
  image: []
})
const carouselHeight = ref('300px') // 轮播图高度
const locationInput = ref('') // 添加作业区域输入框

/** 查询农找活列表 */
function getList() {
  loading.value = true
  const params = {
    ...queryParams.value
  }
  listAgriculturalMachinery(params, pageNum.value, pageSize.value).then(response => {
    agriculturalMachineryList.value = response.data
    total.value = response.total
    loading.value = false
  })
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    name: null,
    typeId: null,
    phoneNumber: null,
    cropId: [],
    locations: [{ codes: [], address: '' }],
    havingGrainCarrier: false,
    image: [],
    endTime: ''
  }
  fileList.value = []
  proxy.resetForm('formRef')
}

/** 搜索按钮操作 */
function handleQuery() {
  pageNum.value = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm('queryFormRef')
  queryParams.value.cropId = []
  queryParams.value.location = []
  queryParams.value.myAgriculturalMachinery = true
  queryParams.value.orderByCreateTimeDesc = true // 重置为默认排序
  handleQuery()
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = '添加农机找活'
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()

  // 设置表单数据
  form.value = {
    id: row.id,
    name: row.name,
    // 直接使用 typeId，这样如果不修改就会保持原值
    typeId: row.typeId,
    phoneNumber: row.phoneNumber,
    // 直接使用 cropId 数组，这样如果不修改就会保持原值
    cropId: row.cropId || [],
    havingGrainCarrier: row.havingGrainCarrier,
    image: row.image || [],
    endTime: row.endTime,
    locations: []
  }

  // 处理图片列表
  if (form.value.image && form.value.image.length > 0) {
    fileList.value = form.value.image.map(url => ({
      name: url.substring(url.lastIndexOf('/') + 1),
      url: url
    }))
  }

  // 处理作业区域
  if (row.location) {
    const locationArray = typeof row.location === 'string'
      ? row.location.split('、')
      : Array.isArray(row.location) ? row.location : []

    form.value.locations = locationArray.map(addr => ({
      codes: [], // 这里会在后续异步填充
      address: addr
    }))

    // 异步获取每个地址的完整代码
    form.value.locations.forEach((location, index) => {
      getAddressCodesFromName(location.address, index)
    })
  }

  // 确保至少有一个作业区域输入框
  if (form.value.locations.length === 0) {
    form.value.locations = [{ codes: [], address: '' }]
  }

  open.value = true
  title.value = '修改农机找活'
}

/** 提交按钮 */
function submitForm() {
  formRef.value.validate(valid => {
    if (valid) {
      const formData = new FormData()

      // 处理表单数据
      const formDataObj = { ...form.value }
      // 确保使用原始的 typeId 和 cropId
      formDataObj.typeId = form.value.typeId
      formDataObj.cropId = form.value.cropId

      // 处理地址
      formDataObj.location = formDataObj.locations.map(loc => loc.address)
      delete formDataObj.locations

      // 处理图片
      // 1. 处理新上传的图片文件
      fileList.value.forEach(file => {
        if (file.raw) {
          formData.append('image', file.raw)
        }
      })

      // 2. 收集所有已有的图片URL
      const existingImages = fileList.value
        .filter(file => !file.raw)
        .map(file => file.url)

      // 3. 设置图片URL到表单数据
      formDataObj.image = existingImages

      // 将对象转换为JSON字符串，并添加到FormData中
      formData.append('agriculturalMachinery', JSON.stringify(formDataObj))

      if (form.value.id) {
        updateAgriculturalMachinery(formData).then(response => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addAgriculturalMachinery(formData).then(response => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const ids = row.id
  proxy.$modal.confirm('是否确下架该数据项').then(function () {
    return delAgriculturalMachinery(ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('下架成功')
  }).catch(() => {
  })
}

/** 获取农机类型选项 */
function getTypeOptions() {
  listAgriculturalMachineryTypes().then(response => {
    typeOptions.value = response.data.map(item => ({
      id: item.id,
      name: item.name
    }))
  })
}

/** 获取作物类型选项 */
function getCropOptions() {
  getCrops().then(response => {
    if (response.code === 200) {
      cropOptions.value = response.data.map(item => ({
        id: item.id,
        name: item.cropName
      }))
    } else {
      ElMessage.error('获取作物类型失败')
    }
  }).catch(error => {
    console.error('获取作物类型出错:', error)
    ElMessage.error('获取作物类型出错')
  })
}

/** 获取省份选项 */
function getProvinceOptions() {
  getProvinces().then(response => {
    if (response.code === 200) {
      addressOptions.value = response.data.map(item => ({
        code: item.code,
        name: item.name,
        children: []
      }))
    } else {
      ElMessage.error('获取省份列表失败')
    }
  })
}

/** 懒加载地选项 */
function lazyLoadAddress(node, resolve) {
  const { level } = node
  const parentCode = node.data ? node.data.code : ''

  // 如果已经有缓存的数据，直接使用缓存
  if (addressCache.has(parentCode)) {
    resolve(addressCache.get(parentCode))
    return
  }

  if (level === 0) {
    // 加载省份
    getProvinces().then(response => {
      if (response.code === 200) {
        const provinces = response.data.map(item => ({
          code: item.code,
          name: item.name,
          leaf: false
        }))
        addressCache.set(parentCode, provinces)
        resolve(provinces)
      } else {
        ElMessage.error('获取省份列表失败')
        resolve([])
      }
    })
  } else {
    // 加载下级地址
    const { code, name } = node.data
    const params = {
      code: code,
      name: name,
      level: level,
      pcode: level > 1 ? node.parent.data.code : 0,
      category: 0
    }

    let apiMethod
    switch (level) {
      case 1:
        apiMethod = getCities
        break
      case 2:
        apiMethod = getDistricts
        break
      case 3:
        apiMethod = getVillages
        break
      case 4:
        apiMethod = getHamlets
        break
      default:
        resolve([])
        return
    }

    apiMethod(params).then(response => {
      if (response.code === 200) {
        const children = response.data.map(item => ({
          code: item.code,
          name: item.name,
          leaf: level === 4
        }))
        addressCache.set(code, children)
        resolve(children)
      } else {
        ElMessage.error(`获取${['城市', '区县', '乡镇', '村镇'][level - 1]}列表失败`)
        resolve([])
      }
    }).catch(error => {
      console.error(`获取${['城市', '区县', '乡镇', '村镇'][level - 1]}列表出错:`, error)
      ElMessage.error(`获取${['城市', '区县', '乡镇', '村镇'][level - 1]}列表出错`)
      resolve([])
    })
  }
}

/** 处理地址变化 */
async function handleAddressChange(value, index) {
  if (!value || value.length === 0) {
    form.value.locations[index].address = ''
    return
  }

  let addressNames = []
  let currentNode = { children: addressOptions.value }

  for (let i = 0; i < value.length; i++) {
    const code = value[i]

    if (!currentNode.children || currentNode.children.length === 0) {
      const cachedChildren = addressCache.get(currentNode.code)
      if (cachedChildren) {
        currentNode.children = cachedChildren
      }
    }

    const matchedNode = currentNode.children.find(node => node.code === code)
    if (matchedNode) {
      addressNames.push(matchedNode.name)
      currentNode = matchedNode
    }
  }

  form.value.locations[index].address = addressNames.join('')
}

/** 添加地址选择器 */
function addLocation() {
  form.value.locations.push({ codes: [], address: '' })
}

/** 移除地址选择器 */
function removeLocation(index) {
  if (form.value.locations.length > 1) {
    form.value.locations.splice(index, 1)
  } else {
    ElMessage.warning('至少保留一个作业区域')
  }
}

/** 图片上传 */
function uploadImage(param) {
  fileList.value.push({
    name: param.file.name,
    url: URL.createObjectURL(param.file),
    raw: param.file // 保存原始文件对象
  })
}

/** 图片预览 */
function handlePreview(file) {
  // 实现图片预览逻辑
}

/** 图片移除 */
function handleRemove(file, fileListData) {
  fileList.value = fileListData
}

/** 格式化日期（使用/分隔符） */
function formatDate(dateString) {
  if (!dateString) return ''
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}/${month}/${day}`
}

/** 格式化作物名称 */
function formatCropNames(cropNames) {
  if (!cropNames) return '-'
  return Array.isArray(cropNames) ? cropNames.join('、') : cropNames
}

/** 格式化作业区域 */
function formatLocations(locations) {
  if (!locations) return ''
  return Array.isArray(locations) ? locations.join('、') : locations
}

/** 格式化单个地址（只显示最后一级） */
function formatSingleLocation(location) {
  if (!location) return ''
  const parts = location.split(',')
  return parts[parts.length - 1] || location
}

/** 查看详情按钮操作 */
function handleView(row) {
  viewForm.value = { ...row }
  viewDialogVisible.value = true
}

// 添加一个方法用于根据地址名称获取完整的地址代码
async function getAddressCodesFromName(address, index) {
  // 这里需要实现一方法来根据地址名称查询完整的地址代码
  // 可能需要调用后端API或使用缓存的地址数据
  try {
    // 示例：假设有一个API可以根据地址名称返回完整的地址代码
    // const response = await getAddressCodesByName(address);
    // this.form.locations[index].codes = response.data;

    // 暂时保持原样
    console.log(`需要为地址 ${address} 查询完整的地址代码`)
  } catch (error) {
    console.error('获取地址代码败:', error)
  }
}

/** 处理作业区域输入框的输入 */
function handleLocationInputConfirm() {
  if (locationInput.value.trim() !== '') {
    queryParams.value.location.push(locationInput.value.trim())
    locationInput.value = ''
  }
}

/** 处理作业区域输入框的关闭 */
function handleLocationClose(index) {
  queryParams.value.location.splice(index, 1)
}

onMounted(() => {
  getList()
  proxy.getDicts('agricultural_machinery_status').then(response => {
    statusOptions.value = response.data
  })
  getTypeOptions()
  getCropOptions()
  getProvinceOptions()
})
</script>

<style scoped>
.app-container {
  height: 100%;
}

.content-wrapper {
  padding: 20px;
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 84px); /* 减去头部和面包屑的高度 */
}

.main-content {
  flex: 1;
  overflow-y: auto;
}

.pagination-container {
  margin-top: auto; /* 将分页推到底部 */
  padding-top: 15px;
  background-color: #fff;
  border-top: 1px solid #e4e7ed;
}

/* 在小屏幕设备上调整样式 */
@media screen and (max-width: 768px) {
  .content-wrapper {
    padding: 10px;
  }
}

/* 保留原有样式 */
.detail-table {
  padding: 20px;
}

.detail-table .el-form-item {
  margin-bottom: 10px;
  margin-right: 20px;
}

.ellipsis {
  display: inline-block;
  width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
}

.el-descriptions {
  padding: 20px;
  margin-bottom: 20px;
}

.carousel-container {
  width: 100%;
  padding: 0 20px;
}

.carousel-title {
  margin: 10px 0;
  color: #606266;
}

.carousel-image-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f7fa;
}

.carousel-image-container .el-image {
  max-width: 100%;
  max-height: 100%;
}

/* 自定义轮播图指示器样式 */
.el-carousel ::v-deep .el-carousel__indicators {
  bottom: -20px;
}

.el-carousel ::v-deep .el-carousel__indicator {
  padding: 12px 4px;
}

.el-carousel ::v-deep .el-carousel__button {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

/* 自定义轮播图按钮样式 */
.el-carousel ::v-deep .el-carousel__arrow {
  background-color: rgba(31, 45, 61, 0.5);
  border-radius: 50%;
}
</style>

