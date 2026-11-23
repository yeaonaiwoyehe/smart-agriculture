<template>
  <el-dialog :visible="visible" title="地块详情" width="min(1280px, 94vw)" top="5vh"
    :fullscreen="fullscreen" append-to-body :close-on-click-modal="false" custom-class="land-detail-dialog"
    :before-close="close" @opened="open" @closed="destroyMap">
    <div class="land-detail">
      <header class="detail-heading">
        <div><p class="eyebrow">地块档案 / 边界与面积</p><h2>{{ plot.landName || '未命名地块' }}</h2><p class="address">{{ plot.address || '暂未填写地址' }}</p></div>
        <span class="verification"><i class="el-icon-warning-outline"></i> 待实测核验</span>
      </header>
      <div class="detail-layout">
        <aside class="plot-facts">
          <div class="area-card">
            <span class="field-label">记录面积</span>
            <div class="main-area">{{ areaMu }}<span>亩</span></div>
            <p>{{ areaSquareMetres }} 平方米</p>
            <span class="area-source">{{ sourceDescription }}</span>
          </div>
          <div v-if="isDemo" class="demo-notice"><i class="el-icon-info"></i> 示例边界，用于验证导入流程，不代表真实农田。</div>
          <dl class="fact-list">
            <div><dt>外围周长（估算）</dt><dd>{{ perimeterText }}</dd></div>
            <div><dt>外边界节点</dt><dd>{{ vertexCount || '—' }} <small v-if="vertexCount">个</small></dd></div>
            <div><dt>地块类型</dt><dd>{{ plot.landType || '未记录' }}</dd></div>
            <div><dt>边界来源</dt><dd>{{ plot.measurementMethod || '未记录' }}</dd></div>
            <div class="id-row"><dt>系统编号</dt><dd>{{ plot.id || '未记录' }}</dd></div>
          </dl>
          <div class="accuracy-note"><strong>怎样判断面积是否正确？</strong><p>先核对边界位置，再与可靠测绘或实测面积对照。地图显示和记录数值一致，仍不等于完成实测核验。</p></div>
          <el-collapse class="more-facts">
            <el-collapse-item title="更多档案信息" name="metadata">
              <p>拥有者：{{ plot.ownerName || '未记录' }}</p><p>联系电话：{{ plot.ownerPhone || '未记录' }}</p>
              <p>创建时间：{{ plot.createTime || '未记录' }}</p><p>更新时间：{{ plot.updateTime || '未记录' }}</p>
              <p>记录原值：{{ validArea === null ? '未记录' : plot.landArea + ' 亩' }}</p>
            </el-collapse-item>
          </el-collapse>
        </aside>
        <section class="map-panel">
          <div class="map-toolbar">
            <el-radio-group v-model="baseMap" size="small" :disabled="!mapReady" @change="switchBaseMap">
              <el-radio-button label="standard">标准地图</el-radio-button><el-radio-button label="satellite">卫星影像</el-radio-button>
            </el-radio-group>
            <div class="map-actions">
              <el-button size="small" :disabled="!mapReady" icon="el-icon-aim" @click="fitBoundary">定位地块</el-button>
              <el-button size="small" :disabled="!mapReady" :type="measuring ? 'primary' : ''" icon="el-icon-rank" @click="toggleRuler">{{ measuring ? '结束测距' : '测距' }}</el-button>
              <el-button size="small" icon="el-icon-full-screen" @click="toggleFullscreen">{{ fullscreen ? '退出全屏' : '全屏' }}</el-button>
            </div>
          </div>
          <div class="map-frame" v-loading="loading" element-loading-text="正在加载地图并转换边界位置…">
            <div ref="mapContainer" class="boundary-canvas"></div>
            <div v-if="mapError" class="map-error" role="alert"><i class="el-icon-warning-outline"></i><strong>暂时无法显示边界</strong><p>{{ mapError }}</p><el-button size="small" type="primary" @click="open">重新加载</el-button></div>
            <div v-if="mapReady" class="map-legend" :class="{satellite: baseMap === 'satellite'}"><span><i class="boundary-swatch"></i>地块边界</span><span v-if="hasExclusions"><i class="exclusion-swatch"></i>排除区域</span></div>
            <div v-if="measuring" class="ruler-tip">在地图上点击选点，双击结束测距。</div>
          </div>
          <div class="map-caption"><span><i :class="mapReady ? 'el-icon-circle-check' : 'el-icon-info'"></i> {{ mapReady ? '已完成坐标转换，边界已对齐高德坐标系' : '边界位置加载后显示' }}</span><span>卫星影像仅供位置参考</span></div>
        </section>
      </div>
      <footer class="detail-footer"><span>面积来自业务记录；平方米由亩数换算，周长仅为估算。</span><el-button size="small" @click="close">关闭详情</el-button></footer>
    </div>
  </el-dialog>
