<template>
  <div class="page" v-loading="loading">
    <PageHeader title="作业调度管理" code="MP-03" group="业务管理" desc="汇聚后端真实「作业需求」数据（/farmWork），依据地块、面积与截止时间人工确认调度；后端暂无机手档案与派单接口，相关能力见页面提示。">
      <template #actions>
        <el-button :icon="Plus" @click="openCreate">发布作业需求</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="load()">刷新</el-button>
      </template>
    </PageHeader>

    <!-- 后端能力说明：不使用假成功，明确告知哪些能力缺失 -->
    <el-alert type="info" :closable="false" class="mt-2" show-icon>
      <template #title>
        真实数据源：<b>POST /farmWork/list</b>（作业需求，status: 0=可接单 / 1=已结束）。
        后端<b>不存在</b> DispatchController，也没有农机派单、机手档案、作业进度与批量调度接口，
        因此「建议农机 / 批量派单 / 重新分配」在本系统内为<span class="warn-text">后端待补充</span>，页面不再展示模拟值。
      </template>
    </el-alert>

    <div class="grid cols-4 mt-3">
      <StatCard label="可接单" :value="stat.available" unit="条" icon="Promotion" tone="amber" foot="status = 0" />
      <StatCard label="已结束" :value="stat.ended" unit="条" icon="Clock" tone="blue" foot="status = 1（到期/下架）" />
      <StatCard label="合计面积" :value="stat.area" unit="亩" icon="MapLocation" foot="当前页求和" />
      <StatCard label="即将到期" :value="stat.soon" unit="条" icon="Warning" tone="red" foot="48 小时内到期" />
    </div>

    <PanelCard title="作业需求调度" sub="按后端真实字段展示 · 支持列表 / 看板 / 地图三种查看方式" class="mt-3">
      <template #extra>
        <div class="seg">
          <button :class="{ active: view === 'list' }" @click="view = 'list'">列表</button>
          <button :class="{ active: view === 'kanban' }" @click="view = 'kanban'">看板</button>
          <button :class="{ active: view === 'map' }" @click="view = 'map'">地图</button>
        </div>
      </template>

      <div class="filter-bar flat">
        <el-select v-model="statusFilter" size="small" style="width: 118px" @change="handleQuery">
          <el-option label="全部状态" value="" />
          <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-select v-model="typeFilter" size="small" style="width: 116px" @change="handleQuery" :loading="workTypeLoading">
          <el-option label="全部作业类型" value="" />
          <el-option v-for="t in workTypeOptions" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
        <el-input v-model="keyword" size="small" clearable placeholder="作业名称 / 地块名" style="width: 190px" @keyup.enter="handleQuery" @clear="handleQuery">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button size="small" type="primary" @click="handleQuery">查询</el-button>
        <span class="filter-hint grow">共 {{ total === null ? `${tasks.length}（后端未返回总数）` : total }} 条</span>
      </div>

      <!-- 列表 -->
      <el-table v-if="view === 'list'" :data="tasks" size="small" row-key="id" style="width: 100%">
        <el-table-column label="作业名称 / ID" min-width="150">
          <template #default="{ row }">
            <div class="cell-main">{{ row.name || '-' }}</div>
            <div class="cell-sub mono" style="font-size: 10px">{{ row.id }}</div>
          </template>
        </el-table-column>
        <el-table-column label="地块 / 作业类型" min-width="150">
          <template #default="{ row }">
            <div class="cell-main">{{ joinText(row.landPlotName) }}</div>
            <div class="cell-sub">{{ joinText(row.workTypeName) }} · {{ row.area }} 亩</div>
          </template>
        </el-table-column>
        <el-table-column label="作物" min-width="86"><template #default="{ row }"><span class="cell-sub">{{ joinText(row.cropName) }}</span></template></el-table-column>
        <el-table-column label="联系方式" width="108"><template #default="{ row }"><span class="mono" style="font-size: 11px">{{ row.phoneNumber || '-' }}</span></template></el-table-column>
        <el-table-column label="截止时间" width="126"><template #default="{ row }"><span class="mono" style="font-size: 11px">{{ fmtTime(row.endTime) }}</span></template></el-table-column>
        <el-table-column label="状态" width="80"><template #default="{ row }"><StatusTag :status="statusText(row.status)" /></template></el-table-column>
        <el-table-column label="操作" width="86" align="right">
          <template #default="{ row }">
            <el-dropdown trigger="click" @command="(cmd) => onRowCommand(cmd, row)">
              <el-button link type="primary" size="small" class="op-btn">操作<el-icon><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="detail">详情</el-dropdown-item>
                  <el-dropdown-item command="edit" :disabled="isEnded(row.status)">编辑</el-dropdown-item>
                  <el-dropdown-item command="revoke" :disabled="isEnded(row.status)">下架</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- 看板：只有后端真实的两态 -->
      <div v-else-if="view === 'kanban'" class="kanban">
        <div v-for="col in kanban" :key="col.status" class="kb-col">
          <div class="kb-head"><span>{{ col.label }}</span><span class="kb-count">{{ col.items.length }}</span></div>
          <div class="kb-body">
            <div v-for="t in col.items" :key="t.id" class="kb-card" @click="openDetail(t)">
              <div class="kb-title">{{ t.name || t.id }}</div>
              <div class="kb-meta">{{ joinText(t.landPlotName) }} · {{ t.area }} 亩</div>
              <div class="kb-meta">{{ joinText(t.workTypeName) }}</div>
              <div class="kb-dead">截止 {{ fmtTime(t.endTime) }}</div>
            </div>
            <div v-if="!col.items.length" class="kb-empty">暂无</div>
          </div>
        </div>
      </div>

      <!-- 地图：按地块关联展示 -->
      <div v-else>
        <FarmMap :lands="lands" :machines="[]" :selected-land="selectedLandId" :height="428" @select-land="onMapLand" />
        <div class="hint-line mt-2"><el-icon :size="13"><InfoFilled /></el-icon><span>点击地块可查看其关联的作业需求。已选：{{ selectedLand?.name || '未选择' }}。农机实时位置接口后端未提供，故图中不展示农机。</span></div>
      </div>

      <div class="pager-wrap" v-if="total !== null">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :current-page="pageNum"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </PanelCard>

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="formVisible" :title="formMode === 'edit' ? '编辑作业需求' : '发布作业需求'" width="620px">
      <el-alert type="warning" :closable="false" style="margin-bottom: 12px" show-icon>
        <template #title>后端 FarmWorkDTO 仅接收：名称、亩数、截止时间、联系方式、地块 / 作物 / 作业类型。截止时间必须晚于当前时间。</template>
      </el-alert>
      <el-form label-width="88px">
        <div class="form-grid">
          <el-form-item label="作业名称" required>
            <el-input v-model="form.name" maxlength="40" show-word-limit placeholder="如：河东村小麦收割" />
          </el-form-item>
          <el-form-item label="亩数" required>
            <el-input-number v-model="form.area" :min="0.01" :precision="2" :step="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="截止时间" required>
            <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY-MM-DD HH:mm" placeholder="必须晚于当前时间" style="width: 100%" />
          </el-form-item>
          <el-form-item label="联系方式" required>
            <el-input v-model="form.phoneNumber" maxlength="20" placeholder="11 位手机号" />
          </el-form-item>
          <el-form-item label="地块" class="full">
            <el-select v-model="form.landPlotId" multiple filterable collapse-tags collapse-tags-tooltip placeholder="可多选地块" style="width: 100%">
              <el-option v-for="l in lands" :key="l.id" :label="`${l.name} · ${l.area}亩`" :value="l.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="作物" class="full">
            <el-select v-model="form.cropId" multiple filterable collapse-tags collapse-tags-tooltip :placeholder="cropHint" style="width: 100%" :loading="cropLoading">
              <el-option v-for="c in cropOptions" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="作业类型" class="full">
            <el-select v-model="form.workTypeId" multiple filterable collapse-tags collapse-tags-tooltip :placeholder="workTypeHint" style="width: 100%" :loading="workTypeLoading">
              <el-option v-for="t in workTypeOptions" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button :disabled="submitting" @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">{{ formMode === 'edit' ? '保存修改' : '发布' }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" title="作业需求详情" size="560px">
      <template v-if="detail">
        <div class="detail-head">
          <div style="min-width: 0">
            <div class="detail-title">{{ detail.name || '-' }}</div>
            <div class="detail-sub mono" style="font-size: 10.5px">{{ detail.id }}</div>
          </div>
          <div style="margin-left: auto"><StatusTag :status="statusText(detail.status)" /></div>
        </div>
        <div class="detail-grid">
          <div class="detail-field"><div class="detail-label">亩数</div><div class="detail-value num">{{ detail.area }} 亩</div></div>
          <div class="detail-field"><div class="detail-label">截止时间</div><div class="detail-value mono">{{ fmtTime(detail.endTime) }}</div></div>
          <div class="detail-field"><div class="detail-label">联系方式</div><div class="detail-value mono">{{ detail.phoneNumber || '-' }}</div></div>
          <div class="detail-field"><div class="detail-label">发布人</div><div class="detail-value">{{ detail.createBy || '-' }}</div></div>
          <div class="detail-field"><div class="detail-label">地块</div><div class="detail-value">{{ joinText(detail.landPlotName) }}</div></div>
          <div class="detail-field"><div class="detail-label">作业类型</div><div class="detail-value">{{ joinText(detail.workTypeName) }}</div></div>
          <div class="detail-field"><div class="detail-label">作物</div><div class="detail-value">{{ joinText(detail.cropName) }}</div></div>
          <div class="detail-field"><div class="detail-label">地块地址</div><div class="detail-value">{{ joinText(detail.landAddress) }}</div></div>
          <div class="detail-field"><div class="detail-label">创建时间</div><div class="detail-value mono">{{ fmtTime(detail.createTime) }}</div></div>
          <div class="detail-field"><div class="detail-label">更新时间</div><div class="detail-value mono">{{ fmtTime(detail.updateTime) }}</div></div>
        </div>

        <div class="divider" />
        <div class="panel-title" style="font-size: 13px"><span class="dot" />可审计的时间戳</div>
        <el-timeline style="padding-left: 2px; margin-top: 12px">
          <el-timeline-item type="success" :hollow="!detail.createTime">
            <div class="strong" style="font-size: 12px">创建</div>
            <div class="hint" style="font-size: 11px">{{ fmtTime(detail.createTime) }} · {{ detail.createBy || '发布人未记录' }}</div>
          </el-timeline-item>
          <el-timeline-item type="primary" :hollow="!detail.updateTime">
            <div class="strong" style="font-size: 12px">最近更新</div>
            <div class="hint" style="font-size: 11px">
              {{ detail.updateTime ? `${fmtTime(detail.updateTime)} · ${detail.updateBy || '操作人未记录'}` : '暂无更新记录' }}
            </div>
          </el-timeline-item>
          <el-timeline-item :type="isEnded(detail.status) ? 'danger' : 'info'" :hollow="!isEnded(detail.status)">
            <div class="strong" style="font-size: 12px">当前状态</div>
            <div class="hint" style="font-size: 11px">{{ statusText(detail.status) }}（后端仅存储 0/1 两态，无派单/接单/作业中流转记录）</div>
          </el-timeline-item>
        </el-timeline>

        <el-alert type="warning" :closable="false" show-icon style="margin-top: 8px">
          <template #title>后端未存储「指派农机 / 机手 / 作业进度 / 优先级」，因此本详情不展示这些字段，也不做任何推算填充。</template>
        </el-alert>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="warning" plain :disabled="isEnded(detail?.status)" @click="revoke(detail)">下架</el-button>
        <el-button type="primary" :disabled="isEnded(detail?.status)" @click="openEdit(detail)">编辑</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, InfoFilled, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createTask, listLands, listTasks, listWorkTypes, revokeTask, updateTask } from '@/services/api'
