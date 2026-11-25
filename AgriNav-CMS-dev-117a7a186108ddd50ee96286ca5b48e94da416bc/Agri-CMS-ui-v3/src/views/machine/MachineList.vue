<template>
  <div class="page" v-loading="loading">
    <PageHeader
      title="农机与农机手"
      code="MP-02"
      group="业务管理"
      desc="统一维护农机档案、机手资质与实时状态，支撑派单匹配、效率评估与合作农机协同管理。"
    >
      <template #actions>
        <el-button :icon="Aim" @click="router.push('/monitor')">查看实时位置</el-button>
      </template>
    </PageHeader>

    <div class="grid cols-4">
      <StatCard label="农机总数" :value="stats.total" unit="台" icon="Van" foot="含合作农机 2 台" />
      <StatCard label="在线率" :value="stats.onlineRate" unit="%" icon="Connection" tone="blue" :foot="`在线 ${stats.online} 台`" />
      <StatCard label="作业中" :value="stats.working" unit="台" icon="VideoPlay" foot="实时回传作业轨迹" />
      <StatCard label="待机 / 离线" :value="stats.idle" unit="台" icon="Warning" tone="red" foot="含维修 1 台" />
    </div>

    <el-tabs v-model="tab" class="mt-3">
      <el-tab-pane label="农机档案" name="machines">
        <div class="filter-bar">
          <el-input
            v-model="keyword"
            placeholder="搜索编号 / 型号 / 机手"
            clearable
            style="width: 200px"
            @keyup.enter="loadTable"
            @clear="loadTable"
          />
          <div class="seg">
            <button v-for="t in typeOptions" :key="t" :class="{ active: type === t }" @click="type = t">{{ t }}</button>
          </div>
          <el-select v-model="district" placeholder="片区" style="width: 118px">
            <el-option v-for="d in districtOptions" :key="d" :label="d" :value="d" />
          </el-select>
          <el-select v-model="workStatus" placeholder="状态" style="width: 108px">
            <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
          </el-select>
          <div class="grow" />
          <span class="filter-hint">共 {{ tableMachines.length }} 台</span>
        </div>

        <PanelCard title="农机档案" sub="合作农机以标签标注，定位可跳转地图分布" tight>
          <el-table :data="pagedMachines" size="small" row-key="id" style="width: 100%" @row-click="openDetail">
            <el-table-column label="编号" width="98">
              <template #default="{ row }">
                <span class="cell-main">{{ row.code }}</span>
                <span v-if="isCollab(row.id)" class="tag lime no-dot collab">合作</span>
              </template>
            </el-table-column>
            <el-table-column prop="model" label="型号" min-width="150" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" width="72" />
            <el-table-column prop="district" label="片区" width="82" />
            <el-table-column prop="operatorName" label="关联机手" width="82" />
            <el-table-column label="在线" width="62">
              <template #default="{ row }"><StatusTag :status="row.online ? 'online' : 'offline'" /></template>
            </el-table-column>
            <el-table-column label="作业状态" width="84">
              <template #default="{ row }"><StatusTag :status="row.workStatus" /></template>
            </el-table-column>
            <el-table-column label="今日面积" width="76" align="right">
              <template #default="{ row }"><span class="num">{{ row.todayArea }} 亩</span></template>
            </el-table-column>
            <el-table-column label="效率" width="66" align="right">
              <template #default="{ row }"><span class="num">{{ row.efficiency }} 亩/h</span></template>
            </el-table-column>
            <el-table-column label="操作" width="108">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click.stop="openDetail(row)">详情</el-button>
                <el-button link type="primary" size="small" @click.stop="locate(row)">定位</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="page"
            :page-size="pageSize"
            :total="tableMachines.length"
            layout="total, prev, pager, next"
            small
            background
          />
        </PanelCard>
      </el-tab-pane>

      <el-tab-pane label="农机手" name="operators">
        <div class="filter-bar">
          <el-input v-model="operatorKeyword" placeholder="搜索姓名 / 电话" clearable style="width: 220px" />
          <div class="grow" />
          <span class="filter-hint">共 {{ filteredOperators.length }} 名机手</span>
        </div>
        <PanelCard title="农机手档案" sub="证书等级与累计作业量来自业务库" tight>
          <el-alert
            v-if="operatorUnavailable"
            type="warning"
            :closable="false"
            show-icon
            title="农机手档案暂不可用"
            :description="operatorUnavailable"
            class="mb-2"
          />
          <el-table :data="filteredOperators" size="small" style="width: 100%">
            <el-table-column prop="name" label="姓名" width="80">
              <template #default="{ row }"><span class="cell-main">{{ row.name }}</span></template>
            </el-table-column>
            <el-table-column prop="phone" label="电话" width="118" />
            <el-table-column prop="district" label="片区" width="92" />
            <el-table-column prop="machineId" label="绑定农机" width="90" />
            <el-table-column label="评分" width="72">
              <template #default="{ row }"><span class="num strong">{{ row.rating }}</span></template>
            </el-table-column>
            <el-table-column label="累计作业量" min-width="102" align="right">
              <template #default="{ row }"><span class="num">{{ row.workload }} 亩</span></template>
            </el-table-column>
            <el-table-column label="证书" width="70">
              <template #default="{ row }"><span class="tag blue no-dot">{{ row.cert }} 级</span></template>
            </el-table-column>
            <el-table-column label="今日工时" width="86" align="right">
              <template #default="{ row }"><span class="num">{{ row.todayHours }} h</span></template>
            </el-table-column>
            <el-table-column label="状态" width="76">
              <template #default="{ row }"><StatusTag :status="row.status" /></template>
            </el-table-column>
          </el-table>
        </PanelCard>
      </el-tab-pane>

      <el-tab-pane label="地图分布" name="map">
        <PanelCard title="农机实时分布" :sub="`${allMachines.length} 台农机 · 点击图标查看档案`">
          <template #extra><span class="tag green">在线 {{ stats.online }} 台</span></template>
          <FarmMap :machines="allMachines" :lands="lands" :height="480" @select-machine="openDetail" />
        </PanelCard>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="drawer" title="农机详情" :size="620" destroy-on-close>
      <div v-if="detail" v-loading="detailLoading">
        <div class="detail-head">
          <div style="min-width: 0">
            <div class="detail-title">{{ detail.code }} · {{ detail.model }}</div>
            <div class="detail-sub">{{ detail.type }} · {{ detail.district }} · 关联机手 {{ detail.operatorName }}</div>
          </div>
          <StatusTag :status="detail.online ? 'online' : 'offline'" />
          <StatusTag :status="detail.workStatus" />
        </div>

        <div class="detail-grid">
          <div class="detail-field"><div class="detail-label">农机编号</div><div class="detail-value mono">{{ detail.id }}</div></div>
          <div class="detail-field"><div class="detail-label">当前地块</div><div class="detail-value">{{ detail.landName }}</div></div>
          <div class="detail-field"><div class="detail-label">今日作业面积</div><div class="detail-value">{{ detail.todayArea }} 亩</div></div>
          <div class="detail-field"><div class="detail-label">作业效率</div><div class="detail-value">{{ detail.efficiency }} 亩/小时</div></div>
          <div class="detail-field"><div class="detail-label">累计作业面积</div><div class="detail-value">{{ detail.totalArea }} 亩</div></div>
          <div class="detail-field"><div class="detail-label">最后上报</div><div class="detail-value">{{ detail.lastActive }}</div></div>
        </div>

        <div class="operator-card mt-3">
          <div class="operator-avatar">{{ (operator?.name || detail.operatorName || detail.phone || '机').slice(0, 1) }}</div>
          <div style="min-width: 0">
            <div v-if="operator || detail.operatorName" class="strong" style="font-size: 12.5px">{{ operator?.name || detail.operatorName }}</div>
            <div v-else class="strong" style="font-size: 12.5px">农机手档案待补充</div>
            <div class="hint" style="font-size: 11px">
              <template v-if="operator">{{ operator.phone }} · {{ operator.district }} · 评分 {{ operator.rating }} · {{ operator.cert }} 级证书</template>
              <template v-else>后端未提供农机手档案接口{{ detail.phone ? ` · 发布联系方式 ${detail.phone}` : '' }}</template>
            </div>
          </div>
          <el-button v-if="operator" text size="small" style="margin-left: auto" @click="viewOperator">机手档案</el-button>
        </div>

        <div v-if="detail.image" class="machine-photo mt-3">
          <el-image
            :src="detail.image"
            :preview-src-list="[detail.image]"
            fit="cover"
            style="width: 100%; height: 150px; border-radius: 10px"
          >
            <template #error><div class="photo-fallback">农机实拍图暂不可用</div></template>
          </el-image>
          <div class="photo-caption">
            <el-icon><Picture /></el-icon>
            <span>农机实拍影像 · 来源于后端农机档案</span>
          </div>
        </div>

        <FarmMap
          :machines="[detail]"
          :lands="[]"
          :height="180"
          :show-labels="false"
          :show-legend="false"
          class="mt-3"
        />

        <el-tabs v-model="detailTab" class="mt-3">
          <el-tab-pane label="历史作业" name="records">
            <el-table v-if="detail.records && detail.records.length" :data="detail.records" size="small">
              <el-table-column prop="id" label="作业单号" width="128" />
              <el-table-column prop="landName" label="地块" min-width="110" />
              <el-table-column prop="date" label="日期" width="90" />
              <el-table-column label="面积" width="72" align="right">
                <template #default="{ row }"><span class="num">{{ row.area }} 亩</span></template>
              </el-table-column>
              <el-table-column label="状态" width="76">
                <template #default="{ row }"><StatusTag :status="row.status" /></template>
              </el-table-column>
            </el-table>
            <div v-else class="empty">暂无历史作业记录</div>
          </el-tab-pane>
          <el-tab-pane label="维保信息" name="maint">
            <div class="detail-grid">
              <div class="detail-field"><div class="detail-label">购置日期</div><div class="detail-value">{{ detail.purchase }}</div></div>
              <div class="detail-field"><div class="detail-label">下次维保</div><div class="detail-value">{{ detail.maintainAt }}</div></div>
              <div class="detail-field"><div class="detail-label">燃油余量</div><div class="detail-value">{{ detail.fuel }}%</div></div>
              <div class="detail-field"><div class="detail-label">当前位置</div><div class="detail-value mono">{{ posText(detail.pos) }}</div></div>
            </div>
            <div class="hint-line mt-2">
              <el-icon :size="13"><Tools /></el-icon>
              <span>维保到期前 7 天将推送提醒至机手与片区队长，逾期未保养自动限制派单。</span>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer>
        <el-button @click="drawer = false">关闭</el-button>
        <el-button type="primary" @click="goTracks">查看轨迹</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Aim, Picture } from '@element-plus/icons-vue'
