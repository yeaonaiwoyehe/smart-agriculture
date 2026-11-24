<template>
  <div class="dashboard-container">
    <div class="top-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card>
            <div class="card-item">
              <div class="card-title">用户活跃率</div>
              <div class="card-value">{{ userInfo.percent || 0 }}%</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card>
            <div class="card-item">
              <div class="card-title">农事任务完成率</div>
              <div class="card-value">{{ farmWorkInfo.percent || 0 }}%</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card>
            <div class="card-item">
              <div class="card-title">总地块面积</div>
              <div class="card-value">{{ landPlotArea.landArea || 0 }}亩</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card>
            <div class="card-item">
              <div class="card-title">最热作物</div>
              <div class="card-value">{{ mostCrop.workName || '-' }}</div>
              <div class="card-sub-value">{{ mostCrop.totalArea || 0 }}亩</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="mt-20">
        <el-col :span="6">
          <el-card>
            <div class="card-item">
              <div class="card-title">发布最多农活的省份</div>
              <div class="card-value">{{ mostFarmWorkProvince.name || '-' }}</div>
              <div class="card-sub-value">{{ mostFarmWorkProvince.value || 0 }}次</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card>
            <div class="card-item">
              <div class="card-title">工作面积最大的田地操作</div>
              <div class="card-value">{{ mostAreaCropWork.workName || '-' }}</div>
              <div class="card-sub-value">{{ mostAreaCropWork.totalArea || 0 }}亩</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card>
            <div class="card-item">
              <div class="card-title">作业次数最多的田地操作</div>
              <div class="card-value">{{ mostCountCropWork.workName || '-' }}</div>
              <div class="card-sub-value">{{ mostCountCropWork.totalArea || 0 }}次</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 图表区域 -->
    <div class="charts-container">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card>
            <div ref="cropWorkChart" style="height: 300px"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <div ref="cropSituationChart" style="height: 300px"></div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="mt-20">
        <el-col :span="12">
          <el-card>
            <div ref="landPlotChart" style="height: 300px"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <div ref="provinceChart" style="height: 300px"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import * as echarts from 'echarts'
import {
  getUserInfo as getUserInfoApi,
  getLandPlotsTotalArea as getLandPlotsTotalAreaApi,
  getFarmWorkInfo as getFarmWorkInfoApi,
  getCropWorkDetail as getCropWorkDetailApi,
  getCropSituation as getCropSituationApi,
  getLandPlotCountByProvince as getLandPlotCountByProvinceApi,
  getMostCrop as getMostCropApi,
  getLandPlotNumByMonth as getLandPlotNumByMonthApi,
  getMostFarmWorkProvince as getMostFarmWorkProvinceApi,
  getMostAreaCropWork as getMostAreaCropWorkApi,
  getMostCountCropWork as getMostCountCropWorkApi
} from '@/api/monitor/instrumentpanel'

defineOptions({ name: 'Dashboard' })

const userInfo = ref({
  percent: 0
})
const farmWorkInfo = ref({ percent: 0 })
const landPlotArea = ref({ landArea: 0 })
const mostCrop = ref({ workName: '', totalArea: 0 })
const cropWorkData = ref([])
const cropSituationData = ref([])
const provinceData = ref([])
const landPlotData = ref([])
const mostFarmWorkProvince = ref({})
const mostAreaCropWork = ref({})
const mostCountCropWork = ref({})

const cropWorkChart = ref()
const cropSituationChart = ref()
const landPlotChart = ref()
const provinceChart = ref()

async function initData() {
  try {
    const results = await Promise.allSettled([
      getFarmWorkInfo(),
      getLandPlotsTotalArea(),
      getCropWorkDetail(),
      getCropSituation(),
      getLandPlotCountByProvince(),
      getMostCrop(),
      getLandPlotNumByMonth(),
      getMostFarmWorkProvince(),
      getMostAreaCropWork(),
      getMostCountCropWork()
    ])

    console.log('landPlotData before init chart:', landPlotData.value)

    await nextTick()

    initLandPlotChart()
    initCropSituationChart()
    initProvinceChart()
    initCropWorkChart()
  } catch (error) {
    console.error('初始化数据失败:', error)
  }
}