import { listCrops } from '@/api/crop'
import { toLongIdList, toStringId, toStringIdList } from '@/utils/adapter'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import FarmMap from '@/components/FarmMap.vue'

/* ---------------- 后端状态常量（Constants：AVAILABLE=0 / NO_LONGER_AVAILABLE=1） ---------------- */
const STATUS_OPTIONS = [
  { value: 0, label: '可接单' },
  { value: 1, label: '已结束' }
]
const KANBAN_COLUMNS = [
  { status: 0, label: '可接单' },
  { status: 1, label: '已结束' }
]

const loading = ref(false)
const loadError = ref('')
const tasks = ref([])
const total = ref(null)
const lands = ref([])
const workTypeOptions = ref([])
const workTypeLoading = ref(false)
const workTypeHint = ref('作业类型')
const cropOptions = ref([])
const cropLoading = ref(false)
const cropHint = ref('作物')
const view = ref('list')
const statusFilter = ref('')
const typeFilter = ref('')
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(20)
const selectedLandId = ref('')
let listSeq = 0

const selectedLand = computed(() => lands.value.find((l) => l.id === selectedLandId.value))

const stat = computed(() => {
  const available = tasks.value.filter((t) => Number(t.status) === 0).length
  const ended = tasks.value.filter((t) => Number(t.status) === 1).length
  const area = tasks.value.reduce((acc, t) => acc + (Number(t.area) || 0), 0)
  const soon = tasks.value.filter((t) => {
    const ts = parseTime(t.endTime)
    return ts !== null && ts - Date.now() > 0 && ts - Date.now() <= 48 * 3600 * 1000
  }).length
  return { available, ended, area: area.toFixed(1), soon }
})

