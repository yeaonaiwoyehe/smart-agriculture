<template>
  <el-dialog
    title="导入地块 · GeoJSON"
    :visible="visible"
    width="min(900px, 94vw)"
    append-to-body
    :close-on-click-modal="false"
    :close-on-press-escape="!busy"
    :show-close="!busy"
    :before-close="closeDialog"
    @closed="reset"
  >
    <div class="geojson-import">
      <p class="intro">上传已有地块边界，先检查文件，再确认导入。想先体验一下？可以直接用示例预检。</p>
      <el-alert
        title="支持 .geojson / .json，最大 10MB，每次最多 1000 块地。请使用 WGS84 经纬度；首版支持无空洞的单面地块。"
        type="info" :closable="false" show-icon
      />
      <div class="file-picker">
        <i class="el-icon-document file-icon" aria-hidden="true"></i>
        <div class="file-info">
          <strong>{{ selectedFile ? selectedFile.name : '选择地块文件' }}</strong>
          <span>{{ selectedFile ? fileSize : '地块编号、地块名称和地址为必填属性。' }}</span>
        </div>
        <input ref="fileInput" class="file-input" type="file" accept=".geojson,.json" :disabled="busy" @change="chooseFile"/>
        <el-button :disabled="busy" icon="el-icon-folder-opened" @click="$refs.fileInput.click()">选择文件</el-button>
      </div>
      <div class="file-actions">
        <el-button type="primary" :loading="previewing" :disabled="!selectedFile || importing || loadingSample" @click="preview">开始预检</el-button>
        <el-button :loading="loadingSample" :disabled="previewing || importing" @click="trySample">用示例预检</el-button>
        <a :href="sampleUrl" download="地块导入示例.geojson" class="sample-link">下载示例文件</a>
      </div>
      <el-alert v-if="errorMessage" class="result-message" :title="errorMessage" type="error" :closable="false" show-icon/>
      <template v-if="result">
        <div class="summary" aria-live="polite">
          <div><span>地块总数</span><strong>{{ result.totalCount }}</strong></div>
          <div><span>校验通过</span><strong class="valid">{{ result.validCount }}</strong></div>
          <div><span>需要修正</span><strong :class="{ invalid: result.invalidCount > 0 }">{{ result.invalidCount }}</strong></div>
          <div><span>已导入</span><strong>{{ result.importedCount }}</strong></div>
        </div>
        <el-alert v-if="result.importedCount > 0" title="导入完成，地块列表已刷新。" type="success" :closable="false" show-icon/>
        <el-alert v-else-if="result.invalidCount === 0 && !errorMessage"
          title="文件校验通过，尚未入库。正式导入时还会检查地块编号是否已被使用。"
          type="success" :closable="false" show-icon/>
        <p v-if="result.invalidCount > 0" class="validation-help">请按下表修正文件，再重新选择并预检。本次没有导入任何地块。</p>
        <el-table :data="result.items" border max-height="300" class="result-table" empty-text="暂无校验结果">
          <el-table-column prop="index" label="序号" width="60"/>
          <el-table-column prop="plotCode" label="地块编号" min-width="140" show-overflow-tooltip/>
          <el-table-column prop="landName" label="地块名称" min-width="130" show-overflow-tooltip/>
          <el-table-column label="状态" width="100">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 'INVALID' ? 'danger' : 'success'" size="small">{{ statusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="校验说明" min-width="220"/>
        </el-table>
        <el-alert v-for="(warning, index) in result.warnings" :key="index" class="result-message" :title="warning" type="warning" :closable="false" show-icon/>
      </template>
    </div>
    <div slot="footer">
      <el-button :disabled="busy" @click="closeDialog">关闭</el-button>
      <el-button type="primary" :disabled="!canImport" :loading="importing" @click="confirmImport">确认导入</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { importGeoJsonLandPlots } from '@/api/field/square'

