<template>
    <div class="map-container" v-if="isMapVisible">
      <div id="map-container-ori" class="amap-container"></div>
      <div class="map-controls">
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
    </div>
  </template>

  <script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElLoading, ElMessage } from 'element-plus'
import { loadAMap } from '@/utils/amap'
import request from '@/utils/request'
import trunkImg from '@/assets/images/trunk.png'

defineOptions({ name: 'VehicleTrajectoryOriginal' })

const props = defineProps({
  trajectoryInfoId: {
    type: [String, Number],
    required: true
  },
  phoneNumber: {
    type: String,
    required: true
  },
  startTime: {
    type: String,
    required: true
  },
  endTime: {
    type: String,
    required: true
  }
})

const map = ref(null)
const path = ref([])
const amap = ref(null)
const isMapVisible = ref(true)
const isMapInitialized = ref(false)
const satelliteLayer = ref(null)
const roadNetLayer = ref(null)
const bluePolyline = ref(null)
const greenPolyline = ref(null)
const marker = ref(null)
const markerIcon = ref(null)
const currentIndex = ref(0)
const animationInterval = ref(null)
const animationSpeed = 0.01
const isAnimationComplete = ref(false)
const lastPosition = ref(null)
const lastTime = ref(0)
const landPlots = ref([])
const animationFrame = ref(null)
const lastAngle = ref(0)