const kanban = computed(() =>
  KANBAN_COLUMNS.map((col) => ({ ...col, items: tasks.value.filter((t) => Number(t.status) === col.status) }))
)

/* ---------------- 展示工具 ---------------- */
function joinText(value) {
  if (Array.isArray(value)) {
    const items = value.filter((v) => v !== null && v !== undefined && v !== '')
    return items.length ? items.join('、') : '-'
  }
  return value || '-'
}
function statusText(status) {
  if (status === null || status === undefined || status === '') return '-'
  const n = Number(status)
  return n === 0 ? '可接单' : n === 1 ? '已结束' : `未知(${status})`
}
function isEnded(status) {
  if (status === null || status === undefined || status === '') return false
  const n = Number(status)
  return n === 1 || n === 2
}
const pad = (n) => String(n).padStart(2, '0')
function parseTime(value) {
  if (!value) return null
  if (Array.isArray(value)) {
    const [y, m = 1, d = 1, h = 0, mi = 0, s = 0] = value
    const dt = new Date(y, m - 1, d, h, mi, s)
    return Number.isNaN(dt.getTime()) ? null : dt.getTime()
  }
  if (typeof value === 'string') {
    // 后端 LocalDateTime 未配置自定义 Serializer，返回 ISO-8601 无时区本地时间
    const norm = /^\d{4}-\d{2}-\d{2}T/.test(value)
      ? value.replace(/(\.\d+)?(Z|[+-]\d{2}:?\d{2})$/, '').replace('T', ' ').replace(/-/g, '/')
      : value.replace(/-/g, '/')
    const dt = new Date(norm)
    return Number.isNaN(dt.getTime()) ? null : dt.getTime()
  }
  const dt = new Date(value)
  return Number.isNaN(dt.getTime()) ? null : dt.getTime()
}
function fmtTime(value) {
  const ts = parseTime(value)
  if (ts === null) return value ? String(value) : '-'
  const d = new Date(ts)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/* ---------------- 数据加载 ---------------- */
async function load() {
  await Promise.all([loadTasks(), loadLands(), loadWorkTypes(), loadCrops()])
}

async function loadTasks() {
  const seq = ++listSeq
  loading.value = true
  loadError.value = ''
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      orderByCreateTimeDesc: true
    }
    if (statusFilter.value !== '' && statusFilter.value !== null) params.status = Number(statusFilter.value)
    if (typeFilter.value) params.workTypeId = [typeFilter.value]
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const res = await listTasks(params)
    if (seq !== listSeq) return
    tasks.value = Array.isArray(res?.list) ? res.list : []
    // 后端 FarmWorkServiceImpl.list 明确返回 total；mock 分支 total 为 null
    total.value = res?.total === undefined ? null : res.total
  } catch (error) {
    if (seq !== listSeq) return
    tasks.value = []
    total.value = null
    loadError.value = error?.msg || error?.payload?.msg || error?.message || '未知错误'
    ElMessage.error('查询作业需求失败：' + loadError.value)
  } finally {
    if (seq === listSeq) loading.value = false
  }
}

