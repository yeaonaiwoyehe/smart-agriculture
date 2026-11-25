<template>
  <div class="app-container">
    <!-- 搜索 -->
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="复核单号" prop="reviewNo">
        <el-input v-model="queryParams.reviewNo" placeholder="请输入复核单号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="手机号" prop="phoneNumber">
        <el-input v-model="queryParams.phoneNumber" placeholder="请输入机手手机号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width:130px">
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
        <el-button type="primary" plain icon="el-icon-plus" size="small" @click="openSubmit">模拟机手提交复核</el-button>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="复核单号" prop="reviewNo" align="center" width="170" />
      <el-table-column label="机手手机号" prop="phoneNumber" align="center" width="120" />
      <el-table-column label="地块" prop="plotId" align="center" />
      <el-table-column label="系统面积(亩)" prop="systemArea" align="center" />
      <el-table-column label="主张面积(亩)" prop="claimedArea" align="center" />
      <el-table-column label="状态" prop="status" align="center">
        <template #default="scope">
          <el-tag :type="statusTag(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200">
        <template #default="scope">
          <el-button size="small" link icon="el-icon-view" @click="openDetail(scope.row)">详情</el-button>
          <el-button v-if="scope.row.status===0" size="small" link icon="el-icon-s-check" @click="openReview(scope.row,'initial')">初核</el-button>
          <el-button v-if="scope.row.status===1" size="small" link icon="el-icon-finished" @click="openReview(scope.row,'final')">终审</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 提交复核弹窗 -->
    <el-dialog title="提交复核申请" v-model="submitOpen" width="520px" append-to-body>
      <el-form ref="submitFormRef" :model="submitForm" :rules="submitRules" label-width="110px">
        <el-form-item label="作业单" prop="trajectoryInfoId">
          <el-select v-model="submitForm.trajectoryInfoId" filterable placeholder="请选择作业单" style="width:100%" @change="onPickWork">
            <el-option v-for="w in workOptions" :key="w.trajectoryInfoId" :label="workLabel(w)" :value="w.trajectoryInfoId" />
          </el-select>
        </el-form-item>
        <el-form-item label="系统面积(亩)">
          <el-input v-model="submitForm.systemArea" disabled />
        </el-form-item>
        <el-form-item label="主张面积(亩)" prop="claimedArea">
          <el-input-number v-model="submitForm.claimedArea" :min="0" :precision="2" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="异议原因" prop="reason">
          <el-input v-model="submitForm.reason" type="textarea" :rows="3" placeholder="如：田埂重复计算、边缘漏算" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitOpen=false">取 消</el-button>
        <el-button type="primary" @click="doSubmit">提 交</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog :title="reviewTitle" v-model="reviewOpen" width="480px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="复核结论">
          <el-radio-group v-model="reviewForm.pass">
            <el-radio :label="true">通过</el-radio>
            <el-radio :label="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="reviewForm.comment" type="textarea" :rows="3" placeholder="请输入审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewOpen=false">取 消</el-button>
        <el-button type="primary" @click="doReview">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog title="复核单详情" v-model="detailOpen" width="640px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="复核单号">{{ detail.reviewNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="机手手机号">{{ detail.phoneNumber }}</el-descriptions-item>
        <el-descriptions-item label="地块">{{ detail.plotId }}</el-descriptions-item>
        <el-descriptions-item label="系统面积(亩)">{{ detail.systemArea }}</el-descriptions-item>
        <el-descriptions-item label="主张面积(亩)">{{ detail.claimedArea }}</el-descriptions-item>
        <el-descriptions-item label="异议原因" :span="2">{{ detail.reason }}</el-descriptions-item>
        <el-descriptions-item label="初核人">{{ detail.initialReviewer || '-' }}</el-descriptions-item>
        <el-descriptions-item label="初核意见">{{ detail.initialComment || '-' }}</el-descriptions-item>
        <el-descriptions-item label="初核时间">{{ detail.initialTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="终审人">{{ detail.finalReviewer || '-' }}</el-descriptions-item>
        <el-descriptions-item label="终审意见">{{ detail.finalComment || '-' }}</el-descriptions-item>
        <el-descriptions-item label="终审时间">{{ detail.finalTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import request from '@/utils/request'
import { listReviewRequest, submitReviewRequest, initialReview, finalReview } from '@/api/closure/reviewRequest'

defineOptions({
  name: 'ReviewRequest'
})

const { proxy } = getCurrentInstance()

const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const dataList = ref([])
const statusOptions = ref([
  { value: 0, label: '待初核' }, { value: 1, label: '初核通过' }, { value: 2, label: '初核驳回' },
  { value: 3, label: '终审通过' }, { value: 4, label: '终审驳回' }
])
const queryParams = ref({ pageNum: 1, pageSize: 10, reviewNo: undefined, phoneNumber: undefined, status: undefined })
const submitOpen = ref(false)
const submitForm = ref({})
const workOptions = ref([])
const submitRules = {
  trajectoryInfoId: [{ required: true, message: '请选择作业单', trigger: 'change' }],
  claimedArea: [{ required: true, message: '请输入主张面积', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入异议原因', trigger: 'blur' }]
}
const reviewOpen = ref(false)
const reviewTitle = ref('')
const reviewForm = ref({ id: undefined, stage: '', pass: true, comment: '' })
const detailOpen = ref(false)
const detail = ref({})
const queryFormRef = ref(null)
const submitFormRef = ref(null)

function getList() {
  loading.value = true
  listReviewRequest(queryParams.value).then(res => {
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
  return { 0: 'warning', 1: 'primary', 2: 'danger', 3: 'success', 4: 'info' }[v]
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryFormRef')
  handleQuery()
}

function workLabel(w) {
  return w.trajectoryInfoId.substring(0, 8) + '…（' + w.workType + '，' + w.area + '亩）'
}

function openSubmit() {
  submitForm.value = { trajectoryInfoId: undefined, systemArea: undefined, claimedArea: undefined, reason: undefined }
  proxy.resetForm('submitFormRef')
  submitOpen.value = true
  request({
    url: '/effectiveTrajectory/conditions',
    method: 'post',
    data: {},
    params: { pageNum: 1, pageSize: 100 }
  }).then(res => {
    workOptions.value = Array.isArray(res.data) ? res.data : []
  })
}

function onPickWork(id) {
  const w = workOptions.value.find(i => i.trajectoryInfoId === id)
  if (w) {
    submitForm.value.systemArea = w.area
  }
}

function doSubmit() {
  submitFormRef.value.validate(valid => {
    if (!valid) return
    submitReviewRequest(submitForm.value).then(() => {
      proxy.$modal.msgSuccess('复核申请已提交')
      submitOpen.value = false
      getList()
    })
  })
}

function openReview(row, stage) {
  reviewForm.value = { id: row.id, stage: stage, pass: true, comment: '' }
  reviewTitle.value = stage === 'initial' ? '队长初核' : '管理员终审'
  reviewOpen.value = true
}

function doReview() {
  const f = reviewForm.value
  const action = f.stage === 'initial' ? initialReview : finalReview
  action(f.id, f.pass, f.comment).then(() => {
    proxy.$modal.msgSuccess('审核完成')
    reviewOpen.value = false
    getList()
  })
}

function openDetail(row) {
  detail.value = row
  detailOpen.value = true
}

onMounted(() => {
  getList()
})
</script>
