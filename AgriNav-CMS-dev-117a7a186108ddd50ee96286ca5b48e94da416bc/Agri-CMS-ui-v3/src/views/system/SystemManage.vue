<template>
  <div class="page">
    <PageHeader title="权限与审计" code="MP-06" group="系统管理"
      desc="用户身份与数据范围由后端校验 · 全链路留痕，越权请求在接口层直接拦截。">
      <template #actions>
        <el-button :icon="Refresh" :loading="currentLoading" @click="reloadCurrent">刷新</el-button>
      </template>
    </PageHeader>

    <el-alert class="mt-2" type="info" :closable="false" show-icon
      title="系统运行告警接口待接入"
      description="后端暂未提供系统运行告警数据契约，本区域不再使用模拟数据。" />

    <el-tabs v-model="tab" class="mt-3">
      <!-- ==================== 用户管理 ==================== -->
      <el-tab-pane label="用户管理" name="users">
        <div class="filter-bar">
          <el-input v-model="userFilters.userName" placeholder="用户账号" clearable style="width: 170px"
            :prefix-icon="Search" @keyup.enter="apply('users')" />
          <el-input v-model="userFilters.phonenumber" placeholder="手机号" clearable style="width: 160px"
            @keyup.enter="apply('users')" />
          <el-select v-model="userFilters.status" style="width: 120px" @change="apply('users')">
            <el-option label="全部状态" value="" />
            <el-option label="正常" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
          <el-button type="primary" @click="apply('users')">查询</el-button>
          <el-button @click="resetUsers">重置</el-button>
          <span class="filter-hint grow">数据范围由角色决定；本页暂不开放用户写操作</span>
        </div>

        <div v-if="state.users.error" class="state-block">
          <el-alert :type="alertType(state.users.error)" :closable="false" show-icon :title="state.users.error.message" />
          <el-button v-if="state.users.error.kind !== 'forbidden'" size="small" @click="loadUsers">重新加载</el-button>
        </div>
        <PanelCard v-else flush>
          <div v-loading="state.users.loading">
            <el-table v-if="state.users.list.length" :data="state.users.list" size="small" style="width: 100%">
              <el-table-column prop="userName" label="用户账号" min-width="120" />
              <el-table-column prop="nickName" label="昵称" min-width="100" />
              <el-table-column label="手机号" min-width="126">
                <template #default="{ row }"><span class="mono">{{ row.phone || '—' }}</span></template>
              </el-table-column>
              <el-table-column prop="email" label="邮箱" min-width="150" show-overflow-tooltip />
              <el-table-column prop="deptName" label="部门" min-width="110">
                <template #default="{ row }">{{ row.deptName || '—' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }"><StatusTag :status="row.statusLabel" /></template>
              </el-table-column>
              <el-table-column label="最后登录" min-width="140">
                <template #default="{ row }"><span class="mono">{{ row.loginDate || '—' }}</span></template>
              </el-table-column>
              <el-table-column label="登录IP" min-width="120">
                <template #default="{ row }"><span class="mono">{{ row.loginIp || '—' }}</span></template>
              </el-table-column>
            </el-table>
            <el-empty v-else-if="!state.users.loading" description="暂无数据" />
          </div>
          <div v-if="state.users.total !== null" class="panel-pager">
            <el-pagination v-model:current-page="state.users.page" v-model:page-size="state.users.size"
              :page-sizes="[10, 20, 50]" :total="state.users.total"
              layout="total, sizes, prev, pager, next" small
              @current-change="loadUsers" @size-change="onSize('users')" />
          </div>
        </PanelCard>
      </el-tab-pane>

      <!-- ==================== 角色权限 ==================== -->
      <el-tab-pane label="角色权限" name="roles">
        <div class="filter-bar">
          <el-input v-model="roleFilters.roleName" placeholder="角色名称" clearable style="width: 170px"
            :prefix-icon="Search" @keyup.enter="apply('roles')" />
          <el-input v-model="roleFilters.roleKey" placeholder="角色权限字符" clearable style="width: 170px"
            @keyup.enter="apply('roles')" />
          <el-select v-model="roleFilters.status" style="width: 120px" @change="apply('roles')">
            <el-option label="全部状态" value="" />
            <el-option label="正常" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
          <el-button type="primary" @click="apply('roles')">查询</el-button>
          <el-button @click="resetRoles">重置</el-button>
          <span class="filter-hint grow">角色保存与数据范围调整本阶段暂未开放</span>
        </div>

        <div v-if="state.roles.error" class="state-block">
          <el-alert :type="alertType(state.roles.error)" :closable="false" show-icon :title="state.roles.error.message" />
          <el-button v-if="state.roles.error.kind !== 'forbidden'" size="small" @click="loadRoles">重新加载</el-button>
        </div>
        <PanelCard v-else flush>
          <div v-loading="state.roles.loading">
            <el-table v-if="state.roles.list.length" :data="state.roles.list" size="small" style="width: 100%">
              <el-table-column prop="roleName" label="角色名称" min-width="140" />
              <el-table-column prop="roleKey" label="权限字符" min-width="140">
                <template #default="{ row }"><span class="mono">{{ row.roleKey || '—' }}</span></template>
              </el-table-column>
              <el-table-column prop="roleSort" label="排序" width="80" align="center">
                <template #default="{ row }">{{ row.roleSort ?? '—' }}</template>
              </el-table-column>
              <el-table-column prop="dataScopeLabel" label="数据范围" min-width="130" />
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }"><StatusTag :status="row.statusLabel" /></template>
              </el-table-column>
              <el-table-column label="操作" width="110">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="selectRole(row)">查看权限</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else-if="!state.roles.loading" description="暂无数据" />
          </div>
          <div v-if="state.roles.total !== null" class="panel-pager">
            <el-pagination v-model:current-page="state.roles.page" v-model:page-size="state.roles.size"
              :page-sizes="[10, 20, 50]" :total="state.roles.total"
              layout="total, sizes, prev, pager, next" small
              @current-change="loadRoles" @size-change="onSize('roles')" />
          </div>
        </PanelCard>

        <PanelCard class="mt-3" :title="selectedRole ? `权限矩阵 · ${selectedRole.roleName}` : '权限矩阵'"
          sub="基于真实菜单树与角色已选菜单推导，仅作只读展示" flush>
          <div v-if="!selectedRole" class="pad-tip">请在角色列表中选择一个角色查看其菜单权限。</div>
          <div v-else-if="roleMenuError" class="state-block pad-tip">
            <el-alert :type="alertType(roleMenuError)" :closable="false" show-icon :title="roleMenuError.message" />
            <el-button v-if="roleMenuError.kind !== 'forbidden'" size="small" @click="selectRole(selectedRole)">重新加载</el-button>
          </div>
          <div v-else v-loading="roleMenuLoading || menuLoading" class="matrix-wrap">
            <el-alert v-if="menuError" type="error" :closable="false" show-icon :title="menuError.message" />
            <el-table v-else-if="menuTree.length" :data="menuTree" size="small" style="width: 100%">
              <el-table-column prop="label" label="模块 / 菜单" min-width="240" />
              <el-table-column label="是否授权" width="120" align="center">
                <template #default="{ row }">
                  <span :class="isGranted(row) ? 'chk' : 'dash'">{{ isGranted(row) ? '✓' : '—' }}</span>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else-if="!roleMenuLoading && !menuLoading" description="菜单树为空" />
          </div>
        </PanelCard>
      </el-tab-pane>

      <!-- ==================== 登录日志 ==================== -->
      <el-tab-pane label="登录日志" name="login">
        <div class="filter-bar">
          <el-input v-model="loginFilters.userName" placeholder="用户账号" clearable style="width: 160px"
            :prefix-icon="Search" @keyup.enter="apply('login')" />
          <el-input v-model="loginFilters.ipaddr" placeholder="登录 IP" clearable style="width: 150px"
            @keyup.enter="apply('login')" />
          <el-select v-model="loginFilters.status" style="width: 120px" @change="apply('login')">
            <el-option label="全部结果" value="" />
            <el-option label="成功" value="0" />
            <el-option label="失败" value="1" />
          </el-select>
          <el-date-picker v-model="loginFilters.dateRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间" end-placeholder="结束时间" style="width: 340px" />
          <el-button type="primary" @click="apply('login')">查询</el-button>
          <el-button @click="resetLogin">重置</el-button>
        </div>

        <div v-if="state.login.error" class="state-block">
          <el-alert :type="alertType(state.login.error)" :closable="false" show-icon :title="state.login.error.message" />
          <el-button v-if="state.login.error.kind !== 'forbidden'" size="small" @click="loadLogin">重新加载</el-button>
        </div>
        <PanelCard v-else flush>
          <div v-loading="state.login.loading">
            <el-table v-if="state.login.list.length" :data="state.login.list" size="small" style="width: 100%">
              <el-table-column label="登录时间" min-width="140">
                <template #default="{ row }"><span class="mono">{{ row.loginTime || '—' }}</span></template>
              </el-table-column>
              <el-table-column prop="userName" label="用户账号" min-width="110" />
              <el-table-column label="IP" min-width="112">
                <template #default="{ row }"><span class="mono">{{ row.ip || '—' }}</span></template>
              </el-table-column>
              <el-table-column prop="location" label="登录地点" min-width="120">
                <template #default="{ row }">{{ row.location || '—' }}</template>
              </el-table-column>
              <el-table-column prop="browserInfo" label="浏览器 / 系统" min-width="150">
                <template #default="{ row }">{{ row.browserInfo || '—' }}</template>
              </el-table-column>
              <el-table-column label="结果" width="86" align="center">
                <template #default="{ row }"><StatusTag :status="row.result" /></template>
              </el-table-column>
              <el-table-column prop="message" label="消息" min-width="150" show-overflow-tooltip>
                <template #default="{ row }">{{ row.message || '—' }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-else-if="!state.login.loading" description="暂无数据" />
          </div>
          <div v-if="state.login.total !== null" class="panel-pager">
            <el-pagination v-model:current-page="state.login.page" v-model:page-size="state.login.size"
              :page-sizes="[10, 20, 50]" :total="state.login.total"
              layout="total, sizes, prev, pager, next" small
              @current-change="loadLogin" @size-change="onSize('login')" />
          </div>
        </PanelCard>
      </el-tab-pane>

      <!-- ==================== 操作日志 ==================== -->
      <el-tab-pane label="操作日志" name="oper">
        <div class="filter-bar">
          <el-input v-model="operFilters.title" placeholder="操作模块" clearable style="width: 150px"
            :prefix-icon="Search" @keyup.enter="apply('oper')" />
          <el-input v-model="operFilters.operName" placeholder="操作人" clearable style="width: 130px"
            @keyup.enter="apply('oper')" />
          <el-select v-model="operFilters.businessType" style="width: 130px" @change="apply('oper')">
            <el-option label="全部类型" value="" />
            <el-option v-for="t in BUSINESS_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
          <el-select v-model="operFilters.status" style="width: 120px" @change="apply('oper')">
            <el-option label="全部状态" value="" />
            <el-option label="正常" value="0" />
            <el-option label="异常" value="1" />
          </el-select>
          <el-date-picker v-model="operFilters.dateRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间" end-placeholder="结束时间" style="width: 340px" />
          <el-button type="primary" @click="apply('oper')">查询</el-button>
          <el-button @click="resetOper">重置</el-button>
        </div>

        <div v-if="state.oper.error" class="state-block">
          <el-alert :type="alertType(state.oper.error)" :closable="false" show-icon :title="state.oper.error.message" />
          <el-button v-if="state.oper.error.kind !== 'forbidden'" size="small" @click="loadOper">重新加载</el-button>
        </div>
        <PanelCard v-else flush>
          <div v-loading="state.oper.loading">
            <el-table v-if="state.oper.list.length" :data="state.oper.list" size="small" style="width: 100%">
              <el-table-column label="操作时间" min-width="140">
                <template #default="{ row }"><span class="mono">{{ row.operTime || '—' }}</span></template>
              </el-table-column>
              <el-table-column prop="title" label="操作模块" min-width="120" />
              <el-table-column prop="operator" label="操作人" min-width="100">
                <template #default="{ row }">{{ row.operator || '—' }}</template>
              </el-table-column>
              <el-table-column prop="businessTypeLabel" label="操作类型" width="100" />
              <el-table-column label="IP" min-width="112">
                <template #default="{ row }"><span class="mono">{{ row.ip || '—' }}</span></template>
              </el-table-column>
              <el-table-column prop="location" label="操作地点" min-width="120">
                <template #default="{ row }">{{ row.location || '—' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="86" align="center">
                <template #default="{ row }"><StatusTag :status="row.statusLabel" /></template>
              </el-table-column>
              <el-table-column label="耗时" width="90" align="right">
                <template #default="{ row }"><span class="mono">{{ msText(row.costTime) }}</span></template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="openOperDetail(row)">详情</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else-if="!state.oper.loading" description="暂无数据" />
          </div>
          <div v-if="state.oper.total !== null" class="panel-pager">
            <el-pagination v-model:current-page="state.oper.page" v-model:page-size="state.oper.size"
              :page-sizes="[10, 20, 50]" :total="state.oper.total"
              layout="total, sizes, prev, pager, next" small
              @current-change="loadOper" @size-change="onSize('oper')" />
          </div>
        </PanelCard>
      </el-tab-pane>

      <!-- ==================== 权限审计（只读快照） ==================== -->
      <el-tab-pane label="权限审计" name="audit">
        <div v-if="!isAdmin" class="state-block">
          <el-alert type="warning" :closable="false" show-icon title="当前账号没有访问该模块的权限。" />
        </div>
        <template v-else>
          <div v-if="state.audit.error" class="state-block">
            <el-alert :type="alertType(state.audit.error)" :closable="false" show-icon :title="state.audit.error.message" />
            <el-button v-if="state.audit.error.kind !== 'forbidden'" size="small" @click="loadAudit">重新加载</el-button>
          </div>
          <div v-else v-loading="state.audit.loading">
            <template v-if="state.audit.snapshot">
              <div class="grid cols-4">
                <div class="card stat"><div class="stat-label">用户总数</div><div class="stat-value">{{ numText(state.audit.snapshot.summary?.userCount) }}</div></div>
                <div class="card stat"><div class="stat-label">角色总数</div><div class="stat-value">{{ numText(state.audit.snapshot.summary?.roleCount) }}</div></div>
                <div class="card stat"><div class="stat-label">无有效角色用户</div><div class="stat-value">{{ numText(state.audit.snapshot.summary?.usersWithoutRole) }}</div></div>
                <div class="card stat"><div class="stat-label">越权告警数</div><div class="stat-value">{{ numText(state.audit.snapshot.summary?.violationCount) }}</div></div>
              </div>

              <PanelCard class="mt-3" title="用户权限分配" sub="只读 · 来自 /permission/audit" flush>
                <el-table v-if="state.audit.snapshot.userPermissions.length" :data="state.audit.snapshot.userPermissions" size="small" style="width: 100%">
                  <el-table-column prop="userName" label="用户账号" min-width="110" />
                  <el-table-column prop="nickName" label="昵称" min-width="100" />
                  <el-table-column prop="roleName" label="角色" min-width="130">
                    <template #default="{ row }">{{ row.roleName || '—' }}</template>
                  </el-table-column>
                  <el-table-column prop="roleKey" label="权限字符" min-width="120">
                    <template #default="{ row }"><span class="mono">{{ row.roleKey || '—' }}</span></template>
                  </el-table-column>
                  <el-table-column prop="dataScopeLabel" label="数据范围" min-width="130" />
                  <el-table-column label="角色有效" width="90" align="center">
                    <template #default="{ row }">{{ row.validRole ? '是' : '否' }}</template>
                  </el-table-column>
                  <el-table-column prop="risk" label="风险说明" min-width="200" show-overflow-tooltip />
                </el-table>
                <el-empty v-else description="暂无数据" />
              </PanelCard>

              <PanelCard class="mt-3" title="角色概况" sub="只读 · 角色与实际用户数" flush>
                <el-table v-if="state.audit.snapshot.roles.length" :data="state.audit.snapshot.roles" size="small" style="width: 100%">
                  <el-table-column prop="roleName" label="角色名称" min-width="140" />
                  <el-table-column prop="roleKey" label="权限字符" min-width="140">
                    <template #default="{ row }"><span class="mono">{{ row.roleKey || '—' }}</span></template>
                  </el-table-column>
                  <el-table-column prop="dataScopeLabel" label="数据范围" min-width="140" />
                  <el-table-column prop="userCount" label="用户数" width="90" align="right">
                    <template #default="{ row }"><span class="mono">{{ numText(row.userCount) }}</span></template>
                  </el-table-column>
                </el-table>
                <el-empty v-else description="暂无数据" />
              </PanelCard>

              <PanelCard class="mt-3" title="越权告警" sub="只读 · 来自智能体审计记录中的权限拒绝" flush>
                <el-table v-if="state.audit.snapshot.violations.length" :data="state.audit.snapshot.violations" size="small" style="width: 100%">
                  <el-table-column label="时间" min-width="140">
                    <template #default="{ row }"><span class="mono">{{ row.createTime || '—' }}</span></template>
                  </el-table-column>
                  <el-table-column prop="userName" label="用户账号" min-width="110">
                    <template #default="{ row }">{{ row.userName || row.userId || '—' }}</template>
                  </el-table-column>
                  <el-table-column prop="toolName" label="工具" min-width="120">
                    <template #default="{ row }">{{ row.toolName || '—' }}</template>
                  </el-table-column>
                  <el-table-column label="状态" width="110" align="center">
                    <template #default="{ row }"><StatusTag :status="row.statusLabel" /></template>
                  </el-table-column>
                  <el-table-column prop="detail" label="详情" min-width="240" show-overflow-tooltip />
                </el-table>
                <el-empty v-else description="暂无越权告警" />
              </PanelCard>

              <PanelCard class="mt-3" title="风险提示">
                <ul v-if="state.audit.snapshot.risks.length" class="risk-list">
                  <li v-for="(risk, i) in state.audit.snapshot.risks" :key="i">{{ risk }}</li>
                </ul>
                <div v-else class="pad-tip">暂无风险提示。</div>
              </PanelCard>
            </template>
            <el-empty v-else-if="!state.audit.loading" description="暂无数据" />
          </div>
        </template>
      </el-tab-pane>

      <!-- ==================== Agent 调用日志 ==================== -->
      <el-tab-pane label="Agent 调用日志" name="agent">
        <div class="filter-bar">
          <el-input v-model="agentFilters.userName" placeholder="用户账号" clearable style="width: 150px"
            :prefix-icon="Search" @keyup.enter="apply('agent')" />
          <el-input v-model="agentFilters.intent" placeholder="意图" clearable style="width: 130px"
            @keyup.enter="apply('agent')" />
          <el-select v-model="agentFilters.status" style="width: 140px" @change="apply('agent')">
            <el-option label="全部状态" value="" />
            <el-option label="已回复" value="REPLY" />
            <el-option label="待确认" value="NEED_CONFIRM" />
            <el-option label="待补充参数" value="NEED_PARAMS" />
            <el-option label="失败" value="FAILED" />
          </el-select>
          <el-select v-model="agentFilters.side" style="width: 120px" @change="apply('agent')">
            <el-option label="全部端" value="" />
            <el-option label="农机端" value="ag" />
            <el-option label="管理端" value="ma" />
          </el-select>
          <el-date-picker v-model="agentFilters.dateRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间" end-placeholder="结束时间" style="width: 330px" />
          <el-button type="primary" @click="apply('agent')">查询</el-button>
          <el-button @click="resetAgent">重置</el-button>
        </div>

        <div v-if="state.agent.error" class="state-block">
          <el-alert :type="alertType(state.agent.error)" :closable="false" show-icon :title="state.agent.error.message" />
          <el-button v-if="state.agent.error.kind !== 'forbidden'" size="small" @click="loadAgent">重新加载</el-button>
        </div>
        <PanelCard v-else flush>
          <div v-loading="state.agent.loading">
            <el-table v-if="state.agent.list.length" :data="state.agent.list" size="small" style="width: 100%">
              <el-table-column label="时间" min-width="140">
                <template #default="{ row }"><span class="mono">{{ row.createTime || '—' }}</span></template>
              </el-table-column>
              <el-table-column prop="user" label="用户" min-width="100" />
              <el-table-column label="端" width="86" align="center">
                <template #default="{ row }">{{ sideText(row.side) }}</template>
              </el-table-column>
              <el-table-column prop="intent" label="意图" min-width="110">
                <template #default="{ row }">{{ row.intent || '—' }}</template>
              </el-table-column>
              <el-table-column prop="question" label="用户问题" min-width="200" show-overflow-tooltip />
              <el-table-column label="工具" min-width="120">
                <template #default="{ row }">{{ row.toolName || '—' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="110" align="center">
                <template #default="{ row }"><StatusTag :status="row.statusLabel" /></template>
              </el-table-column>
              <el-table-column label="耗时" width="90" align="right">
                <template #default="{ row }"><span class="mono">{{ msText(row.costMs) }}</span></template>
              </el-table-column>
              <el-table-column label="回执号" min-width="120">
                <template #default="{ row }"><span class="mono">{{ row.receiptNo || '—' }}</span></template>
              </el-table-column>
              <el-table-column prop="reply" label="回复" min-width="200" show-overflow-tooltip>
                <template #default="{ row }">{{ row.reply || '—' }}</template>
              </el-table-column>
              <el-table-column prop="error" label="错误信息" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">{{ row.error || '—' }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-else-if="!state.agent.loading" description="暂无数据" />
          </div>
          <div v-if="state.agent.total !== null" class="panel-pager">
            <el-pagination v-model:current-page="state.agent.page" v-model:page-size="state.agent.size"
              :page-sizes="[10, 20, 50]" :total="state.agent.total"
              layout="total, sizes, prev, pager, next" small
              @current-change="loadAgent" @size-change="onSize('agent')" />
          </div>
        </PanelCard>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="operDetailOpen" size="420px" title="操作日志详情">
      <template v-if="operDetail">
        <div class="detail-row"><span class="detail-label">操作模块</span><span>{{ operDetail.title || '—' }}</span></div>
        <div class="detail-row"><span class="detail-label">操作类型</span><span>{{ operDetail.businessTypeLabel || '—' }}</span></div>
        <div class="detail-row"><span class="detail-label">操作人</span><span>{{ operDetail.operator || '—' }}</span></div>
        <div class="detail-row"><span class="detail-label">操作时间</span><span class="mono">{{ operDetail.operTime || '—' }}</span></div>
        <div class="detail-row"><span class="detail-label">IP / 地点</span><span class="mono">{{ operDetail.ip || '—' }} · {{ operDetail.location || '—' }}</span></div>
        <div class="detail-row"><span class="detail-label">状态</span><StatusTag :status="operDetail.statusLabel" /></div>
        <div class="detail-row"><span class="detail-label">耗时</span><span class="mono">{{ msText(operDetail.costTime) }}</span></div>
        <div class="detail-row"><span class="detail-label">请求地址</span><span class="mono break">{{ operDetail.operUrl || '—' }}</span></div>
        <div class="detail-row"><span class="detail-label">错误信息</span><span class="break">{{ operDetail.errorMsg || '—' }}</span></div>
        <div class="hint-line mt-3">请求参数可能含敏感信息，本页不展示 operParam。</div>
      </template>
      <el-empty v-else description="暂无数据" />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { isAdminUser } from '@/stores/permission'