async function loadLands() {
  try {
    lands.value = await listLands()
  } catch {
    // 地块失败不阻断主列表；地图视图会为空
    lands.value = []
  }
}

async function loadWorkTypes() {
  workTypeLoading.value = true
  try {
    workTypeOptions.value = await listWorkTypes()
    workTypeHint.value = workTypeOptions.value.length ? '作业类型' : '暂无可选作业类型'
  } catch (error) {
    workTypeOptions.value = []
    const code = Number(error?.code ?? error?.payload?.code ?? error?.status)
    if (code === 403) {
      workTypeHint.value = '暂无作业类型选项：缺少 field:work:check 权限'
    } else {
      const message = error?.payload?.msg || error?.msg || error?.message || '未知错误'
      workTypeHint.value = '作业类型加载失败，请刷新重试'
      ElMessage.error('获取作业类型失败：' + message)
    }
  } finally {
    workTypeLoading.value = false
  }
}

async function loadCrops() {
  cropLoading.value = true
  try {
    const response = await listCrops()
    const rows = Array.isArray(response?.data) ? response.data : []
    cropOptions.value = rows.map((row) => ({
      id: toStringId(row?.id),
      name: row?.cropName || ''
    })).filter((row) => row.id && row.name)
    cropHint.value = cropOptions.value.length ? '作物' : '暂无可选作物'
  } catch (error) {
    cropOptions.value = []
    cropHint.value = '作物加载失败，请刷新重试'
    ElMessage.error('获取作物列表失败：' + (error?.payload?.msg || error?.msg || error?.message || '未知错误'))
  } finally {
    cropLoading.value = false
  }
}

