<template>
  <div class="page">
    <PageHeader
      title="Agent 知识库管理"
      code="AI-CENTER"
      group="AI 智能体"
      desc="按公开制度、内部管理办法、结算与单价规则三类管理知识文档，检索时依据当前用户角色过滤知识范围。"
    >
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Upload" @click="uploadOpen = true">上传文档</el-button>
      </template>
    </PageHeader>

    <div class="grid cols-4">
      <StatCard label="知识文档" :value="docs.length" unit="份" icon="Collection" foot="3 个知识域" />
      <StatCard label="知识切片" :value="totalChunks" unit="段" icon="Files" tone="blue" foot="按 512 token 切分" />
      <StatCard label="已向量化" :value="vectorized" unit="份" icon="CircleCheck" foot="向量库：Milvus" />
      <StatCard label="处理中" :value="docs.length - vectorized" unit="份" icon="Loading" tone="amber" foot="预计 2 分钟内完成" />
    </div>

    <div class="kb-grid mt-3">
      <PanelCard title="文档列表" :sub="`共 ${filteredDocs.length} 份 · 版本与权限可追溯`" flush>
        <div class="kb-filter">
          <div class="seg">
            <button v-for="c in categories" :key="c" :class="{ active: category === c }" @click="category = c">{{ c }}</button>
          </div>
          <el-input v-model="keyword" size="small" placeholder="搜索文档名称" clearable style="width: 200px" :prefix-icon="Search" />
        </div>
        <el-table :data="filteredDocs" size="small">
          <el-table-column label="文档" min-width="230">
            <template #default="{ row }">
              <div class="doc-name">
                <div class="doc-icon"><el-icon :size="14"><Document /></el-icon></div>
                <div style="min-width: 0">
                  <div class="cell-main" style="font-size: 12px">{{ row.name }}</div>
                  <div class="cell-sub">{{ row.category }} · {{ row.size }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="version" label="版本" width="80">
            <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ row.version }}</span></template>
          </el-table-column>
          <el-table-column prop="chunks" label="切片" width="60" align="right" />
          <el-table-column prop="status" label="向量化" width="92">
            <template #default="{ row }"><StatusTag :status="row.status" /></template>
          </el-table-column>
          <el-table-column label="可见角色" width="130">
            <template #default="{ row }">
              <span v-for="r in row.roles" :key="r" class="tag no-dot gray" style="height: 18px; font-size: 9.5px; margin-right: 3px">{{ roleName(r) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新" width="90" />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openVersions(row)">版本</el-button>
              <el-button size="small" text @click="openPerm(row)">权限</el-button>
            </template>
          </el-table-column>
        </el-table>
      </PanelCard>

      <div class="card kb-test">
        <div class="panel-head">
          <div>
            <div class="panel-title"><span class="dot" />知识检索测试</div>
            <div class="panel-sub">按当前角色过滤后召回，展示来源与条款</div>
          </div>
        </div>
        <div class="kb-test-body">
          <div class="role-note">
            <span class="tag green">{{ store.roleMeta.name }}</span>
            <span class="hint">当前可见知识域：{{ visibleCategories }}</span>
          </div>
          <el-input v-model="query" type="textarea" :rows="3" placeholder="例如：质量不达标如何扣减？" />
          <el-button type="primary" size="small" style="width: 100%; margin-top: 8px" :loading="searching" @click="doSearch">检索知识库</el-button>

          <div v-if="hits.length" class="kb-hits">
            <div class="hint" style="font-size: 10.5px; margin-bottom: 6px">命中 {{ hits.length }} 条切片（已按角色过滤）</div>
            <div v-for="(h, i) in hits" :key="i" class="kb-hit">
              <div class="kb-hit-head">
                <span class="kb-score">{{ relevance(i) }}%</span>
                <span>{{ h.doc }}</span>
                <span class="mono hint" style="font-size: 9.5px">{{ h.version }}</span>
              </div>
              <div class="kb-hit-clause">{{ h.clause }} · {{ h.category }}</div>
              <div class="kb-hit-text">{{ h.text }}</div>
            </div>
          </div>
          <div v-else-if="searched" class="empty" style="padding: 24px 6px; font-size: 11.5px">
            当前权限范围内没有命中，请更换问法或联系管理员补充文档。
          </div>
          <div v-else class="hint" style="font-size: 11px; line-height: 1.7; margin-top: 10px">
            检索结果只用于问答引用；结算金额、面积等数值仍由业务接口返回，模型不得自行推算。
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="uploadOpen" title="上传知识文档" width="520px">
      <el-form label-position="top">
        <el-form-item label="文档">
          <el-upload drag :auto-upload="false" :limit="1" :on-change="onFile" style="width: 100%">
            <el-icon :size="28" style="color: var(--brand-500)"><UploadFilled /></el-icon>
            <div style="font-size: 12px; margin-top: 6px">拖拽 PDF / Word / TXT 到此处，或点击选择</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="知识分类">
          <el-select v-model="uploadForm.category" style="width: 100%">
            <el-option v-for="c in categories.slice(1)" :key="c" :value="c" :label="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="可见角色">
          <el-checkbox-group v-model="uploadForm.roles">
            <el-checkbox value="admin">管理员</el-checkbox>
            <el-checkbox value="leader">生产队长</el-checkbox>
            <el-checkbox value="finance">财务</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadOpen = false">取消</el-button>
        <el-button type="primary" @click="confirmUpload">上传并向量化</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="versionOpen" :title="`版本历史 · ${currentDoc?.name}`" size="440px">
      <el-timeline>
        <el-timeline-item v-for="(v, i) in versions" :key="i" :timestamp="v.at" :type="i === 0 ? 'primary' : ''">
          <div class="strong" style="font-size: 12px">{{ v.version }}</div>
          <div class="hint" style="font-size: 11px">{{ v.note }} · {{ v.user }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>

    <el-dialog v-model="permOpen" :title="`知识权限 · ${currentDoc?.name}`" width="420px">
      <div class="hint" style="font-size: 11.5px; margin-bottom: 8px">未勾选的角色在检索时不会召回该文档切片。</div>
      <el-checkbox-group v-model="permRoles">
        <el-checkbox value="admin" style="display: block; margin: 6px 0">公司管理员</el-checkbox>
        <el-checkbox value="leader" style="display: block; margin: 6px 0">片区生产队长</el-checkbox>
        <el-checkbox value="finance" style="display: block; margin: 6px 0">财务人员</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="permOpen = false">取消</el-button>
        <el-button type="primary" @click="savePerm">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { Refresh, Search, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { getKnowledge, searchKnowledge } from '@/services/api'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'

const store = useAppStore()
const docs = ref([])
const chunks = ref([])
const category = ref('全部')
const keyword = ref('')
const query = ref('')
const hits = ref([])
const searching = ref(false)
const searched = ref(false)
const uploadOpen = ref(false)
const versionOpen = ref(false)
const permOpen = ref(false)
const currentDoc = ref(null)
const permRoles = ref([])
const uploadForm = reactive({ category: '公开制度', roles: ['admin', 'leader', 'finance'] })
const pendingFile = ref(null)
let vecTimer = null

const categories = ['全部', '公开制度', '内部管理办法', '结算与单价规则']
const totalChunks = computed(() => docs.value.reduce((s, d) => s + d.chunks, 0))
const vectorized = computed(() => docs.value.filter((d) => d.status === '已向量化').length)
const filteredDocs = computed(() =>
  docs.value.filter((d) => (category.value === '全部' || d.category === category.value) && (!keyword.value || d.name.includes(keyword.value)))
)
const visibleCategories = computed(() => {
  const set = new Set(chunks.value.filter((c) => c.roles.includes(store.role)).map((c) => c.category))
  return [...set].join('、') || '无'
})
const versions = computed(() => [
  { version: currentDoc.value?.version || 'V1.0', at: currentDoc.value?.updatedAt || '2026-11-01', note: '当前生效版本，已完成向量化', user: currentDoc.value?.uploader },
  { version: 'V1.0', at: '2026-09-12', note: '首次上传并切片', user: '王建民' },
  { version: 'V0.9', at: '2026-08-20', note: '草稿版本，未生效', user: '王建民' }
])

const roleName = (r) => ({ admin: '管理员', leader: '队长', finance: '财务' }[r] || r)
const relevance = (i) => 96 - i * 4

async function load() {
  const d = await getKnowledge()
  docs.value = d.docs
  chunks.value = d.chunks
}
onMounted(load)

async function doSearch() {
  if (!query.value.trim()) return ElMessage.warning('请输入要检索的问题')
  searching.value = true
  try {
    hits.value = await searchKnowledge(query.value, store.role)
    searched.value = true
  } finally {
    searching.value = false
  }
}
function onFile(file) {
  pendingFile.value = file
}
function confirmUpload() {
  if (!pendingFile.value) return ElMessage.warning('请选择要上传的文档')
  const name = pendingFile.value.name
  const doc = {
    id: `K${String(docs.value.length + 1).padStart(2, '0')}`,
    name,
    category: uploadForm.category,
    version: 'V1.0',
    chunks: 0,
    status: '处理中',
    updatedAt: '2026-11-23',
    uploader: store.roleMeta.user,
    roles: [...uploadForm.roles],
    size: `${(pendingFile.value.size / 1024).toFixed(0)} KB`
  }
  docs.value.unshift(doc)
  uploadOpen.value = false
  pendingFile.value = null
  ElMessage.success('文档已进入解析队列')
  vecTimer = setTimeout(() => {
    doc.status = '已向量化'
    doc.chunks = 24
    ElMessage.success(`${name} 向量化完成，可被 Agent 检索`)
  }, 2600)
}
function openVersions(doc) {
  currentDoc.value = doc
  versionOpen.value = true
}
function openPerm(doc) {
  currentDoc.value = doc
  permRoles.value = [...doc.roles]
  permOpen.value = true
}
function savePerm() {
  currentDoc.value.roles = [...permRoles.value]
  permOpen.value = false
  ElMessage.success('知识权限已更新，检索过滤将在下一次调用生效')
}
onBeforeUnmount(() => vecTimer && clearTimeout(vecTimer))
</script>

<style scoped>
.kb-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.62fr) minmax(0, 1fr);
  gap: 14px;
}
.kb-filter {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--border);
}
.kb-test {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.kb-test-body {
  padding: 12px 16px 16px;
}
.role-note {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  padding: 7px 9px;
  border-radius: 8px;
  background: var(--surface-2);
}
.role-note .hint {
  font-size: 10.5px;
}
.kb-hits {
  margin-top: 12px;
  max-height: 330px;
  overflow-y: auto;
}
.kb-hit {
  padding: 9px 10px;
  border: 1px solid var(--border);
  border-radius: 9px;
  margin-bottom: 7px;
  background: #fff;
}
.kb-hit:hover {
  border-color: var(--brand-300);
}
.kb-hit-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
}
.kb-score {
  background: var(--brand-100);
  color: var(--brand-700);
  border-radius: 4px;
  padding: 1px 5px;
  font-size: 9.5px;
}
.kb-hit-clause {
  font-size: 10px;
  color: var(--text-3);
  margin-top: 2px;
}
.kb-hit-text {
  font-size: 11px;
  color: var(--text-2);
  line-height: 1.6;
  margin-top: 4px;
}
.doc-name {
  display: flex;
  align-items: center;
  gap: 8px;
}
.doc-icon {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  background: var(--brand-50);
  color: var(--brand-600);
  display: grid;
  place-items: center;
  flex: none;
}
</style>
