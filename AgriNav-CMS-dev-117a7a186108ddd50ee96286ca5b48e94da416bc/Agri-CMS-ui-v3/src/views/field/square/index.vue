<template>
  <div class="app-container">
    <div class="content-wrapper">
      <div class="main-content">
        <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" class="search-form">
          <!-- 第一行 -->
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="地块名称" prop="landName">
                <el-input v-model="queryParams.landName" placeholder="请输入地块名称" clearable
                          @keyup.enter="handleQuery"/>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="地块类型" prop="landType">
                <el-select v-model="queryParams.landType" placeholder="请选择地块类型" clearable>
                  <el-option label="新增" value="新增"/>
                  <el-option label="复测" value="复测"/>
                  <el-option label="插花地" value="插花地"/>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="地址" prop="address">
                <el-input v-model="queryParams.address" placeholder="请输入地址" clearable
                          @keyup.enter="handleQuery"/>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="删除状态" prop="isDeleted">
                <el-select v-model="queryParams.isDeleted" placeholder="请选择删除状态" clearable>
                  <el-option label="未删除" :value="0"/>
                  <el-option label="已删除" :value="1"/>
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 第二行 -->
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="创建时间" class="date-range-item">
                <el-date-picker
                  v-model="dateRange"
                  type="daterange"
                  range-separator="-"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  class="date-range-picker"
                ></el-date-picker>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 第三行 -->
          <el-row :gutter="20">
            <el-col :span="16">
              <el-form-item label="面积范围" class="area-range-item">
                <el-input-number v-model="queryParams.beginArea" placeholder="最小" class="area-input"/>
                <span class="range-separator">-</span>
                <el-input-number v-model="queryParams.endArea" placeholder="最大" class="area-input"/>
              </el-form-item>
            </el-col>
            <el-col :span="8" style="text-align: right;">
              <div class="search-buttons">
                <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
                <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
              </div>
            </el-col>
          </el-row>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button
              type="primary"
              plain
              icon="el-icon-plus"
              size="small"
              @click="handleAdd"
              v-hasPermi="['field:square:add']"
            >新增
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="success"
              plain
              icon="el-icon-edit"
              size="small"
              :disabled="single"
              @click="handleUpdate"
              v-hasPermi="['field:square:edit']"
            >修改
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="danger"
              plain
              icon="el-icon-delete"
              size="small"
              :disabled="multiple"
              @click="handleDelete"
              v-hasPermi="['field:square:remove']"
            >删除
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="warning"
              plain
              icon="el-icon-download"
              size="small"
              @click="handleExport"
              v-hasPermi="['field:square:export']"
            >导出
            </el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="landPlots" @selection-change="handleSelectionChange"
                  class="custom-table">
          <el-table-column type="selection" width="55" align="center"/>
          <el-table-column label="地块名称" align="center" prop="landName"/>
          <el-table-column label="地类类型" align="center" prop="landType">
            <template #default="scope">
              <el-tag type="primary">{{ scope.row.landType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="地块面积(亩)" align="center" prop="landArea">
            <template #default="scope">
              {{ scope.row.landArea || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="删除标志" align="center" prop="isDeleted">
            <template #default="scope">
              <el-tag :type="scope.row.isDeleted === 0 ? 'success' : 'danger'">
                {{ scope.row.isDeleted === 0 ? '未删除' : '已删除' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="地块地址" align="center" prop="address" width="120">
            <template #default="scope">
              <el-button link @click="showFullAddress(scope.row.address)">
                {{ truncateAddress(scope.row.address) }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template #default="scope">
              <el-button
                size="small"
                link
                icon="el-icon-map-location"
                @click="showBoundary(scope.row)"
              >查看地块边界
              </el-button>
              <el-dropdown>
                <el-button type="primary" size="small">
                  操作<i class="el-icon-arrow-down el-icon--right"></i>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <template v-if="scope.row.isDeleted === 0">
                      <el-dropdown-item @click="handleUpdate(scope.row)">修改</el-dropdown-item>
                      <el-dropdown-item @click="handleDelete(scope.row)">删除</el-dropdown-item>
                    </template>
                    <template v-else>
                      <el-dropdown-item @click="handleRestore(scope.row)">恢复</el-dropdown-item>
                    </template>
                    <el-dropdown-item @click="showMoreDetails(scope.row)">更多详情</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="queryParams.current"
          :page-sizes="[10, 20, 30]"
          :page-size="queryParams.size"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          class="custom-pagination">
        </el-pagination>
      </div>
    </div>

    <!-- 添加或修改地块对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="地块名称" prop="landName">
          <el-input v-model="form.landName" placeholder="请输入地块名称"/>
        </el-form-item>
        <el-form-item label="地块类型" prop="landType">
          <el-select v-model="form.landType" placeholder="请选择地块类型">
            <el-option label="新增" value="新增"/>
            <el-option label="复测" value="复测"/>
            <el-option label="插花地" value="插花地"/>
          </el-select>
        </el-form-item>
        <el-form-item label="地块面积" prop="landArea">
          <el-input v-model="form.landArea" placeholder="地块面积" :disabled="true"/>
        </el-form-item>
        <el-form-item label="图片">
          <el-upload
            class="upload-demo"
            action="/upload"
            :http-request="handleImageUpload"
            :show-file-list="true"
            :auto-upload="true"
          >
            <el-button size="small" type="primary">点击上传</el-button>
          </el-upload>
          <img v-if="form.image && typeof form.image === 'string'" :src="form.image"
               style="width: 100px; height: 100px; object-fit: cover;"/>
        </el-form-item>
        <el-form-item label="测量方法" prop="measurementMethod">
          <el-input v-model="form.measurementMethod" placeholder="测量方法" :disabled="true"/>
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 图片查看对话框 -->
    <el-dialog title="地块图片" v-model="imageDialogVisible" width="80%">
      <img :src="currentImage" alt="地块图片" style="max-width: 100%; max-height: 80vh;">
    </el-dialog>

    <!-- 地址详情对话框 -->
    <el-dialog title="完整地址" v-model="addressDialogVisible" width="30%">
      <span>{{ fullAddress }}</span>
    </el-dialog>

    <!-- 添加更多详情对话框 -->
    <el-dialog title="地块详情" v-model="detailsDialogVisible" width="80%">
      <el-row :gutter="20" style="height: 600px;">
        <el-col :span="12" style="height: 100%;">
          <div id="detailBoundaryMap" style="width: 100%; height: 100%;"></div>
        </el-col>
        <el-col :span="12" style="height: 100%; display: flex; flex-direction: column;">
          <el-descriptions :column="1" border class="details-description">
            <el-descriptions-item label="地块名称">
              <span class="detail-value bold">{{ detailsForm.landName }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="地块面积">
              <span class="detail-value bold">{{ detailsForm.landArea }} 亩</span>
            </el-descriptions-item>
            <el-descriptions-item label="拥有者姓名">
              <span class="detail-value">{{ detailsForm.ownerName || '-' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="拥有者电话">
              <span class="detail-value">{{ detailsForm.ownerPhone || '-' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="地块类型">
              <el-tag type="primary">{{ detailsForm.landType }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="删除标志">
              <el-tag :type="detailsForm.isDeleted === 0 ? 'success' : 'danger'">
                {{ detailsForm.isDeleted === 0 ? '未删除' : '已删除' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="地块地址">
              <span class="detail-value">{{ detailsForm.address }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="创建者">
              <span class="detail-value">{{ detailsForm.createBy }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              <span class="detail-value">{{ parseTime(detailsForm.createTime) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="更新者">
              <span class="detail-value">{{ detailsForm.updateBy }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="更新时间">
              <span class="detail-value">{{ parseTime(detailsForm.updateTime) }}</span>
            </el-descriptions-item>
          </el-descriptions>
          <p>地块图片</p>
          <div class="image-container">
            <el-image
              style="width: 100%; height: 150px;"
              :src="detailsForm.image"
              fit="cover"
              :preview-src-list="[detailsForm.image]">
              <template #error>
                <div class="image-slot">
                  <i class="el-icon-picture-outline"></i>
                </div>
              </template>
            </el-image>
          </div>
        </el-col>
      </el-row>
    </el-dialog>

    <!-- 地块边界对话框 -->
    <el-dialog title="地块边界" v-model="boundaryDialogVisible" width="80%">
      <div id="boundaryMap" class="boundary-map"></div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Square'
}
</script>

<script setup>
import { getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { delLandPlot, addLandPlot, updateLandPlot, queryLandPlot, restoreLandPlot } from '@/api/field/square'
import { listLandPlotExclude } from '@/api/field/exclude'
import { loadAMap } from '@/utils/amap'

const { proxy } = getCurrentInstance()

const formRef = ref(null)
const queryFormRef = ref(null)
// 遮罩层
const loading = ref(true)
// 选中数组
const ids = ref([])
// 非单个禁用
const single = ref(true)
// 非多个禁用
const multiple = ref(true)
// 显示搜索件
const showSearch = ref(true)
// 总条数
const total = ref(0)
// 地块表格数据
const landPlots = ref([])
// 弹出层标题
const title = ref('')
// 是否显示弹出层
const open = ref(false)
// 查询参数
const queryParams = ref({
  current: 1,
  size: 10,
  landName: undefined,
  landType: undefined,
  startTime: undefined,
  endTime: undefined,
  beginArea: undefined,
  endArea: undefined,
  address: '',
  isDeleted: undefined
})
const dateRange = ref([])
// 表单参数
const form = ref({})
// 表单校验
const rules = {
  landName: [
    { required: true, message: '地块名称不能为空', trigger: 'blur' }
  ],
  landArea: [
    { required: true, message: '地块面积不能为空', trigger: 'blur' }
  ]
}
const boundaryDialogVisible = ref(false)
const imageDialogVisible = ref(false)
const currentImage = ref('')
const jumpPage = ref(null)
const isLastPage = ref(false)
const addressDialogVisible = ref(false)
const fullAddress = ref('')
const detailsDialogVisible = ref(false)
const detailsForm = ref({})
const selectedBoundary = ref(null)
const selectedBoundaryArea = ref(null)
const map = ref(null)
const amap = ref(null)
const excludeAreas = ref([]) // 存储无用区域数据

/** 条件查询地块列表 */
function getList() {
  loading.value = true

  // 处理日期范围
  if (dateRange.value && dateRange.value.length === 2) {
    queryParams.value.startTime = dateRange.value[0]
    queryParams.value.endTime = dateRange.value[1]
  } else {
    queryParams.value.startTime = undefined
    queryParams.value.endTime = undefined
  }

  // 移除未设置的面积参数
  const params = { ...queryParams.value }
  if (params.beginArea === undefined) delete params.beginArea
  if (params.endArea === undefined) delete params.endArea

  console.log('查询参数:', params) // 添加日志

  queryLandPlot(params).then(response => {
    console.log('API 响应:', response) // 添加日志
    landPlots.value = response.data || []
    total.value = response.total || 0
    loading.value = false

    console.log('获取到的数据条数:', landPlots.value.length) // 添加日志

    if (landPlots.value.length === 0) {
      ElMessage.warning('没有更多的地块了，查看本页之前的地块吧')
    } else if (landPlots.value.length < queryParams.value.size) {
      ElMessage.info('当前页为最后一页')
    }
  }).catch(error => {
    console.error('条件查询地块列表失败:', error)
    proxy.$modal.msgError('条件查询地块列表失败')
    loading.value = false
    total.value = 0
  })
}

function handlePageClick(page) {
  queryParams.value.current = page
  getList()
}

function handleNextPage() {
  queryParams.value.current++
  getList()
}

function handleJumpPage() {
  if (jumpPage.value && jumpPage.value > 0) {
    queryParams.value.current = jumpPage.value
    getList()
  }
  jumpPage.value = null
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    id: undefined,
    landName: undefined,
    landType: undefined,
    landArea: undefined,
    image: undefined,
    measurementMethod: undefined,
    address: undefined
  }
  proxy.resetForm('formRef')
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.current = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm('queryFormRef')
  dateRange.value = []
  queryParams.value = {
    current: 1,
    size: 10,
    landName: '',
    landType: '',
    startTime: undefined,
    endTime: undefined,
    beginArea: undefined,
    endArea: undefined,
    address: '',
    isDeleted: undefined
  }
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = '添加地块'
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  // 直接使用传入的行数据
  form.value = { ...row }
  open.value = true
  title.value = '修改地块'
}

/** 提交按钮 */
function submitForm() {
  formRef.value.validate(valid => {
    if (valid) {
      const formData = new FormData()
      const json = JSON.stringify(form.value)
      formData.append('landPlot', json)

      if (form.value.image instanceof File) {
        formData.append('file', form.value.image)
      } else if (form.value.image) {
        // 如果 image 不是 File 对象，但有值，可能是之前的图片 URL
        formData.append('existingImage', form.value.image)
      }

      if (form.value.id != null) {
        updateLandPlot(formData).then(response => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        }).catch(error => {
          console.error('修改失败:', error)
          proxy.$modal.msgError('修改失败')
        })
      } else {
        addLandPlot(formData).then(response => {
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
  ElMessageBox.confirm('确认删除该地块数据吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    return delLandPlot(row.id)
  }).then(() => {
    getList()
    ElMessage.success('删除成功')
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('field/square/export', {
    ...queryParams.value
  }, `square_${new Date().getTime()}.xlsx`)
}

function formatIsDeleted(row) {
  return row.isDeleted === 0 ? '未删除' : '已删除'
}

function showBoundary(row) {
  selectedBoundary.value = row.boundary
  selectedBoundaryArea.value = row.landArea

  listLandPlotExclude(row.id).then(response => {
    excludeAreas.value = response.data
    boundaryDialogVisible.value = true
    nextTick(() => {
      initMap()
    })
  }).catch(error => {
    console.error('获取无用区域数据失败:', error)
    ElMessage.error('获取无用区域数据失败')
    boundaryDialogVisible.value = true
    nextTick(() => {
      initMap()
    })
  })
}

function initMap() {
  loadAMap(['AMap.Polygon']).then((AMap) => {
    amap.value = AMap
    map.value = new AMap.Map('boundaryMap', {
      zoom: 14,
      viewMode: '3D',
      pitch: 0,  // 添加俯仰角度
      mapStyle: 'amap://styles/normal',  // 使用正常地图样式
      features: ['bg', 'road', 'building']  // 显示背景、道路和建筑物
    })
    drawBoundary()
  }).catch(e => {
    ElMessage.error('高德地图脚本加载失败')
  })
}

function drawBoundary() {
  if (!amap.value || !selectedBoundary.value) return

  const coordinates = parseBoundary(selectedBoundary.value)
  const excludePaths = excludeAreas.value.map(area => parseBoundary(area.boundary))

  // 创建填充多边形 (主地块) - 包含"挖空"的无用区域
  const fillPolygon = new amap.value.Polygon({
    path: [coordinates, ...excludePaths],
    strokeColor: '#3399FF',
    strokeWeight: 2,
    strokeOpacity: 1,
    fillColor: '#C1FFC1',
    fillOpacity: 0.4,
    zIndex: 50
  })

  // 绘制无用区域边框
  const excludePolygons = excludeAreas.value.map(area => {
    const excludeCoords = parseBoundary(area.boundary)
    return new amap.value.Polygon({
      path: excludeCoords,
      strokeColor: '#3399FF',
      strokeWeight: 2,
      strokeOpacity: 1,
      fillColor: '#FFFFFF',
      fillOpacity: 0,
      zIndex: 51
    })
  })

  // 添加图层到地图
  map.value.add([fillPolygon, ...excludePolygons])

  // 只添加主地块的面积标注
  const bounds = fillPolygon.getBounds()
  const center = bounds.getCenter()

  // 创建主地块面积标记
  let areaText = '面积未知'
  if (selectedBoundary.value && selectedBoundaryArea.value !== 'undefined') {
    const formattedArea = Number(selectedBoundaryArea.value).toFixed(3)
    areaText = formattedArea + '亩'
  }

  const marker = new amap.value.Marker({
    position: center,
    content: `<div style="display: inline-flex; white-space: nowrap; font-weight: bold; font-size: 20px;">${areaText}</div>`,
    offset: new amap.value.Pixel(-30, -30)
  })

  map.value.add(marker)
  map.value.setFitView([fillPolygon, ...excludePolygons])
}

function parseBoundary(boundary) {
  const coordsString = boundary.match(/\(\((.*?)\)\)/)[1]
  return coordsString.split(',').map(pair => {
    // WKT 每对为“经度 纬度”，高德 Polygon path 同样需要 [经度, 纬度]
    const [lng, lat] = pair.trim().split(' ')
    return [parseFloat(lng), parseFloat(lat)]
  })
}

function openImageDialog(imageSrc) {
  currentImage.value = imageSrc
  imageDialogVisible.value = true
}

function handleImageUpload(file) {
  form.value.image = file.file
}

function handleSizeChange(val) {
  queryParams.value.size = val
  getList()
}

function handleCurrentChange(val) {
  queryParams.value.current = val
  getList()
}

function truncateAddress(address) {
  return address.length > 5 ? address.slice(0, 5) + '...' : address
}

function showFullAddress(address) {
  fullAddress.value = address
  addressDialogVisible.value = true
}

function showMoreDetails(row) {
  detailsForm.value = { ...row }
  detailsDialogVisible.value = true
  nextTick(() => {
    initDetailMap()
  })
}

function initDetailMap() {
  loadAMap(['AMap.Polygon']).then((AMap) => {
    const detailMap = new AMap.Map('detailBoundaryMap', {
      zoom: 14,
      viewMode: '3D'
    })
    drawDetailBoundary(detailMap, AMap)
  }).catch(e => {
    ElMessage.error('高德地图脚本加载失败')
  })
}

function drawDetailBoundary(map, AMap) {
  if (!detailsForm.value.boundary) return

  const coordinates = parseBoundary(detailsForm.value.boundary)

  // 获取无用区域数据
  listLandPlotExclude(detailsForm.value.id).then(response => {
    const excludeAreasData = response.data
    const excludePaths = excludeAreasData.map(area => parseBoundary(area.boundary))

    // 创建填充多边形 (主地块) - 包含"挖空"的无用区域
    const polygon = new AMap.Polygon({
      path: [coordinates, ...excludePaths],
      strokeColor: '#3399FF',
      strokeWeight: 2,
      strokeOpacity: 1,
      fillColor: '#C1FFC1',
      fillOpacity: 0.4,
      zIndex: 50
    })

    // 绘制无用区域边框
    const excludePolygons = excludeAreasData.map(area => {
      const excludeCoords = parseBoundary(area.boundary)
      return new AMap.Polygon({
        path: excludeCoords,
        strokeColor: '#3399FF',
        strokeWeight: 2,
        strokeOpacity: 1,
        fillColor: '#FFFFFF',
        fillOpacity: 0,
        zIndex: 51
      })
    })

    // 添加图层到地图
    map.add([polygon, ...excludePolygons])

    // 添加面积标注
    const bounds = polygon.getBounds()
    const center = bounds.getCenter()

    const formattedArea = Number(detailsForm.value.landArea).toFixed(3)
    const areaText = formattedArea + '亩'

    const marker = new AMap.Marker({
      position: center,
      content: `<div style="display: inline-flex; white-space: nowrap; font-weight: bold; font-size: 20px;">${areaText}</div>`,
      offset: new AMap.Pixel(-30, -30)
    })

    map.add(marker)
    map.setFitView([polygon, ...excludePolygons])
  }).catch(error => {
    console.error('获取无用区域数据失败:', error)
    ElMessage.error('获取无用区域数据失败')
  })
}

/** 恢复按钮操作 */
function handleRestore(row) {
  ElMessageBox.confirm('确认恢复该地块吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    return restoreLandPlot(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('恢复成功')
  }).catch(() => {
    ElMessage.info('已取消恢复')
  })
}

onMounted(() => {
  queryParams.value.current = 1
  queryParams.value.size = 10
  getList()
})

onBeforeUnmount(() => {
  if (map.value) {
    map.value.destroy()
    map.value = null
  }
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

.custom-table {
  background-color: #ffffff;
}

.custom-table >>> .el-table__header-wrapper {
  background-color: #f5f7fa;
}

.custom-pagination {
  margin-top: 20px;
}

.custom-pagination >>> .btn-prev,
.custom-pagination >>> .btn-next {
  background-color: #f5f7fa;
}

.custom-pagination >>> .el-pager li:not(.active) {
  background-color: #ffffff;
}

.custom-pagination >>> .el-select .el-input {
  margin: 0 5px;
}

.custom-pagination >>> .el-pagination__total,
.custom-pagination >>> .el-pagination__jump {
  margin-left: 10px;
}

.el-table {
  margin-bottom: 20px;
}

.dialog-footer {
  text-align: right;
}

.upload-demo {
  cursor: pointer;
}

.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.avatar-uploader .el-upload:hover {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  line-height: 178px;
  text-align: center;
}

.avatar {
  width: 178px;
  height: 178px;
  display: block;
}

.el-form-item {
  margin-bottom: 18px;
}

.custom-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
}

.custom-pagination .el-button-group {
  margin-right: 10px;
}

.custom-pagination .current-page {
  margin: 0 10px;
}

.custom-pagination .el-input {
  width: 120px;
  margin-left: 10px;
}

/* 响应式布局 */
@media screen and (max-width: 768px) {
  .el-form-item {
    width: 100%;
  }

  .el-table {
    width: 100%;
    overflow-x: auto;
  }

  .el-pagination {
    text-align: center;
  }
}

.el-table .cell {
  white-space: nowrap;
}

.el-button.el-button--text {
  padding: 0;
}

.custom-icon-button {
  font-size: 16px;
  width: 36px;
  height: 36px;
}

.custom-icon-button i {
  font-size: 18px;
}

.el-tag {
  margin-right: 5px;
}

.boundary-map {
  width: 100%;
  height: 500px;
}

.el-button--text {
  padding: 2px 0;
  margin-right: 8px;
}

.el-descriptions {
  margin-top: 20px;
}

.el-descriptions-item__label {
  font-weight: bold;
}

.details-description {
  flex: 1;
  overflow-y: auto;
}

.details-description .el-descriptions-item {
  margin-bottom: 10px;
}

.details-description .el-descriptions-item__label {
  font-weight: bold;
  font-size: 14px;
}

.detail-value {
  font-size: 14px;
}

.detail-value.bold {
  font-weight: bold;
  font-size: 16px;
}

.image-container {
  margin-top: auto;
  padding-top: 10px;
  overflow: hidden; /* 确保超出部分被裁剪 */
}

.el-image {
  display: block; /* 移除图片下方可能的空白 */
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.search-form {
  background-color: #ffffff;
  padding: 20px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.search-form .el-form-item {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.search-form .el-form-item__label {
  width: auto;
  padding-right: 12px;
  line-height: 32px;
}

.search-form .el-input,
.search-form .el-input-number,
.search-form .el-date-editor {
  flex: 1;
}

.date-range-item,
.area-range-item {
  display: flex;
  align-items: center;
}

.date-range-picker {
  width: 100%;
}

.area-input {
  width: 45% !important;
}

.range-separator {
  margin: 0 5px;
}

.search-form .el-button {
  margin-left: 10px;
}

@media (max-width: 768px) {
  .search-form .el-form-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-form .el-form-item__label {
    margin-bottom: 8px;
  }

  .date-range-item,
  .area-range-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .area-input {
    width: 100% !important;
    margin-bottom: 8px;
  }

  .range-separator {
    display: none;
  }
}

/* 添加或修改以下样式 */
.search-buttons {
  display: flex;
  justify-content: flex-end;
  padding-right: 20px; /* 添加右侧内边距 */
}

.search-buttons .el-button {
  margin-left: 8px; /* 按钮之间的间距 */
}

.area-range-item {
  padding-right: 20px; /* 为面积范围添加右侧内边距 */
}
</style>

