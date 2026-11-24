<template>
  <!-- 授权用户 -->
  <el-dialog title="选择用户" v-model="visible" width="800px" top="5vh" append-to-body>
    <el-form :model="queryParams" ref="queryFormRef" size="small" :inline="true">
      <el-form-item label="用户名称" prop="userName">
        <el-input
          v-model="queryParams.userName"
          placeholder="请输入用户名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机号码" prop="phonenumber">
        <el-input
          v-model="queryParams.phonenumber"
          placeholder="请输入手机号码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row>
      <el-table @row-click="clickRow" ref="tableRef" :data="userList" @selection-change="handleSelectionChange"
                height="260px">
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column label="用户名称" prop="userName" :show-overflow-tooltip="true"/>
        <el-table-column label="用户昵称" prop="nickName" :show-overflow-tooltip="true"/>
        <el-table-column label="邮箱" prop="email" :show-overflow-tooltip="true"/>
        <el-table-column label="手机" prop="phonenumber" :show-overflow-tooltip="true"/>
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="180">
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </el-row>
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="handleSelectUser">确 定</el-button>
        <el-button @click="visible = false">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
export default {
  dicts: ['sys_normal_disable']
}
</script>

<script setup>
import { getCurrentInstance, ref } from 'vue'
import { unallocatedUserList, authUserSelectAll } from '@/api/system/role'

const props = defineProps({
  // 角色编号
  roleId: {
    type: [Number, String]
  }
})
const emit = defineEmits(['ok'])
const { proxy } = getCurrentInstance()

const visible = ref(false)
const userIds = ref([])
const total = ref(0)
const userList = ref([])
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  roleId: undefined,
  userName: undefined,
  phonenumber: undefined
})
const tableRef = ref(null)

// 显示弹框
function show() {
  queryParams.value.roleId = props.roleId
  getList()
  visible.value = true
}

function clickRow(row) {
  tableRef.value.toggleRowSelection(row)
}

// 多选框选中数据
function handleSelectionChange(selection) {
  userIds.value = selection.map(item => item.userId)
}

// 查询表数据
function getList() {
  unallocatedUserList(queryParams.value).then(res => {
    userList.value = res.rows
    total.value = res.total
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm('queryFormRef')
  handleQuery()
}

/** 选择授权用户操作 */
function handleSelectUser() {
  const roleId = queryParams.value.roleId
  const userIdsStr = userIds.value.join(',')
  if (userIdsStr == '') {
    proxy.$modal.msgError('请选择要分配的用户')
    return
  }
  authUserSelectAll({ roleId: roleId, userIds: userIdsStr }).then(res => {
    proxy.$modal.msgSuccess(res.msg)
    visible.value = false
    emit('ok')
  })
}

defineExpose({ show })
</script>
