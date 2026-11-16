<template>
  <div class="app-container">
    <el-card class="box-card">
      <div slot="header" class="card-header">
        <span>APP组件管理</span>
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          @click="handleAdd"
        >新增组件</el-button>
      </div>
      
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
          <template slot-scope="scope">
            <span class="id-text">{{ scope.row.componentId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="组件名称" prop="permissionName" width="150" align="left" />
        <el-table-column label="组件标识" prop="componentStr" min-width="200" align="left" />
        <el-table-column label="父级组件ID" prop="parentId" width="100" align="left" class-name="id-column">
          <template slot-scope="scope">
            <span class="id-text">{{ scope.row.parentId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="是否可见" width="80" align="center">
          <template slot-scope="scope">
            <el-tag 
              size="mini" 
              :type="scope.row.isVisible === 0 ? 'success' : 'info'"
              style="cursor: pointer"
              @click="handleVisibilityChange(scope.row)"
            >
              {{ scope.row.isVisible === 0 ? '可见' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template slot-scope="scope">
            <el-button
              type="text"
              size="mini"
              @click="handleUpdate(scope.row)"
            >修改</el-button>
            <el-button
              type="text"
              size="mini"
              @click="handleDelete(scope.row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加或修改对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
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
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPermission, getPermission, addPermission, updatePermission, delPermission } from '@/api/system/appPermission'
import Treeselect from "@riophae/vue-treeselect"
import "@riophae/vue-treeselect/dist/vue-treeselect.css"

export default {
  name: "Permission",
  components: { Treeselect },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 权限列表
      permissionList: [],
      // 权限树选项
      permissionOptions: [],
      // 表单参数
      form: {
        componentId: undefined,
        permissionName: undefined,
        componentStr: undefined,
        parentId: 0,
        isVisible: 0
      },
      // 表单校验
      rules: {
        permissionName: [
          { required: true, message: "组件名称不能为空", trigger: "blur" }
        ],
        componentStr: [
          { required: true, message: "组件标识不能为空", trigger: "blur" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询权限列表 */
    getList() {
      this.loading = true;
      listPermission().then(response => {
        this.permissionList = this.handleTree(response.data, "componentId", "parentId");
        this.loading = false;
      });
    },
    /** 转换权限数据结构 */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children;
      }
      return {
        id: node.componentId,
        label: node.permissionName,
        children: node.children
      };
    },
    /** 查询权限下拉树结构 */
    getTreeselect() {
      listPermission().then(response => {
        this.permissionOptions = [];
        const permission = { componentId: 0, permissionName: '主类目', children: [] };
        permission.children = this.handleTree(response.data, "componentId", "parentId");
        this.permissionOptions.push(permission);
      });
    },
    /** 取消按钮 */
    cancel() {
      this.open = false;
      this.reset();
    },
    /** 表单重置 */
    reset() {
      this.form = {
        componentId: undefined,
        permissionName: undefined,
        componentStr: undefined,
        parentId: 0,
        isVisible: 0
      };
      this.resetForm("form");
    },
    /** 新增按钮操作 */
    handleAdd(row) {
      this.reset();
      this.getTreeselect();
      if (row != null && row.componentId) {
        this.form.parentId = row.componentId;
      } else {
        this.form.parentId = 0;
      }
      this.open = true;
      this.title = "添加组件";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      this.getTreeselect();
      getPermission(row.componentId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改组件";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.componentId != undefined) {
            updatePermission(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addPermission(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.$confirm('是否确认删除该组件?', "警告", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        return delPermission(row.componentId);
      }).then(() => {
        this.getList();
        this.$message.success("删除成功");
      });
    },
    /** 处理可见性变更 */
    handleVisibilityChange(row) {
      const newVisibility = row.isVisible === 0 ? 1 : 0;
      const updatedRow = { ...row, isVisible: newVisibility };
      
      updatePermission(updatedRow).then(response => {
        row.isVisible = newVisibility; // 直接更新本地数据的可见性状态
        this.$modal.msgSuccess("可见性修改成功");
      }).catch(() => {
        this.$modal.msgError("可见性修改失败");
      });
    }
  }
};
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
