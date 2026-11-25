<template>
  <div class="app-container">
    <div class="content-wrapper">
      <div class="main-content">
        <!-- Search Form -->
        <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="用户名称" prop="username">
            <el-input
              v-model="queryParams.username"
              placeholder="请输入用户名称"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="手机号码" prop="phoneNumber">
            <el-input
              v-model="queryParams.phoneNumber"
              placeholder="请输入手机号码"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="反馈内容" prop="content">
            <el-input
              v-model="queryParams.content"
              placeholder="请输入反馈内容"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="处理状态" prop="isChecked">
            <el-select v-model="queryParams.isChecked" placeholder="请选择处理状态" clearable>
              <el-option label="未处理" :value="0" />
              <el-option label="已处理" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="提交时间">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="yyyy-MM-dd HH:mm:ss"
            />
          </el-form-item>
          <el-form-item label="确认时间">
            <el-date-picker
              v-model="checkedDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="yyyy-MM-dd HH:mm:ss"
            />
          </el-form-item>
          <el-form-item label="反馈标签" prop="labels">
            <el-select
              v-model="queryParams.labels"
              multiple
              collapse-tags
              placeholder="请选择反馈标签"
              clearable
              @change="handleLabelChange">
              <el-option
                v-for="item in feedbackTypes"
                :key="item.dictCode"
                :label="item.dictLabel"
                :value="item.dictCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="small" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="small" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <!-- Table -->
        <el-table v-loading="loading" :data="feedbackList">
          <el-table-column label="用户名称" align="center" prop="username" />
          <el-table-column label="手机号码" align="center" prop="phoneNumber" />
          <el-table-column label="反馈内容" align="center" prop="content" :show-overflow-tooltip="true" />
          <el-table-column label="反馈标签" align="center">
            <template #default="scope">
              <el-tag
                v-for="label in scope.row.labels"
                :key="label.dictCode"
                size="small"
                style="margin-right: 5px"
              >
                {{ label.dictLabel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" align="center" prop="commitTime" width="180">
            <template #default="scope">
              <span>{{ formatTime(scope.row.commitTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="处理状态" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.isChecked === 1 ? 'success' : 'warning'">
                {{ scope.row.isChecked === 1 ? '已处理' : '未处理' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template #default="scope">
              <el-button
                size="small"
                link
                icon="el-icon-view"
                @click="handleView(scope.row)"
              >查看</el-button>
              <el-button
                v-if="scope.row.isChecked === 0"
                size="small"
                link
                icon="el-icon-check"
                @click="handleCheck(scope.row)"
              >处理</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Pagination -->
      <div class="pagination-container">
        <pagination
          v-show="total>0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </div>
    </div>

    <!-- View Dialog -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名称">{{ form.username }}</el-descriptions-item>
        <el-descriptions-item label="手机号码">{{ form.phoneNumber }}</el-descriptions-item>
        <el-descriptions-item label="反馈内容" :span="2">{{ form.content }}</el-descriptions-item>
        <el-descriptions-item label="反馈标签" :span="2">
          <el-tag
            v-for="label in form.labels"
            :key="label.dictCode"
            size="small"
            style="margin-right: 5px"
          >{{ label.dictLabel }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatTime(form.commitTime) }}</el-descriptions-item>
        <el-descriptions-item label="处理状态">
          <el-tag :type="form.isChecked === 1 ? 'success' : 'warning'">
            {{ form.isChecked === 1 ? '已处理' : '未处理' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="确认时间" :span="2">
          {{ form.isChecked === 1 ? formatTime(form.checkedTime) : '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- Reply Dialog -->
    <el-dialog
      title="反馈处理"
      v-model="replyDialogVisible"
      width="500px"
      append-to-body>
      <el-form :model="replyForm" label-width="80px">
        <el-form-item label="回复内容" prop="reply">
          <el-input
            v-model="replyForm.reply"
            type="textarea"
            :rows="4"
            placeholder="请输入回复内容">
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="replyDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitReply">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listFeedback, getFeedbackTypes, checkFeedback } from '@/api/feedback/feedback'

defineOptions({
  name: 'Feedback'
})

const { proxy } = getCurrentInstance()

// 遮罩层
const loading = ref(true)
// 显示搜索条件
const showSearch = ref(true)
// 总条数
const total = ref(0)
// 反馈表格数
const feedbackList = ref([])
// 弹出层标题
const title = ref('')
// 是否显示弹出层
const open = ref(false)
// 日期范围
const dateRange = ref([])
// 确认时间范围
const checkedDateRange = ref([])
// 查询参数
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  username: undefined,
  phoneNumber: undefined,
  content: undefined,
  isChecked: undefined,
  labels: [],
  startCommitTime: undefined,
  endCommitTime: undefined,
  startCheckedTime: undefined,
  endCheckedTime: undefined
})
// 表单参数
const form = ref({})
const feedbackTypes = ref([]) // 新增存储反馈类型的数组
const replyDialogVisible = ref(false)
const replyForm = ref({
  id: '',
  phoneNumber: '',
  reply: ''
})
const queryFormRef = ref(null)

/** 查询反馈列表 */
function getList() {
  loading.value = true

  // 创建基础查询参数对象
  const params = {
    pageNum: queryParams.value.pageNum,
    pageSize: queryParams.value.pageSize,
    username: queryParams.value.username,
    phoneNumber: queryParams.value.phoneNumber,
    content: queryParams.value.content,
    isChecked: queryParams.value.isChecked
  }

  // 处理标签
  if (queryParams.value.labels && queryParams.value.labels.length > 0) {
    params.labels = queryParams.value.labels.map(dictCode => ({
      dictCode: dictCode
    }))
  }

  // 处理提交时间范围 (CommitTime)
  if (dateRange.value && dateRange.value.length === 2) {
    params.startCommitTime = dateRange.value[0]
    params.endCommitTime = dateRange.value[1].split(' ')[0] + ' 23:59:59'
  }

  // 处理确认时间范围 (CheckedTime)
  if (checkedDateRange.value && checkedDateRange.value.length === 2) {
    params.startCheckedTime = checkedDateRange.value[0]
    params.endCheckedTime = checkedDateRange.value[1].split(' ')[0] + ' 23:59:59'
  }

  // 移除值为 undefined 或 null 的属性
  Object.keys(params).forEach(key => {
    if (params[key] === undefined || params[key] === null) {
      delete params[key]
    }
  })

  console.log('查询参数:', params)

  listFeedback(params).then(response => {
    feedbackList.value = response.data
    total.value = response.total
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = []
  checkedDateRange.value = []
  proxy.resetForm('queryFormRef')
  queryParams.value.labels = []
  handleQuery()
}

/** 查看按钮操作 */
function handleView(row) {
  form.value = row
  open.value = true
  title.value = '查看反馈详情'
}

// 添加标签变化处理方法
function handleLabelChange(value) {
  console.log('选中的标签:', value)
  queryParams.value.labels = value
}

// 获取字典标签
function getDictLabel(dictCode) {
  const dictList = proxy.dict && proxy.dict.type ? proxy.dict.type.sys_feedback_type : null
  if (!dictList) {
    return ''
  }
  const item = dictList.find(
    dict => dict.dictCode === dictCode || dict.value === dictCode
  )
  return item ? item.dictLabel || item.label : ''
}

// 获取反馈类型列表
function getFeedbackTypeList() {
  getFeedbackTypes().then(response => {
    feedbackTypes.value = response.data
  })
}

/** 处理按钮操作 */
function handleCheck(row) {
  replyForm.value.id = row.id
  replyForm.value.phoneNumber = row.phoneNumber
  replyForm.value.reply = ''
  replyDialogVisible.value = true
}

// Add new method for handling reply submission
function submitReply() {
  if (!replyForm.value.reply) {
    ElMessage.error('请输入回复内容')
    return
  }

  const feedback = {
    id: replyForm.value.id,
    phoneNumber: replyForm.value.phoneNumber
  }

  checkFeedback(feedback, replyForm.value.reply).then(response => {
    proxy.$modal.msgSuccess('处理成功')
    replyDialogVisible.value = false
    getList()
  })
}

// 添加新的时间格式化方法
function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}/${month}/${day} ${hours}:${minutes}:${seconds}`
}

onMounted(() => {
  getFeedbackTypeList()
  getList()
})
</script>

<style scoped>
.app-container {
  height: 100%;
}

.content-wrapper {
  padding: 20px;
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 84px); /* 减去头部和面包屑的高度 */
}

.main-content {
  flex: 1;
  overflow-y: auto;
}

.pagination-container {
  margin-top: auto;
  padding-top: 15px;
  background-color: #fff;
}

/* 在小屏幕设备上调整样式 */
@media screen and (max-width: 768px) {
  .content-wrapper {
    padding: 10px;
  }
}

/* 添加表格容器样式 */
.table-container {
  min-height: calc(100vh - 330px);
  display: flex;
  flex-direction: column;
}

.el-table {
  flex: 1;
  margin-bottom: 10px;
}

.el-tag + .el-tag {
  margin-left: 5px;
}

/* 可以添加一些样式来优化多选框的显示 */
.el-select {
  width: 100%;
}

.el-tag {
  margin-right: 5px;
  margin-bottom: 5px;
}

</style>