import {
  listSysUsers, listSysRoles, listLoginLogs, listOperationLogs,
  getPermissionAuditSnapshot, listAgentAuditLogs
} from '@/services/api'
import { roleMenuTreeselect, treeselect } from '@/api/system/menu'
import PageHeader from '@/components/PageHeader.vue'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'

const BUSINESS_TYPES = [
  { value: 0, label: '其他' },
  { value: 1, label: '新增' },
  { value: 2, label: '修改' },
  { value: 3, label: '删除' },
  { value: 4, label: '授权' },
  { value: 5, label: '导出' },
  { value: 6, label: '导入' },
  { value: 7, label: '强退' },
  { value: 8, label: '生成代码' },
  { value: 9, label: '清空数据' }
]

const tab = ref('users')
const isAdmin = computed(() => isAdminUser())

const listState = () => ({ loading: false, error: null, list: [], total: null, page: 1, size: 10 })
const state = reactive({
  users: listState(),
  roles: listState(),
  login: listState(),
  oper: listState(),
  agent: listState(),
  audit: { loading: false, error: null, snapshot: null }
})

const userFilters = reactive({ userName: '', phonenumber: '', status: '' })
const roleFilters = reactive({ roleName: '', roleKey: '', status: '' })
const loginFilters = reactive({ userName: '', ipaddr: '', status: '', dateRange: [] })
const operFilters = reactive({ title: '', operName: '', businessType: '', status: '', dateRange: [] })
const agentFilters = reactive({ userName: '', intent: '', status: '', side: '', dateRange: [] })

