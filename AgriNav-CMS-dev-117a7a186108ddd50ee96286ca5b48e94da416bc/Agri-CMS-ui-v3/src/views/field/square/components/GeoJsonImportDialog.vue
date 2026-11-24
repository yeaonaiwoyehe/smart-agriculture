<template>
  <el-dialog
    title="导入地块 · GeoJSON / Shapefile"
    :model-value="visible"
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
        title="支持 GeoJSON 或 Shapefile ZIP，最大 10MB，每次最多 1000 块地。请使用 WGS84 经纬度；首版支持无空洞的单面地块。"
        type="info" :closable="false" show-icon
      />
      <div class="file-picker">
        <el-icon class="file-icon" aria-hidden="true"><Document/></el-icon>
        <div class="file-info">
          <strong>{{ selectedFile ? selectedFile.name : '选择地块文件' }}</strong>
          <span>{{ selectedFile ? fileSize : '地块编号、地块名称和地址为必填属性。' }}</span>
        </div>
        <input ref="fileInput" class="file-input" type="file" accept=".geojson,.json,.zip" :disabled="busy" @change="chooseFile"/>
        <el-button :disabled="busy" icon="FolderOpened" @click="$refs.fileInput.click()">选择文件</el-button>
      </div>
      <div v-if="isShapefile" class="shape-options">
        <p>ZIP 内需包含同名 .shp、.shx、.dbf、.prj 文件；属性列为 plotCode、landName、address，可选 landType。</p>
        <label for="shape-encoding">属性文字编码</label>
        <el-select id="shape-encoding" v-model="encoding" size="small" :disabled="busy" @change="invalidatePreview">
          <el-option label="UTF-8" value="UTF-8"/><el-option label="GBK（常见中文数据）" value="GBK"/><el-option label="GB18030" value="GB18030"/>
        </el-select>
        <span>如有 .cpg 文件，优先使用文件声明的编码。</span>
      </div>
      <div class="file-actions">
        <el-button type="primary" :loading="previewing" :disabled="!selectedFile || importing || loadingSample" @click="preview">开始预检</el-button>
        <el-dropdown :disabled="busy" @command="trySample">
          <el-button :loading="loadingSample" :disabled="busy">用示例预检 <el-icon class="el-icon--right"><ArrowDown/></el-icon></el-button>
          <template #dropdown><el-dropdown-menu>
            <el-dropdown-item command="geojson">GeoJSON 示例</el-dropdown-item>
            <el-dropdown-item command="shapefile">Shapefile ZIP 示例</el-dropdown-item>
          </el-dropdown-menu></template>
        </el-dropdown>
        <a :href="sampleUrl" download="地块导入示例.geojson" class="sample-link">下载 GeoJSON 示例</a>
        <a :href="shapeSampleUrl" download="地块导入示例.zip" class="sample-link">下载 ZIP 示例</a>
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
            <template #default="scope">
              <el-tag :type="scope.row.status === 'INVALID' ? 'danger' : 'success'" size="small">{{ statusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="校验说明" min-width="220"/>
        </el-table>
        <el-alert v-for="(warning, index) in result.warnings" :key="index" class="result-message" :title="warning" type="warning" :closable="false" show-icon/>
      </template>
    </div>
    <template #footer><div>
      <el-button :disabled="busy" @click="closeDialog">关闭</el-button>
      <el-button type="primary" :disabled="!canImport" :loading="importing" @click="confirmImport">确认导入</el-button>
    </div></template>
  </el-dialog>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { importGeoJsonLandPlots } from '@/api/field/square'

export default {
  name: 'GeoJsonImportDialog',
  emits: ["update:visible","imported"],
  props: { visible: { type: Boolean, default: false } },
  data() {
    return { selectedFile: null, encoding: 'UTF-8', result: null, errorMessage: '', previewing: false, importing: false, loadingSample: false }
  },
  computed: {
    isShapefile() { return !!this.selectedFile && /\.zip$/i.test(this.selectedFile.name) },
    busy() { return this.previewing || this.importing || this.loadingSample },
    sampleUrl() { return `${import.meta.env.BASE_URL || '/'}land-plots-example.geojson` },
    shapeSampleUrl() { return `${import.meta.env.BASE_URL || '/'}land-plots-example.zip` },
    fileSize() { return `${(this.selectedFile.size / 1024).toFixed(1)} KB` },
    canImport() {
      return !this.busy && !!this.selectedFile && !!this.result && this.result.dryRun === true &&
        this.result.invalidCount === 0 && this.result.totalCount > 0 && !this.errorMessage
    }
  },
  methods: {
    invalidatePreview() { this.result = null; this.errorMessage = '' },
    reset() {
      this.selectedFile = null
      this.encoding = 'UTF-8'
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
      if (!/\.(geojson|json|zip)$/i.test(file.name) || !file.size || file.size > 10 * 1024 * 1024) {
        this.errorMessage = '请选择非空的 .geojson、.json 或 .zip 文件，大小不能超过 10MB。'
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
        this.applyResponse(await importGeoJsonLandPlots(this.selectedFile, true, this.encoding))
      } catch (error) {
        this.errorMessage = this.friendlyError(error.message || error)
      } finally {
        this.previewing = false
      }
    },
    async trySample(format = 'geojson') {
      if (this.busy) return
      this.loadingSample = true
      this.result = null
      this.errorMessage = ''
      try {
        const shape = format === 'shapefile'
        const response = await fetch(shape ? this.shapeSampleUrl : this.sampleUrl)
        if (!response.ok) throw new Error('示例文件加载失败，请稍后重试。')
        const file = new File([await response.blob()], shape ? '地块导入示例.zip' : '地块导入示例.geojson', { type: shape ? 'application/zip' : 'application/geo+json' })
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
        await ElMessageBox.confirm(`将导入文件中的 ${this.result.totalCount} 块地，写入团队共享数据。确认继续吗？`, '确认导入地块', {
          confirmButtonText: '确认导入', cancelButtonText: '取消', type: 'warning'
        })
        this.errorMessage = ''
        const response = await importGeoJsonLandPlots(file, false, this.encoding)
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
.shape-options { margin-top: 14px; padding: 12px; background: #f5f8fc; color: #52677f; font-size: 13px; line-height: 1.8; }
.shape-options p { margin: 0 0 10px; }
.shape-options label { margin-right: 12px; }
.shape-options span { display: block; margin-top: 6px; }
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
