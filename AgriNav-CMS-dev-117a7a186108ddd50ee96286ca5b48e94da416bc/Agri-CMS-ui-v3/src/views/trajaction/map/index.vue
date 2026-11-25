<template>
    <div class="map-container">
      <div id="container" class="amap-container"></div>
      <div class="map-controls">
        <el-button class="control-btn" type="primary" plain @click="showOriginalTrajectory">
          <i class="el-icon-view"></i>
          <span>查看原始轨迹</span>
        </el-button>
        <el-button class="control-btn" type="success" plain @click="toggleSatellite">
          <i class="el-icon-picture-outline"></i>
          <span>{{ satelliteLayer ? '关闭卫星图' : '开启卫星图' }}</span>
        </el-button>
        <el-button class="control-btn" type="warning" plain @click="toggleRoadNet">
          <i class="el-icon-share"></i>
          <span>{{ roadNetLayer ? '关闭路网图' : '开启路网图' }}</span>
        </el-button>
        <el-button class="control-btn" type="primary" plain @click="replayTrajectory">
          <i class="el-icon-refresh"></i>
          <span>重播轨迹</span>
        </el-button>
      </div>
      <!-- 添加对话框组件 -->
      <el-dialog
        title="原始轨迹"
        v-model="showOriginalDialog"
        width="80%"
        class="original-trajectory-dialog"
        :modal="false"
        :close-on-click-modal="false"
      >
        <vehicle-trajectory-original
          :trajectory-info-id="trajectoryInfoId"
          :phone-number="phoneNumber"
          :start-time="startTime"
          :end-time="endTime"
        />
      </el-dialog>
    </div>
  </template>

  <script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElLoading, ElMessage } from 'element-plus'
import { loadAMap } from '@/utils/amap'
import VehicleTrajectoryOriginal from './index_ori.vue'
import request from '@/utils/request' // 确保导入了request
import trunkImg from '@/assets/images/trunk.png'

defineOptions({ name: 'VehicleTrajectory' })

const props = defineProps({
  trajectory: {
    type: Object,
    required: true
  },
  trajectoryInfoId: {
    type: [String, Number],
    required: true
  }
})

const map = ref(null)
const path = ref([]) // 存储车辆轨迹坐标
const polyline = ref(null)
const marker = ref(null)
const currentIndex = ref(0)
const animationInterval = ref(null)
const animationSpeed = 0.01 // 减小动画速度，使动画更平滑
const isAnimationComplete = ref(false)
const showOriginalDialog = ref(false)
const phoneNumber = ref('')
const startTime = ref('')
const endTime = ref('')
const amap = ref(null)
const isFirstLoad = ref(true) // 新增：用于标记是否是第一次加载
const isDialogOpened = ref(false) // 新增：用于跟踪对话框是否打开
const landPlots = ref([]) // 用于存储地块数据
const markerIcon = ref(null)
const lastPosition = ref(null)
const satelliteLayer = ref(null)
const roadNetLayer = ref(null)
const completedPath = ref([]) // 新增：用于存储已完成的路径
const completedPolyline = ref(null) // 新增：用于存储已完成路径的polyline
const bluePolyline = ref(null) // 蓝色的完整轨迹
const greenPolyline = ref(null) // 绿色的动画轨迹
const lastTime = ref(0)
const excludeTimes = ref(new Set()) // 新增：用于存储需要排除的时间点

watch(() => props.trajectory, (newTrajectory) => {
  if (newTrajectory && newTrajectory.points && isDialogOpened.value) {
    path.value = newTrajectory.points.map(point => [point.longitude, point.latitude])
    nextTick(() => {
      resetAndDrawTrajectory()
    })
  }
}, { immediate: true })