const operDetailOpen = ref(false)
const operDetail = ref(null)

/* ---------------- 菜单树与权限矩阵 ---------------- */
const menuTree = ref([])
const menuLoading = ref(false)
const menuError = ref(null)
const selectedRole = ref(null)
const checkedKeys = ref([])
const roleMenuLoading = ref(false)
const roleMenuError = ref(null)

const checkedKeySet = computed(() => new Set((checkedKeys.value || []).map((k) => String(k))))
function isGranted(node) {
  if (!node) return false
  if (checkedKeySet.value.has(String(node.id))) return true
  return Array.isArray(node.children) && node.children.some(isGranted)
}

/* ---------------- 通用工具 ---------------- */
function toError(err) {
  if (err?.status === 403 || err?.code === 403) {
    return { kind: 'forbidden', message: '当前账号没有访问该模块的权限。' }
  }
  if (err?.raw || /连接异常/.test(err?.message || '')) {
    return { kind: 'network', message: '网络或服务异常，加载失败，请稍后重试。' }
  }
  return { kind: 'error', message: err?.payload?.msg || err?.message || '加载失败，请稍后重试。' }
}
function alertType(error) {
  if (error.kind === 'forbidden') return 'warning'
  if (error.kind === 'network') return 'error'
  return 'error'
}
function numText(value) {
  return value === null || value === undefined || value === '' ? '—' : String(value)
}
function msText(value) {
  return value === null || value === undefined || value === '' ? '—' : `${value} ms`
}
function sideText(side) {
  return { ag: '农机端', ma: '管理端' }[side] || side || '—'
}

