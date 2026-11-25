<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="登录地址" prop="ipaddr">
        <el-input
          v-model="queryParams.ipaddr"
          placeholder="请输入登录地址"
          clearable
          style="width: 240px;"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户名称" prop="userName">
        <el-input
          v-model="queryParams.userName"
          placeholder="请输入用户名称"
          clearable
          style="width: 240px;"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="登录状态"
          clearable
          style="width: 240px"
        >
          <el-option
            v-for="dict in dict.type.sys_common_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="登录时间">
        <el-date-picker
          v-model="dateRange"
          style="width: 240px"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="['00:00:00', '23:59:59']"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="small"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['monitor:logininfor:remove']"
        >删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="small"
          @click="handleClean"
          v-hasPermi="['monitor:logininfor:remove']"
        >清空
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-unlock"
          size="small"
          :disabled="single"
          @click="handleUnlock"
          v-hasPermi="['monitor:logininfor:unlock']"
        >解锁
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="small"
          @click="handleExport"
          v-hasPermi="['monitor:logininfor:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table ref="tablesRef" v-loading="loading" :data="list" @selection-change="handleSelectionChange"
              :default-sort="defaultSort" @sort-change="handleSortChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="访问编号" align="center" prop="infoId"/>
      <el-table-column label="用户名称" align="center" prop="userName" :show-overflow-tooltip="true" sortable="custom"
                       :sort-orders="['descending', 'ascending']"/>
      <el-table-column label="登录地址" align="center" prop="ipaddr" width="130" :show-overflow-tooltip="true"/>
      <el-table-column label="登录地点" align="center" prop="loginLocation" :show-overflow-tooltip="true"/>
      <el-table-column label="浏览器" align="center" prop="browser" :show-overflow-tooltip="true"/>
      <el-table-column label="操作系统" align="center" prop="os"/>
      <el-table-column label="登录状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="dict.type.sys_common_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作信息" align="center" prop="msg" :show-overflow-tooltip="true"/>
      <el-table-column label="登录日期" align="center" prop="loginTime" sortable="custom"
                       :sort-orders="['descending', 'ascending']" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.loginTime) }}</span>
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
  </div>
</template>

<script>
export default {
  name: 'Logininfor',
  dicts: ['sys_common_status']
}
</script>

<script setup>
import { ref, reactive, getCurrentInstance, onMounted } from 'vue'
import { list as listLogininfor, delLogininfor, cleanLogininfor, unlockLogininfor } from '@/api/monitor/logininfor'

const { proxy } = getCurrentInstance()

// 遮罩层
const loading = ref(true)
// 选中数组
const ids = ref([])
// 非单个禁用
const single = ref(true)
// 非多个禁用
const multiple = ref(true)
// 选择用户名
const selectName = ref('')
// 显示搜索条件
const showSearch = ref(true)
// 总条数
const total = ref(0)
// 表格数据
const list = ref([])
// 日期范围
const dateRange = ref([])
// 默认排序
const defaultSort = ref({ prop: 'loginTime', order: 'descending' })
// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  ipaddr: undefined,
  userName: undefined,
  status: undefined
})

const tablesRef = ref()

/** 查询登录日志列表 */
function getList() {
  loading.value = true
  listLogininfor(proxy.addDateRange(queryParams, dateRange.value)).then(response => {
    list.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = []
  proxy.resetForm('queryForm')
  queryParams.pageNum = 1
  tablesRef.value.sort(defaultSort.value.prop, defaultSort.value.order)
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.infoId)
  single.value = selection.length != 1
  multiple.value = !selection.length
  selectName.value = selection.map(item => item.userName)
}

/** 排序触发事件 */
function handleSortChange(column, prop, order) {
  queryParams.orderByColumn = column.prop
  queryParams.isAsc = column.order
  getList()
}

/** 删除按钮操作 */
function handleDelete(row) {
  const infoIds = row.infoId || ids.value
  proxy.$modal.confirm('是否确认删除访问编号为"' + infoIds + '"的数据项？').then(function () {
    return delLogininfor(infoIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {
  })
}

/** 清空按钮操作 */
function handleClean() {
  proxy.$modal.confirm('是否确认清空所有登录日志数据项？').then(function () {
    return cleanLogininfor()
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('清空成功')
  }).catch(() => {
  })
}

/** 解锁按钮操作 */
function handleUnlock() {
  const username = selectName.value
  proxy.$modal.confirm('是否确认解锁用户"' + username + '"数据项?').then(function () {
    return unlockLogininfor(username)
  }).then(() => {
    proxy.$modal.msgSuccess('用户' + username + '解锁成功')
  }).catch(() => {
  })
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('monitor/logininfor/export', {
    ...queryParams
  }, `logininfor_${new Date().getTime()}.xlsx`)
}

onMounted(() => {
  getList()
})
</script>