function initMap() {
  loadAMap(['AMap.Polyline', 'AMap.Marker', 'AMap.Scale', 'AMap.ToolBar']).then((AMap) => {
    amap.value = AMap
    map.value = new AMap.Map('container', {
      zoom: 14,
      center: [118.784129, 32.04181],
      viewMode: '3D',
      mapStyle: 'amap://styles/macaron', // 使用马卡龙风格，看起来更加清新
      features: ['bg', 'road', 'building', 'point'],
      showIndoorMap: true,
      showBuildingBlock: true,
      expandZoomRange: true,
      zooms: [3, 20],
      layers: [
        new AMap.TileLayer(),
        new AMap.Buildings({
          zooms: [14, 20],
          zIndex: 10,
          heightFactor: 2
        })
      ]
    })

    map.value.addControl(new AMap.Scale())
    map.value.addControl(new AMap.ToolBar({
      position: 'RB',
      liteStyle: true
    }))

    resetAndDrawTrajectory()

    // 在 initMap 方法中，地图初始化后添加以下代码
    const zoomControl = {
      init: (map) => {
        const zoomInButton = document.createElement('button')
        zoomInButton.innerHTML = '+'
        zoomInButton.className = 'custom-zoom-button'
        zoomInButton.onclick = () => map.zoomIn()

        const zoomOutButton = document.createElement('button')
        zoomOutButton.innerHTML = '-'
        zoomOutButton.className = 'custom-zoom-button'
        zoomOutButton.onclick = () => map.zoomOut()

        const container = document.createElement('div')
        container.className = 'custom-zoom-control'
        container.appendChild(zoomInButton)
        container.appendChild(zoomOutButton)

        map.getContainer().appendChild(container)
      }
    }

    zoomControl.init(map.value)
  }).catch(e => {
    ElMessage.error('高德地图脚本加载失败')
  })
}

function resetAndDrawTrajectory() {
  if (!amap.value) {
    return
  }
  if (animationInterval.value) {
    cancelAnimationFrame(animationInterval.value)
  }
  if (polyline.value) {
    map.value.remove(polyline.value)
  }
  if (marker.value) {
    map.value.remove(marker.value)
  }
  currentIndex.value = 0
  isAnimationComplete.value = false
  isFirstLoad.value = true // 重置为true

  if (props.trajectory && props.trajectory.points && props.trajectory.points.length > 0) {
    path.value = props.trajectory.points.map(point => [point.longitude, point.latitude])
    drawTrajectory()
    fetchAndDrawLandPlots() // 添加这行
  } else {
    map.value.setFitView()
  }
}

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

  // 设置初始位置为第一个点
  const initialPosition = path.value[0]

  marker.value = new amap.value.Marker({
    position: initialPosition,
    icon: markerIcon.value,
    angle: 0,
    anchor: 'center'
  })

  // 修改获取排除点的逻辑
  const excludeTimesSet = new Set()
  if (props.trajectory.trajectoryExclude) {
    props.trajectory.trajectoryExclude.forEach(exclude => {
      if (exclude.pointsExclude) {
        exclude.pointsExclude.forEach(point => {
          excludeTimesSet.add(point.time)
        })
      }
    })
  }

  // 修改分段逻辑
  const segments = []
  let currentSegment = {
    path: [],
    isDashed: false
  }

  path.value.forEach((point, index) => {
    const time = props.trajectory.points[index].time
    const isExcluded = excludeTimesSet.has(time)
    const prevTime = index > 0 ? props.trajectory.points[index - 1].time : null
    const nextTime = index < path.value.length - 1 ? props.trajectory.points[index + 1].time : null
    const isPrevExcluded = prevTime ? excludeTimesSet.has(prevTime) : false
    const isNextExcluded = nextTime ? excludeTimesSet.has(nextTime) : false

    // 当前点是排除点的开始（前一个点不是排除点）
    if (isExcluded && !isPrevExcluded) {
      if (currentSegment.path.length > 0) {
        segments.push(currentSegment)
      }
      // 添加一个实线段连接到排除点
      segments.push({
        path: [currentSegment.path[currentSegment.path.length - 1], point],
        isDashed: false
      })
      currentSegment = {
        path: [point],
        isDashed: true
      }
    }
    // 当前点是排除点的结束（下一个点不是排除点）
    else if (isExcluded && !isNextExcluded) {
      currentSegment.path.push(point)
      segments.push(currentSegment)
      currentSegment = {
        path: [point],
        isDashed: false
      }
    }
    // 其他情况
    else {
      if (currentSegment.path.length === 0 || currentSegment.isDashed === isExcluded) {
        currentSegment.path.push(point)
      } else {
        segments.push(currentSegment)
        currentSegment = {
          path: [point],
          isDashed: isExcluded
        }
      }
    }
  })

  // 添加最后一段
  if (currentSegment.path.length > 0) {
    segments.push(currentSegment)
  }

  // 绘制蓝色轨迹（完整轨迹）
  segments.forEach(segment => {
    const polylineItem = new amap.value.Polyline({
      path: segment.path,
      strokeColor: '#5DA6CB',
      strokeWeight: 6,
      strokeOpacity: 0.8,
      lineJoin: 'round',
      lineCap: 'round',
      showDir: true,
      strokeStyle: segment.isDashed ? 'dashed' : 'solid',
      strokeDasharray: segment.isDashed ? [10, 5] : null
    })
    if (!bluePolyline.value) {
      bluePolyline.value = []
    }
    bluePolyline.value.push(polylineItem)
    map.value.add(polylineItem)
  })

  // 创建绿色的动画轨迹（初始只有起点）
  greenPolyline.value = new amap.value.Polyline({
    path: [path.value[0]],
    strokeColor: '#95DDA2',
    strokeWeight: 6,
    strokeOpacity: 0.8,
    lineJoin: 'round',
    lineCap: 'round',
    showDir: true
  })

  map.value.add([greenPolyline.value, marker.value])

  // 只将地图中心设置到起始点，保持当前缩放级别
  map.value.setCenter(initialPosition)

  // 修改动画逻辑以支持虚线
  excludeTimes.value = excludeTimesSet // 保存排除时间供动画使用
  animateTrajectory()
}

