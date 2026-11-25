<template>
  <div class="page" v-loading="loading">
    <PageHeader title="内部结算管理" code="MP-04" group="业务管理"
      desc="按核算结果生成结算单并跟踪付款。面积 / 单价 / 金额全部来自业务库 · Agent 只解释不计算。">
      <template #actions>
        <el-button :icon="Download" @click="exportCsv">导出 Excel</el-button>
        <el-button type="primary" :icon="Plus" @click="openGenerate">生成结算单</el-button>
      </template>
    </PageHeader>

    <div class="hint-line">
      <el-icon :size="13"><Lock /></el-icon>
      <span>财务人员仅可访问结算与单价数据；生成、改价等写操作均需二次确认并记录审计日志。</span>
    </div>

    <div class="grid cols-4 mt-3">
      <StatCard label="本期结算单数" :value="summary.count" unit="份" icon="Tickets" foot="结算单来自后端 Settlement 表" />
      <StatCard label="已生成金额" :value="money(summary.generated)" unit="元" icon="Warning" tone="amber" foot="后端 status=0 已生成" />
      <StatCard label="已确认金额" :value="money(summary.confirmed)" unit="元" icon="DataLine" tone="blue" foot="后端 status=1 已确认" />
      <StatCard label="已付款金额" :value="money(summary.paid)" unit="元" icon="CircleCheck" tone="green" foot="后端 status=2 已付款（不等于实际到账）" />
    </div>

    <el-tabs v-model="tab" class="mt-3">
      <el-tab-pane label="结算单" name="list">
        <div class="filter-bar">
          <el-select v-model="filters.status" style="width: 120px" option-key="value" option-label="label" option-props="{ label: 'label', value: 'value' }">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <el-input v-model="filters.keyword" placeholder="结算单号 / 机手手机号" clearable style="width: 210px" :prefix-icon="Search" />
          <span class="filter-hint grow">后端金额 = Σ(有效作业面积 × 单价)，不含扣减；后端暂无扣减字段</span>
          <el-button @click="resetFilters">重置筛选</el-button>
        </div>
        <PanelCard flush>
          <el-table :data="pagedStatements" size="small" style="width: 100%">
            <el-table-column label="结算单号" min-width="148"><template #default="{ row }"><span class="table-link mono" @click="openDetail(row)">{{ row.id }}</span></template></el-table-column>
            <el-table-column label="机手手机号" min-width="110"><template #default="{ row }"><span class="cell-main">{{ row.phoneNumber || row.operatorName }}</span></template></el-table-column>
            <el-table-column label="周期" min-width="140"><template #default="{ row }"><div>{{ row.period }}</div></template></el-table-column>
            <el-table-column label="作业面积" width="100" align="right"><template #default="{ row }"><span class="num">{{ row.area !== null && row.area !== undefined ? row.area : '—' }} 亩</span></template></el-table-column>
            <el-table-column label="结算金额" width="118" align="right"><template #default="{ row }"><b class="num">¥ {{ money(row.amount) }}</b></template></el-table-column>
            <el-table-column label="状态" width="88" align="center"><template #default="{ row }"><StatusTag :status="statusLabel(row.statusRaw)" /></template></el-table-column>
            <el-table-column label="操作" width="146">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
                <el-button v-if="row.statusRaw === 0" link type="warning" size="small" @click="reviewStatement(row)">确认</el-button>
                <el-button v-if="row.statusRaw === 1" link type="success" size="small" @click="payStatement(row)">标记已付款</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="panel-pager"><el-pagination v-model:current-page="listPage" :page-size="6" :total="filteredStatements.length" layout="total, prev, pager, next" small /></div>
        </PanelCard>
      </el-tab-pane>

      <el-tab-pane label="单价规则" name="price">
        <div class="flex between center" style="margin-bottom: 10px">
          <span class="hint">单价变更需二次确认，保存后写入审计日志</span>
          <el-button type="primary" plain :icon="Plus" @click="openAddRule">新增规则</el-button>
        </div>
        <PanelCard flush>
          <el-table :data="pagedRules" size="small" style="width: 100%">
            <el-table-column prop="name" label="规则名称" min-width="150"><template #default="{ row }"><span class="cell-main">{{ row.name }}</span></template></el-table-column>
            <el-table-column prop="type" label="作业类型" width="100" />
            <el-table-column prop="grade" label="地块等级" width="100" />
            <el-table-column label="单价" width="110" align="right"><template #default="{ row }"><b class="num">{{ row.price }}</b> <span class="hint">{{ row.unit }}</span></template></el-table-column>
            <el-table-column prop="effective" label="生效日期" width="120" />
            <el-table-column label="状态" width="88" align="center"><template #default="{ row }"><StatusTag :status="row.status" /></template></el-table-column>
            <el-table-column label="操作" width="80"><template #default="{ row }"><el-button link type="primary" size="small" @click="openEditRule(row)">编辑</el-button></template></el-table-column>
          </el-table>
          <div class="panel-pager"><el-pagination v-model:current-page="rulePage" :page-size="6" :total="rules.length" layout="total, prev, pager, next" small /></div>
        </PanelCard>
      </el-tab-pane>

      <el-tab-pane label="扣减规则" name="deduct">
        <div class="hint-line" style="margin-bottom: 12px">
          <el-icon :size="13"><Document /></el-icon>
          <span>扣减规则直接关联制度条款，结算单中的扣减项可逐条回溯到条款原文。</span>
        </div>
        <div class="grid cols-2">
          <div v-for="d in deductions" :key="d.id" class="card deduct-card">
            <div class="flex between center"><span class="strong">{{ d.name }}</span><span class="tag violet no-dot">{{ d.id }}</span></div>
            <p class="hint mt-2">{{ d.desc }}</p>
            <div class="deduct-row"><span class="hint">扣减方式</span><b>{{ d.value }}</b></div>
            <div class="deduct-source"><el-icon :size="12"><Document /></el-icon>{{ d.source }}</div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="结算周期" name="cycle">
        <PanelCard title="结算周期配置" sub="配置项变更需二次确认；Agent 仅解释制度口径">
          <el-form label-width="96px" style="max-width: 620px">
            <el-form-item label="结算周期">
              <el-radio-group v-model="cycle.type"><el-radio-button value="自然月">自然月</el-radio-button><el-radio-button value="半月">半月</el-radio-button></el-radio-group>
            </el-form-item>
            <el-form-item label="出单日">
              <el-input-number v-model="cycle.issueDay" :min="1" :max="31" style="width: 140px" />
              <span class="hint" style="margin-left: 8px">次月第 {{ cycle.issueDay }} 个自然日出具结算单</span>
            </el-form-item>
            <el-form-item label="付款日">
              <el-input-number v-model="cycle.payDay" :min="1" :max="30" style="width: 140px" />
              <span class="hint" style="margin-left: 8px">出单后 {{ cycle.payDay }} 个工作日内完成付款</span>
            </el-form-item>
            <el-form-item label="宽限期">
              <el-input-number v-model="cycle.grace" :min="0" :max="15" style="width: 140px" />
              <span class="hint" style="margin-left: 8px">逾期 {{ cycle.grace }} 天内不计违约金</span>
            </el-form-item>
          </el-form>
          <div class="hint-line"><el-icon :size="13"><Lock /></el-icon><span>依据《合作农机作业结算管理办法》V2.1 第 6 条：次月 5 日前出具结算单，7 个工作日内完成付款。</span></div>
          <el-button type="primary" class="mt-3" @click="saveCycle">保存周期配置</el-button>
        </PanelCard>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="genOpen" title="生成结算单" width="520px" :close-on-click-modal="false">
      <el-form label-width="96px">
        <el-form-item label="机手手机号"><el-input v-model="genPhoneNumber" placeholder="如 13900001234" /></el-form-item>
        <el-form-item label="结算周期类型">
          <el-select v-model="genPeriodType" style="width: 100%">
            <el-option v-for="t in periodTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="周期起始"><el-date-picker v-model="genPeriodStart" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="周期结束"><el-date-picker v-model="genPeriodEnd" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
      </el-form>
      <div class="hint-line"><el-icon :size="13"><Warning /></el-icon><span>后端将校验周期内该机手已核算的有效轨迹并按 workType 匹配 PricingRule 单价；同机手 + 同周期重复生成会被拒绝。</span></div>
      <template #footer><el-button @click="genOpen = false">取消</el-button><el-button type="primary" :loading="genLoading" @click="confirmGenerate">确认生成</el-button></template>
    </el-dialog>

    <el-dialog v-model="priceOpen" title="修改单价" width="440px" :close-on-click-modal="false">
      <el-form label-width="88px">
        <el-form-item label="规则名称"><span class="strong">{{ priceEdit.name }}</span></el-form-item>
        <el-form-item label="当前单价"><span class="num">{{ priceEdit.basePrice }} {{ priceEdit.unit }}</span></el-form-item>
        <el-form-item label="调整后"><el-input-number v-model="priceEdit.price" :min="1" :max="999" style="width: 150px" /><span class="hint" style="margin-left: 8px">{{ priceEdit.unit }}</span></el-form-item>
      </el-form>
      <div class="hint-line"><el-icon :size="13"><Warning /></el-icon><span>单价调整仅影响后续生成的结算单，历史结算单金额不变。</span></div>
      <template #footer><el-button @click="priceOpen = false">取消</el-button><el-button type="primary" @click="savePrice">保存并留痕</el-button></template>
    </el-dialog>

    <el-dialog v-model="addOpen" title="新增单价规则" width="460px" :close-on-click-modal="false">
      <el-form label-width="88px">
        <el-form-item label="规则名称"><el-input v-model="newRule.name" placeholder="如：旋耕 · 一类地" /></el-form-item>
        <el-form-item label="作业类型"><el-select v-model="newRule.type" style="width: 100%"><el-option v-for="t in ['旋耕', '收割', '播种', '植保', '深松']" :key="t" :label="t" :value="t" /></el-select></el-form-item>
        <el-form-item label="地块等级"><el-select v-model="newRule.grade" style="width: 100%"><el-option v-for="g in ['一类', '二类', '三类', '全部']" :key="g" :label="g" :value="g" /></el-select></el-form-item>
        <el-form-item label="单价"><el-input-number v-model="newRule.price" :min="1" :max="999" style="width: 150px" /><span class="hint" style="margin-left: 8px">元/亩</span></el-form-item>
      </el-form>
      <template #footer><el-button @click="addOpen = false">取消</el-button><el-button type="primary" @click="saveNewRule">保存规则</el-button></template>
    </el-dialog>

    <el-drawer v-model="detailOpen" size="720px" :with-header="false">
      <div v-if="detail">
        <div class="detail-head">
          <div><div class="detail-title">{{ detail.id }}</div><div class="detail-sub">{{ detail.phoneNumber }} · {{ detail.periodStart }} ~ {{ detail.periodEnd }}</div></div>
          <StatusTag :status="statusLabel(detail.statusRaw)" style="margin-left: auto" />
        </div>
        <div class="grid cols-3">
          <div class="mini-metric"><div class="v num">{{ money(detail.area) }}</div><div class="l">作业面积（亩）</div></div>
          <div class="mini-metric"><div class="v num">¥ {{ money(detail.amount) }}</div><div class="l">结算金额（元）</div></div>
          <div class="mini-metric"><div class="v num">{{ detail.periodType === 1 ? '月度' : detail.periodType === 2 ? '季度' : '—' }}</div><div class="l">结算周期</div></div>
        </div>
        <div class="row-head mt-3"><span class="strong">结算明细</span><span class="hint" style="font-size: 11px">后端 SettlementItem：轨迹 + 地块 + 面积 × 单价 = 金额</span></div>
        <el-table :data="detailItems" size="small" style="width: 100%">
          <el-table-column label="轨迹ID" min-width="140"><template #default="{ row }"><span class="mono">{{ row.trajectoryInfoId || '—' }}</span></template></el-table-column>
          <el-table-column label="地块ID" width="100"><template #default="{ row }"><span class="mono">{{ row.plotId || '—' }}</span></template></el-table-column>
          <el-table-column label="类型" width="72"><template #default="{ row }">{{ row.type || '—' }}</template></el-table-column>
          <el-table-column label="面积" width="80" align="right"><template #default="{ row }"><span class="num">{{ row.area !== null && row.area !== undefined ? row.area : '—' }}</span></template></el-table-column>
          <el-table-column label="单价" width="80" align="right"><template #default="{ row }"><span class="num">{{ row.price !== null && row.price !== undefined ? row.price : '—' }}</span></template></el-table-column>
          <el-table-column label="金额" width="96" align="right"><template #default="{ row }"><span class="num">{{ money(row.amount) }}</span></template></el-table-column>
          <el-table-column label="时间" min-width="130"><template #default="{ row }"><span class="hint" style="font-size: 11px">{{ row.createTime || '—' }}</span></template></el-table-column>
        </el-table>
        <div v-if="!detailItems.length" class="empty" style="padding: 20px; text-align: center; color: var(--text-3); font-size: 12.5px">
          当前结算单无明细记录。后端可能尚未生成本次 SettlementItem，或轨迹已被删除。
        </div>
        <div class="formula">
          <div class="formula-title">金额构成</div>
          <div class="formula-line"><span>后端 totalAmount</span><b class="num">¥ {{ money(detail.amount) }}</b></div>
          <div class="hint" style="margin-top: 6px; font-size: 11px">后端 SettlementServiceImpl 生成本次 Settlement 时：totalAmount = Σ(每条 SettlementItem.area × unitPrice)。后端暂无扣减字段、单价汇总等二次字段，前端不自行推导。</div>
        </div>
        <div class="row-head mt-3"><span class="strong">状态流转</span><span class="hint" style="font-size: 11px">已生成(0) → 已确认(1) → 已付款(2)</span></div>
        <el-steps :active="stepActive" align-center finish-status="success" style="margin-top: 8px">
          <el-step title="已生成" :description="(detail.createdAt || '').slice(0, 10)" />
          <el-step title="已确认" :description="detail.statusRaw === 1 || detail.statusRaw === 2 ? '后端已更新' : '待确认'" />
          <el-step title="已付款" :description="detail.statusRaw === 2 ? '后端已更新' : '未付款'" />
        </el-steps>
      </div>
    </el-drawer>

    <el-drawer v-model="aiOpen" title="结算 AI 助手" size="480px" append-to-body>
      <AgentSidePanel agent-id="MA-03" title="结算 AI 助手" desc="引用制度原文解释扣减与周期，不计算金额" :height="600" />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Lock, MagicStick, Plus, Search, Warning } from '@element-plus/icons-vue'