function handleQuery() {
  pageNum.value = 1
  loadTasks()
}
function handlePageChange(page) {
  pageNum.value = Number(page)
  loadTasks()
}
function handleSizeChange(size) {
  pageSize.value = Number(size)
  pageNum.value = 1
  loadTasks()
}

onMounted(load)

/* ---------------- 行操作 ---------------- */
function onRowCommand(cmd, row) {
  if (cmd === 'detail') openDetail(row)
  else if (cmd === 'edit') openEdit(row)
  else if (cmd === 'revoke') revoke(row)
}

/* ---------------- 新建 / 编辑 ---------------- */
const formVisible = ref(false)
const formMode = ref('create')
const submitting = ref(false)
const EMPTY_FORM = { id: '', name: '', area: 1, endTime: '', phoneNumber: '', landPlotId: [], cropId: [], workTypeId: [] }
const form = reactive({ ...EMPTY_FORM })

function resetForm(patch = {}) {
  Object.assign(form, EMPTY_FORM, { landPlotId: [], cropId: [], workTypeId: [] }, patch)
}
function openCreate() {
  formMode.value = 'create'
  resetForm()
  formVisible.value = true
}
function openEdit(row) {
  if (!row) return
  formMode.value = 'edit'
  resetForm({
    id: row.id,
    name: row.name || '',
    area: Number(row.area) || 1,
    endTime: toFormDateTime(row.endTime),
    phoneNumber: row.phoneNumber || '',
    landPlotId: toStringIdList(row.landPlotId),
    cropId: toStringIdList(row.cropId),
    workTypeId: toStringIdList(row.workTypeId)
  })
  formVisible.value = true
}

