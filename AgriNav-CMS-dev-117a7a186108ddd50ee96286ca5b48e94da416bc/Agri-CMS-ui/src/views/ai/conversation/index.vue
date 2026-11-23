<template>
  <div class="app-container">
    <!-- 搜索 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="70px">
      <el-form-item label="账号" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="登录账号" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickName">
        <el-input v-model="queryParams.nickName" placeholder="用户昵称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="端" prop="side">
        <el-select v-model="queryParams.side" placeholder="请选择" clearable style="width:120px">
          <el-option label="农机端" value="ag" />
          <el-option label="管理端" value="ma" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
          end-placeholder="结束日期" value-format="yyyy-MM-dd" style="width:240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 会话表格 -->
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="会话标题" prop="title" align="left" :show-overflow-tooltip="true" min-width="200" />
      <el-table-column label="用户" align="center" width="150">
        <template slot-scope="scope">
          {{ scope.row.nickName || '—' }}<div class="sub-text">{{ scope.row.userName }}</div>
        </template>
      </el-table-column>
      <el-table-column label="端" prop="side" align="center" width="80">
        <template slot-scope="scope">{{ sideLabel(scope.row.side) }}</template>
      </el-table-column>
      <el-table-column label="消息数" prop="messageCount" align="center" width="80" />
      <el-table-column label="最后消息" prop="lastMessageTime" align="center" width="160" />
      <el-table-column label="创建时间" prop="createTime" align="center" width="160" />
      <el-table-column label="留存到期" prop="expireTime" align="center" width="160" />
      <el-table-column label="操作" align="center" width="90" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleDetail(scope.row)">核对</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 会话核对弹窗 -->
    <el-dialog title="会话核对" :visible.sync="open" width="780px" append-to-body custom-class="conv-dialog">
      <el-descriptions :column="3" border size="small" class="conv-meta">
        <el-descriptions-item label="标题">{{ detail.conversation && detail.conversation.title }}</el-descriptions-item>
        <el-descriptions-item label="用户">
          {{ detail.conversation && detail.conversation.nickName }}（{{ detail.conversation && detail.conversation.userName }}）
        </el-descriptions-item>
        <el-descriptions-item label="留存到期">{{ detail.conversation && detail.conversation.expireTime }}</el-descriptions-item>
      </el-descriptions>

      <div class="chat-box">
        <div v-for="msg in detail.messages" :key="msg.id" :class="['msg-row', msg.role]">
          <div class="msg-meta">
            {{ msg.role === 'user' ? '用户提问' : '助手回答' }} · {{ msg.createTime }}
          </div>
          <div :class="['bubble', msg.role]">
            <div class="bubble-content">{{ displayContent(msg) }}</div>

            <!-- 助手消息的路由、来源、状态与业务单跳转 -->
            <template v-if="msg.role === 'assistant'">
              <div class="bubble-tags">
                <el-tag size="mini" :type="routeTagType(msg.routeType)">{{ routeLabel(msg.routeType) }}</el-tag>
                <el-tag size="mini" :type="statusTagType(msg.status)">{{ statusLabel(msg.status) }}</el-tag>
                <el-tag v-if="msg.toolName" size="mini" type="primary" effect="plain">工具：{{ msg.toolName }}</el-tag>
              </div>
              <div class="bubble-source">{{ msg.sourceLabel }}</div>
              <div v-if="msg.bizRefNo" class="bubble-biz">
                <el-button type="text" size="mini" icon="el-icon-link" @click="goBiz(msg)">
                  查看关联{{ msg.bizRefType }}：{{ msg.bizRefNo }}
                </el-button>
              </div>
            </template>
          </div>
        </div>
      </div>

      <div slot="footer">
        <el-button type="primary" @click="open=false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listConversation, getConversation } from '@/api/ai/conversation'