import { confirmStatement, generateStatements, getStatement, getStatementItems, listPriceRules, listStatements, updatePriceRule } from '@/services/api'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import AgentSidePanel from '@/components/AgentSidePanel.vue'

const loading = ref(false)
const tab = ref('list')
const statements = ref([])
const rules = ref([])
const deductions = ref([])
const detail = ref(null)
const detailItems = ref([])
const detailOpen = ref(false)
const aiOpen = ref(false)
const genOpen = ref(false)
const genLoading = ref(false)
const genPhoneNumber = ref('')
const genPeriodType = ref(1)
const genPeriodStart = ref('')
const genPeriodEnd = ref('')
const priceOpen = ref(false)
const priceEdit = reactive({ id: '', name: '', workType: 0, grade: '', minArea: 0, maxArea: 0, price: 0, basePrice: 0, unit: '元/亩', status: 0 })
const addOpen = ref(false)
const newRule = reactive({ name: '', workType: '', grade: '一类', minArea: 0, maxArea: 0, price: 45, status: 0 })
const cycle = reactive({ type: '半月', issueDay: 5, payDay: 7, grace: 3 })
const filters = reactive({ status: '', keyword: '' })
const statusOptions = [
  { label: '全部', value: '' },
  { label: '已生成', value: 0 },
  { label: '已确认', value: 1 },
  { label: '已付款', value: 2 }
]
const periodTypeOptions = [
  { label: '月度', value: 1 },
  { label: '季度', value: 2 }
]
const listPage = ref(1)
const rulePage = ref(1)
const money = (n) => {
  if (n === null || n === undefined || n === '') return '—'
  const num = Number(n)
  if (!Number.isFinite(num)) return '—'
  return num.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}
