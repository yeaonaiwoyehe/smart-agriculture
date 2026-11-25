<template>
  <div class="page" v-loading="loading">
    <PageHeader
      title="地块管理"
      code="MP-01"
      group="业务管理"
      desc="一地一码 · 边界可信 · 面积可追溯，统一管理地块矢量边界、权属作物与版本变更，为派单、核算与结算提供唯一地块主数据。"
    >
      <template #actions>
        <el-button :icon="Upload" @click="importVisible = true">导入地块</el-button>
        <el-button type="primary" :icon="MagicStick" @click="agentVisible = true">地块 AI 助手</el-button>
      </template>
    </PageHeader>

    <div class="grid cols-4">
      <StatCard label="地块总数" :value="lands.length" unit="块" icon="MapLocation" foot="一地一码主数据" />
      <StatCard label="总面积" :value="totalArea" unit="亩" icon="DataLine" foot="矢量边界核算面积" />
      <StatCard label="一地一码覆盖率" :value="codeRate" unit="%" icon="CircleCheck" foot="全部地块已赋码" />
      <StatCard label="待作业地块" :value="pendingCount" unit="块" icon="Warning" tone="amber" foot="需纳入下一轮派单" />
    </div>

    <div class="filter-bar mt-3">
      <el-input v-model="keyword" placeholder="搜索名称 / 编码 / 村" clearable style="width: 210px" />
      <el-select v-model="district" placeholder="片区" style="width: 118px">
        <el-option v-for="d in districtOptions" :key="d" :label="d" :value="d" />
      </el-select>
      <el-select v-model="crop" placeholder="作物" style="width: 104px">
        <el-option v-for="c in cropOptions" :key="c" :label="c" :value="c" />
      </el-select>
      <el-select v-model="status" placeholder="状态" style="width: 108px">
        <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
      </el-select>
      <div class="grow" />
      <span class="filter-hint">已选 {{ selectedRows.length }} 块</span>
      <el-button :disabled="selectedRows.length !== 1" @click="batchSplit">拆分</el-button>
      <el-button :disabled="selectedRows.length < 2" @click="batchMerge">合并</el-button>
    </div>

    <div class="land-grid">
      <PanelCard title="地块分布" sub="点击地块可聚焦边界，右侧台账同步高亮">
        <template #extra><span class="tag green">{{ filtered.length }} 块</span></template>
        <FarmMap
          :lands="filtered"
          :selected-land="selectedLandId"
          :height="430"
          @select-land="onMapSelect"
        />
      </PanelCard>

      <PanelCard title="地块台账" :sub="`共 ${filtered.length} 块 · 面积与版本可追溯`" tight>
        <el-table
          :data="pagedLands"
          size="small"
          row-key="id"
          highlight-current-row
          style="width: 100%"
          @selection-change="onSelectionChange"
          @row-click="onRowClick"
        >
          <el-table-column type="selection" width="30" />
          <el-table-column label="编码" width="52">
            <template #default="{ row }">
              <span class="code-cell" :title="row.code">{{ row.code.replace('YD-2026-', 'YD-') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="名称" min-width="72" show-overflow-tooltip>
            <template #default="{ row }"><span class="cell-main">{{ row.name }}</span></template>
          </el-table-column>
          <el-table-column label="村/片区" min-width="64">
            <template #default="{ row }">
              <div class="name-cell">{{ row.village }}</div>
              <div class="sub-cell">{{ row.district }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="crop" label="作物" width="38" />
          <el-table-column label="面积" width="44" align="right">
            <template #default="{ row }"><span class="num">{{ row.area }}</span></template>
          </el-table-column>
          <el-table-column prop="grade" label="等级" width="36" />
          <el-table-column label="单价" width="42" align="right">
            <template #default="{ row }"><span class="num">{{ row.price }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="72">
            <template #default="{ row }"><StatusTag :status="row.status" /></template>
          </el-table-column>
          <el-table-column label="操作" width="38">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click.stop="openDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="filtered.length"
          layout="total, prev, pager, next"
          small
          background
        />
      </PanelCard>
    </div>

    <el-drawer v-model="detailVisible" title="地块详情" :size="640" destroy-on-close>
      <div v-if="detail" v-loading="detailLoading">
        <div class="detail-head">
          <div style="min-width: 0">
            <div class="detail-title">{{ detail.name }}</div>
            <div class="detail-sub mono">{{ detail.code }} · {{ detail.village }} · 当前版本 {{ detail.version }}</div>
          </div>
          <StatusTag :status="detail.status" />
          <el-button size="small" :icon="CopyDocument" class="copy-btn" @click="copyCode">复制一地一码</el-button>
        </div>

        <div class="detail-grid">
          <div class="detail-field"><div class="detail-label">权属</div><div class="detail-value">{{ detail.owner }}</div></div>
          <div class="detail-field"><div class="detail-label">土壤类型</div><div class="detail-value">{{ detail.soil }}</div></div>
          <div class="detail-field"><div class="detail-label">坡度</div><div class="detail-value">{{ detail.slope }}</div></div>
          <div class="detail-field"><div class="detail-label">标准幅宽</div><div class="detail-value">{{ detail.width }} 米</div></div>
          <div class="detail-field"><div class="detail-label">质量要求</div><div class="detail-value">{{ detail.quality }}</div></div>
          <div class="detail-field"><div class="detail-label">更新时间</div><div class="detail-value">{{ detail.updatedAt }}</div></div>
          <div class="detail-field"><div class="detail-label">核算面积</div><div class="detail-value">{{ detail.area }} 亩</div></div>
          <div class="detail-field"><div class="detail-label">作业单价</div><div class="detail-value">¥ {{ detail.price }} / 亩</div></div>
        </div>

        <div v-if="detail.image" class="detail-photo mt-3">
          <el-image
            :src="detail.image"
            :preview-src-list="[detail.image]"
            fit="cover"
            style="width: 100%; height: 170px; border-radius: 10px"
          >
            <template #error><div class="photo-fallback">地块实拍图暂不可用</div></template>
          </el-image>
          <div class="photo-caption">
            <el-icon><Picture /></el-icon>
            <span>地块实拍影像 · 来源于后端地块档案</span>
          </div>
        </div>

        <FarmMap
          :lands="[detail]"
          :selected-land="detail.id"
          :height="220"
          :show-labels="false"
          :show-legend="false"
          class="mt-3"
        />

        <el-tabs v-model="detailTab" class="mt-3">
          <el-tab-pane label="基础信息" name="base">
            <div class="grid cols-2">
              <div class="mini-metric"><div class="v num">{{ detail.workCount }}</div><div class="l">累计作业次数</div></div>
              <div class="mini-metric"><div class="v num">{{ detail.lastWork }}</div><div class="l">最近作业日期</div></div>
              <div class="mini-metric"><div class="v num">{{ detail.grade }}</div><div class="l">地力等级</div></div>
              <div class="mini-metric"><div class="v num">{{ detail.version }}</div><div class="l">当前版本</div></div>
            </div>
            <div class="hint-line mt-2">
              <el-icon :size="13"><Lock /></el-icon>
              <span>地块编码由测绘组赋码，任何边界变更都会生成新版本并保留历史记录。</span>
            </div>
          </el-tab-pane>
          <el-tab-pane label="历史版本" name="history">
            <el-timeline>
              <el-timeline-item v-for="h in detail.history" :key="h.version" :timestamp="h.at" placement="top">
                <div class="strong" style="font-size: 12.5px">{{ h.version }} · {{ h.user }}</div>
                <div class="hint" style="font-size: 11.5px">{{ h.note }}</div>
              </el-timeline-item>
            </el-timeline>
          </el-tab-pane>
          <el-tab-pane label="作业记录" name="orders">
            <el-table v-if="landOrders.length" :data="landOrders" size="small">
              <el-table-column prop="id" label="作业单号" width="132" />
              <el-table-column label="作业面积" width="86">
                <template #default="{ row }"><span class="num">{{ row.effectiveArea }} 亩</span></template>
              </el-table-column>
              <el-table-column prop="operatorName" label="农机手" min-width="76" />
              <el-table-column label="状态" width="76">
                <template #default="{ row }"><StatusTag :status="row.status" /></template>
              </el-table-column>
            </el-table>
            <div v-else class="empty">该地块暂无作业记录</div>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer>
        <el-button @click="adjustBoundary">边界微调</el-button>
        <el-button @click="splitCurrent">拆分地块</el-button>
        <el-button type="primary" @click="mergeCurrent">合并地块</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="importVisible" title="导入地块" width="480px">
      <div class="upload-zone">
        <el-icon :size="26"><UploadFilled /></el-icon>
        <div class="upload-title">{{ fileName || '支持 Shapefile / GeoJSON / KML 矢量文件' }}</div>
        <div class="hint" style="font-size: 11px">单次导入不超过 50MB，系统自动进行坐标系与拓扑校验</div>
        <input ref="fileInput" type="file" accept=".zip,.json,.geojson,.kml" hidden @change="onFilePicked" />
        <el-button size="small" class="mt-2" @click="pickFile">选择文件</el-button>
      </div>
      <div class="hint-line mt-2">
        <el-icon :size="13"><InfoFilled /></el-icon>
        <span>导入成功后自动赋码并生成初始版本，历史版本与作业单关联不受影响。</span>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="confirmImport">确认导入</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="agentVisible" :size="520" :with-header="false">
      <AgentSidePanel agent-id="MA-05" title="地块 AI 助手" desc="基于地块主数据回答，不修改正式数据" :height="660" />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument, MagicStick, Upload, Picture } from '@element-plus/icons-vue'
import {
  listLands,
  getLand,
  listWorkorders,
  importLands,
  splitLand,
  mergeLands
} from '@/services/api'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import FarmMap from '@/components/FarmMap.vue'
import AgentSidePanel from '@/components/AgentSidePanel.vue'

const lands = ref([])
const loading = ref(false)
const keyword = ref('')
const district = ref('全部')
const crop = ref('全部')
const status = ref('全部')
const page = ref(1)
const pageSize = ref(8)
const selectedLandId = ref('')
const selectedRows = ref([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const detailTab = ref('base')
const landOrders = ref([])

const importVisible = ref(false)
const importing = ref(false)
const fileName = ref('')
const fileInput = ref(null)
const agentVisible = ref(false)

const totalArea = computed(() => lands.value.reduce((s, l) => s + l.area, 0).toFixed(1))
const codeRate = computed(() => (lands.value.length ? 100 : 0))
const pendingCount = computed(() => lands.value.filter((l) => l.status === '待作业').length)
const districtOptions = computed(() => ['全部', ...new Set(lands.value.map((l) => l.district))])
const cropOptions = computed(() => ['全部', ...new Set(lands.value.map((l) => l.crop))])
const statusOptions = ['全部', '作业中', '待作业', '待复核', '已完成']
const filtered = computed(() => {
  const k = keyword.value.trim()
  return lands.value.filter((l) => {
    const hitKey = !k || l.name.includes(k) || l.code.includes(k) || l.village.includes(k)
    return hitKey
      && (district.value === '全部' || l.district === district.value)
      && (crop.value === '全部' || l.crop === crop.value)
      && (status.value === '全部' || l.status === status.value)
  })
})
const pagedLands = computed(() => filtered.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))

async function loadLands() {
  loading.value = true
  try {
    lands.value = await listLands()
  } catch (e) {
    lands.value = []
    ElMessage.error(`地块列表加载失败：${e.message}`)
  } finally {
    loading.value = false
  }
}
onMounted(loadLands)

watch([keyword, district, crop, status], () => { page.value = 1 })

function onMapSelect(land) {
  selectedLandId.value = land.id
}
function onSelectionChange(rows) {
  selectedRows.value = rows
}
function onRowClick(row, column) {
  if (column?.type === 'selection') return
  openDetail(row)
}

async function openDetail(row) {
  if (!row) return
  selectedLandId.value = row.id
  detailTab.value = 'base'
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await getLand(row.id)
    const orders = await listWorkorders().catch(() => [])
    landOrders.value = orders.filter((w) => w.landId === row.id)
  } catch (e) {
    detail.value = null
    landOrders.value = []
    ElMessage.error(`地块详情加载失败：${e.message}`)
  } finally {
    detailLoading.value = false
  }
}

async function copyCode() {
  if (!detail.value) return
  try {
    if (!navigator.clipboard) throw new Error('clipboard unavailable')
    await navigator.clipboard.writeText(detail.value.code)
    ElMessage.success(`已复制一地一码 ${detail.value.code}`)
  } catch (e) {
    ElMessage.warning('复制失败，请手动选择编码')
  }
}

async function batchSplit() {
  const row = selectedRows.value[0]
  if (!row) return
  try {
    await ElMessageBox.confirm(
      `地块 ${row.code}（${row.name}）将拆分为 2 个新地块，原版本保留历史记录，是否继续？`,
      '拆分地块',
      { type: 'warning', confirmButtonText: '确认拆分', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  const res = await splitLand(row.id, 2).catch((e) => { ElMessage.warning(e.message); return null })
  if (!res) return
  ElMessage.success(res.message)
  selectedRows.value = []
  await loadLands()
}

async function batchMerge() {
  if (selectedRows.value.length < 2) return
  try {
    await ElMessageBox.confirm(
      `将把 ${selectedRows.value.length} 个地块合并为一个新地块，原编码与历史版本保留，是否继续？`,
      '合并地块',
      { type: 'warning', confirmButtonText: '确认合并', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  const res = await mergeLands(selectedRows.value.map((r) => r.id))
    .catch((e) => { ElMessage.warning(e.message); return null })
  if (!res) return
  ElMessage.success(res.message)
  selectedRows.value = []
  await loadLands()
}

async function splitCurrent() {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm('拆分后原地块将保留版本快照，作业单按原编码追溯，是否继续？', '拆分地块', {
      type: 'warning', confirmButtonText: '确认拆分', cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  const res = await splitLand(detail.value.id, 2).catch((e) => { ElMessage.warning(e.message); return null })
  if (!res) return
  ElMessage.success(res.message)
  detailVisible.value = false
  await loadLands()
}

async function mergeCurrent() {
  if (!detail.value) return
  const others = selectedRows.value.filter((r) => r.id !== detail.value.id)
  if (!others.length) {
    ElMessage.warning('请先在列表中勾选至少 1 个其他地块后再合并')
    return
  }
  try {
    await ElMessageBox.confirm(`将把 ${detail.value.name} 与勾选的 ${others.length} 个地块合并，是否继续？`, '合并地块', {
      type: 'warning', confirmButtonText: '确认合并', cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  const res = await mergeLands([detail.value.id, ...others.map((r) => r.id)])
    .catch((e) => { ElMessage.warning(e.message); return null })
  if (!res) return
  ElMessage.success(res.message)
  detailVisible.value = false
  selectedRows.value = []
  await loadLands()
}

async function adjustBoundary() {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm('边界微调将重新解析该地块矢量边界并生成新版本，是否继续？', '边界微调', {
      type: 'warning', confirmButtonText: '提交微调', cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  const res = await importLands(1).catch((e) => { ElMessage.warning(e.message); return null })
  if (!res) return
  ElMessage.success(res.message)
  detailVisible.value = false
  await loadLands()
}

function pickFile() {
  fileInput.value?.click()
}
function onFilePicked(e) {
  const file = e.target.files?.[0]
  fileName.value = file ? file.name : ''
}
async function confirmImport() {
  importing.value = true
  try {
    const res = await importLands(3).catch((e) => { ElMessage.warning(e.message); return null })
    if (!res) return
    ElMessage.success(res.message)
    importVisible.value = false
    fileName.value = ''
    await loadLands()
  } finally {
    importing.value = false
  }
}
</script>

<style scoped>
.land-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}
.code-cell {
  font-family: var(--font-mono);
  font-size: 10.5px;
  color: var(--text-2);
}
.name-cell {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-1);
}
.sub-cell {
  font-size: 10px;
  color: var(--text-3);
}
:deep(.el-table .cell) {
  padding: 0 5px;
}
:deep(.el-table .el-button.is-link) {
  font-size: 11.5px;
  padding: 0;
}
.copy-btn {
  margin-left: auto;
  flex: none;
}
.upload-zone {
  border: 1px dashed var(--border-strong);
  border-radius: 12px;
  background: var(--surface-2);
  padding: 24px 18px;
  text-align: center;
  color: var(--brand-600);
}
.upload-title {
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--text-1);
  font-weight: 550;
}
.detail-photo {
  position: relative;
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