async function getUserInfo() {
  try {
    const response = await getUserInfoApi()
    userInfo.value = {
      percent: response.percent || 0
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    userInfo.value = {
      percent: 0
    }
  }
}

async function getLandPlotsTotalArea() {
  try {
    const response = await getLandPlotsTotalAreaApi()
    landPlotArea.value = {
      landArea: response.landArea || 0
    }
  } catch (error) {
    console.error('获取地块面积失败:', error)
    landPlotArea.value = {
      landArea: 0
    }
  }
}

async function getFarmWorkInfo() {
  try {
    const response = await getFarmWorkInfoApi()
    farmWorkInfo.value = {
      percent: response.percent || 0
    }
  } catch (error) {
    console.error('获取农事工作信息失败:', error)
    farmWorkInfo.value = {
      percent: 0
    }
  }
}

async function getCropWorkDetail() {
  try {
    const response = await getCropWorkDetailApi()
    cropWorkData.value = response || []
  } catch (error) {
    console.error('获取农事工作面积分布数据失败:', error)
    cropWorkData.value = []
  }
}

async function getCropSituation() {
  try {
    const response = await getCropSituationApi()
    cropSituationData.value = response || []
  } catch (error) {
    console.error('获取作物分布情况失败:', error)
    cropSituationData.value = []
  }
}

async function getLandPlotCountByProvince() {
  try {
    const response = await getLandPlotCountByProvinceApi()
    provinceData.value = response || []
  } catch (error) {
    console.error('获取省份地块分布数据失败:', error)
    provinceData.value = []
  }
}

async function getLandPlotNumByMonth() {
  try {
    const response = await getLandPlotNumByMonthApi()
    landPlotData.value = response || []
  } catch (error) {
    console.error('获取地块数量月度趋势数据失败:', error)
    landPlotData.value = []
  }
}

async function getMostCrop() {
  try {
    const response = await getMostCropApi()
    mostCrop.value = {
      workName: response.workName || '-',
      totalArea: response.totalArea || 0
    }
  } catch (error) {
    console.error('获取最热作物信息失败:', error)
    mostCrop.value = {
      workName: '-',
      totalArea: 0
    }
  }
}

async function getMostFarmWorkProvince() {
  try {
    const response = await getMostFarmWorkProvinceApi()
    mostFarmWorkProvince.value = response || {
      name: '-',
      value: 0
    }
  } catch (error) {
    console.error('获取发布最多农活的省份数据失败:', error)
    mostFarmWorkProvince.value = {
      name: '-',
      value: 0
    }
  }
}

async function getMostAreaCropWork() {
  try {
    const response = await getMostAreaCropWorkApi()
    mostAreaCropWork.value = response || {
      workName: '-',
      totalArea: 0
    }
  } catch (error) {
    console.error('获取工作面积最大的田地操作数据失败:', error)
    mostAreaCropWork.value = {
      workName: '-',
      totalArea: 0
    }
  }
}

async function getMostCountCropWork() {
  try {
    const response = await getMostCountCropWorkApi()
    mostCountCropWork.value = response || {
      workName: '-',
      totalArea: 0
    }
  } catch (error) {
    console.error('获取作业次数最多的田地操作数据失败:', error)
    mostCountCropWork.value = {
      workName: '-',
      totalArea: 0
    }
  }
}

function initCropWorkChart() {
  if (!cropWorkChart.value) return
  const chart = echarts.init(cropWorkChart.value)
      const option = {
        title: {
          text: '农事作业统计',
          left: 'center',
          top: 20
        },
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}亩 ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left',
          bottom: '10%',
          containLabel: true
        },
        series: [{
          type: 'pie',
          radius: ['0%', '60%'],
          center: ['50%', '55%'],  // 调整饼图中心点位置
          data: cropWorkData.value && cropWorkData.value.length > 0
            ? cropWorkData.value.map(item => ({
                name: item.workName,
                value: item.totalArea
              }))
            : [{
                name: '暂无数据',
                value: 1,
                itemStyle: {
                  color: '#eee'
                },
                label: {
                  show: true,
                  position: 'center',
                  fontSize: 14
                }
              }]
        }]
      }
      chart.setOption(option)
}