const statusLabel = (v) => ({ 0: '已生成', 1: '已确认', 2: '已付款' }[v] ?? '')

const summary = computed(() => {
  const list = statements.value
  const sum = (statusRaw) => list
    .filter((s) => s.statusRaw === statusRaw)
    .reduce((a, b) => a + (Number.isFinite(Number(b.amount)) ? Number(b.amount) : 0), 0)
  return {
    count: list.length,
    generated: sum(0),
    confirmed: sum(1),
    paid: sum(2),
    totalAmount: list.reduce((a, b) => a + (Number.isFinite(Number(b.amount)) ? Number(b.amount) : 0), 0)
  }
})
const filteredStatements = computed(() => {
  const k = filters.keyword.trim()
  return statements.value.filter((s) => {
    if (filters.status !== '' && s.statusRaw !== null && s.statusRaw !== filters.status) return false
    if (filters.period !== undefined && filters.period !== '全部' && s.period !== filters.period) return false
    return !k || s.id.includes(k) || s.phoneNumber.includes(k) || s.operatorName.includes(k)
  })
})
const pagedStatements = computed(() => filteredStatements.value.slice((listPage.value - 1) * 6, listPage.value * 6))
const pagedRules = computed(() => rules.value.slice((rulePage.value - 1) * 6, rulePage.value * 6))
/** 后端 status: 0 已生成 / 1 已确认 / 2 已付款 → 步骤 active index */
const stepActive = computed(() => {
  const raw = detail.value?.statusRaw
  if (raw === 2) return 3
  if (raw === 1) return 2
  if (raw === 0) return 1
  return 0
})
const loadList = async () => {
  loading.value = true
  try { statements.value = await listStatements({ status: filters.status, keyword: filters.keyword }) }
  finally { loading.value = false }
}
async function load() {
  loading.value = true
  try {
    const [list, price] = await Promise.all([loadList(), listPriceRules()])
    statements.value = list
    rules.value = price.rules || []
    deductions.value = price.deductions || []
  } finally {
    loading.value = false
  }
}
onMounted(load)

