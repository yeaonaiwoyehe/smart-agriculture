<template>
  <div class="app-container">
    <el-button
      type="primary"
      plain
      icon="el-icon-plus"
      size="small"
      @click="handleAdd"
      style="margin-bottom: 15px"
    >新增变更记录</el-button>

    <el-table
      v-loading="loading"
      :data="releaseNoteList"
      style="width: 100%"
      :header-cell-style="{ background: '#F5F7FA' }"
    >
      <el-table-column type="expand">
        <template #default="props">
          <div class="changelog-list">
            <el-table 
              :data="props.row.changeLogs" 
              size="small"
              :header-cell-style="{ background: '#F5F7FA' }"
            >
              <el-table-column label="变更类型" align="center" width="120">
                <template #default="scope">
                  <el-tag :type="getChangeTypeTag(scope.row.type)" size="small">
                    {{ getChangeTypeText(scope.row.type) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="变更内容" prop="content" min-width="300" show-overflow-tooltip />
              <el-table-column label="操作" align="center" width="150">
                <template #default="scope">
                  <el-button size="small" link icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
                  <el-button size="small" link icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!props.row.changeLogs || props.row.changeLogs.length === 0" class="changelog-empty">
              <el-empty description="暂无变更记录"></el-empty>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column
        prop="version"
        label="版本号"
        width="180"
      >
        <template #default="scope">
          {{ scope.row.version }}
          <el-tag size="small" :type="scope.row.status === 'PUBLISHED' ? 'success' : 'info'">
            {{ scope.row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="title"
        label="标题"
        width="200"
      />
      <el-table-column
        prop="description"
        label="描述"
      />
      <el-table-column
        prop="releaseDate"
        label="发布时间"
        width="180"
      >
        <template #default="scope">
          {{ parseTime(scope.row.releaseDate) }}
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="发布记录" prop="releaseNoteId">
          <el-select v-model="form.releaseNoteId" placeholder="请选择发布记录">
            <el-option
              v-for="item in releaseNoteList"
              :key="item.id"
              :label="item.version"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="变更类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择变更类型">
            <el-option label="CMS系统" value="CMS" />
            <el-option label="GPS系统" value="GPS" />
            <el-option label="系统优化" value="OPTIMIZE" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number 
            v-model="form.sortOrder" 
            :min="0" 
            :max="999"
            controls-position="right"
            placeholder="请输入排序号"
          />
        </el-form-item>
        <el-form-item label="变更内容" prop="content">
          <el-input type="textarea" v-model="form.content" placeholder="请输入变更内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import { listReleaseNote } from "@/api/system/releaseNote"
import { listChangeLog, getChangeLog, addChangeLog, updateChangeLog, delChangeLog } from "@/api/system/changeLog"

defineOptions({
  name: 'ChangeLog'
})

const { proxy } = getCurrentInstance()

const loading = ref(true)
const title = ref('')
const open = ref(false)
const releaseNoteList = ref([])
const form = ref({
  id: undefined,
  releaseNoteId: undefined,
  type: undefined,
  content: undefined,
  sortOrder: 0
})
const rules = {
  releaseNoteId: [
    { required: true, message: "发布记录不能为空", trigger: "change" }
  ],
  type: [
    { required: true, message: "变更类型不能为空", trigger: "change" }
  ],
  content: [
    { required: true, message: "变更内容不能为空", trigger: "blur" },
    { max: 500, message: "变更内容长度不能超过500个字符", trigger: "blur" }
  ]
}
const formRef = ref(null)

function getList() {
  loading.value = true
  // 先获取所有发布记录
  listReleaseNote().then(response => {
    releaseNoteList.value = response.data
    // 为每个发布记录获取其变更日志
    const promises = releaseNoteList.value.map(note => {
      return listChangeLog(note.id).then(res => {
        note.changeLogs = res.data
        note.isExpanded = false
      })
    })

    Promise.all(promises).finally(() => {
      loading.value = false
    })
  })
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    id: undefined,
    releaseNoteId: undefined,
    type: undefined,
    content: undefined,
    sortOrder: 0
  }
  proxy.resetForm("formRef")
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加变更日志"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const id = row.id
  getChangeLog(id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改变更日志"
  })
}

/** 提交按钮 */
function submitForm() {
  formRef.value.validate(valid => {
    if (valid) {
      if (form.value.id != undefined) {
        updateChangeLog(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addChangeLog(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const id = row.id
  proxy.$modal.confirm('是否确认删除该变更日志？').then(function () {
    return delChangeLog(id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

function getChangeTypeTag(type) {
  const typeMap = {
    'CMS': 'success',
    'GPS': 'warning',
    'OPTIMIZE': 'info'
  }
  return typeMap[type] || 'info'
}

function getChangeTypeText(type) {
  const typeMap = {
    'CMS': 'CMS系统',
    'GPS': 'GPS系统',
    'OPTIMIZE': '系统优化'
  }
  return typeMap[type] || '其他'
}

function handleRowClick(row) {
  // 切换展开状态
  row.isExpanded = !row.isExpanded
  // 关闭其他展开的行
  releaseNoteList.value.forEach(note => {
    if (note.id !== row.id) {
      note.isExpanded = false
    }
  })
}

function tableRowClassName({ row }) {
  return row.isExpanded ? 'expanded-row' : ''
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.table-header {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  background-color: #F5F7FA;
  border: 1px solid #EBEEF5;
  border-bottom: none;
  font-weight: 500;
  color: #606266;
}

.release-note-row {
  display: flex;
  align-items: center;
  width: 100%;
}

.header-item,
.row-item {
  padding: 0 10px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.version {
  width: 180px;
}

.title {
  width: 200px;
}

.desc {
  flex: 1;
}

.date {
  width: 180px;
}

.action {
  width: 50px;
  text-align: right;
}

.el-collapse {
  border-top: none;
}

.el-collapse-item {
  border: 1px solid #EBEEF5;
  margin-bottom: -1px;
}

.changelog-list {
  padding: 15px 50px;
  background-color: #f8f8f9;
  border-top: 1px solid #EBEEF5;
}

.changelog-empty {
  padding: 20px;
  text-align: center;
}

.el-tag {
  margin-left: 8px;
}

/* 覆盖 element-ui 的默认样式 */
.el-collapse-item__header {
  padding: 0 20px;
}

.el-collapse-item__content {
  padding-bottom: 0;
}

.el-table {
  margin-bottom: 0;
}

/* 添加箭头旋转动画 */
.el-icon-arrow-right {
  transition: transform 0.3s;
}

.el-icon-arrow-right.expanded {
  transform: rotate(90deg);
}

/* 让表格行可点击 */
.el-table tbody tr {
  cursor: pointer;
}

/* 悬停效果 */
.el-table tbody tr:hover {
  background-color: #f5f7fa;
}

/* 展开行的样式 */
.el-table .expanded-row {
  background-color: #f8f8f9;
}

/* 展开行下方的边框 */
.el-table .expanded-row td {
  border-bottom: none;
}
</style>