function initMap() {
  loadAMap(['AMap.Polyline', 'AMap.Marker', 'AMap.Scale', 'AMap.ToolBar']).then((AMap) => {
    amap.value = AMap
    map.value = new AMap.Map('map-container-ori', {
      zoom: 14,
      center: [118.784129, 32.04181],
      viewMode: '3D',
      mapStyle: 'amap://styles/macaron',
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

    fetchOriginalTrajectory()
    map.value.on('complete', () => {
      isMapInitialized.value = true
      resetAndDrawTrajectory()
    })
  }).catch(e => {
    console.error('Failed to load AMap API:', e)
  })
}

function fetchOriginalTrajectory() {
  const params = {
    phone: props.phoneNumber,
    startTime: props.startTime,
    endTime: props.endTime
  }
  request({
    url: '/originalTrajectory/queryOriginalTrajectoryByPhoneAndTime',
    method: 'get',
    params: params
  }).then(response => {
    if (response.code === 200 && response.data) {
      path.value = response.data.map(point => [point.longitude, point.latitude])
      console.log('轨迹数据:', path.value) // 添加日志
      nextTick(() => {
        resetAndDrawTrajectory()
        fetchAndDrawLandPlots()
      })
    } else {
      ElMessage.error('获取轨迹数据失败')
    }
  }).catch(error => {
    console.error('获取轨迹数据失败', error)
    ElMessage.error('获取轨迹数据失败')
  })
}

function resetAndDrawTrajectory() {
  if (!amap.value || !isMapInitialized.value) {
    return
  }
  clearMap()
  currentIndex.value = 0
  isAnimationComplete.value = false
  lastPosition.value = null
  lastTime.value = 0

  if (path.value.length > 0) {
    drawTrajectory()
    fetchAndDrawLandPlots()
  } else {
    map.value.setFitView()
  }
}

function clearMap() {
  if (animationInterval.value) {
    cancelAnimationFrame(animationInterval.value)
  }
  if (bluePolyline.value) {
    map.value.remove(bluePolyline.value)
  }
  if (greenPolyline.value) {
    map.value.remove(greenPolyline.value)
  }
  if (marker.value) {
    map.value.remove(marker.value)
  }
}

function drawTrajectory() {
  if (!amap.value || path.value.length === 0) {
    ElMessage.warning('轨迹数据为空')
    return
  }

  // 创建标记和轨迹线
  markerIcon.value = new amap.value.Icon({
    size: new amap.value.Size(20, 20),
    image: trunkImg,
    imageSize: new amap.value.Size(20, 20)
  })

  const initialPosition = path.value[0]
  marker.value = new amap.value.Marker({
    position: initialPosition,
    icon: markerIcon.value,
    angle: 0,
    anchor: 'center'
  })

  bluePolyline.value = new amap.value.Polyline({
    path: path.value,
    strokeColor: '#5DA6CB',
    strokeWeight: 6,
    strokeOpacity: 0.8,
    lineJoin: 'round',
    lineCap: 'round',
    showDir: true
  })

  greenPolyline.value = new amap.value.Polyline({
    path: [path.value[0]],
    strokeColor: '#95DDA2',
    strokeWeight: 6,
    strokeOpacity: 0.8,
    lineJoin: 'round',
    lineCap: 'round',
    showDir: true
  })

  map.value.add([bluePolyline.value, greenPolyline.value, marker.value])

  // 只将地图中心设置到起始点，保持当前缩放级别
  map.value.setCenter(initialPosition)

  // 确保最小缩放级别为14
  if (map.value.getZoom() < 14) {
    map.value.setZoom(14)
  }

  animateTrajectory()
}

function animateTrajectory() {
  const animate = (currentTime) => {
    if (!lastTime.value) lastTime.value = currentTime
    const deltaTime = currentTime - lastTime.value
    lastTime.value = currentTime

    if (currentIndex.value < path.value.length - 1) {
      currentIndex.value += animationSpeed * (deltaTime / 16.67)
      const currentPosition = interpolatePosition(currentIndex.value)

      const completedPath = path.value.slice(0, Math.floor(currentIndex.value) + 1)
      completedPath.push(currentPosition)
      greenPolyline.value.setPath(completedPath)

      if (lastPosition.value) {
        const newAngle = calculateAngle(lastPosition.value, currentPosition)
        // 使用插值函数平滑旋转
        const interpolatedAngle = interpolateAngle(lastAngle.value, newAngle, 0.1)
        marker.value.setAngle(interpolatedAngle)
        lastAngle.value = interpolatedAngle
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

      animationFrame.value = requestAnimationFrame(animate)
    } else {
      console.log('动画完成') // 添加日志
      isAnimationComplete.value = true
      cancelAnimationFrame(animationFrame.value)
    }
  }
  animationFrame.value = requestAnimationFrame(animate)
}

function interpolatePosition(index) {
  const floorIndex = Math.floor(index)
  const ceilIndex = Math.min(floorIndex + 1, path.value.length - 1)
  const fraction = index - floorIndex

  const start = path.value[floorIndex]
  const end = path.value[ceilIndex]

  const interpolated = [
    start[0] + (end[0] - start[0]) * fraction,
    start[1] + (end[1] - start[1]) * fraction
  ]

  console.log('插值计算:', { floorIndex, ceilIndex, fraction, start, end, interpolated }) // 添加日志

  return interpolated
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
  console.log('重播轨迹') // 添加日志
  currentIndex.value = 0
  isAnimationComplete.value = false
  lastPosition.value = null
  if (path.value.length > 0) {
    greenPolyline.value.setPath([path.value[0]])
    marker.value.setPosition(path.value[0])
    animateTrajectory()
  } else {
    console.log('轨迹数据为空，无法重播') // 添加日志
  }
}

function fetchAndDrawLandPlots() {
  if (path.value.length === 0) return

  const centerIndex = Math.floor(path.value.length / 2)
  const centerPoint = path.value[centerIndex]

  request({
    url: '/landPlot/byCoordinate',
    method: 'get',
    params: {
      longitude: centerPoint[0],
      latitude: centerPoint[1],
      distance: 5000
    }
  }).then(response => {
    if (response.code === 200 && response.data) {
      landPlots.value = response.data
      drawLandPlots()
    }
  }).catch(error => {
    console.error('获取地块数据失败', error)
  })
}

function drawLandPlots() {
  if (!landPlots.value.length) return

  const loading = ElLoading.service({
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
            strokeColor: '#237CBE',
            strokeWeight: 2,
            strokeOpacity: 0.8,
            fillColor: '#E0F0DD',
            fillOpacity: 0.8,
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
      loading.close()
    })
}

function parseBoundary(boundary) {
  const coordsString = boundary.match(/\(\((.*?)\)\)/)[1]
  return coordsString.split(',').map(pair => {
    // WKT 每对为“经度 纬度”，高德需要 [经度, 纬度]
    const [lng, lat] = pair.trim().split(' ')
    return [parseFloat(lng), parseFloat(lat)]
  })
}

function showMap() {
  isMapVisible.value = true
  nextTick(() => {
    initMap()
  })
}

function hideMap() {
  isMapVisible.value = false
  if (map.value) {
    map.value.destroy()
    map.value = null
  }
}

onMounted(() => {
  initMap()
})

onBeforeUnmount(() => {
  if (animationFrame.value) {
    cancelAnimationFrame(animationFrame.value)
  }
  if (map.value) {
    map.value.destroy()
    map.value = null
  }
})
</script>

  <style scoped>
  .map-container {
    width: 100%;
    height: 100%;
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
    width: 97%;
  }

  .control-btn i {
    margin-right: 5px;
  }

  .control-btn span {
    display: inline;
  }
  </style>