/** 筛选条件统一转查询参：只带后端契约字段，日期区间转为 beginTime/endTime。 */
function queryOf(filters, st) {
  const { dateRange, ...rest } = filters
  const query = { ...rest, pageNum: st.page, pageSize: st.size }
  if (Array.isArray(dateRange) && dateRange.length === 2) {
    query.beginTime = dateRange[0]
    query.endTime = dateRange[1]
  }
  return query
}

async function runList(key, fetcher) {
  const st = state[key]
  st.loading = true
  st.error = null
  try {
    const { list, total } = await fetcher()
    st.list = Array.isArray(list) ? list : []
    st.total = total
  } catch (err) {
    st.list = []
    st.total = null
    st.error = toError(err)
  } finally {
    st.loading = false
  }
}

/* ---------------- 列表加载 ---------------- */
function loadUsers() { return runList('users', () => listSysUsers(queryOf(userFilters, state.users))) }
function loadRoles() { return runList('roles', () => listSysRoles(queryOf(roleFilters, state.roles))) }
function loadLogin() { return runList('login', () => listLoginLogs(queryOf(loginFilters, state.login))) }
function loadOper() { return runList('oper', () => listOperationLogs(queryOf(operFilters, state.oper))) }
function loadAgent() { return runList('agent', () => listAgentAuditLogs(queryOf(agentFilters, state.agent))) }

