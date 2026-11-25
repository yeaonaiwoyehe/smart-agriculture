<template>
  <div class="full-screen-heatmap">
    <h1>全屏热力图展示</h1>
    <el-button @click="toggleViewMode">切换{{ viewMode === '3D' ? '2D' : '3D' }}模式</el-button>
    <div id="full-screen-container" class="map-container"></div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getHeatmapData } from '@/api/field/square'
import { loadAMap } from '@/utils/amap'

defineOptions({ name: 'FullScreenHeatmap' })

const map = ref(null)
const heatmap = ref(null)
const amap = ref(null)
const viewMode = ref('3D')

function initMap() {
  loadAMap(['AMap.HeatMap', 'AMap.DistrictLayer']).then((AMap) => {
    amap.value = AMap
    map.value = new AMap.Map('full-screen-container', {
      zoom: 15,
      center: [114.52400, 37.99800],
      viewMode: viewMode.value,
      mapStyle: 'amap://styles/blue',
      backgroundColor: '#000B45',
      zooms: [4, 18],
      showIndoorMap: false
    })

    const chinaLayer = new AMap.DistrictLayer.Country({
      zIndex: 10,
      SOC: 'CHN',
      depth: 1,
      styles: {
        'nation-stroke': '#00C5FF',
        'coastline-stroke': '#00C5FF',
        'fill': 'rgba(0, 11, 69, 0.8)'
      }
    })
    map.value.add(chinaLayer)

    map.value.on('complete', fetchHeatmapData)
  }).catch(e => {
    ElMessage.error('高德地图脚本加载失败')
  })
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
    radius: 25,
    opacity: [0, 0.8],
    gradient: {
      0: '#000B45',
      0.2: '#000080',
      0.4: '#008000',
      0.6: '#00FF00',
      0.7: '#FFFF00',
      0.8: '#FFA500',
      1.0: '#FF0000'
    },
    zooms: [3, 18]
  })

  const maxWeight = Math.max(...heatmapData.map(item => item.count))

  heatmap.value.setDataSet({
    data: heatmapData,
    max: maxWeight
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
.full-screen-heatmap {
  width: 100vw;
  height: 100vh;
}

.map-container {
  width: 100%;
  height: 100%;
}
</style>
