<template>
  <div class="app-container">
    <!-- 搜索 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="规则名称" prop="ruleName">
        <el-input v-model="queryParams.ruleName" placeholder="请输入规则名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="作业类型" prop="workType">
        <el-select v-model="queryParams.workType" placeholder="请选择" clearable style="width:140px">
          <el-option v-for="w in workTypeOptions" :key="w.value" :label="w.label" :value="w.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width:120px">
          <el-option label="启用" :value="0" />
          <el-option label="停用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete">删除</el-button>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="规则名称" prop="ruleName" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="作业类型" prop="workType" align="center">
        <template slot-scope="scope">{{ workTypeLabel(scope.row.workType) }}</template>
      </el-table-column>
      <el-table-column label="单价(元/亩)" prop="unitPrice" align="center" />
      <el-table-column label="面积下限(亩)" prop="minArea" align="center" />
      <el-table-column label="面积上限(亩)" prop="maxArea" align="center">
        <template slot-scope="scope">{{ scope.row.maxArea == null ? '不限' : scope.row.maxArea }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 0 ? 'success' : 'info'">{{ scope.row.status === 0 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="title" :visible.sync="open" width="520px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="如：耕地作业基准单价" />
        </el-form-item>
        <el-form-item label="作业类型" prop="workType">
          <el-select v-model="form.workType" placeholder="请选择" style="width:100%">
            <el-option v-for="w in workTypeOptions" :key="w.value" :label="w.label" :value="w.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="单价(元/亩)" prop="unitPrice">
          <el-input-number v-model="form.unitPrice" :min="0" :precision="2" :step="1" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="面积下限(亩)" prop="minArea">
          <el-input-number v-model="form.minArea" :min="0" :precision="2" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="面积上限(亩)" prop="maxArea">
          <el-input-number v-model="form.maxArea" :min="0" :precision="2" controls-position="right" style="width:100%" placeholder="留空表示不限" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="0">启用</el-radio>
            <el-radio :label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="open=false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPricingRule, addPricingRule, updatePricingRule, delPricingRule } from '@/api/closure/pricingRule'

export default {
  name: 'PricingRule',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      dataList: [],
      multiple: true,
      ids: [],
      open: false,
      title: '',
      workTypeOptions: [
        { value: 4001, label: '耕地' },
        { value: 4002, label: '旋耕' },
        { value: 4003, label: '播种' },
        { value: 4004, label: '植保' },
        { value: 4005, label: '收割' },
        { value: 4006, label: '秸秆还田' }
      ],
      queryParams: { pageNum: 1, pageSize: 10, ruleName: undefined, workType: undefined, status: undefined },
      form: {},
      rules: {
        ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
        workType: [{ required: true, message: '请选择作业类型', trigger: 'change' }],
        unitPrice: [{ required: true, message: '请输入单价', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listPricingRule(this.queryParams).then(res => {
        this.dataList = res.rows
        this.total = res.total
        this.loading = false
      })
    },
    workTypeLabel(val) {
      const item = this.workTypeOptions.find(w => w.value === val)
      return item ? item.label : val
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(i => i.id)
      this.multiple = !selection.length
    },
    reset() {
      this.form = { id: undefined, ruleName: undefined, workType: undefined, unitPrice: 0, minArea: 0, maxArea: undefined, status: 0 }
      this.resetForm('form')
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增单价规则'
    },
    handleEdit(row) {
      this.form = Object.assign({}, row)
      this.open = true
      this.title = '修改单价规则'
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        if (this.form.id != null) {
          updatePricingRule(this.form).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.open = false
            this.getList()
          })
        } else {
          addPricingRule(this.form).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      const ids = row.id ? [row.id] : this.ids
      this.$modal.confirm('确认删除选中的单价规则？').then(() => delPricingRule(ids)).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>