async function loadAudit() {
  const st = state.audit
  st.loading = true
  st.error = null
  st.snapshot = null
  // 管理员门禁：登录信息未加载或非管理员时不发送请求（isAdminUser 未加载时为 false）。
  if (!isAdminUser()) {
    st.error = { kind: 'forbidden', message: '当前账号没有访问该模块的权限。' }
    st.loading = false
    return
  }
  try {
    st.snapshot = await getPermissionAuditSnapshot()
  } catch (err) {
    st.error = toError(err)
  } finally {
    st.loading = false
  }
}

async function loadMenuTree() {
  if (menuTree.value.length) return
  menuLoading.value = true
  menuError.value = null
  try {
    const res = await treeselect()
    menuTree.value = Array.isArray(res?.data) ? res.data : []
  } catch (err) {
    menuError.value = toError(err)
  } finally {
    menuLoading.value = false
  }
}

async function selectRole(role) {
  if (!role) return
  selectedRole.value = role
  checkedKeys.value = []
  roleMenuError.value = null
  roleMenuLoading.value = true
  try {
    const res = await roleMenuTreeselect(role.id)
    const checked = res?.checkedKeys ?? res?.data?.checkedKeys
    checkedKeys.value = Array.isArray(checked) ? checked : []
  } catch (err) {
    roleMenuError.value = toError(err)
  } finally {
    roleMenuLoading.value = false
  }
}

