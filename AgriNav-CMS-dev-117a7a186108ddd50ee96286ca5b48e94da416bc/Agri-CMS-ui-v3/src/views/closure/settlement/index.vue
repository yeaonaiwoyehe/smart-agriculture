<template>
  <div class="app-container">
    <!-- 搜索 -->
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="结算单号" prop="settlementNo">
        <el-input v-model="queryParams.settlementNo" placeholder="请输入结算单号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="手机号" prop="phoneNumber">
        <el-input v-model="queryParams.phoneNumber" placeholder="请输入机手手机号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="周期" prop="periodType">
        <el-select v-model="queryParams.periodType" placeholder="请选择" clearable style="width:110px">
          <el-option label="月度" :value="1" />
          <el-option label="季度" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width:110px">
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="small" @click="openGenerate">生成结算单</el-button>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="结算单号" prop="settlementNo" align="center" width="170" />
      <el-table-column label="周期" prop="periodType" align="center">
        <template #default="scope">{{ scope.row.periodType === 1 ? '月度' : '季度' }}</template>
      </el-table-column>
      <el-table-column label="周期开始" prop="periodStart" align="center" />
      <el-table-column label="周期结束" prop="periodEnd" align="center" />
      <el-table-column label="机手手机号" prop="phoneNumber" align="center" width="120" />
      <el-table-column label="总亩数" prop="totalArea" align="center" />
      <el-table-column label="总金额(元)" prop="totalAmount" align="center" />
      <el-table-column label="状态" prop="status" align="center">
        <template #default="scope">
          <el-tag :type="statusTag(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="270">
        <template #default="scope">
          <el-button size="small" link icon="el-icon-view" @click="openItems(scope.row)">明细</el-button>
          <el-button v-if="scope.row.status===0" size="small" link icon="el-icon-check" @click="changeStatus(scope.row,1)">确认</el-button>
          <el-button v-if="scope.row.status===1" size="small" link icon="el-icon-money" @click="changeStatus(scope.row,2)">标记付款</el-button>
          <el-button size="small" link icon="el-icon-download" @click="doExport(scope.row)">导出</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 生成弹窗 -->
    <el-dialog title="生成结算单" v-model="generateOpen" width="520px" append-to-body>
      <el-form ref="genFormRef" :model="genForm" :rules="genRules" label-width="110px">
        <el-form-item label="机手手机号" prop="phoneNumber">
          <el-input v-model="genForm.phoneNumber" placeholder="请输入机手手机号" />
        </el-form-item>
        <el-form-item label="周期类型" prop="periodType">
          <el-radio-group v-model="genForm.periodType">
            <el-radio :label="1">月度</el-radio>
            <el-radio :label="2">季度</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="周期开始" prop="periodStart">
          <el-date-picker v-model="genForm.periodStart" type="date" value-format="yyyy-MM-dd" placeholder="选择开始日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="周期结束" prop="periodEnd">
          <el-date-picker v-model="genForm.periodEnd" type="date" value-format="yyyy-MM-dd" placeholder="选择结束日期" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateOpen=false">取 消</el-button>
        <el-button type="primary" @click="doGenerate">生 成</el-button>
      </template>
    </el-dialog>

    <!-- 明细弹窗 -->
    <el-dialog title="结算明细" v-model="itemsOpen" width="780px" append-to-body>
      <el-table :data="items" border size="small">
        <el-table-column label="作业单" prop="trajectoryInfoId" align="center">
          <template #default="scope">{{ scope.row.trajectoryInfoId.substring(0,8) }}…</template>
        </el-table-column>
        <el-table-column label="地块" prop="plotId" align="center" />
        <el-table-column label="作业类型" prop="workType" align="center" />
        <el-table-column label="面积(亩)" prop="area" align="center" />
        <el-table-column label="单价(元/亩)" prop="unitPrice" align="center" />
        <el-table-column label="金额(元)" prop="amount" align="center" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import { listSettlement, generateSettlement, getSettlementItems, updateSettlementStatus, exportSettlement } from '@/api/closure/settlement'

defineOptions({
  name: 'Settlement'
})

const { proxy } = getCurrentInstance()

const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const dataList = ref([])
const statusOptions = ref([
  { value: 0, label: '已生成' }, { value: 1, label: '已确认' }, { value: 2, label: '已付款' }
])
const queryParams = ref({ pageNum: 1, pageSize: 10, settlementNo: undefined, phoneNumber: undefined, periodType: undefined, status: undefined })
const generateOpen = ref(false)
const genForm = ref({ phoneNumber: '15888888888', periodType: 1, periodStart: undefined, periodEnd: undefined })
const genRules = {
  phoneNumber: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  periodType: [{ required: true, message: '请选择周期类型', trigger: 'change' }],
  periodStart: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  periodEnd: [{ required: true, message: '请选择结束日期', trigger: 'change' }]
}
const itemsOpen = ref(false)
const items = ref([])
const queryFormRef = ref(null)
const genFormRef = ref(null)

function getList() {
  loading.value = true
  listSettlement(queryParams.value).then(res => {
    dataList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function statusLabel(v) {
  const s = statusOptions.value.find(i => i.value === v)
  return s ? s.label : v
}

function statusTag(v) {
  return { 0: 'warning', 1: 'primary', 2: 'success' }[v]
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryFormRef')
  handleQuery()
}

function openGenerate() {
  genForm.value = { phoneNumber: '15888888888', periodType: 1, periodStart: undefined, periodEnd: undefined }
  proxy.resetForm('genFormRef')
  generateOpen.value = true
}

function doGenerate() {
  genFormRef.value.validate(valid => {
    if (!valid) return
    generateSettlement(genForm.value).then(res => {
      proxy.$modal.msgSuccess('结算单已生成，总金额 ' + res.data.totalAmount + ' 元')
      generateOpen.value = false
      getList()
    })
  })
}

function openItems(row) {
  getSettlementItems(row.id).then(res => {
    items.value = res.data
    itemsOpen.value = true
  })
}

function changeStatus(row, status) {
  updateSettlementStatus(row.id, status).then(() => {
    proxy.$modal.msgSuccess('状态已更新')
    getList()
  })
}

function doExport(row) {
  exportSettlement(row.id).then(blob => {
    const url = window.URL.createObjectURL(new Blob([blob]))
    const link = document.createElement('a')
    link.href = url
    link.download = row.settlementNo + '.xls'
    link.click()
    window.URL.revokeObjectURL(url)
  })
}

onMounted(() => {
  getList()
})
</script>
