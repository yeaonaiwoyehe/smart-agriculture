<template>
  <div class="heatmap-container">
    <h1>热力图展示</h1>
    <el-button @click="toggleViewMode">切换{{ viewMode === '3D' ? '2D' : '3D' }}模式</el-button>
    <el-button @click="goToFullScreen">全屏展示</el-button>
    <div id="container" class="map-container"></div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getHeatmapData } from '@/api/field/square'
import { loadAMap } from '@/utils/amap'

defineOptions({ name: 'Heatmap' })

const router = useRouter()

const map = ref(null)
const heatmap = ref(null)
const amap = ref(null)
const viewMode = ref('3D')

function initMap() {
  loadAMap(['AMap.HeatMap', 'AMap.DistrictLayer']).then((AMap) => {
    amap.value = AMap
    map.value = new AMap.Map('container', {
      zoom: 15,
      center: [114.52400, 37.99800],
      viewMode: viewMode.value,
      mapStyle: 'amap://styles/blue',
      backgroundColor: '#000B45',
      zooms: [4, 18],
      showIndoorMap: false
    })

    // 添加中国轮廓图层
    const chinaLayer = new AMap.DistrictLayer.Country({
      zIndex: 10,
      SOC: 'CHN',
      depth: 1,
      styles: {
        'nation-stroke': '#00C5FF',         // 更亮的蓝色边界线
        'coastline-stroke': '#00C5FF',      // 海岸线同样的颜色
        'fill': 'rgba(0, 11, 69, 0.8)'      // 更深的蓝色填充
      }
    })
    map.value.add(chinaLayer)

    map.value.on('complete', fetchHeatmapData)
  }).catch(e => {
    ElMessage.error('高德地图脚本加载失败')
  })
}

async function fetchHeatmapData() {
  try {
    const response = await getHeatmapData()
    if (response.code === 200 && response.data) {
      createHeatmap(response.data)
    } else {
      ElMessage.error('获取热力图数据失败')
    }
  } catch (error) {
    console.error('Error:', error)
    ElMessage.error('获取热力图数据失败')
  }
}

function createHeatmap(data) {
  const heatmapData = data.map(item => ({
    lng: Number(item.longitude),
    lat: Number(item.latitude),
    count: Number(item.weight)
  }))

  heatmap.value = new amap.value.HeatMap(map.value, {
    radius: 28,
    opacity: [0, 0.85],
    gradient: {
      0: '#000B45',     // 深邃藏青色（与背景相同）
      0.2: '#000080',   // 深蓝色
      0.4: '#008000',   // 深绿色
      0.6: '#00FF00',   // 亮绿色
      0.7: '#FFFF00',   // 黄色
      0.8: '#FFA500',   // 橙色
      1.0: '#FF0000'    // 红色
    },
    zooms: [3, 18]
  })

  // 每个作业点权重为 1，max 固定为 4：单点不直接满红，
  // 多个相邻点的热力圈叠加后才逐级变热，形成有层次的作业覆盖带。
  heatmap.value.setDataSet({
    data: heatmapData,
    max: 4
  })

  fitMapBounds(heatmapData)
}

function fitMapBounds(heatmapData) {
  const bounds = new amap.value.Bounds(
    [Math.min(...heatmapData.map(p => p.lng)), Math.min(...heatmapData.map(p => p.lat))],
    [Math.max(...heatmapData.map(p => p.lng)), Math.max(...heatmapData.map(p => p.lat))]
  )
  map.value.setBounds(bounds)
}

function toggleViewMode() {
  viewMode.value = viewMode.value === '3D' ? '2D' : '3D'
  if (map.value) {
    map.value.destroy()
    map.value = null
  }
  nextTick(() => {
    initMap()
  })
}

function goToFullScreen() {
  const fullScreenUrl = router.resolve({ name: 'FullScreenHeatmap' }).href
  window.open(fullScreenUrl, '_blank')
}

onMounted(() => {
  initMap()
})

onBeforeUnmount(() => {
  if (map.value) {
    map.value.destroy()
    map.value = null
  }
})
</script>

<style scoped>
.heatmap-container {
  width: 100%;
  height: 100%;
}

.map-container {
  width: 100%;
  height: 500px;
}
</style>