/* ---------------- 按需加载 ---------------- */
const loaders = {
  users: loadUsers,
  roles: loadRoles,
  login: loadLogin,
  oper: loadOper,
  audit: loadAudit,
  agent: loadAgent
}
const loaded = reactive({ users: false, roles: false, login: false, oper: false, audit: false, agent: false })

function ensure(key) {
  if (loaded[key]) return
  loaded[key] = true
  if (key === 'roles') loadMenuTree()
  loaders[key]()
}
watch(tab, (value) => ensure(value))
onMounted(() => ensure('users'))

/* ---------------- 查询 / 分页 / 重置 ---------------- */
function apply(key) {
  state[key].page = 1
  loaders[key]()
}
function onSize(key) {
  state[key].page = 1
  loaders[key]()
}
function resetUsers() {
  Object.assign(userFilters, { userName: '', phonenumber: '', status: '' })
  apply('users')
}
function resetRoles() {
  Object.assign(roleFilters, { roleName: '', roleKey: '', status: '' })
  apply('roles')
}
function resetLogin() {
  Object.assign(loginFilters, { userName: '', ipaddr: '', status: '', dateRange: [] })
  apply('login')
}
function resetOper() {
  Object.assign(operFilters, { title: '', operName: '', businessType: '', status: '', dateRange: [] })
  apply('oper')
}
function resetAgent() {
  Object.assign(agentFilters, { userName: '', intent: '', status: '', side: '', dateRange: [] })
  apply('agent')
}