// LocalDateTime 按 ISO-8601 无时区格式回填，不做时区换算。
function toFormDateTime(value) {
  const ts = parseTime(value)
  if (ts === null) return ''
  const d = new Date(ts)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

async function submitForm() {
  if (submitting.value) return
  if (!form.name.trim()) return ElMessage.warning('请填写作业名称')
  const area = Number(form.area)
  if (!Number.isFinite(area) || area <= 0) return ElMessage.warning('亩数必须为大于 0 的有效数字')
  if (!form.endTime) return ElMessage.warning('请选择截止时间')
  if (!form.phoneNumber.trim()) return ElMessage.warning('请填写联系方式')
  const endTs = parseTime(form.endTime)
  // 后端会校验 endTime > now，提前本地拦截，避免一次无意义的失败请求
  if (endTs !== null && endTs <= Date.now()) return ElMessage.warning('截止时间必须晚于当前时间')

  submitting.value = true
  try {
    const payload = {
      name: form.name.trim(),
      area,
      endTime: form.endTime,
      phoneNumber: form.phoneNumber.trim(),
      landPlotId: toStringIdList(form.landPlotId),
      cropId: toLongIdList(form.cropId),
      workTypeId: toLongIdList(form.workTypeId)
    }
    if (formMode.value === 'edit') {
      const res = await updateTask({ ...payload, id: form.id })
      ElMessage.success(res?.message || '已保存')
    } else {
      const res = await createTask(payload)
      ElMessage.success(res?.message || '已发布')
    }
    formVisible.value = false
    await loadTasks()
  } catch (error) {
    ElMessage.error(`${formMode.value === 'edit' ? '保存' : '发布'}失败：${error?.msg || error?.payload?.msg || error?.message || '未知错误'}`)
  } finally {
    submitting.value = false
  }
}

/* ---------------- 下架（后端软删除，status -> 1） ---------------- */
async function revoke(row) {
  if (!row) return
  try {
    await ElMessageBox.confirm(
      `确认下架「${row.name || row.id}」？后端会将状态置为「已结束」，列表将不再可接单（不做物理删除）。`,
      '下架确认',
      { type: 'warning', confirmButtonText: '确认下架', cancelButtonText: '取消' }
    )
  } catch { return }
  try {
    const res = await revokeTask(row.id)
    detailVisible.value = false
    await loadTasks()
    ElMessage.success(res?.message || '已下架')
  } catch (error) {
    ElMessage.error('下架失败：' + (error?.msg || error?.payload?.msg || error?.message || '未知错误'))
  }
}

/* ---------------- 详情 ---------------- */
const detailVisible = ref(false)
const detail = ref(null)
function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

/* ---------------- 地图 ---------------- */
function onMapLand(l) {
  if (!l) return
  selectedLandId.value = l.id
  const related = tasks.value.filter((t) => (t.landPlotId || []).includes(String(l.id)))
  if (related.length) {
    ElMessage.info(`${l.name}：关联 ${related.length} 条作业需求（${related.map((t) => statusText(t.status)).join('、')}）`)
  } else {
    ElMessage.info(`${l.name}：暂无关联作业需求`)
  }
}
</script>

<style scoped>
.filter-bar.flat { background: var(--surface-2); margin-bottom: 12px; padding: 9px 11px; }
.op-btn { padding: 0 3px; }
.warn-text { color: var(--amber-600); font-weight: 600; }
.kanban { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.kb-col { background: var(--surface-2); border: 1px solid var(--border); border-radius: 10px; padding: 8px; min-height: 300px; }
.kb-head { display: flex; align-items: center; gap: 5px; font-size: 11.5px; font-weight: 650; padding: 2px 3px 7px; }
.kb-count { margin-left: auto; min-width: 17px; height: 17px; padding: 0 4px; border-radius: 9px; background: #fff; border: 1px solid var(--border); color: var(--text-2); font-size: 10px; display: grid; place-items: center; }
.kb-body { display: flex; flex-direction: column; gap: 6px; max-height: 420px; overflow-y: auto; }
.kb-card { background: #fff; border: 1px solid var(--border); border-radius: 8px; padding: 7px 8px; cursor: pointer; transition: border-color 0.16s, box-shadow 0.16s; }
.kb-card:hover { border-color: var(--brand-300); box-shadow: var(--shadow-sm); }
.kb-title { font-size: 11.5px; font-weight: 650; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.kb-meta, .kb-dead { font-size: 10px; color: var(--text-3); margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.kb-dead { color: var(--text-2); }
.kb-empty { padding: 22px 0; text-align: center; font-size: 10.5px; color: var(--text-3); }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 14px; }
.form-grid .full { grid-column: 1 / -1; }
.pager-wrap { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
