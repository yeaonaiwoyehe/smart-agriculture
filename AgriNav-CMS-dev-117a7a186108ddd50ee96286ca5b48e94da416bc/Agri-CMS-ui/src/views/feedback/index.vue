<template>
  <div class="app-container">
    <div class="content-wrapper">
      <div class="main-content">
        <!-- Search Form -->
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="用户名称" prop="username">
            <el-input
              v-model="queryParams.username"
              placeholder="请输入用户名称"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item label="手机号码" prop="phoneNumber">
            <el-input
              v-model="queryParams.phoneNumber"
              placeholder="请输入手机号码"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item label="反馈内容" prop="content">
            <el-input
              v-model="queryParams.content"
              placeholder="请输入反馈内容"
              clearable
              @keyup.enter.native="handleQuery"
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
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <!-- Table -->
        <el-table v-loading="loading" :data="feedbackList">
          <el-table-column label="用户名称" align="center" prop="username" />
          <el-table-column label="手机号码" align="center" prop="phoneNumber" />
          <el-table-column label="反馈内容" align="center" prop="content" :show-overflow-tooltip="true" />
          <el-table-column label="反馈标签" align="center">
            <template slot-scope="scope">
              <el-tag
                v-for="label in scope.row.labels"
                :key="label.dictCode"
                size="mini"
                style="margin-right: 5px"
              >
                {{ label.dictLabel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" align="center" prop="commitTime" width="180">
            <template slot-scope="scope">
              <span>{{ formatTime(scope.row.commitTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="处理状态" align="center">
            <template slot-scope="scope">
              <el-tag :type="scope.row.isChecked === 1 ? 'success' : 'warning'">
                {{ scope.row.isChecked === 1 ? '已处理' : '未处理' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template slot-scope="scope">
              <el-button
                size="mini"
                type="text"
                icon="el-icon-view"
                @click="handleView(scope.row)"
              >查看</el-button>
              <el-button
                v-if="scope.row.isChecked === 0"
                size="mini"
                type="text"
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
          :page.sync="queryParams.pageNum"
          :limit.sync="queryParams.pageSize"
          @pagination="getList"
        />
      </div>
    </div>

    <!-- View Dialog -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名称">{{ form.username }}</el-descriptions-item>
        <el-descriptions-item label="手机号码">{{ form.phoneNumber }}</el-descriptions-item>
        <el-descriptions-item label="反馈内容" :span="2">{{ form.content }}</el-descriptions-item>
        <el-descriptions-item label="反馈标签" :span="2">
          <el-tag
            v-for="label in form.labels"
            :key="label.dictCode"
            size="mini"
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
      :visible.sync="replyDialogVisible"
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
      <div slot="footer" class="dialog-footer">
        <el-button @click="replyDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitReply">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listFeedback, getFeedback, getFeedbackTypes, checkFeedback } from "@/api/feedback/feedback";

export default {
  name: "Feedback",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 反馈表格数
      feedbackList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 日期范围
      dateRange: [],
      // 确认时间范围
      checkedDateRange: [],
      // 查询参数
      queryParams: {
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
      },
      // 表单参数
      form: {},
      feedbackTypes: [], // 新增存储反馈类型的数组
      replyDialogVisible: false,
      replyForm: {
        id: '',
        phoneNumber: '',
        reply: '',
      }
    };
  },
  created() {
    this.getFeedbackTypeList();
    this.getList();
  },
  methods: {
    /** 查询反馈列表 */
    getList() {
      this.loading = true;

      // 创建基础查询参数对象
      const queryParams = {
        pageNum: this.queryParams.pageNum,
        pageSize: this.queryParams.pageSize,
        username: this.queryParams.username,
        phoneNumber: this.queryParams.phoneNumber,
        content: this.queryParams.content,
        isChecked: this.queryParams.isChecked
      };

      // 处理标签
      if (this.queryParams.labels && this.queryParams.labels.length > 0) {
        queryParams.labels = this.queryParams.labels.map(dictCode => ({
          dictCode: dictCode
        }));
      }

      // 处理提交时间范围 (CommitTime)
      if (this.dateRange && this.dateRange.length === 2) {
        queryParams.startCommitTime = this.dateRange[0];
        queryParams.endCommitTime = this.dateRange[1].split(' ')[0] + ' 23:59:59';
      }

      // 处理确认时间范围 (CheckedTime)
      if (this.checkedDateRange && this.checkedDateRange.length === 2) {
        queryParams.startCheckedTime = this.checkedDateRange[0];
        queryParams.endCheckedTime = this.checkedDateRange[1].split(' ')[0] + ' 23:59:59';
      }

      // 移除值为 undefined 或 null 的属性
      Object.keys(queryParams).forEach(key => {
        if (queryParams[key] === undefined || queryParams[key] === null) {
          delete queryParams[key];
        }
      });

      console.log('查询参数:', queryParams);

      listFeedback(queryParams).then(response => {
        this.feedbackList = response.data;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.dateRange = [];
      this.checkedDateRange = [];
      this.resetForm("queryForm");
      this.queryParams.labels = [];
      this.handleQuery();
    },
    /** 查看按钮操作 */
    handleView(row) {
      this.form = row;
      this.open = true;
      this.title = "查看反馈详情";
    },
    // 添加标签变化处理方法
    handleLabelChange(value) {
      console.log('选中的标签:', value);
      this.queryParams.labels = value;
    },
    // 获取字典标签
    getDictLabel(dictCode) {
      if (!this.dict.type.sys_feedback_type) {
        return '';
      }
      const item = this.dict.type.sys_feedback_type.find(
        dict => dict.dictCode === dictCode || dict.value === dictCode
      );
      return item ? item.dictLabel || item.label : '';
    },
    // 获取反馈类型列表
    getFeedbackTypeList() {
      getFeedbackTypes().then(response => {
        this.feedbackTypes = response.data;
      });
    },
    /** 处理按钮操作 */
    handleCheck(row) {
      this.replyForm.id = row.id;
      this.replyForm.phoneNumber = row.phoneNumber;
      this.replyForm.reply = '';
      this.replyDialogVisible = true;
    },
    // Add new method for handling reply submission
    submitReply() {
      if (!this.replyForm.reply) {
        this.$message.error('请输入回复内容');
        return;
      }
      
      const feedback = {
        id: this.replyForm.id,
        phoneNumber: this.replyForm.phoneNumber
      };
      
      checkFeedback(feedback, this.replyForm.reply).then(response => {
        this.$modal.msgSuccess("处理成功");
        this.replyDialogVisible = false;
        this.getList();
      });
    },
    // 添加新的时间格式化方法
    formatTime(time) {
      if (!time) return '';
      const date = new Date(time);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      return `${year}/${month}/${day} ${hours}:${minutes}:${seconds}`;
    },
  }
};
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
