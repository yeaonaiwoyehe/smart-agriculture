<template>
  <div class="page" v-loading="loading">
    <PageHeader title="权限与审计" code="MP-06" group="系统管理"
      desc="用户身份与数据范围由后端校验 · 全链路留痕，越权请求在接口层直接拦截并生成告警。">
      <template #actions><el-button :icon="Refresh" @click="load">刷新</el-button></template>
    </PageHeader>

    <div v-for="a in highAlarms" :key="a.id" class="alarm-bar">
      <span class="alarm-ico"><el-icon :size="16"><WarningFilled /></el-icon></span>
      <div class="alarm-main">
        <div class="alarm-title"><span class="tag red no-dot">高</span>{{ a.title }}</div>
        <div class="alarm-detail">{{ a.detail }}</div>
      </div>
      <span class="mono hint alarm-time">{{ a.at }}</span>
      <StatusTag :status="a.status" />
      <el-button type="danger" plain size="small" @click="handleAlarm(a)">处理</el-button>
    </div>

    <div class="grid cols-3 mt-2">
      <div v-for="a in normalAlarms" :key="a.id" class="card alarm-card">
        <div class="flex between center"><span class="tag" :class="levelTone(a.level)">{{ a.level }}风险</span><StatusTag :status="a.status" /></div>
        <div class="alarm-card-title">{{ a.title }}</div>
        <div class="alarm-card-desc">{{ a.detail }}</div>
        <div class="alarm-card-foot"><span class="mono hint">{{ a.at }}</span><el-button link type="primary" size="small" @click="handleAlarm(a)">处理</el-button></div>
      </div>
    </div>

    <el-tabs v-model="tab" class="mt-3">
      <el-tab-pane label="用户管理" name="users">
        <div class="filter-bar">
          <el-input v-model="uKey" placeholder="姓名 / 手机号" clearable style="width: 190px" :prefix-icon="Search" />
          <el-select v-model="uRole" style="width: 150px"><el-option label="全部角色" value="全部" /><el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.code" /></el-select>
          <span class="filter-hint grow">数据范围由角色决定，停用后立即失效</span>
          <el-button type="primary" :icon="Plus" @click="openUser">新增用户</el-button>
        </div>
        <PanelCard flush>
          <el-table :data="pagedUsers" size="small" style="width: 100%">
            <el-table-column label="姓名" min-width="118"><template #default="{ row }"><div class="cell-main">{{ row.name }}</div><div class="cell-sub mono">{{ row.id }}</div></template></el-table-column>
            <el-table-column prop="phone" label="手机号" min-width="126"><template #default="{ row }"><span class="mono">{{ row.phone }}</span></template></el-table-column>
            <el-table-column label="角色" min-width="118"><template #default="{ row }"><span class="tag" :class="roleTone(row.role)">{{ row.roleName }}</span></template></el-table-column>
            <el-table-column prop="district" label="数据范围" min-width="98" />
            <el-table-column label="状态" width="86" align="center"><template #default="{ row }"><el-switch :model-value="row.status === '启用'" @change="toggleStatus(row)" /></template></el-table-column>
            <el-table-column prop="lastLogin" label="最后登录" min-width="130"><template #default="{ row }"><span class="mono">{{ row.lastLogin }}</span></template></el-table-column>
            <el-table-column label="操作" width="94"><template #default="{ row }"><el-button link type="primary" size="small" @click="resetPwd(row)">重置密码</el-button></template></el-table-column>
          </el-table>
          <div class="panel-pager"><el-pagination v-model:current-page="uPage" :page-size="5" :total="filteredUsers.length" layout="total, prev, pager, next" small /></div>
        </PanelCard>
      </el-tab-pane>

      <el-tab-pane label="角色权限" name="roles">
        <div class="grid cols-4">
          <div v-for="r in roles" :key="r.id" class="card role-box">
            <div class="flex between center"><span class="strong">{{ r.name }}</span><span class="tag blue no-dot">{{ r.code }}</span></div>
            <div class="role-row"><span class="hint">数据范围</span><b>{{ r.scope }}</b></div>
            <div class="role-row"><span class="hint">用户数</span><b>{{ r.users }} 人</b></div>
            <div class="role-perms mt-2">
              <span v-for="p in r.perms.slice(0, 4)" :key="p" class="tag no-dot gray">{{ p }}</span>
              <span v-if="r.perms.length > 4" class="tag no-dot outline">+{{ r.perms.length - 4 }}</span>
            </div>
            <el-button size="small" type="primary" plain class="role-btn" @click="openRole(r)">配置权限</el-button>
          </div>
        </div>
        <PanelCard title="权限矩阵" sub="模块 × 角色 · 由角色权限项自动推导" class="mt-3" flush>
          <el-table :data="MATRIX" size="small" style="width: 100%">
            <el-table-column prop="module" label="模块" min-width="150" />
            <el-table-column v-for="r in roles" :key="r.id" :label="r.name" min-width="102" align="center">
              <template #default="{ row }"><span :class="hasPerm(r, row.keys) ? 'chk' : 'dash'">{{ hasPerm(r, row.keys) ? '✓' : '—' }}</span></template>
            </el-table-column>
          </el-table>
        </PanelCard>
      </el-tab-pane>

      <el-tab-pane label="登录日志" name="login">
        <div class="filter-bar">
          <el-select v-model="lResult" style="width: 120px"><el-option label="全部结果" value="全部" /><el-option label="成功" value="成功" /><el-option label="失败" value="失败" /></el-select>
          <el-input v-model="lKey" placeholder="用户 / IP" clearable style="width: 200px" :prefix-icon="Search" />
          <span class="filter-hint grow">失败登录已写入风控记录</span>
        </div>
        <PanelCard flush>
          <el-table :data="pagedLogin" size="small" style="width: 100%">
            <el-table-column prop="at" label="时间" min-width="132"><template #default="{ row }"><span class="mono">{{ row.at }}</span></template></el-table-column>
            <el-table-column label="用户" min-width="94"><template #default="{ row }"><div class="cell-main">{{ row.user }}</div><div class="cell-sub">{{ roleLabel(row.role) }}</div></template></el-table-column>
            <el-table-column prop="ip" label="IP" min-width="112"><template #default="{ row }"><span class="mono">{{ row.ip }}</span></template></el-table-column>
            <el-table-column prop="ua" label="登录方式" min-width="140" />
            <el-table-column prop="location" label="地点" min-width="106" />
            <el-table-column label="结果" width="82" align="center"><template #default="{ row }"><StatusTag :status="row.result" /></template></el-table-column>
          </el-table>
          <div class="panel-pager"><el-pagination v-model:current-page="lPage" :page-size="5" :total="filteredLogin.length" layout="total, prev, pager, next" small /></div>
        </PanelCard>
      </el-tab-pane>

      <el-tab-pane label="操作日志" name="operation">
        <div class="filter-bar">
          <el-select v-model="opModule" style="width: 150px"><el-option label="全部模块" value="全部" /><el-option v-for="m in opModules" :key="m" :label="m" :value="m" /></el-select>
          <span class="filter-hint grow">所有写操作均记录操作人、IP 与业务详情</span>
        </div>
        <PanelCard flush>
          <el-table :data="pagedOps" size="small" style="width: 100%">
            <el-table-column prop="at" label="时间" min-width="132"><template #default="{ row }"><span class="mono">{{ row.at }}</span></template></el-table-column>
            <el-table-column prop="user" label="操作人" width="86" />
            <el-table-column prop="action" label="操作" min-width="118"><template #default="{ row }"><span class="cell-main">{{ row.action }}</span></template></el-table-column>
            <el-table-column label="模块" width="110"><template #default="{ row }"><span class="tag no-dot">{{ row.module }}</span></template></el-table-column>
            <el-table-column prop="detail" label="详情" min-width="230" show-overflow-tooltip />
            <el-table-column label="结果" width="82" align="center"><template #default="{ row }"><StatusTag :status="row.result" /></template></el-table-column>
          </el-table>
          <div class="panel-pager"><el-pagination v-model:current-page="oPage" :page-size="5" :total="filteredOps.length" layout="total, prev, pager, next" small /></div>
        </PanelCard>
      </el-tab-pane>

      <el-tab-pane label="审计记录" name="audit">
        <div class="filter-bar">
          <el-input v-model="auKey" placeholder="对象 / 动作 / 原因" clearable style="width: 220px" :prefix-icon="Search" />
          <span class="filter-hint grow">审计记录不可编辑，仅可查询与导出</span>
        </div>
        <PanelCard flush>
          <el-table :data="pagedAudit" size="small" style="width: 100%">
            <el-table-column prop="at" label="时间" min-width="132"><template #default="{ row }"><span class="mono">{{ row.at }}</span></template></el-table-column>
            <el-table-column prop="target" label="对象" min-width="150"><template #default="{ row }"><span class="cell-main">{{ row.target }}</span></template></el-table-column>
            <el-table-column prop="action" label="动作" width="108" />
            <el-table-column prop="user" label="操作人" width="82" />
            <el-table-column label="变更前 → 变更后" min-width="196">
              <template #default="{ row }"><span class="before">{{ row.before }}</span><el-icon :size="11" class="arrow"><Right /></el-icon><span class="after">{{ row.after }}</span></template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="196" show-overflow-tooltip />
          </el-table>
          <div class="panel-pager"><el-pagination v-model:current-page="aPage" :page-size="5" :total="filteredAudit.length" layout="total, prev, pager, next" small /></div>
        </PanelCard>
      </el-tab-pane>

      <el-tab-pane label="Agent 调用日志" name="agent">
        <div class="filter-bar">
          <el-select v-model="aAgent" style="width: 136px"><el-option label="全部 Agent" value="全部" /><el-option v-for="a in agentOptions" :key="a" :label="a" :value="a" /></el-select>
          <el-select v-model="aResult" style="width: 120px"><el-option label="全部结果" value="全部" /><el-option label="成功" value="成功" /><el-option label="失败" value="失败" /></el-select>
          <span class="filter-hint grow">Agent 仅读取业务数据，写操作须人工审批</span>
        </div>
        <PanelCard flush>
          <el-table :data="pagedAgent" size="small" style="width: 100%">
            <el-table-column prop="at" label="时间" min-width="132"><template #default="{ row }"><span class="mono">{{ row.at }}</span></template></el-table-column>
            <el-table-column label="Agent" width="82"><template #default="{ row }"><span class="tag violet no-dot">{{ row.agent }}</span></template></el-table-column>
            <el-table-column prop="user" label="用户" width="72" />
            <el-table-column prop="question" label="问题" min-width="188" show-overflow-tooltip />
            <el-table-column label="结果" width="80" align="center"><template #default="{ row }"><StatusTag :status="row.result" /></template></el-table-column>
            <el-table-column label="耗时" width="76" align="right"><template #default="{ row }"><span class="num">{{ (row.ms / 1000).toFixed(1) }}s</span></template></el-table-column>
            <el-table-column label="Token" width="76" align="right"><template #default="{ row }"><span class="num">{{ row.tokens }}</span></template></el-table-column>
            <el-table-column label="工具" width="62" align="right"><template #default="{ row }"><span class="num">{{ row.toolCalls }}</span></template></el-table-column>
            <el-table-column label="错误信息" min-width="160" show-overflow-tooltip><template #default="{ row }"><span class="hint" style="font-size: 11px">{{ row.error || '—' }}</span></template></el-table-column>
          </el-table>
          <div class="panel-pager"><el-pagination v-model:current-page="gPage" :page-size="6" :total="filteredAgent.length" layout="total, prev, pager, next" small /></div>
        </PanelCard>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="userOpen" title="新增用户" width="460px" :close-on-click-modal="false">
      <el-form label-width="76px">
        <el-form-item label="姓名"><el-input v-model="newUser.name" placeholder="请输入真实姓名" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="newUser.phone" placeholder="11 位手机号" maxlength="11" /></el-form-item>
        <el-form-item label="角色"><el-select v-model="newUser.role" style="width: 100%"><el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.code" /></el-select></el-form-item>
        <el-form-item label="片区"><el-select v-model="newUser.district" style="width: 100%"><el-option v-for="d in districts" :key="d" :label="d" :value="d" /></el-select></el-form-item>
      </el-form>
      <div class="hint-line"><el-icon :size="13"><Lock /></el-icon><span>初始密码通过短信下发，首次登录必须修改；数据范围随角色自动收敛。</span></div>
      <template #footer><el-button @click="userOpen = false">取消</el-button><el-button type="primary" @click="saveUser">保存用户</el-button></template>
    </el-dialog>

    <el-drawer v-model="roleOpen" size="480px" :title="`配置权限 · ${roleEdit.name}`">
      <div class="drawer-label">数据范围</div>
      <el-radio-group v-model="roleEdit.scope" class="mt-2"><el-radio value="全公司">全公司</el-radio><el-radio value="本片区">本片区</el-radio><el-radio value="本人">本人</el-radio></el-radio-group>
      <div class="divider" />
      <div class="drawer-label">功能权限</div>
      <el-checkbox-group v-model="roleEdit.perms" class="perm-grid mt-2"><el-checkbox v-for="p in permOptions" :key="p" :value="p">{{ p }}</el-checkbox></el-checkbox-group>
      <div class="hint-line mt-3"><el-icon :size="13"><Lock /></el-icon><span>权限变更保存后立即生效，并写入审计记录；越权访问仍由接口层兜底拦截。</span></div>
      <template #footer><el-button @click="roleOpen = false">取消</el-button><el-button type="primary" @click="saveRole">保存权限</el-button></template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Lock, Plus, Refresh, Search, WarningFilled } from '@element-plus/icons-vue'
