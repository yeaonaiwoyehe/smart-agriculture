<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>APP组件管理</span>
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            @click="handleAdd"
          >新增组件</el-button>
        </div>
      </template>
      
      <el-table 
        v-loading="loading" 
        :data="permissionList" 
        row-key="componentId" 
        :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
        style="width: 100%"
      >
        <el-table-column 
          label="组件ID" 
          prop="componentId" 
          width="90" 
          align="left"
          class-name="id-column"
        >
          <template #default="scope">
            <span class="id-text">{{ scope.row.componentId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="组件名称" prop="permissionName" width="150" align="left" />
        <el-table-column label="组件标识" prop="componentStr" min-width="200" align="left" />
        <el-table-column label="父级组件ID" prop="parentId" width="100" align="left" class-name="id-column">
          <template #default="scope">
            <span class="id-text">{{ scope.row.parentId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="是否可见" width="80" align="center">
          <template #default="scope">
            <el-tag 
              size="small" 
              :type="scope.row.isVisible === 0 ? 'success' : 'info'"
              style="cursor: pointer"
              @click="handleVisibilityChange(scope.row)"
            >
              {{ scope.row.isVisible === 0 ? '可见' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="scope">
            <el-button
              link
              size="small"
              @click="handleUpdate(scope.row)"
            >修改</el-button>
            <el-button
              link
              size="small"
              @click="handleDelete(scope.row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加或修改对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="父级组件" prop="parentId">
          <treeselect
            v-model="form.parentId"
            :options="permissionOptions"
            :normalizer="normalizer"
            :show-count="true"
            placeholder="选择父级组件"
          />
        </el-form-item>
        <el-form-item label="组件ID" prop="componentId" v-if="form.componentId">
          <el-input v-model="form.componentId" placeholder="请输入组件ID" disabled />
        </el-form-item>
        <el-form-item label="组件名称" prop="permissionName">
          <el-input v-model="form.permissionName" placeholder="请输入组件名称" />
        </el-form-item>
        <el-form-item label="组件标识" prop="componentStr">
          <el-input v-model="form.componentStr" placeholder="请输入组件标识" />
        </el-form-item>
        <el-form-item label="是否可见" prop="isVisible">
          <el-radio-group v-model="form.isVisible">
            <el-radio :label="0">可见</el-radio>
            <el-radio :label="1">隐藏</el-radio>
          </el-radio-group>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPermission, getPermission, addPermission, updatePermission, delPermission } from '@/api/system/appPermission'
import Treeselect from '@/components/Treeselect/index.vue'

defineOptions({
  name: 'Permission'
})

const { proxy } = getCurrentInstance()

// 遮罩层
const loading = ref(true)
// 弹出层标题
const title = ref('')
// 是否显示弹出层
const open = ref(false)
// 权限列表
const permissionList = ref([])
// 权限树选项
const permissionOptions = ref([])
// 表单参数
const form = ref({
  componentId: undefined,
  permissionName: undefined,
  componentStr: undefined,
  parentId: 0,
  isVisible: 0
})
// 表单校验
const rules = {
  permissionName: [
    { required: true, message: "组件名称不能为空", trigger: "blur" }
  ],
  componentStr: [
    { required: true, message: "组件标识不能为空", trigger: "blur" }
  ]
}
const formRef = ref(null)

/** 查询权限列表 */
function getList() {
  loading.value = true
  listPermission().then(response => {
    permissionList.value = proxy.handleTree(response.data, "componentId", "parentId")
    loading.value = false
  })
}

/** 转换权限数据结构 */
function normalizer(node) {
  if (node.children && !node.children.length) {
    delete node.children
  }
  return {
    id: node.componentId,
    label: node.permissionName,
    children: node.children
  }
}

/** 查询权限下拉树结构 */
function getTreeselect() {
  listPermission().then(response => {
    permissionOptions.value = []
    const permission = { componentId: 0, permissionName: '主类目', children: [] }
    permission.children = proxy.handleTree(response.data, "componentId", "parentId")
    permissionOptions.value.push(permission)
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    componentId: undefined,
    permissionName: undefined,
    componentStr: undefined,
    parentId: 0,
    isVisible: 0
  }
  proxy.resetForm("formRef")
}

/** 新增按钮操作 */
function handleAdd(row) {
  reset()
  getTreeselect()
  if (row != null && row.componentId) {
    form.value.parentId = row.componentId
  } else {
    form.value.parentId = 0
  }
  open.value = true
  title.value = "添加组件"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  getTreeselect()
  getPermission(row.componentId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改组件"
  })
}

/** 提交按钮 */
function submitForm() {
  formRef.value.validate(valid => {
    if (valid) {
      if (form.value.componentId != undefined) {
        updatePermission(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addPermission(form.value).then(response => {
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
  ElMessageBox.confirm('是否确认删除该组件?', "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(() => {
    return delPermission(row.componentId)
  }).then(() => {
    getList()
    ElMessage.success("删除成功")
  })
}

/** 处理可见性变更 */
function handleVisibilityChange(row) {
  const newVisibility = row.isVisible === 0 ? 1 : 0
  const updatedRow = { ...row, isVisible: newVisibility }

  updatePermission(updatedRow).then(response => {
    row.isVisible = newVisibility // 直接更新本地数据的可见性状态
    proxy.$modal.msgSuccess("可见性修改成功")
  }).catch(() => {
    proxy.$modal.msgError("可见性修改失败")
  })
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.el-table {
  margin-top: 10px;
}

.el-table .cell {
  padding-left: 10px !important;
}

.el-table .el-button--text {
  padding: 0 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.id-column {
  width: 90px;
}

.id-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