/* ---------------- 顶部刷新 ---------------- */
const currentLoading = computed(() => {
  if (tab.value === 'audit') return state.audit.loading
  return state[tab.value]?.loading || false
})
function reloadCurrent() {
  loaders[tab.value]()
}

/* ---------------- 操作日志详情 ---------------- */
function openOperDetail(row) {
  operDetail.value = row
  operDetailOpen.value = true
}
</script>

<style scoped>
.filter-bar { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 10px; }
.filter-hint { font-size: 11.5px; color: var(--text-3); }
.grow { flex: 1; }
.state-block { display: flex; flex-direction: column; gap: 8px; align-items: flex-start; margin: 8px 0; }
.pad-tip { padding: 14px 16px; font-size: 12.5px; color: var(--text-2); }
.matrix-wrap { min-height: 80px; }
.panel-pager { padding: 6px 14px 10px; display: flex; justify-content: flex-end; }
.chk { color: var(--brand-600); font-weight: 700; }
.dash { color: var(--text-3); }
.stat { padding: 12px 14px; }
.stat-label { font-size: 11.5px; color: var(--text-2); }
.stat-value { font-size: 22px; font-weight: 700; margin-top: 4px; }
.risk-list { margin: 0; padding-left: 18px; font-size: 12.5px; line-height: 1.8; color: var(--text-2); }
.detail-row { display: flex; gap: 10px; padding: 8px 0; border-bottom: 1px dashed var(--border); font-size: 12.5px; }
.detail-label { flex: none; width: 88px; color: var(--text-2); }
.break { word-break: break-all; }
.mono { font-variant-numeric: tabular-nums; }
</style>
