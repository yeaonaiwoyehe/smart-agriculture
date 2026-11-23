<template>
  <div class="app-container">
    <!-- 搜索 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="70px">
      <el-form-item label="用户" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="登录账号" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width:140px">
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="端" prop="side">
        <el-select v-model="queryParams.side" placeholder="请选择" clearable style="width:120px">
          <el-option label="农机端" value="ag" />
          <el-option label="管理端" value="ma" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
          end-placeholder="结束日期" value-format="yyyy-MM-dd" style="width:240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="时间" prop="createTime" align="center" width="160" />
      <el-table-column label="用户" prop="userName" align="center" width="120" />
      <el-table-column label="指令原文" prop="message" align="left" :show-overflow-tooltip="true" min-width="200" />
      <el-table-column label="调用工具" prop="toolName" align="center" width="130">
        <template slot-scope="scope">{{ scope.row.toolName || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" align="center" width="120">
        <template slot-scope="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="回执编号" prop="receiptNo" align="center" width="180">
        <template slot-scope="scope">{{ scope.row.receiptNo || '—' }}</template>
      </el-table-column>
      <el-table-column label="耗时(ms)" prop="costMs" align="center" width="90" />
      <el-table-column label="操作" align="center" width="90" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 详情弹窗 -->
    <el-dialog title="审计记录详情" :visible.sync="open" width="640px" append-to-body>
      <el-descriptions :column="1" border size="medium">
        <el-descriptions-item label="记录编号">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ detail.userName }}（{{ detail.nickName }}）</el-descriptions-item>
        <el-descriptions-item label="所属端">{{ detail.side || '—' }}</el-descriptions-item>
        <el-descriptions-item label="指令原文">{{ detail.message }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="调用工具">{{ detail.toolName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="工具参数">
          <pre class="json-box">{{ detail.toolArguments || '—' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="工具结果/摘要">{{ detail.toolResult || '—' }}</el-descriptions-item>
        <el-descriptions-item label="回执编号">{{ detail.receiptNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="智能体回复">
          <pre class="json-box">{{ detail.reply || '—' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息">{{ detail.errorMsg || '—' }}</el-descriptions-item>
        <el-descriptions-item label="位置">
          {{ detail.longitude != null ? detail.longitude + ', ' + detail.latitude : '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.costMs }} ms</el-descriptions-item>
        <el-descriptions-item label="记录时间">{{ detail.createTime }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer">
        <el-button type="primary" @click="open=false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAudit, getAudit } from '@/api/ai/audit'

export default {
  name: 'AgentAudit',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      dataList: [],
      open: false,
      detail: {},
      dateRange: [],
      statusOptions: [
        { value: 'REPLY', label: '直接回复' },
        { value: 'NEED_CONFIRM', label: '待确认' },
        { value: 'NEED_PARAMS', label: '缺参' },
        { value: 'FAILED', label: '失败' }
      ],
      queryParams: { pageNum: 1, pageSize: 10, userName: undefined, status: undefined, side: undefined }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      const params = { ...this.queryParams }
      if (this.dateRange && this.dateRange.length === 2) {
        params.beginTime = this.dateRange[0]
        params.endTime = this.dateRange[1]
      }
      listAudit(params).then(res => {
        this.dataList = res.rows
        this.total = res.total
        this.loading = false
      })
    },
    handleDetail(row) {
      getAudit(row.id).then(res => {
        this.detail = res.data
        this.open = true
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.dateRange = []
      this.resetForm('queryForm')
      this.handleQuery()
    },
    statusLabel(val) {
      const item = this.statusOptions.find(s => s.value === val)
      return item ? item.label : val
    },
    statusTagType(val) {
      switch (val) {
        case 'REPLY': return 'success'
        case 'NEED_CONFIRM': return 'warning'
        case 'NEED_PARAMS': return 'info'
        case 'FAILED': return 'danger'
        default: return 'info'
      }
    }
  }
}
</script>

<style scoped>
.json-box {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: Consolas, Menlo, monospace;
  font-size: 12px;
  max-height: 220px;
  overflow-y: auto;
}
</style>