export default {
  name: 'GeoJsonImportDialog',
  props: { visible: { type: Boolean, default: false } },
  data() {
    return { selectedFile: null, result: null, errorMessage: '', previewing: false, importing: false, loadingSample: false }
  },
  computed: {
    busy() { return this.previewing || this.importing || this.loadingSample },
    sampleUrl() { return `${process.env.BASE_URL || '/'}land-plots-example.geojson` },
    fileSize() { return `${(this.selectedFile.size / 1024).toFixed(1)} KB` },
    canImport() {
      return !this.busy && !!this.selectedFile && !!this.result && this.result.dryRun === true &&
        this.result.invalidCount === 0 && this.result.totalCount > 0 && !this.errorMessage
    }
  },
  methods: {
    reset() {
      this.selectedFile = null
      this.result = null
      this.errorMessage = ''
      if (this.$refs.fileInput) this.$refs.fileInput.value = ''
    },
    closeDialog() {
      if (!this.busy) this.$emit('update:visible', false)
    },
    setFile(file) {
      this.result = null
      this.errorMessage = ''
      this.selectedFile = null
      if (!file) return false
      if (!/\.(geojson|json)$/i.test(file.name) || !file.size || file.size > 10 * 1024 * 1024) {
        this.errorMessage = '请选择非空的 .geojson 或 .json 文件，大小不能超过 10MB。'
        return false
      }
      this.selectedFile = file
      return true
    },
    chooseFile(event) {
      this.setFile(event.target.files[0])
      // 同一文件在外部修改后，仍可再次选择并重新校验。
      event.target.value = ''
    },
    friendlyError(message) {
      const text = String(message || '请求失败，请稍后重试。')
      if (text.includes('geojson_import_code_ddl.sql')) return '正式导入功能尚未初始化，请联系管理员。文件预检仍可使用。'
      if (text.includes('No static resource') || text.includes('404')) return '导入接口尚未就绪，请重启后端后重试。'
      return text
    },
    applyResponse(response) {
      this.result = response.data && Array.isArray(response.data.items) ? response.data : null
      this.errorMessage = response.code === 200 ? '' : this.friendlyError(response.msg)
    },
    async preview() {
      if (!this.selectedFile || this.previewing || this.importing) return
      this.previewing = true
      this.result = null
      this.errorMessage = ''
      try {
        this.applyResponse(await importGeoJsonLandPlots(this.selectedFile, true))
      } catch (error) {
        this.errorMessage = this.friendlyError(error.message || error)
      } finally {
        this.previewing = false
      }
    },
    async trySample() {
      if (this.busy) return
      this.loadingSample = true
      this.result = null
      this.errorMessage = ''
      try {
        const response = await fetch(this.sampleUrl)
        if (!response.ok) throw new Error('示例文件加载失败，请稍后重试。')
        const file = new File([await response.blob()], '地块导入示例.geojson', { type: 'application/geo+json' })
        if (this.setFile(file)) await this.preview()
      } catch (error) {
        this.errorMessage = this.friendlyError(error.message || error)
      } finally {
        this.loadingSample = false
      }
    },
    async confirmImport() {
      if (!this.canImport) return
      const file = this.selectedFile
      this.importing = true
      try {
        await this.$confirm(`将导入文件中的 ${this.result.totalCount} 块地，写入团队共享数据。确认继续吗？`, '确认导入地块', {
          confirmButtonText: '确认导入', cancelButtonText: '取消', type: 'warning'
        })
        this.errorMessage = ''
        const response = await importGeoJsonLandPlots(file, false)
        this.applyResponse(response)
        if (response.code === 200 && this.result && this.result.importedCount > 0) this.$emit('imported')
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') this.errorMessage = this.friendlyError(error.message || error)
      } finally {
        this.importing = false
      }
    },
    statusText(status) { return { READY: '校验通过', INVALID: '需要修正', IMPORTED: '已导入' }[status] || status }
  }
}
</script>

<style scoped>
.intro { margin: 0 0 16px; color: #475569; font-size: 14px; line-height: 1.8; }
.file-picker { display: flex; gap: 14px; align-items: center; padding: 22px 18px; margin-top: 20px; border: 1px dashed #b5c9e0; border-radius: 8px; background: #f7faff; }
.file-icon { font-size: 32px; color: #409eff; }
.file-info { flex: 1; min-width: 0; }
.file-info strong, .file-info span { display: block; overflow-wrap: anywhere; }
.file-info strong { color: #263445; font-size: 15px; margin-bottom: 6px; }
.file-info span { font-size: 13px; color: #64748b; }
.file-input { display: none; }
.file-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; margin: 16px 0 20px; }
.file-actions .el-button + .el-button { margin-left: 0; }
.sample-link { color: #1677c7; font-size: 14px; padding: 8px; }
.summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin: 20px 0; }
.summary div { padding: 14px 18px; border: 1px solid #e2e8f0; border-radius: 6px; }
.summary span { display: block; color: #64748b; font-size: 13px; }
.summary strong { display: block; margin-top: 6px; color: #27364a; font-size: 24px; }
.summary .valid { color: #28844a; }
.summary .invalid { color: #c53535; }
.result-table { margin-top: 16px; }
.result-message { margin-top: 14px; }
.validation-help { color: #b63737; line-height: 1.7; }
@media (max-width: 600px) { .summary { grid-template-columns: repeat(2, 1fr); } .file-picker { flex-wrap: wrap; } }
</style>