function initCropSituationChart() {
      if (!cropSituationChart.value) return
      const chart = echarts.init(cropSituationChart.value)
      const option = {
        title: {
          text: '作物分布情况',
          left: 'center',
          top: 20
        },
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}亩 ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left',
          bottom: '10%',
          containLabel: true
        },
        series: [{
          type: 'pie',
          radius: ['0%', '60%'],
          center: ['50%', '55%'],
          data: cropSituationData.value && cropSituationData.value.length > 0
            ? cropSituationData.value
            : [{
                name: '暂无数据',
                value: 1,
                itemStyle: {
                  color: '#eee'
                },
                label: {
                  show: true,
                  position: 'center',
                  fontSize: 14
                }
              }]
        }]
      }
      chart.setOption(option)
}

function initLandPlotChart() {
      if (!landPlotChart.value) return
      const chart = echarts.init(landPlotChart.value)

      // 确保数据不为空，如果为空则提供默认数据
      const hasData = landPlotData.value && landPlotData.value.length > 0
      const xAxisData = hasData ? landPlotData.value.map(item => item.name) : ['暂无数据']
      const seriesData = hasData ? landPlotData.value.map(item => item.value) : [0]

      const option = {
        title: {
          text: '地块数量月度趋势',
          left: 'center',
          top: 20
        },
        tooltip: {
          trigger: 'axis',
          formatter: '{b}: {c}块'
        },
        grid: {
          top: '15%',
          left: '5%',
          right: '5%',
          bottom: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: xAxisData,
          axisTick: {
            alignWithLabel: true
          }
        },
        yAxis: {
          type: 'value',
          name: '地块数量',
          minInterval: 1
        },
        series: [{
          data: seriesData,
          type: 'line',
          smooth: true,
          itemStyle: {
            color: hasData ? '#409EFF' : '#eee'
          },
          label: hasData ? {} : {
            show: true,
            position: 'top',
            formatter: '暂无数据',
            fontSize: 14,
            color: '#999'
          }
        }]
      }
      chart.setOption(option)
}

function initProvinceChart() {
      if (!provinceChart.value) return
      const chart = echarts.init(provinceChart.value)
      const option = {
        title: {
          text: '省份地块分布',
          left: 'center',
          top: 20
        },
        tooltip: {
          trigger: 'axis',
          formatter: '{b}: {c}块'
        },
        grid: {
          top: '15%',
          left: '5%',
          right: '5%',
          bottom: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: provinceData.value.filter(item => item.value > 0).map(item => item.name),
          axisLabel: {
            interval: 0,
            rotate: 30
          }
        },
        yAxis: { type: 'value' },
        series: [{
          data: provinceData.value.filter(item => item.value > 0).map(item => item.value),
          type: 'bar'
        }]
      }
      chart.setOption(option)
}

onMounted(() => {
  getUserInfo()
  initData()
})
</script>

<style lang="scss" scoped>
.dashboard-container {
  padding: 20px;

  .top-cards {
    margin-bottom: 40px;

    .el-card {
      height: 120px;
      .el-card__body {
        padding: 20px;
        height: 100%;
      }
    }

    .mt-20 {
      margin-top: 30px;
    }

    .card-item {
      text-align: center;
      height: 100%;
      display: flex;
      flex-direction: column;
      justify-content: center;

      .card-title {
        font-size: 14px;
        color: #666;
        margin-bottom: 15px;
      }

      .card-value {
        font-size: 24px;
        font-weight: bold;
        color: #409EFF;
        margin-top: 10px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .card-sub-value {
        font-size: 14px;
        color: #999;
        margin-top: 8px;
      }
    }
  }

  .charts-container {
    .mt-20 {
      margin-top: 30px;
    }
  }
}
</style>