import { getAgentMonitor, getSystemData } from '@/services/api'
import PageHeader from '@/components/PageHeader.vue'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'

const loading = ref(false)
const tab = ref('users')
const users = ref([])
const roles = ref([])
const loginLogs = ref([])
const operationLogs = ref([])
const auditLogs = ref([])
const alarms = ref([])
const agentLogs = ref([])
const uKey = ref('')
const uRole = ref('全部')
const lResult = ref('全部')
const lKey = ref('')
const opModule = ref('全部')
const aAgent = ref('全部')
const aResult = ref('全部')
const auKey = ref('')
const uPage = ref(1)
const lPage = ref(1)
const oPage = ref(1)
const aPage = ref(1)
const gPage = ref(1)
const userOpen = ref(false)
const roleOpen = ref(false)
const newUser = reactive({ name: '', phone: '', role: 'leader', district: '河东片区' })
const roleEdit = reactive({ id: '', name: '', scope: '全公司', perms: [] })
const districts = ['全公司', '河东片区', '河西片区', '南滩片区', '北塬片区']
const MATRIX = [
  { module: '地块管理', keys: ['地块'] },
  { module: '作业调度', keys: ['派单'] },
  { module: '轨迹与核算', keys: ['轨迹', '面积'] },
  { module: '内部结算', keys: ['结算'] },
  { module: '统计报表', keys: ['报表'] },
  { module: 'Agent 中心', keys: ['Agent'] },
  { module: '系统与审计', keys: ['审计'] }
]