</template>

<script>
import { loadAMap } from '@/utils/amap'
import { listLandPlotExclude } from '@/api/field/exclude'
import { parsePolygonWkt, outerPerimeterMetres, convertWgs84Paths } from '@/utils/landPlotGeometry.js'

export default {
  name: 'LandPlotDetailDialog',
  props: { visible: { type: Boolean, default: false }, plot: { type: Object, default: () => ({}) } },
  data() { return { loading: false, mapReady: false, mapError: '', baseMap: 'standard', fullscreen: false, measuring: false, perimeter: null, vertexCount: 0, hasExclusions: false } },
  computed: {
    validArea() { const value = this.plot.landArea; return value !== null && value !== undefined && value !== '' && Number.isFinite(Number(value)) && Number(value) >= 0 ? Number(value) : null },
    areaMu() { return this.validArea === null ? '—' : this.validArea.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) },
    areaSquareMetres() { return this.validArea === null ? '—' : (this.validArea / 0.0015).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) },
    perimeterText() { return this.perimeter === null ? '—' : `约 ${this.perimeter.toFixed(2)} 米` },
    isDemo() { return this.plot.measurementMethod === 'GeoJSON' && this.plot.landName === 'dev-b导入测试田' },
    sourceDescription() { return this.plot.measurementMethod === 'GeoJSON' ? '由导入边界计算，尚未实测核验' : '现有业务记录，尚未独立复核' }
  },
  methods: {
    async open() {
      this.destroyMap()
      const version = this._version
      this.loading = true; this.mapError = ''; this.mapReady = false; this.perimeter = null; this.vertexCount = 0
      try {
        const rings = parsePolygonWkt(this.plot.boundary)
        this.perimeter = outerPerimeterMetres(rings[0]); this.vertexCount = rings[0].length - 1
        const [AMap, response] = await Promise.all([
          loadAMap(['AMap.Scale', 'AMap.ToolBar', 'AMap.RangingTool']), listLandPlotExclude(this.plot.id)
        ])
        if (!this.visible || version !== this._version) return
        if (!Array.isArray(response.data)) throw new Error('排除区域数据无效，请重新加载。')
        const exclusions = response.data.flatMap(item => parsePolygonWkt(item.boundary))
        const allPaths = rings.concat(exclusions)
        const displayPaths = await convertWgs84Paths(AMap, allPaths)
        if (!this.visible || version !== this._version) return
        this._AMap = AMap
        this._standard = new AMap.TileLayer()
        this._satellite = new AMap.TileLayer.Satellite()
        this._roadNet = new AMap.TileLayer.RoadNet()
        this._map = new AMap.Map(this.$refs.mapContainer, { viewMode: '2D', zoom: 17, zooms: [3, 20], layers: [this._standard], center: displayPaths[0][0] })
        this._map.addControl(new AMap.Scale({ position: 'LB' }))
        this._map.addControl(new AMap.ToolBar({ position: { right: '16px', top: '16px' } }))
        this._map.on('resize', () => this.fitBoundary())
        const polygon = new AMap.Polygon({ path: displayPaths, strokeColor: '#15845c', strokeWeight: 3, fillColor: '#22a66e', fillOpacity: 0.16 })
        const excluded = displayPaths.slice(1).map(path => new AMap.Polygon({ path, strokeColor: '#c94c46', strokeWeight: 2, strokeStyle: 'dashed', fillColor: '#f87171', fillOpacity: 0.12 }))
        this._map.add([polygon, ...excluded]); this._polygon = polygon
        this._ruler = new AMap.RangingTool(this._map)
        this._ruler.on('end', () => { this.measuring = false })
        this.hasExclusions = displayPaths.length > 1
        this.mapReady = true; this.baseMap = 'standard'
        await this.$nextTick()
        if (this.visible && version === this._version) this.fitBoundary()
      } catch (error) {
        if (version === this._version && this.visible) { this.mapError = error.message || '地图加载失败，请稍后重试。'; this.mapReady = false }
      } finally { if (version === this._version) this.loading = false }
    },
    fitBoundary() { if (this._map && this._polygon) this._map.setFitView([this._polygon], true, [60, 60, 80, 80], 20) },
    switchBaseMap() {
      if (!this._map) return
      const satellite = this.baseMap === 'satellite'
      this._map.setLayers(satellite ? [this._satellite, this._roadNet] : [this._standard])
      this._polygon.setOptions({ strokeColor: satellite ? '#efc341' : '#15845c', fillColor: satellite ? '#ffe292' : '#22a66e', fillOpacity: satellite ? 0.08 : 0.16 })
    },
    toggleRuler() {
      if (!this._ruler) return
      this.measuring = !this.measuring
      if (this.measuring) this._ruler.turnOn(); else this._ruler.turnOff(true)
    },
    async toggleFullscreen() { this.fullscreen = !this.fullscreen; await this.$nextTick(); this.fitBoundary() },
    close() { this.destroyMap(); this.fullscreen = false; this.$emit('update:visible', false) },
    destroyMap() {
      this._version = (this._version || 0) + 1
      if (this._ruler) this._ruler.turnOff(true)
      if (this._map) this._map.destroy()
      this._map = null; this._polygon = null; this._ruler = null; this._AMap = null
      this.mapReady = false; this.loading = false; this.measuring = false
    }
  },
  beforeDestroy() { this.destroyMap() }
}
</script>

