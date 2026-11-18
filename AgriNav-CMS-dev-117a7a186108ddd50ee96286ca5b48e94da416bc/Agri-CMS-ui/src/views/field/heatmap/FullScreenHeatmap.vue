<template>
  <div class="full-screen-heatmap">
    <h1>全屏热力图展示</h1>
    <el-button @click="toggleViewMode">切换{{ viewMode === '3D' ? '2D' : '3D' }}模式</el-button>
    <div id="full-screen-container" class="map-container"></div>
  </div>
</template>

<script>
import {getHeatmapData} from '@/api/field/square'
import { loadAMap } from '@/utils/amap'

export default {
  name: 'FullScreenHeatmap',
  data() {
    return {
      map: null,
      heatmap: null,
      viewMode: '3D'
    }
  },
  mounted() {
    this.initMap()
  },
  methods: {
    initMap() {
      loadAMap(['AMap.HeatMap', 'AMap.DistrictLayer']).then((AMap) => {
        this.map = new AMap.Map('full-screen-container', {
          zoom: 15,
          center: [114.52400, 37.99800],
          viewMode: this.viewMode,
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
        this.map.add(chinaLayer)

        this.map.on('complete', this.fetchHeatmapData)
      }).catch(e => {
        this.$message.error('高德地图脚本加载失败')
      })
    },
    toggleViewMode() {
      this.viewMode = this.viewMode === '3D' ? '2D' : '3D'
      if (this.map) {
        this.map.destroy()
        this.map = null
      }
      this.$nextTick(() => {
        this.initMap()
      })
    },
    async fetchHeatmapData() {
      try {
        const response = await getHeatmapData()
        if (response.code === 200 && response.data) {
          this.createHeatmap(response.data)
        } else {
          this.$message.error('获取热力图数据失败')
        }
      } catch (error) {
        console.error('Error:', error)
        this.$message.error('获取热力图数据失败')
      }
    },
    createHeatmap(data) {
      const heatmapData = data.map(item => ({
        lng: Number(item.longitude),
        lat: Number(item.latitude),
        count: Number(item.weight)
      }))

      this.heatmap = new AMap.HeatMap(this.map, {
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

      this.heatmap.setDataSet({
        data: heatmapData,
        max: maxWeight
      })

      this.fitMapBounds(heatmapData)
    },
    fitMapBounds(heatmapData) {
      const bounds = new AMap.Bounds(
        [Math.min(...heatmapData.map(p => p.lng)), Math.min(...heatmapData.map(p => p.lat))],
        [Math.max(...heatmapData.map(p => p.lng)), Math.max(...heatmapData.map(p => p.lat))]
      )
      this.map.setBounds(bounds)
    }
  }
}
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