const highAlarms = computed(() => alarms.value.filter((a) => a.level === '高'))
const normalAlarms = computed(() => alarms.value.filter((a) => a.level !== '高'))
const opModules = computed(() => [...new Set(operationLogs.value.map((l) => l.module))])
const agentOptions = computed(() => [...new Set(agentLogs.value.map((l) => l.agent))])
const permOptions = computed(() => [...new Set(roles.value.flatMap((r) => r.perms))])
const filteredUsers = computed(() => {
  const k = uKey.value.trim()
  return users.value.filter((u) => (uRole.value === '全部' || u.role === uRole.value) && (!k || u.name.includes(k) || u.phone.includes(k)))
})
const filteredLogin = computed(() => {
  const k = lKey.value.trim()
  return loginLogs.value.filter((l) => (lResult.value === '全部' || l.result === lResult.value) && (!k || l.user.includes(k) || l.ip.includes(k)))
})
const filteredOps = computed(() => (opModule.value === '全部' ? operationLogs.value : operationLogs.value.filter((l) => l.module === opModule.value)))
const filteredAgent = computed(() => agentLogs.value.filter((l) => (aAgent.value === '全部' || l.agent === aAgent.value) && (aResult.value === '全部' || l.result === aResult.value)))
const pagedUsers = computed(() => filteredUsers.value.slice((uPage.value - 1) * 5, uPage.value * 5))
const pagedLogin = computed(() => filteredLogin.value.slice((lPage.value - 1) * 5, lPage.value * 5))
const pagedOps = computed(() => filteredOps.value.slice((oPage.value - 1) * 5, oPage.value * 5))
const filteredAudit = computed(() => {
  const k = auKey.value.trim()
  return k ? auditLogs.value.filter((l) => l.target.includes(k) || l.action.includes(k) || l.reason.includes(k) || l.user.includes(k)) : auditLogs.value
})
const pagedAudit = computed(() => filteredAudit.value.slice((aPage.value - 1) * 5, aPage.value * 5))
const pagedAgent = computed(() => filteredAgent.value.slice((gPage.value - 1) * 6, gPage.value * 6))