function animateTrajectory() {
  let lastAngle = 0
  const animate = (currentTime) => {
    if (!lastTime.value) lastTime.value = currentTime
    const deltaTime = currentTime - lastTime.value
    lastTime.value = currentTime

    if (currentIndex.value < path.value.length - 1) {
      currentIndex.value += animationSpeed * (deltaTime / 16.67)
      const currentPosition = interpolatePosition(currentIndex.value)

      // 修改这部分：使用与蓝色轨迹完全相同的分段逻辑
      const completedPathData = path.value.slice(0, Math.floor(currentIndex.value) + 1)
      completedPathData.push(currentPosition)

      // 移除旧的绿色轨迹
      if (greenPolyline.value) {
        if (Array.isArray(greenPolyline.value)) {
          map.value.remove(greenPolyline.value)
        } else {
          map.value.remove([greenPolyline.value])
        }
      }

      // 使用与蓝色轨迹完全相同的分段逻辑
      const segments = []
      let currentSegment = {
        path: [],
        isDashed: false
      }

      completedPathData.forEach((point, index) => {
        const time = props.trajectory.points[Math.min(index, props.trajectory.points.length - 1)].time
        const isExcluded = excludeTimes.value.has(time)
        const prevTime = index > 0 ? props.trajectory.points[Math.min(index - 1, props.trajectory.points.length - 1)].time : null
        const nextTime = index < completedPathData.length - 1 ? props.trajectory.points[Math.min(index + 1, props.trajectory.points.length - 1)].time : null
        const isPrevExcluded = prevTime ? excludeTimes.value.has(prevTime) : false
        const isNextExcluded = nextTime ? excludeTimes.value.has(nextTime) : false

        // 当前点是排除点的开始（前一个点不是排除点）
        if (isExcluded && !isPrevExcluded) {
          if (currentSegment.path.length > 0) {
            segments.push(currentSegment)
          }
          segments.push({
            path: [currentSegment.path[currentSegment.path.length - 1], point],
            isDashed: false
          })
          currentSegment = {
            path: [point],
            isDashed: true
          }
        }
        // 当前点是排除点的结束（下一个点不是排除点）
        else if (isExcluded && !isNextExcluded) {
          currentSegment.path.push(point)
          segments.push(currentSegment)
          currentSegment = {
            path: [point],
            isDashed: false
          }
        }
        // 其他情况
        else {
          if (currentSegment.path.length === 0 || currentSegment.isDashed === isExcluded) {
            currentSegment.path.push(point)
          } else {
            segments.push(currentSegment)
            currentSegment = {
              path: [point],
              isDashed: isExcluded
            }
          }
        }
      })

      if (currentSegment.path.length > 0) {
        segments.push(currentSegment)
      }

      // 创建新的绿色轨迹
      greenPolyline.value = segments.map(segment => new amap.value.Polyline({
        path: segment.path,
        strokeColor: '#95DDA2',
        strokeWeight: 6,
        strokeOpacity: 0.8,
        lineJoin: 'round',
        lineCap: 'round',
        showDir: true,
        strokeStyle: segment.isDashed ? 'dashed' : 'solid',
        strokeDasharray: segment.isDashed ? [10, 5] : null
      }))

      map.value.add(greenPolyline.value)

      if (lastPosition.value) {
        const newAngle = calculateAngle(lastPosition.value, currentPosition)
        const interpolatedAngle = interpolateAngle(lastAngle, newAngle, 0.1)
        marker.value.setAngle(interpolatedAngle)
        lastAngle = interpolatedAngle
      }
      lastPosition.value = currentPosition

      marker.value.setPosition(currentPosition)

      const bounds = map.value.getBounds()
      if (!bounds.contains(currentPosition)) {
        map.value.setCenter(currentPosition)
      }

      if (map.value.getZoom() < 14) {
        map.value.setZoom(14)
      }

      animationInterval.value = requestAnimationFrame(animate)
    } else {
      isAnimationComplete.value = true
      cancelAnimationFrame(animationInterval.value)
    }
  }
  animationInterval.value = requestAnimationFrame(animate)
}

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

