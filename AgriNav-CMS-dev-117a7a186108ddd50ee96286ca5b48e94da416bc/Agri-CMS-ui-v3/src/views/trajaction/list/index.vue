<template>
  <div class="trajectory-list-container">
    <!-- 添加条件查询表单 -->
    <el-form :inline="true" :model="queryForm" class="query-form">
      <el-form-item label="日期">
        <el-date-picker v-model="queryForm.localDate" type="date" placeholder="选择日期"></el-date-picker>
      </el-form-item>
      <el-form-item label="开始时间">
        <el-time-picker v-model="queryForm.beginTime" placeholder="选择开始时间"></el-time-picker>
      </el-form-item>
      <el-form-item label="结束时间">
        <el-time-picker v-model="queryForm.endTime" placeholder="选择结束时间"></el-time-picker>
      </el-form-item>
      <el-form-item label="作业类型">
        <el-select
          v-model="queryForm.workType"
          filterable
          remote
          reserve-keyword
          placeholder="请输入关键词"
          :remote-method="remoteSearch"
          :loading="loading">
          <el-option
            v-for="item in workTypes"
            :key="item.id"
            :label="item.workName"
            :value="item.id">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="作业地址">
        <el-input v-model="queryForm.address" placeholder="输入作业地址"></el-input>
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="queryForm.phone" placeholder="输入手机号"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="trajectories" style="width: 100%" v-loading="loading">
      <el-table-column prop="phoneNumber" label="电话号码" width="180"></el-table-column>
      <el-table-column prop="workType" label="工作类型" width="180"></el-table-column>
      <el-table-column prop="crop" label="作物" width="180"></el-table-column>
      <el-table-column prop="location" label="位置" width="180"></el-table-column>
      <el-table-column prop="area" label="面积(亩)" width="100">
        <template #default="scope">
          {{ scope.row.area ? scope.row.area.toFixed(2) : '0.00' }}
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="200">
        <template #default="scope">
          {{ formatDate(scope.row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="endTime" label="结束时间" width="200">
        <template #default="scope">
          {{ formatDate(scope.row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button
            size="small"
            type="primary"
            @click="viewTrajectory(scope.row)"
            :disabled="!scope.row || !scope.row.trajectoryInfoId"
          >
            查看
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="no-data-message" v-if="trajectories.length === 0 && !loading">
      <p>暂无轨迹数据</p>
    </div>

    <!-- 将分页组件移到这里,并移除条件判断 -->
    <div class="pagination-container">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-sizes="[6, 12, 18, 24]"
        :page-size="pageSize"
        v-model:current-page="currentPage"
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
      ></el-pagination>
    </div>

    <el-dialog
      title="有效轨迹详情"
      v-model="dialogVisible"
      width="80%"
      center
    >
      <VehicleTrajectory
        v-if="selectedTrajectory && selectedTrajectory.trajectoryInfoId"
        :trajectory="selectedTrajectory"
        :trajectoryInfoId="selectedTrajectory.trajectoryInfoId"
        :key="selectedTrajectory.trajectoryInfoId"
      />
      <div v-else>
        <p>无法显示轨迹，数据无效</p>
      </div>
    </el-dialog>

    <el-dialog
      title="原始轨迹"
      v-model="showOriginalDialog"
      width="80%"
      center
      @opened="handleDialogOpened"
    >
      <VehicleTrajectoryOriginal
        :trajectoryInfoId="selectedTrajectory.trajectoryInfoId"
        v-if="showOriginalDialog"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import VehicleTrajectory from '../map/index.vue'
import VehicleTrajectoryOriginal from '../map/index_ori.vue'

defineOptions({ name: 'TrajectoryList' })

const trajectories = ref([])
const dialogVisible = ref(false)
const selectedTrajectory = ref(null)
const showOriginalDialog = ref(false)
const total = ref(0)
const pageSize = ref(6)
const currentPage = ref(1)
const loading = ref(false)
const queryForm = ref({
  localDate: '',
  beginTime: '',
  endTime: '',
  workType: '',
  address: '',
  phone: ''
})
const workTypes = ref([]) // 用于存储作业类型选项
const allWorkTypes = ref([]) // 存储所有工作类型

function loadTrajectories() {
  loading.value = true

  // 格式化日期和时间
  const formattedQuery = {
    ...queryForm.value,
    localDate: formatDate(queryForm.value.localDate),
    beginTime: formatTime(queryForm.value.beginTime),
    endTime: formatTime(queryForm.value.endTime)
  }

  request({
    url: '/effectiveTrajectory/conditions',
    method: 'post',
    data: formattedQuery,
    params: {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
  }).then(response => {
    if (response.code === 200) {
      if (response.msg === '没查询到数据') {
        trajectories.value = []
        total.value = 0
        ElMessage.info('暂无轨迹数据')
      } else {
        trajectories.value = Array.isArray(response.data) ? response.data : []
        total.value = response.total || 0
      }
    } else {
      ElMessage.error(`获取轨迹数据失败: ${response.msg || '未知错误'}`)
      trajectories.value = []
      total.value = 0
    }
  }).catch(error => {
    console.error('获取轨迹数据失败', error)
    ElMessage.error('获取轨迹数据失败，请检查网络连接')
    trajectories.value = []
    total.value = 0
  }).finally(() => {
    loading.value = false
  })
}

function handleCurrentChange(val) {
  currentPage.value = val
  loadTrajectories()
}

function handleSizeChange(val) {
  pageSize.value = val
  currentPage.value = 1
  loadTrajectories()
}

function handleQuery() {
  currentPage.value = 1
  loadTrajectories()
}

function resetQuery() {
  queryForm.value = {
    localDate: '',
    beginTime: '',
    endTime: '',
    workType: '',
    address: '',
    phone: ''
  }
  currentPage.value = 1
  loadTrajectories()
}

function viewTrajectory(row) {
  if (row && row.trajectoryInfoId) {
    selectedTrajectory.value = {
      ...row,
      id: row.trajectoryInfoId,
      points: row.points // 确保传递 points 数据
    }
    dialogVisible.value = true
  } else {
    ElMessage.error('无法显示轨迹，数据无效')
  }
}

function formatDate(date) {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}

function formatTime(time) {
  if (!time) return ''
  if (typeof time === 'string') {
    // 如果是字符串，假设格式为 "HH:mm" 或 "HH:mm:ss"
    return time.substring(0, 5)
  } else if (time instanceof Date) {
    // 如果是 Date 对象
    return `${String(time.getHours()).padStart(2, '0')}:${String(time.getMinutes()).padStart(2, '0')}`
  } else {
    // 如果是其他类型，返回空字符串或者适当的默认值
    console.warn('Unexpected time format:', time)
    return ''
  }
}

function getWorkTypes() {
  loading.value = true
  request({
    url: '/fieldWork/list',
    method: 'get'
  }).then(response => {
    if (response.code === 200) {
      // 对工作类型进行去重
      const uniqueWorkTypes = removeDuplicates(response.data, 'workName')
      allWorkTypes.value = uniqueWorkTypes
      workTypes.value = uniqueWorkTypes.slice(0, 20) // 初始只显示前20个选项
    } else {
      ElMessage.error('获取作业类型失败')
    }
  }).catch(error => {
    console.error('获取作业类型失败', error)
    ElMessage.error('获取作业类型失败，请检查网络连接')
  }).finally(() => {
    loading.value = false
  })
}

function removeDuplicates(array, key) {
  return Array.from(new Map(array.map(item => [item[key], item])).values())
}

function remoteSearch(query) {
  if (query !== '') {
    loading.value = true
    setTimeout(() => {
      loading.value = false
      workTypes.value = allWorkTypes.value.filter(item => {
        return item.workName.toLowerCase().includes(query.toLowerCase())
      })
    }, 200)
  } else {
    workTypes.value = allWorkTypes.value.slice(0, 20)
  }
}

function handleDialogOpened() {
  window.dispatchEvent(new Event('resize'))
}

onMounted(() => {
  loadTrajectories()
  getWorkTypes() // 在组件创建时获取作业类型
})
</script>

<style scoped>
.trajectory-list-container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 84px); /* 假设顶部导航栏高度为84px,根据实际情况调整 */
}

.query-form {
  margin-bottom: 20px;
}

.no-data-message {
  text-align: center;
  margin: 20px 0;
}

.pagination-container {
  margin-top: auto;
  padding-top: 20px;
}
</style>
