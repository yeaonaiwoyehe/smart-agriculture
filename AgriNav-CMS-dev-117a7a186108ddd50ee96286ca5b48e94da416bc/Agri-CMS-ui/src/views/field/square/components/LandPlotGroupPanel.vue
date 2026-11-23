<template>
  <section class="plot-groups" aria-label="地块分组" v-loading="loading">
    <div class="group-toolbar">
      <strong>地块分组</strong>
      <el-radio-group v-model="type" size="small" aria-label="分组类型" @change="changeType">
        <el-radio-button label="VILLAGE">行政村</el-radio-button>
        <el-radio-button label="REGION">片区</el-radio-button>
      </el-radio-group>
      <el-select v-model="selectedGroup" :disabled="!available || loading" placeholder="选择分组" size="small" aria-label="选择分组" @change="emitFilter">
        <el-option label="全部分组" value="__ALL__"/>
        <el-option v-for="group in groups" :key="group.groupCode || '__NONE__'" :value="group.groupCode || '__NONE__'"
                   :label="`${group.groupName}（${group.plotCount} 块）`"/>
      </el-select>
      <el-button size="small" :disabled="!available || saving" v-hasPermi="['field:square:update']" @click="openCreate">新建{{ typeLabel }}</el-button>
      <el-button type="primary" plain size="small" :disabled="!available || !selectedIds.length || saving"
                 v-hasPermi="['field:square:update']" @click="openAssignment">批量归组{{ selectedIds.length ? `（${selectedIds.length}）` : '' }}</el-button>
    </div>
    <p class="group-caption" v-if="available">
      <span class="group-stat">{{ currentSummary.name }}：<b>{{ currentSummary.count }}</b> 块 · <b>{{ area(currentSummary.area) }}</b> 亩</span>
      <span>统计本人全部未删除地块。选中列表中的地块后可批量归组。</span>
    </p>
    <el-alert v-if="error" :title="error" type="warning" :closable="false" show-icon/>

    <el-dialog :title="`新建${typeLabel}`" :visible.sync="createVisible" width="min(460px, 94vw)" append-to-body :close-on-click-modal="false" :show-close="!saving">
      <el-form label-position="top" @submit.native.prevent>
        <el-form-item :label="type === 'VILLAGE' ? '12位村级区划代码' : '片区编号'">
          <el-input v-model="draftCode" :maxlength="type === 'VILLAGE' ? 12 : 64" :disabled="saving" :placeholder="type === 'VILLAGE' ? '填写数据提供方给出的村级代码' : '例如 AREA-NORTH'"/>
        </el-form-item>
        <el-form-item :label="`${typeLabel}名称`"><el-input v-model="draftName" maxlength="100" :disabled="saving" placeholder="填写完整名称，便于区分同名地点"/></el-form-item>
      </el-form>
      <p class="dialog-help">编号用于识别同一个{{ typeLabel }}。已有编号不能另存为其他名称。</p>
      <el-alert v-if="dialogError" :title="dialogError" type="error" :closable="false"/>
      <span slot="footer"><el-button :disabled="saving" @click="createVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveGroup">保存分组</el-button></span>
    </el-dialog>

    <el-dialog :title="`批量设置${typeLabel}`" :visible.sync="assignmentVisible" width="min(460px, 94vw)" append-to-body :close-on-click-modal="false" :show-close="!saving">
      <p>将更新选中的 <strong>{{ assignmentIds.length }}</strong> 块地。每块地可以分别归属一个行政村和一个片区。</p>
      <el-select v-model="assignmentCode" :disabled="saving" placeholder="选择目标分组" class="assignment-select" aria-label="目标分组">
        <el-option label="移出当前类别分组" value="__NONE__"/>
        <el-option v-for="group in namedGroups" :key="group.groupCode" :label="`${group.groupName} · ${group.groupCode}`" :value="group.groupCode"/>
      </el-select>
      <p class="dialog-help">确认后修改团队共享数据中的归属关系，仅能操作您创建的未删除地块。</p>
      <el-alert v-if="dialogError" :title="dialogError" type="error" :closable="false"/>
      <span slot="footer"><el-button :disabled="saving" @click="assignmentVisible = false">取消</el-button><el-button type="primary" :disabled="!assignmentCode" :loading="saving" @click="saveAssignment">确认归组</el-button></span>
    </el-dialog>
  </section>
</template>

<script>
import { getPlotGroups, getPlotGroupAssignments, createPlotGroup, assignPlotGroup } from '@/api/field/plotGroups'

