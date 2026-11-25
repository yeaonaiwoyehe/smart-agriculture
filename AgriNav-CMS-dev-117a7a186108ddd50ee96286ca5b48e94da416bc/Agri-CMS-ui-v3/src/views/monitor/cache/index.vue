<template>
  <div class="app-container">
    <el-row>
      <el-col :span="24" class="card-box">
        <el-card>
          <template #header>
            <div><span><i class="el-icon-monitor"></i> 基本信息</span></div>
          </template>
          <div class="el-table el-table--enable-row-hover el-table--medium">
            <table cellspacing="0" style="width: 100%">
              <tbody>
              <tr>
                <td class="el-table__cell is-leaf">
                  <div class="cell">Redis版本</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell" v-if="cache.info">{{ cache.info.redis_version }}</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell">运行模式</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell" v-if="cache.info">{{
                      cache.info.redis_mode == "standalone" ? "单机" : "集群"
                    }}
                  </div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell">端口</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell" v-if="cache.info">{{ cache.info.tcp_port }}</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell">客户端数</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell" v-if="cache.info">{{ cache.info.connected_clients }}</div>
                </td>
              </tr>
              <tr>
                <td class="el-table__cell is-leaf">
                  <div class="cell">运行时间(天)</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell" v-if="cache.info">{{ cache.info.uptime_in_days }}</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell">使用内存</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell" v-if="cache.info">{{ cache.info.used_memory_human }}</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell">使用CPU</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell" v-if="cache.info">{{
                      parseFloat(cache.info.used_cpu_user_children).toFixed(2)
                    }}
                  </div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell">内存配置</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell" v-if="cache.info">{{ cache.info.maxmemory_human }}</div>
                </td>
              </tr>
              <tr>
                <td class="el-table__cell is-leaf">
                  <div class="cell">AOF是否开启</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell" v-if="cache.info">{{ cache.info.aof_enabled == "0" ? "否" : "是" }}</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell">RDB是否成功</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell" v-if="cache.info">{{ cache.info.rdb_last_bgsave_status }}</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell">Key数量</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell" v-if="cache.dbSize">{{ cache.dbSize }}</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell">网络入口/出口</div>
                </td>
                <td class="el-table__cell is-leaf">
                  <div class="cell" v-if="cache.info">{{
                      cache.info.instantaneous_input_kbps
                    }}kps/{{ cache.info.instantaneous_output_kbps }}kps
                  </div>
                </td>
              </tr>
              </tbody>
            </table>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12" class="card-box">
        <el-card>
          <template #header>
            <div><span><i class="el-icon-pie-chart"></i> 命令统计</span></div>
          </template>
          <div class="el-table el-table--enable-row-hover el-table--medium">
            <div ref="commandstats" style="height: 420px"/>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12" class="card-box">
        <el-card>
          <template #header>
            <div><span><i class="el-icon-odometer"></i> 内存信息</span></div>
          </template>
          <div class="el-table el-table--enable-row-hover el-table--medium">
            <div ref="usedmemory" style="height: 420px"/>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance, onMounted } from 'vue'
import { getCache } from '@/api/monitor/cache'
import * as echarts from 'echarts'

defineOptions({ name: 'Cache' })

const { proxy } = getCurrentInstance()

// 命令统计图表容器
const commandstats = ref(null)
// 使用内存图表容器
const usedmemory = ref(null)
// cache信息
const cache = ref([])

let commandstatsChart = null
let usedmemoryChart = null

/** 查缓存询信息 */
function getList() {
  getCache().then((response) => {
    cache.value = response.data
    proxy.$modal.closeLoading()

    commandstatsChart = echarts.init(commandstats.value, 'macarons')
    commandstatsChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b} : {c} ({d}%)'
      },
      series: [
        {
          name: '命令',
          type: 'pie',
          roseType: 'radius',
          radius: [15, 95],
          center: ['50%', '38%'],
          data: response.data.commandStats,
          animationEasing: 'cubicInOut',
          animationDuration: 1000
        }
      ]
    })
    usedmemoryChart = echarts.init(usedmemory.value, 'macarons')
    usedmemoryChart.setOption({
      tooltip: {
        formatter: '{b} <br/>{a} : ' + cache.value.info.used_memory_human
      },
      series: [
        {
          name: '峰值',
          type: 'gauge',
          min: 0,
          max: 1000,
          detail: {
            formatter: cache.value.info.used_memory_human
          },
          data: [
            {
              value: parseFloat(cache.value.info.used_memory_human),
              name: '内存消耗'
            }
          ]
        }
      ]
    })
    window.addEventListener('resize', () => {
      commandstatsChart.resize()
      usedmemoryChart.resize()
    })
  })
}

// 打开加载层
function openLoading() {
  proxy.$modal.loading('正在加载缓存监控数据，请稍候！')
}

onMounted(() => {
  getList()
  openLoading()
})
</script>