function resetFilters() {
  filters.status = ''
  filters.period = undefined
  filters.keyword = ''
  listPage.value = 1
}
async function openDetail(row) {
  // 后端详情接口 GET /settlement/{id} 接收 Long 主键（字符串序列化）
  const id = row.raw?.id ?? row.id
  detail.value = await getStatement(id, statements.value)
  // 结算明细独立接口 GET /settlement/items/{id}
  detailItems.value = await getStatementItems(id).catch(() => [])
  detailOpen.value = true
}
async function reviewStatement(row) {
  try { await ElMessageBox.confirm(`确认结算单 ${row.id} 进入「已确认」状态？后端记录变更时间和操作人。`, '结算确认', { type: 'warning', confirmButtonText: '确认变更' }) } catch { return }
  try {
    await confirmStatement(row.raw?.id ?? row.id, 1)
    ElMessage.success(`结算单 ${row.id} 已标记为已确认`)
  } catch (error) {
    ElMessage.error('结算状态更新失败：' + (error?.payload?.msg || error?.message || '未知错误'))
    return
  }
  await loadList()
  if (detail.value?.id === row.id) detail.value = statements.value.find((s) => s.id === row.id) || detail.value
}
async function payStatement(row) {
  try { await ElMessageBox.confirm(`确认结算单 ${row.id} 变更为「已付款」状态？后端无付款凭证回填字段，请勿视为实际到账。`, '标记已付款', { type: 'warning', confirmButtonText: '确认变更' }) } catch { return }
  try {
    await confirmStatement(row.raw?.id ?? row.id, 2)
    ElMessage.success(`结算单 ${row.id} 已标记为已付款`)
  } catch (error) {
    ElMessage.error('结算状态更新失败：' + (error?.payload?.msg || error?.message || '未知错误'))
    return
  }
  await loadList()
  if (detail.value?.id === row.id) detail.value = statements.value.find((s) => s.id === row.id) || detail.value
}
function openGenerate() {
  genPhoneNumber.value = ''
  genPeriodType.value = 1
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  genPeriodStart.value = `${y}-${m}-01`
  genPeriodEnd.value = `${y}-${m}-${String(new Date(y, m, 0).getDate()).padStart(2, '0')}`
  genOpen.value = true
}
async function confirmGenerate() {
  const phone = genPhoneNumber.value.trim()
  if (!phone) return ElMessage.warning('请输入机手手机号')
  if (!genPeriodStart.value || !genPeriodEnd.value) return ElMessage.warning('请填写结算周期')
  try {
    await ElMessageBox.confirm(`将为手机号 ${phone} 生成 ${genPeriodStart.value} ~ ${genPeriodEnd.value} 的结算单。后端会校验周期内是否存在该机手已核算的有效轨迹，且防重复生成。`, '生成结算单', { type: 'warning', confirmButtonText: '确认生成' })
  } catch { return }
  genLoading.value = true
  try {
    const res = await generateStatements({ periodType: genPeriodType.value, periodStart: genPeriodStart.value, periodEnd: genPeriodEnd.value, phoneNumber: phone })
    ElMessage.success(res.message)
    genOpen.value = false
    await loadList()
  } catch (error) {
    const msg = error?.payload?.msg || error?.message || '生成结算单失败'
    ElMessage.error('生成结算单失败：' + msg)
  } finally {
    genLoading.value = false
  }
}
function openEditRule(row) {
  Object.assign(priceEdit, { id: row.id, name: row.name, workType: row.workType || 0, grade: row.grade || '', minArea: row.minArea || 0, maxArea: row.maxArea || null, price: row.price, basePrice: row.price, unit: row.unit || '元/亩', status: row.status === '停用' ? 1 : 0 })
  priceOpen.value = true
}
async function savePrice() {
  if (!priceEdit.price || priceEdit.price <= 0) return ElMessage.warning('请输入有效单价')
  try { await ElMessageBox.confirm(`将「${priceEdit.name}」单价由 ${priceEdit.basePrice} 调整为 ${priceEdit.price} 元/亩？变更将记录审计日志。`, '确认修改单价', { type: 'warning', confirmButtonText: '确认修改' }) } catch { return }
  try {
    const res = await updatePriceRule({ id: priceEdit.id, price: Number(priceEdit.price), workType: Number(priceEdit.workType), grade: priceEdit.grade, minArea: Number(priceEdit.minArea || 0), maxArea: priceEdit.maxArea == null ? null : Number(priceEdit.maxArea), status: priceEdit.status })
    ElMessage.success(res.message)
    priceOpen.value = false
    await listPriceRules().then((p) => { rules.value = p.rules || [] })
  } catch (error) {
    ElMessage.error('单价规则更新失败：' + (error?.payload?.msg || error?.message || '未知错误'))
  }
}
function openAddRule() {
  Object.assign(newRule, { name: '', workType: '', grade: '一类', minArea: 0, maxArea: 0, price: 45, status: 0 })
  addOpen.value = true
}
async function saveNewRule() {
  if (!newRule.name.trim() || !newRule.price) return ElMessage.warning('请填写规则名称与单价')
  // 后端 PricingRuleController 提供 POST 新增，但本轮不动 write 权限和按钮 UI
  ElMessage.info('后端已提供 POST /pricingRule 新增接口，本轮未启用新增按钮的真实提交。请由 S10 补接。')
  addOpen.value = false
}
async function saveCycle() {
  // 后端无结算周期配置表和 API；当前页面的 cycle 表单为纯前端展示
  ElMessage.warning('后端暂无结算周期配置接口。结算周期由 SettlementServiceImpl.generate(periodType/Start/End) 参数决定，无需全局配置。')
}