function calculateAngle(start, end) {
  const dx = end[0] - start[0]
  const dy = end[1] - start[1]
  let angle = Math.atan2(dy, dx) * 180 / Math.PI

  // 调整角度使其与地图坐标系统一致
  angle = 90 - angle
  if (angle < 0) {
    angle += 360
  }

  return angle
}

function interpolateAngle(startAngle, endAngle, factor) {
  const shortestAngle = ((((endAngle - startAngle) % 360) + 540) % 360) - 180
  return startAngle + shortestAngle * factor
}

function showOriginalTrajectory() {
  // 设置需要的参数
  phoneNumber.value = props.trajectory.phoneNumber || ''
  startTime.value = props.trajectory.startTime || ''
  endTime.value = props.trajectory.endTime || ''

  // 检查是否所有必要的参数都已设置
  if (!phoneNumber.value || !startTime.value || !endTime.value) {
    ElMessage.error('缺少必要的参数，无法显示原始轨迹')
    return
  }

  // 显示对话框
  showOriginalDialog.value = true
}

function fetchAndDrawLandPlots() {
  if (path.value.length === 0) return

  const centerIndex = Math.floor(path.value.length / 2)
  const centerPoint = path.value[centerIndex]

  // 显示加载状态
  const loadingInstance = ElLoading.service({
    lock: true,
    text: '加载地块数据中...',
    spinner: 'el-icon-loading'
  })

  request({
    url: '/landPlot/byCoordinate',
    method: 'get',
    params: {
      longitude: centerPoint[0],
      latitude: centerPoint[1],
      distance: 5000
    }
  })
    .then(response => {
      if (response.code === 200 && response.data) {
        landPlots.value = response.data
        drawLandPlots()
      }
    })
    .catch(error => {
      console.error('获取地块数据失败:', error)
      ElMessage.error('获取地块数据失败，请稍后重试')
    })
    .finally(() => {
      loadingInstance.close()
    })
}

function drawLandPlots() {
  if (!landPlots.value.length) return

  const loadingInstance = ElLoading.service({
    lock: true,
    text: '加载地块数据中...',
    spinner: 'el-icon-loading'
  })

  // 存储所有多边形和与轨迹相关的多边形
  const allPolygons = []
  const relatedPolygons = []

  request({
    url: '/landPlotExclude/batchQuery',
    method: 'post',
    data: landPlots.value.map(plot => plot.id.toString())
  })
    .then(response => {
      if (response.code !== 200) {
        throw new Error(response.msg || '获取插花地数据失败')
      }

      const excludeAreasMap = new Map(
        response.data.map(item => [
          item.landPlotId.toString(),
          item.landPlotExcludes || []
        ])
      )

      landPlots.value.forEach(plot => {
        const boundary = plot.boundary
        if (boundary) {
          const coordinates = parseBoundary(boundary)
          const plotIdStr = plot.id.toString()
          const excludeAreas = excludeAreasMap.get(plotIdStr) || []
          const excludePaths = excludeAreas.map(area => parseBoundary(area.boundary))

          // 创建地块多边形
          const plotPolygon = new amap.value.Polygon({
            path: [coordinates, ...excludePaths],
            strokeColor: '#A9DAF3',
            strokeWeight: 2,
            strokeOpacity: 0.8,
            fillColor: '#E0F0DD',
            fillOpacity: 1,
            zIndex: 11,
            extData: { plotId: plotIdStr }
          })

          // 检查地块是否与轨迹相交
          const plotBounds = plotPolygon.getBounds()
          const isRelated = path.value.some(point => plotBounds.contains(point))

          // 如果地块与轨迹相关，添加到相关多边形数组
          if (isRelated) {
            relatedPolygons.push(plotPolygon)
          }

          allPolygons.push(plotPolygon)

          // 添加插花地边框
          excludeAreas.forEach(excludePlot => {
            if (excludePlot.boundary) {
              const excludePolygon = new amap.value.Polygon({
                path: parseBoundary(excludePlot.boundary),
                strokeColor: '#A9DAF3',
                strokeWeight: 2,
                strokeOpacity: 0.8,
                fillOpacity: 0,
                zIndex: 51,
                extData: { excludeId: excludePlot.id.toString() }
              })
              allPolygons.push(excludePolygon)
            }
          })
        }
      })

      // 添加所有多边形到地图
      map.value.add(allPolygons)

      // 调整视野
      if (relatedPolygons.length > 0) {
        // 如果有相关地块，聚焦到相关地块
        map.value.setFitView(relatedPolygons)
      } else {
        // 如果没有相关地块，聚焦到轨迹路径
        const pathPolygon = new amap.value.Polygon({
          path: path.value
        })
        map.value.setFitView([pathPolygon])
      }
    })
    .catch(error => {
      console.error('加载地块数据失败:', error)
      ElMessage.error('加载地块数据失败，请稍后重试')
    })
    .finally(() => {
      loadingInstance.close()
    })
}