export default {
  name: 'LandPlotGroupPanel',
  props: { selectedIds: { type: Array, default: () => [] }, plotIds: { type: Array, default: () => [] } },
  data() {
    return { type: 'VILLAGE', selectedGroup: '__ALL__', groups: [], available: false, loading: false, saving: false,
      error: '', dialogError: '', createVisible: false, assignmentVisible: false, draftCode: '', draftName: '',
      assignmentCode: '', assignmentIds: [], summaryRequest: 0, assignmentRequest: 0 }
  },
  computed: {
    typeLabel() { return this.type === 'VILLAGE' ? '行政村' : '片区' },
    namedGroups() { return this.groups.filter(g => g.groupCode !== null) },
    currentSummary() {
      if (this.selectedGroup === '__ALL__') return { name: '全部', count: this.groups.reduce((n, g) => n + Number(g.plotCount), 0), area: this.groups.reduce((n, g) => n + Number(g.totalArea), 0) }
      const group = this.groups.find(g => (g.groupCode || '__NONE__') === this.selectedGroup)
      return group ? { name: group.groupName, count: group.plotCount, area: group.totalArea } : { name: '当前分组', count: 0, area: 0 }
    }
  },
  watch: { plotIds: { immediate: true, handler() { this.loadAssignments() } } },
  mounted() { this.refresh() },
  beforeDestroy() { this.summaryRequest++; this.assignmentRequest++ },
  methods: {
    area(value) { return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2, minimumFractionDigits: 2 }) },
    message(error) {
      const text = String(error.message || error)
      const unavailable = /404|No static resource/.test(text) || (text.includes('参数[id]') && text.includes('groups'))
      return unavailable ? '分组接口暂不可用，请确认后端已更新并重启。' : text
    },
    checked(response) { if (response.code !== 200) throw new Error(response.msg || '操作失败'); return response.data },
    async refresh() {
      const request = ++this.summaryRequest
      this.loading = true
      try {
        const data = this.checked(await getPlotGroups(this.type))
        if (request !== this.summaryRequest) return
        this.available = data.available
        this.groups = data.groups
        this.error = data.available ? '' : '分组功能尚未初始化，请联系管理员。'
      } catch (error) { if (request === this.summaryRequest) { this.error = this.message(error); this.available = false } }
      finally { if (request === this.summaryRequest) this.loading = false }
    },
    async loadAssignments() {
      const request = ++this.assignmentRequest
      if (!this.plotIds.length) { this.$emit('names', {}); return }
      const placeholders = label => Object.fromEntries(this.plotIds.map(id => [id, { VILLAGE: label, REGION: label }]))
      this.$emit('names', placeholders('加载中'))
      try {
        const rows = this.checked(await getPlotGroupAssignments(this.plotIds))
        if (request !== this.assignmentRequest) return
        const names = {}
        rows.forEach(row => { if (!names[row.plotId]) names[row.plotId] = {}; names[row.plotId][row.groupType] = row.groupName })
        this.$emit('names', names)
      } catch (error) { if (request === this.assignmentRequest) this.$emit('names', placeholders('暂不可用')) }
    },
    clearFilter() { this.selectedGroup = '__ALL__' },
    changeType() { this.clearFilter(); this.emitFilter(); this.refresh() },
    emitFilter() {
      this.$emit('filter', this.selectedGroup === '__ALL__' ? {} : {
        groupType: this.type, groupCode: this.selectedGroup === '__NONE__' ? undefined : this.selectedGroup, ungrouped: this.selectedGroup === '__NONE__'
      })
    },
    openCreate() { this.draftCode = ''; this.draftName = ''; this.dialogError = ''; this.createVisible = true },
    openAssignment() { this.assignmentIds = this.selectedIds.map(String); this.assignmentCode = ''; this.dialogError = ''; this.assignmentVisible = true },
    async saveGroup() {
      if (this.saving) return
      const code = this.draftCode.trim(), name = this.draftName.trim()
      const validCode = this.type === 'VILLAGE' ? /^[0-9]{12}$/.test(code) : /^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$/.test(code)
      if (!validCode || !name || name.length > 100 || /[\x00-\x1f\x7f]/.test(name)) { this.dialogError = '请填写有效编号和名称。村级代码为12位数字；片区编号使用字母、数字、下划线或短横线。'; return }
      this.saving = true; this.dialogError = ''
      try {
        this.checked(await createPlotGroup({ type: this.type, code, name }))
        this.createVisible = false; this.$message.success('分组已保存'); await this.refresh()
      } catch (error) { this.dialogError = this.message(error) }
      finally { this.saving = false }
    },
    async saveAssignment() {
      if (this.saving || !this.assignmentCode) return
      this.saving = true; this.dialogError = ''
      try {
        this.checked(await assignPlotGroup({ type: this.type, groupCode: this.assignmentCode === '__NONE__' ? null : this.assignmentCode, plotIds: this.assignmentIds }))
        this.assignmentVisible = false; this.$message.success('归组已更新'); await this.refresh(); this.$emit('changed')
      } catch (error) { this.dialogError = this.message(error) }
      finally { this.saving = false }
    }
  }
}
</script>

<style scoped>
.plot-groups { padding: 16px 18px; margin: 4px 0 20px; border: 1px solid #dce7e3; border-radius: 8px; background: #f7fbf9; }
.group-toolbar { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; }
.group-toolbar strong { color: #244a3d; font-size: 16px; }
.group-toolbar .el-select { min-width: 220px; }
.group-toolbar .el-button { margin-left: 0; }
.group-caption { display: flex; flex-wrap: wrap; gap: 8px 20px; color: #65776f; font-size: 13px; margin: 14px 0 0; line-height: 1.7; }
.group-stat { font-size: 14px; color: #344f43; }
.plot-groups > .el-alert { margin-top: 12px; }
.assignment-select { width: 100%; margin: 12px 0; }
.dialog-help { font-size: 13px; line-height: 1.8; color: #66766e; }
@media (max-width: 600px) { .group-toolbar .el-select { width: 100%; } }
</style>