watch([uKey, uRole], () => (uPage.value = 1))
watch([lResult, lKey], () => (lPage.value = 1))
watch(opModule, () => (oPage.value = 1))
watch(auKey, () => (aPage.value = 1))
watch([aAgent, aResult], () => (gPage.value = 1))

const levelTone = (level) => ({ 高: 'red', 中: 'amber', 低: 'gray' }[level] || 'gray')
const roleTone = (code) => ({ admin: 'green', leader: 'blue', finance: 'violet', operator: 'gray' }[code] || 'gray')
const roleLabel = (code) => roles.value.find((r) => r.code === code)?.name || code
const hasPerm = (role, keys) => keys.some((k) => role.perms.some((p) => p.includes(k)))

async function load() {
  loading.value = true
  try {
    const [sys, monitor] = await Promise.all([getSystemData(), getAgentMonitor()])
    users.value = sys.users
    roles.value = sys.roles
    loginLogs.value = sys.loginLogs
    operationLogs.value = sys.operationLogs
    auditLogs.value = sys.auditLogs
    alarms.value = sys.alarms
    agentLogs.value = monitor.logs
  } finally {
    loading.value = false
  }
}
onMounted(load)

async function handleAlarm(a) {
  try { await ElMessageBox.confirm(`确认将告警「${a.title}」标记为已处理？处理动作将写入审计日志。`, '处理告警', { type: 'warning', confirmButtonText: '确认处理' }) } catch { return }
  a.status = '已处理'
  ElMessage.success('告警已处理，已记录审计日志')
}
async function toggleStatus(row) {
  const next = row.status === '启用' ? '停用' : '启用'
  try { await ElMessageBox.confirm(`确认将账号「${row.name}」${next}？${next === '停用' ? '停用后该用户将立即失去访问权限。' : ''}`, '状态变更', { type: 'warning', confirmButtonText: `确认${next}` }) } catch { return }
  row.status = next
  ElMessage.success(`账号「${row.name}」已${next}`)
}
async function resetPwd(row) {
  try { await ElMessageBox.confirm(`确认重置「${row.name}」的登录密码？新密码将通过短信下发。`, '重置密码', { type: 'warning' }) } catch { return }
  ElMessage.success('密码已重置，短信已发送（演示环境）')
}
function openUser() {
  Object.assign(newUser, { name: '', phone: '', role: 'leader', district: '河东片区' })
  userOpen.value = true
}
function saveUser() {
  if (!newUser.name.trim()) return ElMessage.warning('请输入姓名')
  if (!/^1\d{10}$/.test(newUser.phone)) return ElMessage.warning('请输入正确的 11 位手机号')
  const role = roles.value.find((r) => r.code === newUser.role)
  users.value.unshift({
    id: `U${String(users.value.length + 1).padStart(3, '0')}`, name: newUser.name.trim(), phone: newUser.phone,
    role: newUser.role, roleName: role?.name || '未分配', district: newUser.district,
    status: '启用', lastLogin: '—', avatar: newUser.name.trim().slice(0, 1)
  })
  ElMessage.success('用户已创建，初始密码已通过短信下发')
  userOpen.value = false
}
function openRole(r) {
  Object.assign(roleEdit, { id: r.id, name: r.name, scope: { 结算范围: '全公司', 仅本人: '本人' }[r.scope] || r.scope, perms: [...r.perms] })
  roleOpen.value = true
}
async function saveRole() {
  if (!roleEdit.perms.length) return ElMessage.warning('请至少保留一项功能权限')
  try { await ElMessageBox.confirm(`确认保存「${roleEdit.name}」的权限配置？变更将立即生效并写入审计日志。`, '保存角色权限', { type: 'warning', confirmButtonText: '确认保存' }) } catch { return }
  const target = roles.value.find((r) => r.id === roleEdit.id)
  if (target) { target.perms = [...roleEdit.perms]; target.scope = roleEdit.scope }
  ElMessage.success('角色权限已保存，已记录审计日志')
  roleOpen.value = false
}
</script>