function parseBoundary(boundary) {
  // 解析 POLYGON 字符串
  const coordsString = boundary.match(/\(\((.*?)\)\)/)[1]
  return coordsString.split(',').map(pair => {
    // WKT 每对为“经度 纬度”，高德需要 [经度, 纬度]
    const [lng, lat] = pair.trim().split(' ')
    return [parseFloat(lng), parseFloat(lat)]
  })
}

function toggleSatellite() {
  if (satelliteLayer.value) {
    map.value.remove(satelliteLayer.value)
    satelliteLayer.value = null
  } else {
    satelliteLayer.value = new amap.value.TileLayer.Satellite()
    map.value.add(satelliteLayer.value)
  }
}

function toggleRoadNet() {
  if (roadNetLayer.value) {
    map.value.remove(roadNetLayer.value)
    roadNetLayer.value = null
  } else {
    roadNetLayer.value = new amap.value.TileLayer.RoadNet()
    map.value.add(roadNetLayer.value)
  }
}

function replayTrajectory() {
  if (isAnimationComplete.value || currentIndex.value >= path.value.length - 1) {
    // 取消当前动画
    if (animationInterval.value) {
      cancelAnimationFrame(animationInterval.value)
    }

    // 移除旧的绿色轨迹
    if (greenPolyline.value) {
      if (Array.isArray(greenPolyline.value)) {
        map.value.remove(greenPolyline.value)
      } else {
        map.value.remove([greenPolyline.value])
      }
    }

    // 重置状态
    currentIndex.value = 0
    isAnimationComplete.value = false
    lastPosition.value = null
    lastTime.value = 0

    // 重置标记位置到起点
    const initialPosition = path.value[0]
    marker.value.setPosition(initialPosition)
    marker.value.setAngle(0)

    // 创建新的绿色轨迹
    greenPolyline.value = new amap.value.Polyline({
      path: [initialPosition],
      strokeColor: '#95DDA2',
      strokeWeight: 6,
      strokeOpacity: 0.8,
      lineJoin: 'round',
      lineCap: 'round',
      showDir: true
    })

    // 添加新的绿色轨迹到地图
    map.value.add(greenPolyline.value)

    // 重新开始动画
    animateTrajectory()
  }
}

onMounted(() => {
  initMap()
  isDialogOpened.value = true // 设置对话框为打开状态
})

onBeforeUnmount(() => {
  if (animationInterval.value) {
    cancelAnimationFrame(animationInterval.value)
  }
  if (map.value) {
    map.value.destroy()
  }
  isDialogOpened.value = false // 设置对话框为关闭状态
})
</script>

  <style scoped>
  .map-container {
    width: 100%;
    height: 600px;
    position: relative;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }

  .amap-container {
    width: 100%;
    height: 100%;
    z-index: 1000;
  }

  .custom-zoom-button {
    width: 30px;
    height: 30px;
    background-color: white;
    border: 1px solid #ccc;
    border-radius: 2px;
    font-size: 18px;
    font-weight: bold;
    color: #666;
    cursor: pointer;
    margin: 2px;
  }

  .custom-zoom-button:hover {
    background-color: #f0f0f0;
  }

  .map-controls {
    position: absolute;
    top: 20px;
    right: 20px;
    z-index: 1000;
    display: flex;
    flex-direction: column;
    gap: 10px;
    width: 170px;
  }

  .control-btn {
    width: 100%;
    height: 40px;
    border-radius: 20px;
    font-size: 14px;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s ease;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }

  .control-btn:first-child {
    margin-left: auto;
    width: 97%; /* 第一个按钮稍微窄一些,实现右移效果 */
  }

  .control-btn i {
    margin-right: 5px;
  }

  /* 移除之前可能影响对齐的样式 */
  .control-btn span {
    display: inline;
  }
  </style>

