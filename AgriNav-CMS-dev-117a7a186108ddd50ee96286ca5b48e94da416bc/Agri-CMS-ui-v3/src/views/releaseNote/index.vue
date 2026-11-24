<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="68px" @submit.prevent>
      <el-form-item label="版本号" prop="version">
        <el-input
          v-model="queryParams.version"
          placeholder="请输入版本号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="small"
          @click="handleAdd"
        >新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="releaseNoteList">
      <el-table-column label="版本号" align="center" prop="version" />
      <el-table-column label="标题" align="center" prop="title" />
      <el-table-column label="描述" align="center" prop="description" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'PUBLISHED' ? 'success' : 'info'">
            {{ scope.row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" align="center" prop="releaseDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.releaseDate) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template #default="scope">
          <el-button
            size="small"
            link
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
          >修改</el-button>
          <el-button
            size="small"
            link
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="版本号" prop="version">
          <el-input v-model="form.version" placeholder="请输入版本号" />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="form.description" placeholder="请输入描述内容" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态">
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="草稿" value="DRAFT" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布时间" prop="releaseDate">
          <el-date-picker
            v-model="form.releaseDate"
            type="datetime"
            placeholder="选择发布时间"
          />
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
import { listReleaseNote, getReleaseNote, addReleaseNote, updateReleaseNote, delReleaseNote } from "@/api/system/releaseNote"

defineOptions({
  name: 'ReleaseNote'
})

const { proxy } = getCurrentInstance()

// 遮罩层
const loading = ref(true)
// 弹出层标题
const title = ref('')
// 是否显示弹出层
const open = ref(false)
// 是否显示搜索条件
const showSearch = ref(true)
// 发布记录列表
const releaseNoteList = ref([])
// 查询参数
const queryParams = ref({
  version: undefined
})
// 表单参数
const form = ref({
  id: undefined,
  version: undefined,
  title: undefined,
  description: undefined,
  status: 'DRAFT',
  releaseDate: undefined
})
// 表单校验
const rules = {
  version: [
    { required: true, message: "版本号不能为空", trigger: "blur" },
    { max: 50, message: "版本号长度不能超过50个字符", trigger: "blur" }
  ],
  title: [
    { required: true, message: "标题不能为空", trigger: "blur" },
    { max: 200, message: "标题长度不能超过200个字符", trigger: "blur" }
  ],
  description: [
    { required: true, message: "描述不能为空", trigger: "blur" }
  ],
  status: [
    { required: true, message: "状态不能为空", trigger: "change" }
  ],
  releaseDate: [
    { required: true, message: "发布时间不能为空", trigger: "blur" }
  ]
}
const queryFormRef = ref(null)
const formRef = ref(null)

/** 查询发布记录列表 */
function getList() {
  loading.value = true
  listReleaseNote(queryParams.value.version).then(response => {
    releaseNoteList.value = response.data
    loading.value = false
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
    version: undefined,
    title: undefined,
    description: undefined,
    status: 'DRAFT',
    releaseDate: undefined
  }
  proxy.resetForm("formRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryFormRef")
  queryParams.value.version = undefined
  handleQuery()
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加发布记录"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const id = row.id
  getReleaseNote(id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改发布记录"
  })
}

/** 提交按钮 */
function submitForm() {
  formRef.value.validate(valid => {
    if (valid) {
      if (form.value.id != undefined) {
        updateReleaseNote(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addReleaseNote(form.value).then(response => {
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
  proxy.$modal.confirm('是否确认删除该发布记录？').then(function () {
    return delReleaseNote(id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

onMounted(() => {
  getList()
})
</script>