<style scoped>
.alarm-bar { display: flex; align-items: center; gap: 12px; padding: 12px 15px; border-radius: var(--radius-md); background: #fff6f6; border: 1px solid #f4cccc; }
.alarm-ico { width: 32px; height: 32px; border-radius: 9px; display: grid; place-items: center; background: var(--red-100); color: var(--red-600); flex: none; }
.alarm-main { flex: 1; min-width: 0; }
.alarm-title { display: flex; align-items: center; gap: 7px; font-size: 13px; font-weight: 650; color: var(--text-1); }
.alarm-detail { margin-top: 2px; font-size: 11.5px; color: var(--text-2); }
.alarm-time { font-size: 10.5px; flex: none; }
.alarm-card { padding: 13px 14px; }
.alarm-card-title { margin-top: 9px; font-size: 12.5px; font-weight: 650; }
.alarm-card-desc { margin-top: 3px; font-size: 11.5px; color: var(--text-2); line-height: 1.55; min-height: 36px; }
.alarm-card-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 7px; padding-top: 7px; border-top: 1px dashed var(--border); font-size: 10.5px; }
.role-box { padding: 14px; }
.role-row { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; font-size: 12px; }
.role-perms { display: flex; flex-wrap: wrap; gap: 5px; min-height: 52px; align-content: flex-start; }
.role-btn { width: 100%; margin-top: 8px; }
.panel-pager { padding: 4px 14px 10px; }
.chk { color: var(--brand-600); font-weight: 700; }
.dash { color: var(--text-3); }
.drawer-label { font-size: 12px; font-weight: 650; }
.perm-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 2px 10px; }
.before { color: var(--text-2); }
.after { color: var(--brand-700); font-weight: 650; }
.arrow { color: var(--text-3); margin: 0 5px; }
</style>