export default {
  name: 'AgentConversation',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      dataList: [],
      open: false,
      detail: { conversation: {}, messages: [] },
      dateRange: [],
      statusOptions: [
        { value: 'REPLY', label: '直接回复' },
        { value: 'NEED_CONFIRM', label: '待确认' },
        { value: 'NEED_PARAMS', label: '缺参' },
        { value: 'FAILED', label: '失败' }
      ],
      routeOptions: [
        { value: 'BUSINESS_DATA', label: '业务数据' },
        { value: 'KNOWLEDGE', label: '制度知识' },
        { value: 'CHITCHAT', label: '闲聊' },
        { value: 'UNKNOWN', label: '无法识别' }
      ],
      // 业务单类型 -> 现有菜单路由；回执暂无独立页面，走复制兜底
      bizRouteMap: {
        '结算单': '/agri/settlement',
        '复核单': '/agri/review',
        '作业单': '/agri/workorder'
      },
      queryParams: { pageNum: 1, pageSize: 10, userName: undefined, nickName: undefined, side: undefined }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      const params = { ...this.queryParams }
      if (this.dateRange && this.dateRange.length === 2) {
        params.beginTime = this.dateRange[0]
        params.endTime = this.dateRange[1]
      }
      listConversation(params).then(res => {
        this.dataList = res.rows
        this.total = res.total
        this.loading = false
      })
    },
    handleDetail(row) {
      getConversation(row.id).then(res => {
        this.detail = res.data
        this.open = true
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.dateRange = []
      this.resetForm('queryForm')
      this.handleQuery()
    },
    displayContent(msg) {
      if (msg.content && msg.content.trim()) {
        return msg.content
      }
      // 待确认等场景回答正文可能为空，用来源标注兜底，避免空白气泡
      return msg.sourceLabel || '（无文本内容）'
    },
    goBiz(msg) {
      const path = this.bizRouteMap[msg.bizRefType]
      if (path) {
        this.$router.push({ path, query: { refNo: msg.bizRefNo } }).catch(() => {})
        return
      }
      // 回执等暂无独立页面：复制单号并提示
      const text = msg.bizRefNo
      const input = document.createElement('textarea')
      input.value = text
      document.body.appendChild(input)
      input.select()
      try {
        document.execCommand('copy')
        this.$message.success(msg.bizRefType + '编号已复制：' + text + '（该类型暂无独立页面）')
      } catch (e) {
        this.$message.info(msg.bizRefType + '编号：' + text)
      }
      document.body.removeChild(input)
    },
    sideLabel(val) {
      if (val === 'ag') return '农机端'
      if (val === 'ma') return '管理端'
      return val || '—'
    },
    routeLabel(val) {
      const item = this.routeOptions.find(r => r.value === val)
      return item ? item.label : val
    },
    routeTagType(val) {
      switch (val) {
        case 'BUSINESS_DATA': return 'primary'
        case 'KNOWLEDGE': return 'success'
        case 'CHITCHAT': return 'info'
        default: return 'info'
      }
    },
    statusLabel(val) {
      const item = this.statusOptions.find(s => s.value === val)
      return item ? item.label : val
    },
    statusTagType(val) {
      switch (val) {
        case 'REPLY': return 'success'
        case 'NEED_CONFIRM': return 'warning'
        case 'NEED_PARAMS': return 'info'
        case 'FAILED': return 'danger'
        default: return 'info'
      }
    }
  }
}
</script>

<style scoped>
.sub-text {
  font-size: 12px;
  color: #909399;
}

.conv-meta {
  margin-bottom: 12px;
}

.chat-box {
  max-height: 520px;
  overflow-y: auto;
  padding: 8px 4px;
  background: #f5f7fa;
  border-radius: 6px;
}

.msg-row {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
}

.msg-row.user {
  align-items: flex-start;
}

.msg-row.assistant {
  align-items: flex-end;
}

.msg-meta {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.bubble {
  max-width: 78%;
  padding: 10px 12px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

.bubble.user {
  background: #ffffff;
  border: 1px solid #e4e7ed;
}

.bubble.assistant {
  background: #ecf5ff;
  border: 1px solid #d9ecff;
}

.bubble-content {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.6;
}

.bubble-tags {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.bubble-source {
  margin-top: 6px;
  font-size: 12px;
  color: #606266;
}

.bubble-biz {
  margin-top: 4px;
}
</style>