function downloadCsv(rows, filename) {
  const csv = '\uFEFF' + rows.map((r) => r.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8;' }))
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
function exportCsv() {
  const rows = [
    ['结算单号', '机手手机号', '周期', '作业面积(亩)', '结算金额(元)', '状态'],
    ...filteredStatements.value.map((s) => [s.id, s.phoneNumber || s.operatorName, s.period, s.area !== null && s.area !== undefined ? s.area : '', s.amount !== null && s.amount !== undefined ? s.amount : '', statusLabel(s.statusRaw)])
  ]
  downloadCsv(rows, `结算单_${new Date().toISOString().slice(0, 10)}.csv`)
  ElMessage.success(`已导出 ${rows.length - 1} 条结算记录`)
}
</script>

<style scoped>
.panel-pager { padding: 4px 14px 10px; }
.warn { color: var(--amber-600); }
.deduct-card { padding: 14px 16px; }
.deduct-row { display: flex; align-items: center; justify-content: space-between; padding: 8px 0; margin-top: 4px; border-top: 1px dashed var(--border); font-size: 12px; }
.deduct-source { display: flex; align-items: center; gap: 5px; padding: 7px 9px; border-radius: 7px; background: var(--surface-2); color: var(--text-3); font-size: 11px; }
.row-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.formula { margin-top: 14px; padding: 12px 14px; border-radius: 10px; background: var(--surface-2); border: 1px solid var(--border); }
.formula-title { font-size: 12px; font-weight: 650; margin-bottom: 7px; }
.formula-line { display: flex; justify-content: space-between; padding: 4px 0; font-size: 12px; color: var(--text-2); }
.formula-line.total { margin-top: 4px; padding-top: 7px; border-top: 1px solid var(--border); color: var(--text-1); font-weight: 650; font-size: 13px; }
</style>
