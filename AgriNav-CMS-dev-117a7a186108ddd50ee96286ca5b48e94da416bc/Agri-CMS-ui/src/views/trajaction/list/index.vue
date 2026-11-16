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
        <template slot-scope="scope">
          {{ scope.row.area ? scope.row.area.toFixed(2) : '0.00' }}
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="200">
        <template slot-scope="scope">
          {{ formatDate(scope.row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="endTime" label="结束时间" width="200">
        <template slot-scope="scope">
          {{ formatDate(scope.row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template slot-scope="scope">
          <el-button
            size="mini"
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
        :current-page.sync="currentPage"
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
      ></el-pagination>
    </div>

    <el-dialog
      title="有效轨迹详情"
      :visible.sync="dialogVisible"
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
      :visible.sync="showOriginalDialog"
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

<script>
import request from '@/utils/request'
import VehicleTrajectory from '../map/index.vue'
import VehicleTrajectoryOriginal from '../map/index_ori.vue'

export default {
  name: 'TrajectoryList',
  components: {
    VehicleTrajectory,
    VehicleTrajectoryOriginal
  },
  data() {
    return {
      trajectories: [],
      dialogVisible: false,
      selectedTrajectory: null,
      showOriginalDialog: false,
      total: 0,
      pageSize: 6,
      currentPage: 1,
      loading: false,
      queryForm: {
        localDate: '',
        beginTime: '',
        endTime: '',
        workType: '',
        address: '',
        phone: ''
      },
      workTypes: [], // 用于存储作业类型选项
      allWorkTypes: [], // 存储所有工作类型
    }
  },
  created() {
    this.loadTrajectories()
    this.getWorkTypes() // 在组件创建时获取作业类型
  },
  methods: {
    loadTrajectories() {
      this.loading = true;

      // 格式化日期和时间
      const formattedQuery = {
        ...this.queryForm,
        localDate: this.formatDate(this.queryForm.localDate),
        beginTime: this.formatTime(this.queryForm.beginTime),
        endTime: this.formatTime(this.queryForm.endTime)
      };

      request({
        url: '/effectiveTrajectory/conditions',
        method: 'post',
        data: formattedQuery,
        params: {
          pageNum: this.currentPage,
          pageSize: this.pageSize
        }
      }).then(response => {
        if (response.code === 200) {
          if (response.msg === "没查询到数据") {
            this.trajectories = [];
            this.total = 0;
            this.$message.info('暂无轨迹数据');
          } else {
            this.trajectories = Array.isArray(response.data) ? response.data : [];
            this.total = response.total || 0;
          }
        } else {
          this.$message.error(`获取轨迹数据失败: ${response.msg || '未知错误'}`);
          this.trajectories = [];
          this.total = 0;
        }
      }).catch(error => {
        console.error('获取轨迹数据失败', error);
        this.$message.error('获取轨迹数据失败，请检查网络连接');
        this.trajectories = [];
        this.total = 0;
      }).finally(() => {
        this.loading = false;
      });
    },
    handleCurrentChange(val) {
      this.currentPage = val;
      this.loadTrajectories();
    },
    handleSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
      this.loadTrajectories();
    },
    handleQuery() {
      this.currentPage = 1;
      this.loadTrajectories();
    },
    resetQuery() {
      this.queryForm = {
        localDate: '',
        beginTime: '',
        endTime: '',
        workType: '',
        address: '',
        phone: ''
      };
      this.currentPage = 1;
      this.loadTrajectories();
    },
    viewTrajectory(row) {
      if (row && row.trajectoryInfoId) {
        this.selectedTrajectory = {
          ...row,
          id: row.trajectoryInfoId,
          points: row.points // 确保传递 points 数据
        };
        this.dialogVisible = true;
      } else {
        this.$message.error('无法显示轨迹，数据无效');
      }
    },
    formatDate(date) {
      if (!date) return '';
      const d = new Date(date);
      return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`;
    },
    formatTime(time) {
      if (!time) return '';
      if (typeof time === 'string') {
        // 如果是字符串，假设格式为 "HH:mm" 或 "HH:mm:ss"
        return time.substring(0, 5);
      } else if (time instanceof Date) {
        // 如果是 Date 对象
        return `${String(time.getHours()).padStart(2, '0')}:${String(time.getMinutes()).padStart(2, '0')}`;
      } else {
        // 如果是其他类型，返回空字符串或者适当的默认值
        console.warn('Unexpected time format:', time);
        return '';
      }
    },
    getWorkTypes() {
      this.loading = true
      request({
        url: '/fieldWork/list',
        method: 'get'
      }).then(response => {
        if (response.code === 200) {
          // 对工作类型进行去重
          const uniqueWorkTypes = this.removeDuplicates(response.data, 'workName')
          this.allWorkTypes = uniqueWorkTypes
          this.workTypes = uniqueWorkTypes.slice(0, 20) // 初始只显示前20个选项
        } else {
          this.$message.error('获取作业类型失败')
        }
      }).catch(error => {
        console.error('获取作业类型失败', error)
        this.$message.error('获取作业类型失败，请检查网络连接')
      }).finally(() => {
        this.loading = false
      })
    },
    removeDuplicates(array, key) {
      return Array.from(new Map(array.map(item => [item[key], item])).values())
    },
    remoteSearch(query) {
      if (query !== '') {
        this.loading = true
        setTimeout(() => {
          this.loading = false
          this.workTypes = this.allWorkTypes.filter(item => {
            return item.workName.toLowerCase().includes(query.toLowerCase())
          })
        }, 200)
      } else {
        this.workTypes = this.allWorkTypes.slice(0, 20)
      }
    },
    handleDialogOpened() {
      window.dispatchEvent(new Event('resize'))
    }
  }
}
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
