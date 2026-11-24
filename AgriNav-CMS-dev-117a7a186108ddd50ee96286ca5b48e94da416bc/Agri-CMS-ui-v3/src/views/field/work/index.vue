<template>
  <div class="app-container">
    <div class="content-wrapper">
      <div class="main-content">
        <!-- 搜索区域 -->
        <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" class="search-form">
          <el-form-item label="地址" prop="address">
            <el-input
              v-model="queryParams.address"
              placeholder="请输入地址"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <!-- 表格区域 -->
        <el-table v-loading="loading" :data="workingLandPlots">
          <el-table-column label="地块名称" align="center" prop="landName" />
          <el-table-column label="地块类型" align="center" prop="landType">
            <template #default="scope">
              <el-tag type="primary">{{ scope.row.landType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="面积(亩)" align="center" prop="landArea">
            <template #default="scope">
              {{ scope.row.landArea.toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="地块地址" align="center" prop="address" />
          <el-table-column label="所有者" align="center" prop="ownerName" />
          <el-table-column label="联系电话" align="center" prop="ownerPhone" />
          <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
          <el-table-column label="地块图片" align="center" width="100">
            <template #default="scope">
              <el-image
                v-if="scope.row.image"
                :src="scope.row.image"
                :preview-src-list="[scope.row.image]"
                fit="cover"
                style="width: 50px; height: 50px"
              >
                <template #error>
                  <div class="image-slot">
                    <i class="el-icon-picture-outline"></i>
                  </div>
                </template>
              </el-image>
              <i v-else class="el-icon-picture-outline" style="font-size: 20px; color: #909399"></i>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="120">
            <template #default="scope">
              <el-button
                link
                size="small"
                icon="el-icon-map-location"
                @click="viewTrajectory(scope.row)"
              >查看轨迹</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页区域 -->
        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />

        <!-- 地块边界对话框 -->
        <el-dialog
          title="地块边界"
          v-model="boundaryDialogVisible"
          width="80%"
          @closed="handleDialogClosed"
        >
          <div id="boundaryMap" style="width: 100%; height: 500px;"></div>
        </el-dialog>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'WorkingLandPlot'
}
</script>

<script setup>
import { getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { queryWorkingLandPlot } from '@/api/field/work'
import { loadAMap } from '@/utils/amap'
import request from '@/utils/request'
import trunkImg from '@/assets/images/trunk.png'

const { proxy } = getCurrentInstance()

const queryFormRef = ref(null)
// 遮罩层
const loading = ref(true)
// 示搜索条件
const showSearch = ref(true)
// 总条数
const total = ref(0)
// 工作地块列表
const workingLandPlots = ref([])
// 查询参数
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  address: undefined
})
const map = ref(null)
const amap = ref(null)
const boundaryDialogVisible = ref(false)
const polygon = ref(null)
const excludePolygons = ref([])
const path = ref([]) // 存储轨迹坐标
const marker = ref(null)
const polyline = ref(null)
const currentIndex = ref(0)
const animationInterval = ref(null)
const animationSpeed = 0.01
const isAnimationComplete = ref(false)
const markerIcon = ref(null)
const lastPosition = ref(null)
const lastTime = ref(0)
const bluePolyline = ref(null) // 蓝色完整轨迹
const greenPolyline = ref(null) // 绿色动态轨迹
const excludeTimes = ref(new Set()) // 存储需要排除的时间点
const trajectory = ref(null) // 用存储轨迹数据
const trajectorySegments = ref([]) // 存储完整的轨迹段信息
const greenSegments = ref([]) // 存储绿色轨迹的段
const websocket = ref(null)
const wsUrl = import.meta.env.VITE_APP_BASE_API.replace('http', 'ws') + '/websocket/workingLandPlot'
const wsTimer = ref(null)
const currentLandPlot = ref(null)

/** 查询工作地块列表 */
function getList() {
  loading.value = true
  queryWorkingLandPlot(queryParams.value).then(response => {
    workingLandPlots.value = response.data
    total.value = response.total
    loading.value = false
  }).catch(error => {
    console.error('获取工作地块列表失败:', error)
    ElMessage.error('获取工作地块列表失败')
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 查看轨迹按钮操作 */
async function viewTrajectory(row) {
  boundaryDialogVisible.value = true
  currentLandPlot.value = row // 保存当前地块信息
  await nextTick()

  try {
    // 初始化地图（如果还没有初始化）
    if (!map.value) {
      const AMap = await loadAMap(['AMap.Polygon', 'AMap.Polyline', 'AMap.Marker'])

      amap.value = AMap
      map.value = new AMap.Map('boundaryMap', {
        zoom: 14,
        viewMode: '3D',
        pitch: 0,
        mapStyle: 'amap://styles/normal',
        features: ['bg', 'road', 'building']
      })
    }

    // 清除之前的图层
    clearMapLayers()

    // 绘制地块边界
    if (row.landType === '新增') {
      drawBoundary(row)
    } else if (row.landType === '插花地') {
      drawBoundaryWithExcludes(row)
    }

    // 初始化 WebSocket 连接
    initWebSocket()
  } catch (error) {
    console.error('初始化失败:', error)
    ElMessage.error('初始化失败，请稍后重试')
  }
}

/** 绘制轨迹 */
function drawTrajectory() {
  if (!amap.value || path.value.length === 0) {
    ElMessage.warning('轨迹数据为空')
    return
  }

  // 创建标记
  markerIcon.value = new amap.value.Icon({
    size: new amap.value.Size(20, 20),
    image: trunkImg,
    imageSize: new amap.value.Size(20, 20)
  })

  // 设置初始位置为倒数第二个点
  const initialPosition = path.value[path.value.length - 2] || path.value[0]
  currentIndex.value = path.value.length - 2

  marker.value = new amap.value.Marker({
    position: initialPosition,
    icon: markerIcon.value,
    angle: 0,
    anchor: 'center',
    zIndex: 120
  })

  // 构建初始轨迹段
  const initialPath = path.value.slice(0, path.value.length - 1)
  const segments = buildTrajectorySegments(initialPath)
  drawGreenSegments(segments)

  map.value.add(marker.value)
  map.value.setFitView([...greenSegments.value, marker.value])

  // 计算初始角度
  if (path.value.length >= 2) {
    const lastPoint = path.value[path.value.length - 2]
    const endPoint = path.value[path.value.length - 1]
    const initialAngle = calculateAngle(lastPoint, endPoint)
    marker.value.setAngle(initialAngle)
  }

  // 开始动画
  animateTrajectory()
}

/** 轨迹动画 */
function animateTrajectory() {
  const animate = (currentTime) => {
    if (!lastTime.value) lastTime.value = currentTime
    const deltaTime = currentTime - lastTime.value
    lastTime.value = currentTime

    if (currentIndex.value < path.value.length - 1) {
      const lastIndex = currentIndex.value
      currentIndex.value += animationSpeed * (deltaTime / 16.67)
      const currentPosition = interpolatePosition(currentIndex.value)

      // 计算运动方向的角度
      const nextIndex = Math.min(Math.ceil(currentIndex.value), path.value.length - 1)
      const angle = calculateAngle(
        interpolatePosition(lastIndex),
        interpolatePosition(nextIndex)
      )

      // 更新小车位置和角度
      marker.value.setPosition(currentPosition)
      marker.value.setAngle(angle)
      lastPosition.value = currentPosition

      // 更新绿色轨迹
      const completedPath = path.value.slice(0, Math.floor(currentIndex.value) + 1)
      completedPath.push(currentPosition)

      // 清除现有的绿色轨迹段
      if (greenSegments.value.length) {
        map.value.remove(greenSegments.value)
      }

      // 重新构建并绘制轨迹段
      const segments = buildTrajectorySegments(completedPath)
      drawGreenSegments(segments)

      animationInterval.value = requestAnimationFrame(animate)
    } else {
      isAnimationComplete.value = true
      animationInterval.value = requestAnimationFrame(animate)
    }
  }

  animationInterval.value = requestAnimationFrame(animate)
}

/** 位置插值计算 */
function interpolatePosition(index) {
  const floorIndex = Math.floor(index)
  const ceilIndex = Math.min(floorIndex + 1, path.value.length - 1)
  const fraction = index - floorIndex

  const start = path.value[floorIndex]
  const end = path.value[ceilIndex]

  return [
    start[0] + (end[0] - start[0]) * fraction,
    start[1] + (end[1] - start[1]) * fraction
  ]
}

/** 计算角度 */
function calculateAngle(start, end) {
  const dx = end[0] - start[0]
  const dy = end[1] - start[1]
  let angle = Math.atan2(dy, dx) * 180 / Math.PI

  // 将角度转换为与正北方向的夹角
  angle = 90 - angle
  if (angle < 0) {
    angle += 360
  }

  return angle
}

/** 清除地图图层 */
function clearMapLayers() {
  if (animationInterval.value) {
    cancelAnimationFrame(animationInterval.value)
  }

  if (greenSegments.value.length) {
    map.value.remove(greenSegments.value)
    greenSegments.value = []
  }

  if (marker.value) {
    map.value.remove(marker.value)
    marker.value = null
  }

  if (polygon.value) {
    map.value.remove(polygon.value)
    polygon.value = null
  }
}

/** 解析边界坐标 */
function parseBoundary(boundary) {
  const coordsString = boundary.match(/\(\((.*?)\)\)/)[1]
  return coordsString.split(',').map(pair => {
    // WKT 每对为“经度 纬度”，高德需要 [经度, 纬度]
    const [lng, lat] = pair.trim().split(' ')
    return [parseFloat(lng), parseFloat(lat)]
  })
}

/** 绘制地块边界 */
function drawBoundary(landPlot) {
  const coordinates = parseBoundary(landPlot.boundary)
  polygon.value = new amap.value.Polygon({
    path: coordinates,
    strokeColor: '#237CBE',
    strokeWeight: 2,
    strokeOpacity: 0.8,
    fillColor: '#E0F0DD',
    fillOpacity: 0.8,
    zIndex: 50
  })

  map.value.add(polygon.value)
  map.value.setFitView([polygon.value])
}

/** 绘制带无用区域的地块边界 */
function drawBoundaryWithExcludes(landPlot) {
  // 获取无用区域数据
  request({
    url: '/landPlotExclude/queryByLandPlotId',
    method: 'get',
    params: {
      landPlotId: landPlot.id
    }
  }).then(response => {
    if (response.code === 200) {
      const excludes = response.data

      // 获得主地块的坐标
      const mainPath = parseBoundary(landPlot.boundary)
      // 获取所有无用区域的坐标
      const excludePaths = excludes.map(exclude => parseBoundary(exclude.boundary))

      // 创建包含镂空区域的多边形
      polygon.value = new amap.value.Polygon({
        path: [mainPath, ...excludePaths], // 第一个路径是外部轮廓，后续的路径会形成镂空区域
        strokeColor: '#237CBE',
        strokeWeight: 2,
        strokeOpacity: 0.8,
        fillColor: '#E0F0DD',
        fillOpacity: 0.8,
        zIndex: 50
      })

      // 添加到地图并调整视野
      map.value.add(polygon.value)
      map.value.setFitView([polygon.value])
    }
  }).catch(error => {
    console.error('获取无用区域数据失败:', error)
    ElMessage.error('获取无用区域据失败')
  })
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm('queryFormRef')
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    address: undefined
  }
  handleQuery()
}

function handleDialogClosed() {
  // 关闭 WebSocket 连接
  if (websocket.value) {
    websocket.value.close()
  }
  if (wsTimer.value) {
    clearInterval(wsTimer.value)
  }

  // 清理动画和图层
  if (animationInterval.value) {
    cancelAnimationFrame(animationInterval.value)
  }
  if (map.value) {
    clearMapLayers()
  }

  // 重置轨迹相关数据，但保留地图实例
  path.value = []
  currentIndex.value = 0
  lastPosition.value = null
  lastTime.value = 0
  isAnimationComplete.value = false
  trajectory.value = null
  currentLandPlot.value = null
}

function initWebSocket() {
  // 关闭已存在的连接
  if (websocket.value) {
    websocket.value.close()
  }
  if (wsTimer.value) {
    clearInterval(wsTimer.value)
  }

  // 创建 WebSocket 连接
  websocket.value = new WebSocket(wsUrl)

  websocket.value.onopen = () => {
    console.log('WebSocket连接已建立')
    // 连接建立后立即发送第一次请求
    sendTrajectoryRequest()
    // 设置定时器，每5秒发送一次请求
    wsTimer.value = setInterval(() => {
      sendTrajectoryRequest()
    }, 5000)
  }

  websocket.value.onmessage = (event) => {
    try {
      const response = JSON.parse(event.data)
      if (response.code === 200 && response.data && response.data.length > 0) {
        const newTrajectory = response.data[0]

        // 如果是第一次接收数据
        if (!trajectory.value) {
          trajectory.value = newTrajectory
          path.value = trajectory.value.points.map(point => [point.longitude, point.latitude])
          drawTrajectory()
        } else {
          // 更新轨迹数据
          const newPoints = newTrajectory.points
          const oldPoints = trajectory.value.points

          // 只添加新的点
          if (newPoints.length > oldPoints.length) {
            trajectory.value = newTrajectory
            path.value = trajectory.value.points.map(point => [point.longitude, point.latitude])

            // 如果动画已完成，则重新开始动画
            if (isAnimationComplete.value) {
              continueAnimation()
            }
          }
        }
      }
    } catch (error) {
      console.error('处理WebSocket消息失败:', error)
    }
  }

  websocket.value.onerror = (error) => {
    console.error('WebSocket错误:', error)
    ElMessage.error('WebSocket连接错误')
  }

  websocket.value.onclose = () => {
    console.log('WebSocket连接已关闭')
    if (wsTimer.value) {
      clearInterval(wsTimer.value)
    }
  }
}

function sendTrajectoryRequest() {
  if (websocket.value && websocket.value.readyState === WebSocket.OPEN && currentLandPlot.value) {
    const request = {
      id: currentLandPlot.value.id,
      landType: currentLandPlot.value.landType
    }
    websocket.value.send(JSON.stringify(request))
  }
}

function updateTrajectoryDisplay() {
  // 清除现有的轨迹显示
  if (greenSegments.value.length) {
    map.value.remove(greenSegments.value)
    greenSegments.value = []
  }
  if (bluePolyline.value) {
    map.value.remove(bluePolyline.value)
  }
  if (marker.value) {
    map.value.remove(marker.value)
  }

  // 重新绘制轨迹
  drawTrajectory()
  // 开始动画
  animateTrajectory()
}

function continueAnimation() {
  // 重置动画状态
  isAnimationComplete.value = false

  // 保持当前位置
  const currentPosition = path.value[Math.floor(currentIndex.value)]

  // 清除现有的绿色轨迹
  if (greenSegments.value.length) {
    map.value.remove(greenSegments.value)
    greenSegments.value = []
  }

  // 重新开始动画
  animateTrajectory()
}

function buildTrajectorySegments(completedPath) {
  const segments = []
  let currentSegment = {
    path: [],
    isDashed: false
  }

  // 辅助函数：检查点是否在排除区域内
  const isPointExcluded = (point, index) => {
    if (index >= trajectory.value.points.length) return false
    const time = trajectory.value.points[index].time
    return trajectory.value?.trajectoryExclude?.some(exclude =>
      exclude.pointsExclude?.some(p =>
        p.time === time && p.longitude === point[0] && p.latitude === point[1]
      )
    )
  }

  completedPath.forEach((point, index) => {
    const currentPointExcluded = isPointExcluded(point, index)
    const prevPointExcluded = index > 0 ? isPointExcluded(completedPath[index - 1], index - 1) : false

    // 开始新的段落的条件：
    // 1. 第一个点
    // 2. 当前点和前一个点的排除状态不同，且不是边界连接点
    if (index === 0) {
      currentSegment.path.push(point)
      currentSegment.isDashed = currentPointExcluded
    } else {
      // 处理排除区域的边界点
      if (currentPointExcluded !== prevPointExcluded) {
        // 结束当前段落
        currentSegment.path.push(point)
        segments.push({ ...currentSegment })

        // 开始新的段落
        currentSegment = {
          path: [point],
          isDashed: currentPointExcluded
        }
      } else {
        // 继续当前段落
        currentSegment.path.push(point)
      }
    }
  })

  // 添加最后一个段落
  if (currentSegment.path.length > 0) {
    segments.push(currentSegment)
  }

  return segments
}

function drawGreenSegments(segments) {
  greenSegments.value = segments.map(segment => new amap.value.Polyline({
    path: segment.path,
    strokeColor: '#95DDA2',
    strokeWeight: 6,
    strokeOpacity: 0.8,
    lineJoin: 'round',
    lineCap: 'round',
    showDir: true,
    strokeStyle: segment.isDashed ? 'dashed' : 'solid',
    strokeDasharray: segment.isDashed ? [10, 5] : null,
    zIndex: 111
  }))

  if (greenSegments.value.length > 0) {
    map.value.add(greenSegments.value)
  }
}

onMounted(() => {
  getList()
})

onBeforeUnmount(() => {
  if (websocket.value) {
    websocket.value.close()
  }
  if (wsTimer.value) {
    clearInterval(wsTimer.value)
  }
  if (animationInterval.value) {
    cancelAnimationFrame(animationInterval.value)
  }
  // 只在组件销毁时才销毁地图
  if (map.value) {
    map.value.destroy()
    map.value = null
    amap.value = null
  }
})
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
}

.content-wrapper {
  background-color: #fff;
  padding: 20px;
  border-radius: 4px;
}

.search-form {
  margin-bottom: 20px;
  padding: 18px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 20px;
}

#boundaryMap {
  width: 100%;
  height: 500px;
  background: #f5f5f5;
}

.el-dialog__body {
  padding: 10px 20px;
}
</style>