<style scoped>
.detail-heading { display:flex; justify-content:space-between; align-items:center; gap:20px; padding:0 0 24px; }
.eyebrow { color:#74808f; font-size:13px; letter-spacing:1px; margin:0 0 8px; }
h2 { color:#22312d; font-size:24px; line-height:1.4; margin:0 0 8px; font-weight:650; }
.address { color:#66766f; font-size:14px; line-height:1.5; margin:0; }
.verification { flex-shrink:0; color:#986414; background:#fff5dd; border:1px solid #f1ddb4; border-radius:20px; padding:8px 14px; font-size:14px; }
.detail-layout { display:grid; grid-template-columns:290px minmax(0,1fr); height:min(650px, calc(100vh - 300px)); min-height:460px; border:1px solid #e0e8e3; border-radius:10px; overflow:hidden; }
.plot-facts { background:#fff; padding:24px; border-right:1px solid #e0e8e3; overflow-y:auto; }
.field-label { font-size:14px; color:#65786d; }
.main-area { font-size:44px; font-weight:650; color:#176b4c; line-height:1.3; margin:8px 0; letter-spacing:-1px; }
.main-area span { font-size:16px; margin-left:8px; font-weight:400; letter-spacing:0; }
.area-card p { font-size:15px; color:#455b50; margin:0 0 10px; }
.area-source { display:block; color:#7a8780; font-size:12px; line-height:1.7; }
.demo-notice { padding:10px 12px; background:#f0f6ff; color:#49678a; font-size:13px; line-height:1.7; border-radius:6px; margin:18px 0 0; }
.fact-list { margin:22px 0; font-size:14px; }
.fact-list div { display:flex; justify-content:space-between; gap:12px; padding:10px 0; border-bottom:1px solid #f0f3f1; }
dt { color:#76847b; flex-shrink:0; } dd { color:#34483c; margin:0; text-align:right; overflow-wrap:anywhere; } dd small { font-size:13px; }
.id-row { flex-direction:column; gap:5px !important; } .id-row dd { text-align:left; font-size:12px; }
.accuracy-note { background:#f5f8f6; border-radius:6px; padding:12px; color:#63726a; font-size:13px; line-height:1.7; }
.accuracy-note strong { color:#3d5848; font-size:14px; font-weight:500; } .accuracy-note p { margin:6px 0 0; }
.more-facts { margin-top:16px; } .more-facts p { margin:4px 0; overflow-wrap:anywhere; }
.map-panel { min-width:0; display:flex; flex-direction:column; background:#f8faf9; }
.map-toolbar { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:14px; border-bottom:1px solid #e3eae5; flex-wrap:wrap; background:#fff; }
.map-actions { display:flex; gap:6px; } .map-actions .el-button + .el-button { margin-left:0; }
.map-frame { position:relative; flex:1; min-height:0; background:#edf2ee; }
.boundary-canvas { position:absolute; inset:0; }
.map-error { position:absolute; inset:0; z-index:100; display:flex; flex-direction:column; justify-content:center; align-items:center; padding:24px; text-align:center; background:#f5f8f6; color:#65766c; }
.map-error>i { font-size:32px; margin-bottom:14px; color:#a77b37; } .map-error strong { font-size:17px; color:#43564a; }.map-error p { max-width:380px; line-height:1.7; font-size:14px; }
.map-legend { position:absolute; z-index:100; bottom:35px; right:16px; padding:8px 12px; background:rgba(255,255,255,.95); border:1px solid #dce5df; border-radius:6px; display:flex; gap:14px; font-size:13px; color:#4c6255; }
.map-legend span { display:flex; gap:6px; align-items:center; }.boundary-swatch,.exclusion-swatch { display:inline-block; width:15px; height:10px; border:2px solid #15845c; background:#d9eee1; }.exclusion-swatch { border-color:#c94c46; background:#ffe2df; }
.satellite .boundary-swatch { border-color:#efc341; background:#fff1c9; }
.map-caption { display:flex; justify-content:space-between; flex-wrap:wrap; gap:8px; padding:10px 14px; color:#758079; background:#fff; border-top:1px solid #e3eae5; font-size:12px; line-height:1.6; }
.map-caption i { color:#298664; }.ruler-tip { position:absolute; z-index:101; left:50%; top:20px; transform:translateX(-50%); white-space:nowrap; padding:10px 14px; background:#fff; color:#456550; border-radius:6px; box-shadow:0 3px 12px #0002; }
.detail-footer { display:flex; align-items:center; justify-content:space-between; gap:16px; margin-top:18px; color:#7b8780; font-size:12px; line-height:1.6; }
@media(min-width:1000px) { .land-detail { max-height:calc(90vh - 100px); overflow:auto; } }
@media(max-width:900px) { .detail-layout { grid-template-columns:1fr; height:auto; }.plot-facts { max-height:350px; border-right:0; border-bottom:1px solid #e0e8e3; }.map-frame { min-height:440px; }.detail-heading { align-items:flex-start; }.verification { font-size:12px; padding:6px 10px; } }
</style>
<style>
.land-detail-dialog .el-dialog__body { padding:8px 24px 20px; }
.land-detail-dialog.is-fullscreen .land-detail { max-height:none; }
.land-detail-dialog.is-fullscreen .detail-layout { height:calc(100vh - 240px); }
</style>
