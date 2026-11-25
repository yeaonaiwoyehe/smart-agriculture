<template>
  <div class="page" v-loading="loading">
    <PageHeader
      title="农田作业管理"
      code="MP-05A"
      group="业务管理"
      desc="统一承接农户发布的农田作业需求：按作物、作业类型、面积与作业区域检索，查看地块与联系方式明细，为派单与结算提供任务来源。"
    >
      <template #actions>
        <el-button :icon="Refresh" @click="handleQuery">刷新列表</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增农田作业</el-button>
      </template>
    </PageHeader>

    <div class="grid cols-4">
      <StatCard label="当前页任务" :value="list.length" unit="条" icon="Tickets" :foot="`第 ${pageNum} 页`" />
      <StatCard label="符合条件的任务" :value="total" unit="条" icon="Files" tone="blue" :foot="query.myFarmWork ? '仅我发布的需求' : '全部发布需求'" />
      <StatCard label="本页面积合计" :value="pageArea" unit="亩" icon="DataLine" foot="按本页招工面积累加" />
      <StatCard label="已过期" :value="expiredCount" unit="条" icon="Warning" tone="amber" foot="截止时间早于当前" />
    </div>

    <div class="filter-bar mt-3">
      <el-input
        v-model="query.name"
        placeholder="招工名称"
        clearable
        style="width: 180px"
        @keyup.enter="handleQuery"
        @clear="handleQuery"
      />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="handleQuery">
        <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-select
        v-model="query.cropId"
        multiple
        collapse-tags
        collapse-tags-tooltip
        placeholder="作物"
        clearable
        style="width: 190px"
        :loading="cropLoading"
        @change="handleCropChange"
      >
        <el-option v-for="c in cropOptions" :key="String(c.id)" :label="c.cropName" :value="String(c.id)" />
      </el-select>
      <el-select
        v-model="query.workTypeId"
        multiple
        collapse-tags
        collapse-tags-tooltip
        :placeholder="workTypePlaceholder"
        :disabled="!query.cropId.length"
        style="width: 190px"
        :loading="workTypeLoading"
        @change="handleQuery"
      >
        <el-option v-for="w in workTypeOptions" :key="String(w.id)" :label="w.workName" :value="String(w.id)" />
      </el-select>
      <el-input-number
        v-model="query.minArea"
        :min="0"
        :precision="2"
        :controls="false"
        placeholder="最小面积"
        style="width: 100px"
        @change="handleAreaChange"
      />
      <span class="filter-hint">-</span>
      <el-input-number
        v-model="query.maxArea"
        :min="0"
        :precision="2"
        :controls="false"
        placeholder="最大面积"
        style="width: 100px"
        @change="handleAreaChange"
      />
      <div class="grow" />
      <div class="seg">
        <button :class="{ active: query.orderByCreateTimeDesc }" @click="setOrder(true)">最新发布</button>
        <button :class="{ active: !query.orderByCreateTimeDesc }" @click="setOrder(false)">最早发布</button>
      </div>
      <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
      <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="locationInput"
        placeholder="作业区域关键词，回车添加"
        style="width: 240px"
        @keyup.enter="addLocation"
      >
        <template #append>
          <el-button :icon="Plus" @click="addLocation" />
        </template>
      </el-input>
      <el-tag
        v-for="(loc, index) in query.location"
        :key="loc + index"
        closable
        :disable-transitions="true"
        @close="removeLocation(index)"
      >
        {{ loc }}
      </el-tag>
      <span v-if="!query.location.length" class="filter-hint">作业区域按地块地址模糊匹配，可添加多个关键词</span>
      <div class="grow" />
      <!-- 后端 FarmWorkController.list 对 admin 强制 setMyFarmWork(false)，开关对管理员无效，
           因此管理员态禁用并给出说明，避免出现一个“点了没反应”的筛选。 -->
      <el-tooltip
        :disabled="!isAdmin"
        content="管理员默认查看全部农田作业"
        placement="top"
      >
        <el-checkbox v-model="query.myFarmWork" :disabled="isAdmin" @change="handleQuery">只看我发布的</el-checkbox>
      </el-tooltip>
      <span v-if="isAdmin" class="filter-hint">管理员默认查看全部农田作业</span>
    </div>

    <PanelCard title="农田作业台账" :sub="`共 ${total} 条 · 列表数据来自 /farmWork/list`" tight>
      <template #extra>
        <span class="tag green no-dot">{{ list.length }} / {{ total }}</span>
      </template>

      <el-table :data="list" size="small" row-key="id" style="width: 100%" @row-click="openDetail">
        <el-table-column label="招工名称" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="cell-main">{{ row.name || '-' }}</div>
            <div class="cell-sub mono">{{ row.id }}</div>
          </template>
        </el-table-column>
        <el-table-column label="作物 / 作业类型" min-width="140">
          <template #default="{ row }">
            <div class="cell-main">{{ joinText(row.cropName) }}</div>
            <div class="cell-sub">{{ joinText(row.workTypeName) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="地块 / 地址" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="cell-main">{{ joinText(row.landPlotName) }}</div>
            <div class="cell-sub">{{ joinText(row.landAddress) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="面积" width="80" align="right">
          <template #default="{ row }"><span class="num">{{ formatArea(row.area) }}</span></template>
        </el-table-column>
        <el-table-column label="联系方式" width="116">
          <template #default="{ row }"><span class="mono">{{ row.phoneNumber || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }"><span class="mono">{{ formatDateTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="截止时间" width="150">
          <template #default="{ row }">
            <span class="mono">{{ formatDateTime(row.endTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="88">
          <template #default="{ row }"><StatusTag :status="statusText(row.status)" /></template>
        </el-table-column>
        <el-table-column label="操作" width="168" align="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openDetail(row)">查看</el-button>
            <el-button link type="primary" size="small" @click.stop="openEdit(row)">编辑</el-button>
            <!-- 后端 deleteById 是逻辑下架（status→1），不是物理删除；已结束的记录不再给可点入口。 -->
            <el-button
              v-if="!isWorkEnded(row.status)"
              link
              type="warning"
              size="small"
              :loading="endingRowId !== '' && endingRowId === String(row.id)"
              :disabled="endingRowId !== '' && endingRowId !== String(row.id)"
              @click.stop="handleEndWork(row)"
            >
              结束作业
            </el-button>
            <span v-else class="cell-sub">已结束</span>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty">
            <div class="empty-title">{{ loadError ? '列表加载失败' : '暂无农田作业数据' }}</div>
            <div class="hint">{{ loadError || '调整筛选条件后重试，或确认后端 /farmWork/list 是否有符合条件的数据' }}</div>
          </div>
        </template>
      </el-table>

      <div class="pager">
        <Pagination
          v-model:page="pageNum"
          v-model:limit="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @pagination="handlePagination"
        />
      </div>
    </PanelCard>

    <el-dialog
      v-model="formVisible"
      :title="formMode === 'create' ? '新增农田作业' : '编辑农田作业'"
      width="620"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="handleFormClosed"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="88px">
        <div class="form-grid">
          <el-form-item label="作业名称" prop="name" class="full">
            <el-input v-model="form.name" maxlength="60" show-word-limit placeholder="如：小麦收割作业" />
          </el-form-item>
          <el-form-item label="作业面积" prop="area">
            <el-input-number v-model="form.area" :min="0.01" :precision="2" :step="10" :controls="false" placeholder="亩" style="width: 100%" />
          </el-form-item>
          <el-form-item label="截止时间" prop="endTime">
            <!-- 后端 LocalDateTime 无时区语义，这里不做 +8h 补偿，按所选本地时间直传。 -->
            <el-date-picker
              v-model="form.endTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              format="YYYY-MM-DD HH:mm"
              placeholder="选择截止时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="联系电话" prop="phoneNumber" class="full">
            <el-input v-model="form.phoneNumber" maxlength="11" placeholder="用于农机手联系，11 位手机号" />
          </el-form-item>
          <el-form-item label="作物" class="full">
            <el-select
              v-model="form.cropId"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
              placeholder="可不选，支持多选"
              style="width: 100%"
              :loading="cropLoading"
              @change="handleFormCropChange"
            >
              <el-option v-for="c in formCropOptions" :key="String(c.value)" :label="c.label" :value="c.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="作业类型" class="full">
            <el-select
              v-model="form.workTypeId"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
              :placeholder="form.cropId.length ? '可不选，支持多选' : '请先选择作物'"
              :disabled="!form.cropId.length"
              style="width: 100%"
              :loading="formWorkTypeLoading"
            >
              <el-option v-for="w in formWorkTypeSelectOptions" :key="String(w.value)" :label="w.label" :value="w.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="地块" class="full">
            <el-select
              v-model="form.landPlotId"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
              filterable
              placeholder="可不选，支持多选"
              style="width: 100%"
              :loading="landPlotLoading"
            >
              <el-option v-for="p in formLandPlotOptions" :key="String(p.value)" :label="p.label" :value="p.value" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <div v-if="formDetachedLandPlots.length" class="hint-line">
        <el-icon :size="13"><InfoFilled /></el-icon>
        <span>
          当前作业关联的 {{ formDetachedLandPlots.length }} 个地块不在你的地块列表中（由其他用户创建），
          已按原名称回填并保留关联，不做清空。
        </span>
      </div>
      <div v-if="formDetachedCrops.length || formDetachedWorkTypes.length" class="hint-line">
        <el-icon :size="13"><InfoFilled /></el-icon>
        <span>部分原有作物 / 作业类型不在当前可选范围内，已按原始 ID 保留，未自动清空。</span>
      </div>
      <template #footer>
        <!-- 提交中禁止取消：避免 Dialog 被关掉后请求仍在跑、结果无处落地。 -->
        <el-button :disabled="submitting" @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ formMode === 'create' ? '提交新增' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="农田作业详情" :size="640" destroy-on-close>
      <div v-if="detail">
        <div class="detail-head">
          <div style="min-width: 0">
            <div class="detail-title">{{ detail.name || '-' }}</div>
            <div class="detail-sub mono">{{ detail.id }}</div>
          </div>
          <StatusTag :status="statusText(detail.status)" />
        </div>

        <div class="detail-grid">
          <div class="detail-field">
            <div class="detail-label">面积</div>
            <div class="detail-value">{{ formatArea(detail.area) }} 亩</div>
          </div>
          <div class="detail-field">
            <div class="detail-label">联系方式</div>
            <div class="detail-value mono">{{ detail.phoneNumber || '-' }}</div>
          </div>
          <div class="detail-field">
            <div class="detail-label">截止时间</div>
            <div class="detail-value mono">{{ formatDateTime(detail.endTime) }}</div>
          </div>
          <div class="detail-field">
            <div class="detail-label">创建时间</div>
            <div class="detail-value mono">{{ formatDateTime(detail.createTime) }}</div>
          </div>
          <div class="detail-field">
            <div class="detail-label">作物名称</div>
            <div class="detail-value">{{ joinText(detail.cropName) }}</div>
          </div>
          <div class="detail-field">
            <div class="detail-label">作业类型</div>
            <div class="detail-value">{{ joinText(detail.workTypeName) }}</div>
          </div>
          <div class="detail-field">
            <div class="detail-label">地块名称</div>
            <div class="detail-value">{{ joinText(detail.landPlotName) }}</div>
          </div>
          <div class="detail-field">
            <div class="detail-label">地块地址</div>
            <div class="detail-value">{{ joinText(detail.landAddress) }}</div>
          </div>
          <div class="detail-field">
            <div class="detail-label">更新时间</div>
            <div class="detail-value mono">{{ formatDateTime(detail.updateTime) }}</div>
          </div>
        </div>

        <div v-if="detailImages.length" class="detail-photo mt-3">
          <el-carousel height="260px" :autoplay="false" trigger="click" indicator-position="outside">
            <el-carousel-item v-for="(url, index) in detailImages" :key="url + index">
              <el-image
                :src="url"
                fit="contain"
                style="width: 100%; height: 100%"
                :preview-src-list="detailImages"
                :initial-index="index"
              >
                <template #error><div class="photo-fallback">地块图片加载失败</div></template>
              </el-image>
            </el-carousel-item>
          </el-carousel>
          <div class="photo-caption">
            <el-icon :size="13"><Picture /></el-icon>
            <span>地块实拍影像 · 来自列表返回的 landPlotImage</span>
          </div>
        </div>
        <div v-else class="hint-line mt-3">
          <el-icon :size="13"><InfoFilled /></el-icon>
          <span>该作业未返回地块图片</span>
        </div>

        <div class="hint-line mt-3">
          <el-icon :size="13"><Lock /></el-icon>
          <span>详情直接使用列表行数据展示，后端未提供单条农田作业详情接口。</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled, Lock, Picture, Plus, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import { addFarmWork, delFarmWork, listFarmWork, updateFarmWork } from '@/api/farmWork/farmWork'
import { listCrops } from '@/api/crop'
import { listFieldWorkOptions } from '@/api/fieldWork'
import { get } from '@/utils/request'
import { isAdminUser } from '@/stores/permission'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import Pagination from '@/components/Pagination/index.vue'

// 状态取值来自后端 Common Constants：AVAILABLE=0（可接单）、NO_LONGER_AVAILABLE=1（已下架/过期），
// list 查询固定排除 DELETE=2。未在字典表中找到对应的 label 定义，此处按后端常量标注。
const STATUS_OPTIONS = [
  { value: '0', label: '可接单' },
  { value: '1', label: '已结束' }
]
const STATUS_TEXT = { 0: '可接单', 1: '已结束' }

const loading = ref(false)
const loadError = ref('')
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
// 后端默认 pageSize=6，这里显式指定，避免依赖后端默认值。
const pageSize = ref(10)

const cropOptions = ref([])
const cropLoading = ref(false)
const workTypeOptions = ref([])
const workTypeLoading = ref(false)
const locationInput = ref('')

const detailVisible = ref(false)
const detail = ref(null)

// 管理员判定复用 stores/permission 中的统一口径（admin 角色或 *:*:* 通配权限），
// 与 appStore.canAccess 内部判断一致，不在此处硬编码角色名。
const isAdmin = computed(() => isAdminUser())

// 请求序号：并发/快速切换时只允许“最后一次”请求写入状态，防止旧响应覆盖新结果。
let listSeq = 0
let workTypeSeq = 0

const query = reactive({
  name: '',
  status: '',
  cropId: [],
  workTypeId: [],
  minArea: undefined,
  maxArea: undefined,
  location: [],
  orderByCreateTimeDesc: true,
  myFarmWork: false
})

const pageArea = computed(() => {
  const sum = list.value.reduce((acc, row) => acc + (Number(row.area) || 0), 0)
  return sum.toFixed(2)
})
const expiredCount = computed(() => list.value.filter((row) => isExpired(row.endTime)).length)
const workTypePlaceholder = computed(() =>
  query.cropId.length ? '作业类型' : '请先选择作物'
)
const detailImages = computed(() =>
  Array.isArray(detail.value?.landPlotImage) ? detail.value.landPlotImage.filter(Boolean) : []
)

/* ---------------- 展示工具 ---------------- */

// 后端返回的 cropName / workTypeName / landPlotName / landAddress 均为 List<String>，
// 必须按数组拼接，不能直接当字符串渲染。
function joinText(value) {
  if (Array.isArray(value)) {
    const items = value.filter((v) => v !== null && v !== undefined && v !== '')
    return items.length ? items.join('、') : '-'
  }
  return value || '-'
}

function formatArea(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(2) : '-'
}

function pad(num) {
  return String(num).padStart(2, '0')
}

// 后端 LocalDateTime 未做全局格式化（JacksonConfig 里的自定义 Serializer 全被注释），实际返回 ISO-8601 本地时间，
// 语义是「无时区的本地日期时间」。展示层按本地时间宽松解析，无法解析时原样返回。
const TZ_SUFFIX = /(\.\d+)?(Z|[+-]\d{2}:?\d{2})$/

function formatDateTime(value) {
  if (value === null || value === undefined || value === '') return '-'
  let date
  const hasTime = Array.isArray(value)
    ? value.length >= 4
    : (typeof value === 'string' ? /\d{2}:\d{2}/.test(value) : true)
  if (Array.isArray(value)) {
    const [y, m = 1, d = 1, h = 0, mi = 0, s = 0] = value
    date = new Date(y, m - 1, d, h, mi, s)
  } else if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value)) {
    // 与 toFormDateTime 保持同一语义：剥离时区后缀后取字面量，避免同一字段出现“列表一个时间、编辑另一个时间”。
    const local = value.replace(TZ_SUFFIX, '').replace('T', ' ').replace(/-/g, '/')
    date = new Date(local)
  } else {
    date = new Date(value)
  }
  if (Number.isNaN(date.getTime())) return String(value)
  const day = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  // 只有“后端确实没给时间”才只显示日期：数组形式不足 4 位、字符串形式不含时间部分。
  // 不能用 getHours()||getMinutes()||getSeconds() 判空——00:00:00 会全部取假，把午夜误判成无时间。
  if (!hasTime) return day
  return `${day} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function isExpired(value) {
  if (!value) return false
  const date = new Date(Array.isArray(value) ? value.slice(0, 5).join('-') : value)
  return !Number.isNaN(date.getTime()) && date.getTime() < Date.now()
}

function statusText(status) {
  if (status === null || status === undefined || status === '') return '-'
  return STATUS_TEXT[Number(status)] || String(status)
}

// 2 = DELETE 是后端软删除标记，list 查询已排除，列表不会出现；这里一并视为不可再操作的终态。
function isWorkEnded(status) {
  if (status === null || status === undefined || status === '') return false
  const num = Number(status)
  return num === 1 || num === 2
}

/* ---------------- 列表查询 ---------------- */

function buildQuery() {
  const payload = {
    name: query.name || undefined,
    orderByCreateTimeDesc: query.orderByCreateTimeDesc,
    myFarmWork: query.myFarmWork
  }
  // 状态按后端 Integer 约定传数字。
  if (query.status !== '' && query.status !== null && query.status !== undefined) {
    payload.status = Number(query.status)
  }
  // 面积区间只在有值时提交，0 是合法值，不能用真值判断。
  if (query.minArea !== undefined && query.minArea !== null) payload.minArea = query.minArea
  if (query.maxArea !== undefined && query.maxArea !== null) payload.maxArea = query.maxArea
  // 多选 id 数组有值才提交。
  if (query.cropId.length) payload.cropId = toQueryIds(query.cropId)
  if (query.workTypeId.length) payload.workTypeId = toQueryIds(query.workTypeId)
  if (query.location.length) payload.location = query.location.slice()
  return payload
}

// 作物/作业项 id 在实体中是 Long，后端 useGeneratedKeys 为雪花风格长整型。
// 前端下拉 value 统一使用字符串，避免超过 Number.MAX_SAFE_INTEGER 时精度丢失。
// 非纯数字返回 null（而不是 NaN）——JSON.stringify 会把 NaN 变成 null 发出去，
// 后端 List<Long> 收到 null 元素会在 forEach 处 NPE，必须在源头拦住。
function toQueryId(value) {
  const str = String(value).trim()
  if (!/^\d+$/.test(str)) return null
  return str.length > 15 ? str : Number(str)
}

// 过滤掉非法 id，避免把 null / NaN 提交到 List<Long> 关联数组里。
function toQueryIds(list) {
  return (Array.isArray(list) ? list : []).map(toQueryId).filter((id) => id !== null)
}

// 请求序号保护：每次调用自增 seq，异步返回后只有 seq 仍等于最新值时才允许写回状态。
// 旧请求晚返回时直接丢弃，且不改动 loading，避免“旧请求把新请求的 loading 提前关掉”。
async function getList() {
  const seq = ++listSeq
  loading.value = true
  loadError.value = ''
  try {
    // pageNum / pageSize 由 listFarmWork 内部放到 query，其余条件进 body。
    const response = await listFarmWork({ ...buildQuery(), pageNum: pageNum.value, pageSize: pageSize.value })
    if (seq !== listSeq) return
    const data = Array.isArray(response?.data) ? response.data : []
    list.value = data
    total.value = Number(response?.total) || 0
  } catch (error) {
    if (seq !== listSeq) return
    list.value = []
    total.value = 0
    loadError.value = error?.msg || error?.payload?.msg || error?.message || '未知错误'
    ElMessage.error('查询农田作业列表失败：' + loadError.value)
  } finally {
    if (seq === listSeq) loading.value = false
  }
}

function handleQuery() {
  pageNum.value = 1
  getList()
}

// 公共 Pagination 只在 currentPage*limit > total 时才回第 1 页，改“每页条数”时可能停在中间页。
// 这里在本页面内兜底：只要每页条数变了，就回到第 1 页再查询；仅切页码则按组件给的页码查询。
function handlePagination({ page, limit } = {}) {
  if (limit !== undefined && limit !== null && Number(limit) !== Number(pageSize.value)) {
    pageSize.value = Number(limit)
    pageNum.value = 1
  } else if (page !== undefined && page !== null) {
    pageNum.value = Number(page)
  }
  getList()
}

function handleAreaChange() {
  if (query.minArea !== undefined && query.minArea !== null
    && query.maxArea !== undefined && query.maxArea !== null
    && query.minArea > query.maxArea) {
    ElMessage.warning('最小面积不能大于最大面积')
    query.maxArea = undefined
    return
  }
  handleQuery()
}

function setOrder(desc) {
  if (query.orderByCreateTimeDesc === desc) return
  query.orderByCreateTimeDesc = desc
  handleQuery()
}

function resetQuery() {
  query.name = ''
  query.status = ''
  query.cropId = []
  query.workTypeId = []
  query.minArea = undefined
  query.maxArea = undefined
  query.location = []
  query.orderByCreateTimeDesc = true
  query.myFarmWork = false
  locationInput.value = ''
  workTypeOptions.value = []
  pageNum.value = 1
  getList()
}

function addLocation() {
  const value = locationInput.value.trim()
  if (!value) return
  if (query.location.includes(value)) {
    ElMessage.warning('该作业区域已添加')
    return
  }
  query.location.push(value)
  locationInput.value = ''
  handleQuery()
}

function removeLocation(index) {
  query.location.splice(index, 1)
  handleQuery()
}

/* ---------------- 下拉选项 ---------------- */

async function loadCrops() {
  cropLoading.value = true
  try {
    const response = await listCrops()
    cropOptions.value = Array.isArray(response?.data) ? response.data : []
  } catch (error) {
    cropOptions.value = []
    ElMessage.error('获取作物列表失败：' + (error?.msg || error?.payload?.msg || error?.message || '未知错误'))
  } finally {
    cropLoading.value = false
  }
}

// 601 是后端 AjaxResult.warn，属于「请求成功但业务上没有数据」的正常空态，不是错误。
// FieldWorkServiceImpl.getAvailableFieldWorks 在「当前作物没有可用作业」时返回 601，
// request.js 对 code!==200 一律 throw，所以这类响应只能从 catch 里按 code 识别。
const WARN_CODE = 601

function isWarnError(error) {
  return Number(error?.code ?? error?.payload?.code) === WARN_CODE
}

// 作业类型依赖作物：/fieldWork/getWork?cropId=X 是单作物查询，一次只接受一个 cropId。
// 多选作物时逐个请求并按 id 去重合并，不做“共用同一组选项”的假设。
// 用 allSettled 而不是 all：某个作物返回 601 或请求失败时，其余作物已取到的选项不能一起丢弃。
// 返回 { status, options }：status 取值如下——
//   'ok'      正常返回（可能为空数组，属于正常业务态，不是错误）
//   'error'   全部请求都真实异常（网络失败/5xx 等），选项已清空，调用方需同步清理 workTypeId
//   'stale'   本批次已被更新的批次取代，结果被丢弃，调用方不应基于它做任何状态变更
async function loadWorkTypeOptions() {
  const seq = ++workTypeSeq
  // 只保留合法数字 id：toQueryId 对非法值返回 null，不能拿去发请求（后端 @RequestParam @NotNull Long）。
  const cropIds = query.cropId.map(toQueryId).filter((id) => id !== null)
  if (!cropIds.length) {
    workTypeOptions.value = []
    return { status: 'ok', options: [] }
  }
  workTypeLoading.value = true
  try {
    const settled = await Promise.allSettled(cropIds.map((cropId) => listFieldWorkOptions(cropId)))
    // 快速连续切换作物时，较慢的旧批次后返回会覆盖新批次，这里统一丢弃过期批次。
    if (seq !== workTypeSeq) return { status: 'stale', options: [] }
    const merged = new Map()
    let firstError = ''
    let okCount = 0
    settled.forEach((result) => {
      if (result.status === 'fulfilled') {
        const response = result.value
        if (response?.code !== undefined && response.code !== 200) {
          firstError = firstError || response.msg || ''
          return
        }
        okCount += 1
        const rows = Array.isArray(response?.data) ? response.data : []
        rows.forEach((item) => {
          if (item && item.id !== undefined && item.id !== null) merged.set(String(item.id), item)
        })
        return
      }
      const error = result.reason
      // 601 = 该作物本来就没有可用作业，属于正常空态，既不报错也不计入成功数。
      if (isWarnError(error)) return
      firstError = firstError || error?.message || error?.payload?.msg || ''
    })
    const options = [...merged.values()]
    workTypeOptions.value = options
    // 只有「所有作物都没能正常拿到选项」时才提示，避免正常空态弹红条。
    if (firstError && okCount === 0 && !options.length) {
      ElMessage.warning('获取作业类型失败：' + firstError)
    } else if (firstError) {
      ElMessage.warning('部分作物的作业类型未能加载：' + firstError)
    }
    // 只要有一个作物正常返回（含空数组），就视为正常态，调用方按 options 清理失效 id。
    return { status: okCount > 0 || !firstError ? 'ok' : 'error', options }
  } catch (error) {
    if (seq !== workTypeSeq) return { status: 'stale', options: [] }
    workTypeOptions.value = []
    ElMessage.error('获取作业类型失败：' + (error?.msg || error?.payload?.msg || error?.message || '未知错误'))
    return { status: 'error', options: [] }
  } finally {
    if (seq === workTypeSeq) workTypeLoading.value = false
  }
}

// 作物与作业类型是联动关系，前端只做“选项失效即清理”，不改后端关联规则。
// 按当前有效的作业类型集合剔除已失效的 workTypeId。
function pruneWorkTypeIds(options) {
  if (!query.workTypeId.length) return
  const validIds = new Set((Array.isArray(options) ? options : []).map((item) => item && String(item.id)))
  query.workTypeId = query.workTypeId.filter((id) => validIds.has(String(id)))
}

// 作物变化后的顺序必须是：先等新的作业类型加载完 -> 用最新选项清理失效 workTypeId -> 再查询。
// 否则会拿上一批作物的选项做清理，残留不属于新作物的作业类型。
async function handleCropChange() {
  const { status, options } = await loadWorkTypeOptions()
  // 被更新批次取代：新批次会自行完成清理与查询，这里直接退出，避免把新写入的 workTypeId 清掉。
  if (status === 'stale') return
  if (status === 'ok') {
    // 正常返回（含“当前作物暂无可用作业类型”这类空结果）：按最新集合剔除失效选择。
    pruneWorkTypeIds(options)
  } else if (query.workTypeId.length) {
    // 真实异常：选项已清空，同步清空残留选中值，避免“下拉无选项但筛选值还在”。
    query.workTypeId = []
  }
  handleQuery()
}

/* ---------------- 新增 / 编辑 ---------------- */

// 字段严格对齐后端 FarmWorkDTO：id / name / area / endTime / phoneNumber /
// cropId(List<Long>) / workTypeId(List<Long>) / landPlotId(List<String>)。
// 注意：不提交 createBy（VO 中是用户名，DTO 要 userId），也不展开列表行对象。
const formVisible = ref(false)
const formMode = ref('create')
const submitting = ref(false)
const formRef = ref(null)
const formWorkTypeOptions = ref([])
const formWorkTypeLoading = ref(false)
const landPlotOptions = ref([])
const landPlotLoading = ref(false)
// 地块是当前用户维度的稳定数据，加载一次即可复用；这里持有进行中的 promise 供并发调用等待。
let landPlotPromise = null
let landPlotLoaded = false
// 回显时若记录关联的作物/作业类型/地块不在当前可选范围内，暂存原始值，避免静默丢失。
const formExtraCropOptions = ref([])
const formExtraWorkTypeOptions = ref([])
const formExtraLandPlotOptions = ref([])
// 表单内的作业类型请求序号，与列表筛选的 workTypeSeq 相互独立。
let formWorkTypeSeq = 0

const EMPTY_FORM = () => ({
  id: '',
  name: '',
  area: undefined,
  endTime: '',
  phoneNumber: '',
  cropId: [],
  workTypeId: [],
  landPlotId: []
})

const form = reactive(EMPTY_FORM())

// 表单内可选的作物 / 作业类型 / 地块：现有选项 + 回显时保留的原始选项（按 id 去重）。
const formCropOptions = computed(() => mergeOptions(cropOptions.value, formExtraCropOptions.value))
const formWorkTypeSelectOptions = computed(() => mergeOptions(formWorkTypeOptions.value, formExtraWorkTypeOptions.value))
const formLandPlotOptions = computed(() => mergeOptions(landPlotOptions.value, formExtraLandPlotOptions.value))
// 仅用于提示：这些关联项不在“本人可选”的范围内。
const formDetachedCrops = computed(() => formExtraCropOptions.value)
const formDetachedWorkTypes = computed(() => formExtraWorkTypeOptions.value)
const formDetachedLandPlots = computed(() => formExtraLandPlotOptions.value)

function mergeOptions(base, extra) {
  const merged = new Map()
  ;(Array.isArray(base) ? base : []).forEach((item) => {
    if (item && item.value !== undefined && item.value !== null) merged.set(String(item.value), item)
  })
  ;(Array.isArray(extra) ? extra : []).forEach((item) => {
    if (item && item.value !== undefined && item.value !== null && !merged.has(String(item.value))) {
      merged.set(String(item.value), item)
    }
  })
  return [...merged.values()]
}

function isEmptyId(value) {
  return value === null || value === undefined || value === ''
}

// 时间格式化：后端 LocalDateTime 走 ISO-8601（无时区），这里按本地时间直接拼出 `YYYY-MM-DDTHH:mm:ss`。
// 不做任何时区加减，旧页面的 +8h 补偿是错的，不要照搬。
function toIsoLocal(date) {
  const padNum = (n) => String(n).padStart(2, '0')
  const day = `${date.getFullYear()}-${padNum(date.getMonth() + 1)}-${padNum(date.getDate())}`
  return `${day}T${padNum(date.getHours())}:${padNum(date.getMinutes())}:${padNum(date.getSeconds())}`
}

// 把后端返回的 endTime（ISO 字符串 / 数组 / 带时区字符串）统一转成选择器需要的字符串。
// 与 formatDateTime 同一套语义：后端 LocalDateTime 无时区，带 Z / +08:00 时剥离后缀、取字面量，不做任何加减。
// 若后端将来改为按 UTC 序列化，这里与展示层需一并调整——当前源码无法证明后端会输出带偏移的值，故不做猜测。
function toFormDateTime(value) {
  if (isEmptyId(value)) return ''
  if (Array.isArray(value)) {
    const [y, m = 1, d = 1, h = 0, mi = 0, s = 0] = value
    return toIsoLocal(new Date(y, m - 1, d, h, mi, s))
  }
  if (typeof value === 'string') {
    // 只截取字面量，不经过 Date 换算，避免时区后缀引发平移。
    const match = value.match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?/)
    if (match) {
      const [, y, m, d, h, mi, s = '00'] = match
      return `${y}-${m}-${d}T${h}:${mi}:${s}`
    }
  }
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : toIsoLocal(date)
}

const PHONE_PATTERN = /^1[3-9]\d{9}$/

const formRules = {
  name: [{ required: true, message: '请输入作业名称', trigger: 'blur' }],
  area: [
    { required: true, message: '请输入作业面积', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value === undefined || value === null || value === '') return callback(new Error('请输入作业面积'))
        const num = Number(value)
        if (!Number.isFinite(num) || num <= 0) return callback(new Error('作业面积必须为大于 0 的数字'))
        callback()
      },
      trigger: ['blur', 'change']
    }
  ],
  endTime: [
    { required: true, message: '请选择截止时间', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (!value) return callback(new Error('请选择截止时间'))
        const date = new Date(String(value).replace('T', ' ').replace(/-/g, '/'))
        if (Number.isNaN(date.getTime())) return callback(new Error('截止时间格式不正确'))
        if (date.getTime() <= Date.now()) return callback(new Error('截止时间必须晚于当前时间'))
        callback()
      },
      trigger: ['blur', 'change']
    }
  ],
  phoneNumber: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const phone = String(value || '').trim()
        if (!phone) return callback(new Error('请输入联系电话'))
        if (!PHONE_PATTERN.test(phone)) return callback(new Error('请输入正确的 11 位手机号'))
        callback()
      },
      trigger: 'blur'
    }
  ]
}

async function loadLandPlots() {
  // 成功加载过一次就复用（即使结果为空数组）；失败不缓存，下次打开可重试。
  if (landPlotLoaded) return
  if (landPlotPromise) return landPlotPromise
  landPlotLoading.value = true
  landPlotPromise = (async () => {
    try {
      // GET /landPlot 必填 current/size，且只返回当前用户创建的地块（管理员同样受限）。
      const response = await get('/landPlot', { current: 1, size: 1000 })
      const rows = Array.isArray(response?.data) ? response.data : []
      landPlotOptions.value = rows
        .filter((item) => item && !isEmptyId(item.id))
        .map((item) => ({
          value: String(item.id),
          label: `${item.landName || '未命名地块'}${item.landArea ? ` · ${item.landArea}亩` : ''}`
        }))
      landPlotLoaded = true
    } catch (error) {
      landPlotOptions.value = []
      ElMessage.error('获取地块列表失败：' + (error?.msg || error?.payload?.msg || error?.message || '未知错误'))
    } finally {
      landPlotLoading.value = false
      landPlotPromise = null
    }
  })()
  return landPlotPromise
}

// 表单内按作物拉取作业类型：多作物逐个请求并按 id 去重合并，与列表筛选同一契约。
// 这里只看“最后一次请求”，避免快速切换作物时旧结果覆盖新结果。
// 与 loadWorkTypeOptions 同理用 allSettled：601（该作物没有可用作业）是正常空态，
// 不能因为它把其他作物已取到的作业类型一起丢掉，也不能弹红条。
async function loadFormWorkTypeOptions() {
  const seq = ++formWorkTypeSeq
  // 同 loadWorkTypeOptions：过滤非法 id，避免用 null 发请求。
  const cropIds = form.cropId.map(toQueryId).filter((id) => id !== null)
  if (!cropIds.length) {
    formWorkTypeOptions.value = []
    return { status: 'ok', options: [] }
  }
  formWorkTypeLoading.value = true
  try {
    const settled = await Promise.allSettled(cropIds.map((cropId) => listFieldWorkOptions(cropId)))
    if (seq !== formWorkTypeSeq) return { status: 'stale', options: [] }
    const merged = new Map()
    let firstError = ''
    let okCount = 0
    settled.forEach((result) => {
      if (result.status === 'fulfilled') {
        const response = result.value
        if (response?.code !== undefined && response.code !== 200) {
          firstError = firstError || response.msg || ''
          return
        }
        okCount += 1
        const rows = Array.isArray(response?.data) ? response.data : []
        rows.forEach((item) => {
          if (item && !isEmptyId(item.id)) merged.set(String(item.id), item)
        })
        return
      }
      const error = result.reason
      if (isWarnError(error)) return
      firstError = firstError || error?.message || error?.payload?.msg || ''
    })
    const options = [...merged.values()]
    formWorkTypeOptions.value = options
    if (firstError && okCount === 0 && !options.length) {
      ElMessage.warning('获取作业类型失败：' + firstError)
    }
    return { status: okCount > 0 || !firstError ? 'ok' : 'error', options }
  } catch (error) {
    if (seq !== formWorkTypeSeq) return { status: 'stale', options: [] }
    formWorkTypeOptions.value = []
    ElMessage.error('获取作业类型失败：' + (error?.msg || error?.payload?.msg || error?.message || '未知错误'))
    return { status: 'error', options: [] }
  } finally {
    if (seq === formWorkTypeSeq) formWorkTypeLoading.value = false
  }
}

// 表单里作物变化：先等选项加载完，再按最新集合剔除失效的作业类型。
// 注意：这里只在用户“主动改动作物”时清理，编辑回显阶段不走此函数，
// 因此不会因为异步选项未就绪而丢掉原有 workTypeId。
async function handleFormCropChange() {
  const { status, options } = await loadFormWorkTypeOptions()
  if (status === 'stale') return
  if (status === 'error') {
    form.workTypeId = []
    formExtraWorkTypeOptions.value = []
    return
  }
  const validIds = new Set(options.map((item) => String(item.id)))
  form.workTypeId = form.workTypeId.filter((id) => validIds.has(String(id)))
  formExtraWorkTypeOptions.value = formExtraWorkTypeOptions.value.filter((item) => validIds.has(item.value))
  formRef.value?.clearValidate?.(['workTypeId'])
}

function resetForm() {
  Object.assign(form, EMPTY_FORM())
  formWorkTypeOptions.value = []
  formExtraCropOptions.value = []
  formExtraWorkTypeOptions.value = []
  formExtraLandPlotOptions.value = []
  formWorkTypeSeq += 1
  formWorkTypeLoading.value = false
  formRef.value?.clearValidate?.()
}

function openCreate() {
  formMode.value = 'create'
  resetForm()
  formVisible.value = true
  // 打开时并行准备下拉数据，失败不阻塞填写。
  loadFormWorkTypeOptions()
  loadLandPlots()
}

// 编辑回显必须“先拿到选项、再做归属判断”，否则 landPlotOptions 尚未加载时
// 会把所有地块都误判成“不在可选范围”，提示信息失去意义。
async function openEdit(row) {
  if (!row || typeof row !== 'object' || Array.isArray(row)) return
  // 已结束/已下架的记录不再进入编辑：后端 update 会带上 status 字段，误操作可能把 1 写回 0。
  if (isWorkEnded(row.status)) {
    ElMessage.warning('该作业已结束，不能再编辑。如需重新发布，请新增一条农田作业。')
    return
  }
  formMode.value = 'edit'
  resetForm()
  form.id = isEmptyId(row.id) ? '' : String(row.id)
  form.name = row.name || ''
  form.area = row.area === null || row.area === undefined ? undefined : Number(row.area)
  form.endTime = toFormDateTime(row.endTime)
  form.phoneNumber = row.phoneNumber || ''

  // 关联 ID 先落进表单（保持原始 ID，绝不因选项缺失而丢弃）。
  const cropNames = Array.isArray(row.cropName) ? row.cropName : []
  const cropIds = Array.isArray(row.cropId) ? row.cropId : []
  cropIds.forEach((id) => {
    if (!isEmptyId(id)) form.cropId.push(String(id))
  })
  const workTypeNames = Array.isArray(row.workTypeName) ? row.workTypeName : []
  const workTypeIds = Array.isArray(row.workTypeId) ? row.workTypeId : []
  workTypeIds.forEach((id) => {
    if (!isEmptyId(id)) form.workTypeId.push(String(id))
  })
  const landPlotNames = Array.isArray(row.landPlotName) ? row.landPlotName : []
  const landPlotIds = Array.isArray(row.landPlotId) ? row.landPlotId : []
  landPlotIds.forEach((id) => {
    if (!isEmptyId(id)) form.landPlotId.push(String(id))
  })

  formVisible.value = true

  // 同时准备地块与作业类型选项；作物选项在页面初始化时已加载，这里只在为空时补一次。
  if (!cropOptions.value.length) await loadCrops()
  await Promise.all([loadLandPlots(), loadFormWorkTypeOptions()])

  // 选项就绪后，仅把“确实不在可选范围”的关联项登记为保留项并给出提示。
  // 名称按 ID 反查原始下标，避免 cropId 中的空值被过滤后索引错位、把别人的名字套到当前 ID 上。
  const cropUsable = cropNames.length === cropIds.length
  form.cropId.forEach((value) => {
    if (cropOptions.value.some((c) => String(c.id) === value)) return
    const index = cropIds.findIndex((id) => String(id) === value)
    const label = cropUsable && index >= 0 && cropNames[index] ? cropNames[index] : `作物 ${value}`
    formExtraCropOptions.value.push({ value, label })
  })

  // 作业类型名称数组可能因后端过滤 null 而与 ID 数组错位，长度不等时不按索引套用名称。
  const workTypeUsable = workTypeNames.length === workTypeIds.length
  form.workTypeId.forEach((value) => {
    if (formWorkTypeOptions.value.some((w) => String(w.id) === value)) return
    const index = workTypeIds.findIndex((id) => String(id) === value)
    const label = workTypeUsable && index >= 0 && workTypeNames[index] ? workTypeNames[index] : `作业类型 ${value}`
    formExtraWorkTypeOptions.value.push({ value, label })
  })

  // landPlotName 只覆盖“能查到详情的地块”，与 landPlotId 不保证一一对应，取不到名称时显示原始 ID。
  form.landPlotId.forEach((value) => {
    if (landPlotOptions.value.some((p) => String(p.value) === value)) return
    const index = landPlotIds.findIndex((id) => String(id) === value)
    const label = landPlotNames[index] || `地块 ${value}`
    formExtraLandPlotOptions.value.push({ value, label })
  })
}

// 组装提交对象：只放 DTO 认识的字段，三个关联数组永远存在（空也传 []）。
function buildSubmitPayload() {
  const cropId = toQueryIds(form.cropId)
  const workTypeId = toQueryIds(form.workTypeId)
  // landPlotId 后端是 List<String>，保持字符串形态，避免雪花 ID 精度丢失。
  const landPlotId = form.landPlotId.map((id) => String(id))
  const payload = {
    name: String(form.name || '').trim(),
    area: Number(form.area),
    endTime: form.endTime,
    phoneNumber: String(form.phoneNumber || '').trim(),
    cropId,
    workTypeId,
    landPlotId
  }
  if (formMode.value === 'edit') payload.id = form.id
  return payload
}

async function submitForm() {
  if (submitting.value) return
  if (!formRef.value) return
  // 编辑分支的 ID 防御：payload 不带 id 时后端 updateById 会匹配 0 行却仍返回成功（假成功）。
  // 注意 "0" 是合法字符串，不当空值处理；只在 null / undefined / 空串时拦截。
  if (formMode.value === 'edit') {
    if (isEmptyId(form.id)) {
      ElMessage.error('缺少作业 ID，无法保存。请关闭弹窗后从列表重新点击「编辑」。')
      return
    }
    if (!/^\d+$/.test(String(form.id))) {
      ElMessage.error(`作业 ID 不合法（${form.id}），无法保存。请关闭弹窗后从列表重新点击「编辑」。`)
      return
    }
  }
  // Element Plus 的 validate() 校验不通过时 reject，这里统一转成布尔值，校验失败不发请求。
  const valid = await formRef.value.validate().then(() => true).catch(() => false)
  if (!valid) return
  const payload = buildSubmitPayload()
  submitting.value = true
  try {
    if (formMode.value === 'create') {
      await addFarmWork(payload)
      ElMessage.success('新增农田作业成功')
    } else {
      await updateFarmWork(payload)
      ElMessage.success('保存农田作业成功')
    }
    formVisible.value = false
    handleQuery()
  } catch (error) {
    // 失败时保留表单内容，仅提示错误。
    ElMessage.error(
      (formMode.value === 'create' ? '新增失败：' : '保存失败：')
      + (error?.msg || error?.payload?.msg || error?.message || '未知错误')
    )
  } finally {
    submitting.value = false
  }
}

// Dialog 关闭后清理临时状态，防止下次打开残留上一次的数据。
function handleFormClosed() {
  submitting.value = false
  resetForm()
}

/* ---------------- 结束作业（逻辑下架） ---------------- */

// 后端 FarmWorkServiceImpl.deleteById 只做一件事：把主表 status 置为 NO_LONGER_AVAILABLE(1)。
// 不物理删除记录、不动 farm_work_crop / farm_work_type / farm_work_land_plot 三张关联表。
// 所以这里绝不清理关联数据、也不把记录从 list 里移除，成功后一律重新查询列表，以后端返回为准。
const endingRowId = ref('')

async function handleEndWork(row) {
  if (!row || typeof row !== 'object' || Array.isArray(row)) return
  const id = isEmptyId(row.id) ? '' : String(row.id)
  if (!id) {
    ElMessage.error('缺少作业 ID，无法结束该作业。')
    return
  }
  // 已结束不再触发请求（按钮已隐藏，这里是二重保护：防止旧 DOM 或键盘操作触发）。
  if (isWorkEnded(row.status)) {
    ElMessage.info('该作业已是结束状态，无需重复操作。')
    return
  }
  // 全局单飞：任一记录正在结束中，其余记录的操作按钮均为 disabled。
  if (endingRowId.value !== '') return

  try {
    await ElMessageBox.confirm(
      `确认结束农田作业「${row.name || id}」？<br/><br/>`
      + '结束后该作业状态将变为「已结束」，不再作为可接单任务展示给农机手。<br/>'
      + '该操作<strong>不是物理删除</strong>：作业记录、作物关联、作业类型关联、地块关联都会保留，可随时在台账中查看。',
      '结束作业确认',
      {
        dangerouslyUseHTMLString: true,
        type: 'warning',
        confirmButtonText: '确认结束',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--warning'
      }
    )
  } catch {
    // 用户取消：不发任何请求。
    return
  }

  endingRowId.value = id
  try {
    await delFarmWork(id)
    ElMessage.success('已结束该农田作业，列表已按后端最新状态刷新。')
    // 重新查询列表，状态显示完全以服务端为准；结束后记录仍在列表中展示为「已结束」。
    await getList()
  } catch (error) {
    ElMessage.error('结束作业失败：' + (error?.msg || error?.payload?.msg || error?.message || '未知错误'))
  } finally {
    endingRowId.value = ''
  }
}

/* ---------------- 详情 ---------------- */

function openDetail(row) {
  // 行数据理论上必为对象，这里做防御：el-table 的 row-click 与按钮点击共用，避免异常入参打断渲染。
  if (!row || typeof row !== 'object' || Array.isArray(row)) return
  detail.value = row
  detailVisible.value = true
}

onMounted(() => {
  getList()
  loadCrops()
})
</script>

<style scoped>
.detail-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
}
.detail-title {
  font-size: 15px;
  font-weight: 650;
  color: var(--text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.detail-sub {
  margin-top: 2px;
  font-size: 10.5px;
  color: var(--text-3);
}
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
  margin-top: 14px;
}
.detail-field {
  min-width: 0;
}
.detail-label {
  font-size: 11px;
  color: var(--text-3);
}
.detail-value {
  margin-top: 2px;
  font-size: 12.5px;
  font-weight: 550;
  color: var(--text-1);
  word-break: break-all;
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
.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 10px;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}
.form-grid .full {
  grid-column: 1 / -1;
}
.empty {
  padding: 18px 0;
  text-align: center;
}
.empty-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-2);
}
.empty .hint {
  margin-top: 4px;
  font-size: 11.5px;
  color: var(--text-3);
}
.mono {
  font-family: var(--font-mono);
  font-size: 11px;
}
:deep(.el-table .cell) {
  padding: 0 6px;
}
:deep(.el-table .el-button.is-link) {
  font-size: 11.5px;
  padding: 0;
}
:deep(.pagination-container) {
  background: transparent;
  padding: 0;
}
</style>