import { listMachines, listOperators, listLands, getMachine } from '@/services/api'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import FarmMap from '@/components/FarmMap.vue'

const router = useRouter()
const loading = ref(false)
const tab = ref('machines')

const allMachines = ref([])
const tableMachines = ref([])
const operators = ref([])
const lands = ref([])
const typeOptions = ['全部', '拖拉机', '收割机', '植保机']
const type = ref('全部')
const district = ref('全部')
const workStatus = ref('全部')
const keyword = ref('')
const statusOptions = ['全部', '作业中', '待机', '离线', '维修']
const page = ref(1)
const pageSize = ref(6)

const operatorKeyword = ref('')
const operatorUnavailable = ref('')
const drawer = ref(false)
const detail = ref(null)
const detailLoading = ref(false)
const detailTab = ref('records')
const COLLAB = ['NJ-004', 'NJ-008']

const stats = computed(() => {
  const total = allMachines.value.length
  const online = allMachines.value.filter((m) => m.online).length
  return {
    total,
    online,
    onlineRate: total ? +((online / total) * 100).toFixed(1) : 0,
    working: allMachines.value.filter((m) => m.workStatus === '作业中').length,
    idle: allMachines.value.filter((m) => ['待机', '离线', '维修'].includes(m.workStatus)).length
  }
})
const districtOptions = computed(() => ['全部', ...new Set(allMachines.value.map((m) => m.district))])
const pagedMachines = computed(() => tableMachines.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
const filteredOperators = computed(() => {
  const k = operatorKeyword.value.trim()
  if (!k) return operators.value
  return operators.value.filter((o) => o.name.includes(k) || o.phone.includes(k))
})
const operator = computed(() => operators.value.find((o) => o.id === detail.value?.operatorId))

const isCollab = (id) => COLLAB.includes(id)
const posText = (pos) => (Array.isArray(pos) && pos.length >= 2 ? `${pos[0]}, ${pos[1]}` : '暂无定位数据')

async function loadBase() {
  loading.value = true
  // 三个数据源互不依赖：用 allSettled，避免机手/地块接口不可用时拖垮整页农机档案
  const [ms, ops, ls] = await Promise.allSettled([listMachines(), listOperators(), listLands()])
  if (ms.status === 'fulfilled') {
    allMachines.value = ms.value || []
  } else {
    allMachines.value = []
    ElMessage.error(`农机列表加载失败：${ms.reason?.message || '未知错误'}`)
  }
  if (ops.status === 'fulfilled') {
    operators.value = ops.value || []
    operatorUnavailable.value = ''
  } else {
    operators.value = []
    operatorUnavailable.value = ops.reason?.message || '农机手档案不可用'
  }
  if (ls.status === 'fulfilled') lands.value = ls.value || []
  else lands.value = []
  loading.value = false
}
async function loadTable() {
  try {
    tableMachines.value = await listMachines({
      type: type.value,
      district: district.value,
      status: workStatus.value,
      keyword: keyword.value.trim()
    })
  } catch (e) {
    tableMachines.value = []
    ElMessage.error(`农机档案加载失败：${e.message}`)
  }
  page.value = 1
}
onMounted(async () => {
  await loadBase()
  await loadTable()
})
watch([type, district, workStatus], loadTable)

async function openDetail(row) {
  if (!row) return
  detailTab.value = 'records'
  drawer.value = true
  detailLoading.value = true
  try {
    detail.value = await getMachine(row.id)
    if (!detail.value) ElMessage.warning('未查询到该农机档案')
  } catch (e) {
    detail.value = null
    ElMessage.error(`农机详情加载失败：${e.message}`)
  } finally {
    detailLoading.value = false
  }
}

function locate(row) {
  tab.value = 'map'
  ElMessage.success(`已在分布地图定位 ${row.code}（${row.district}）`)
}

function viewOperator() {
  if (!detail.value) return
  drawer.value = false
  tab.value = 'operators'
  operatorKeyword.value = detail.value.operatorName || ''
  ElMessage.info(`已筛选机手 ${detail.value.operatorName}`)
}

function goTracks() {
  if (!detail.value) return
  ElMessage.info(`正在打开 ${detail.value.code} 的作业轨迹`)
  router.push('/workorders')
}
</script>

<style scoped>
.collab {
  height: 17px;
  padding: 0 5px;
  margin-left: 4px;
  font-size: 9.5px;
}
:deep(.el-table .el-button.is-link) {
  font-size: 11.5px;
  padding: 0;
}
.operator-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 12px;
  border-radius: 10px;
  background: var(--surface-2);
  border: 1px solid var(--border);
}
.operator-avatar {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
  color: #0a2e21;
  background: linear-gradient(145deg, #d8f5e6, #8be0b4);
  flex: none;
}
.photo-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  background: var(--surface-2);
  color: var(--text-3);
  font-size: 12px;
}
.photo-caption {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 6px;
  font-size: 11px;
  color: var(--text-3);
}
</style>
