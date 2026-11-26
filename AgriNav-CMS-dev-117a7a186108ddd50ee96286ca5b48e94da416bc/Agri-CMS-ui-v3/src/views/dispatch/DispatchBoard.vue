<template>
  <div class="page" v-loading="loading">
    <PageHeader title="作业调度管理" code="MP-03" group="业务管理" desc="作业需求来自 /farmWork，派单记录来自 /dispatch：管理员可派单、批量派单、取消与刷新超时；机手只能查看并接单/完成分配给自己的派单。">
      <template #actions>
        <el-button :icon="Plus" @click="openCreate">发布作业需求</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="load()">刷新</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="activeTab" class="mt-3" @tab-change="handleTabChange">
      <el-tab-pane label="作业需求" name="tasks">
    <!-- 后端能力说明：不使用假成功，明确告知哪些能力缺失 -->
    <el-alert type="info" :closable="false" class="mt-2" show-icon>
      <template #title>
        真实数据源：<b>POST /farmWork/list</b>（作业需求，status: 0=可接单 / 1=已结束）。
        「派单调度」页签对接后端 <b>/dispatch</b>（10 个真实端点）；派单、批量派单、取消、刷新超时、调度建议仅管理员可用，
        机手仅能查看分配给自己的派单并接单/完成。后端未提供的字段（如派单的作业类型）显示“—”，不做推算。
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
      </el-tab-pane>

      <el-tab-pane label="派单调度" name="dispatch" lazy>
        <div v-if="admin" class="grid cols-4 mt-3">
          <StatCard label="总派单数" :value="stats.total" unit="单" icon="Tickets" foot="GET /dispatch/stats" />
          <StatCard label="待接单" :value="stats.dispatched" unit="单" icon="Promotion" tone="amber" foot="DISPATCHED" />
          <StatCard label="进行中" :value="stats.accepted" unit="单" icon="Van" tone="blue" foot="ACCEPTED" />
          <StatCard label="已完成" :value="stats.completed" unit="单" icon="CircleCheck" tone="green" foot="COMPLETED" />
          <StatCard label="已超时" :value="stats.timeout" unit="单" icon="Warning" tone="red" foot="TIMEOUT" />
          <StatCard label="已取消" :value="stats.cancelled" unit="单" icon="CircleClose" foot="CANCELLED" />
          <StatCard label="完成率" :value="rateText(stats.completionRate)" unit="%" icon="DataLine" foot="后端返回百分比，前端不再换算" />
          <StatCard label="超时率" :value="rateText(stats.timeoutRate)" unit="%" icon="Timer" tone="amber" foot="后端返回百分比，前端不再换算" />
        </div>

        <PanelCard title="派单记录" sub="GET /dispatch/list · 后端已按派单时间倒序；该查询会触发后端 markTimeout()" class="mt-3">
          <template #extra>
            <div class="flex center" style="gap: 8px">
              <el-button v-if="admin" size="small" type="primary" :icon="Plus" @click="openAssign('single')">新建派单</el-button>
              <el-button v-if="admin" size="small" :icon="DocumentAdd" @click="openAssign('batch')">批量派单</el-button>
              <el-button v-if="admin" size="small" :icon="Timer" :loading="timeoutLoading" @click="refreshTimeout">刷新超时状态</el-button>
            </div>
          </template>

          <div class="filter-bar flat">
            <el-select v-model="dispatchStatus" size="small" style="width: 130px" @change="handleDispatchQuery">
              <el-option label="全部状态" value="" />
              <el-option v-for="s in DISPATCH_STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
            <span class="filter-hint grow">
              {{ admin ? '管理员可查看全部派单' : '仅展示分配给当前账号的派单' }} ·
              共 {{ dispatchTotal === null ? '后端未返回总数' : dispatchTotal }} 条
            </span>
          </div>

          <el-table :data="dispatches" size="small" row-key="id" style="width: 100%" v-loading="dispatchLoading">
            <el-table-column label="派单单号" min-width="152">
              <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ row.dispatchNo || row.id || '-' }}</span></template>
            </el-table-column>
            <el-table-column label="作业名称" min-width="130">
              <template #default="{ row }">{{ row.farmWorkName || '-' }}</template>
            </el-table-column>
            <el-table-column label="地块 ID" min-width="110">
              <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ row.plotId || '-' }}</span></template>
            </el-table-column>
            <el-table-column label="作业类型" width="94">
              <template #default="{ row }">{{ row.workType || '—' }}</template>
            </el-table-column>
            <el-table-column label="机手" width="118">
              <template #default="{ row }">{{ row.assigneeNick || row.assigneeName || row.assigneeId || '-' }}</template>
            </el-table-column>
            <el-table-column label="截止时间" width="126">
              <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ fmtTime(row.deadline) }}</span></template>
            </el-table-column>
            <el-table-column label="状态" width="88">
              <template #default="{ row }"><StatusTag :status="row.statusLabel || row.status" /></template>
            </el-table-column>
            <el-table-column label="派单时间" width="126">
              <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ fmtTime(row.dispatchTime) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="156" align="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openDispatchDetail(row)">详情</el-button>
                <el-button v-if="canAccept(row)" link type="success" size="small" @click="doAccept(row)">接单</el-button>
                <el-button v-if="canComplete(row)" link type="success" size="small" @click="doComplete(row)">完成</el-button>
                <el-button v-if="canCancel(row)" link type="danger" size="small" @click="doCancel(row)">取消</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager-wrap" v-if="admin ? dispatchTotal !== null : true">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next"
              :total="dispatchTotal ?? dispatches.length"
              :current-page="dispatchPage"
              :page-size="dispatchPageSize"
              :page-sizes="[10, 20, 50]"
              @current-change="handleDispatchPage"
              @size-change="handleDispatchSize"
            />
          </div>
        </PanelCard>

        <PanelCard v-if="admin" title="调度建议" sub="GET /dispatch/suggestion · 后端规则化建议（非机器学习）" class="mt-3">
          <div v-if="suggestionsLoading" class="hint">加载中…</div>
          <template v-else>
            <div class="panel-title" style="font-size: 13px"><span class="dot" />待派作业（{{ pendingTasks.length }}）</div>
            <el-table :data="pendingTasks" size="small" style="width: 100%; margin-top: 6px">
              <el-table-column label="作业" min-width="150"><template #default="{ row }">{{ row.name || row.farmWorkId || '-' }}</template></el-table-column>
              <el-table-column label="面积(亩)" width="92" align="right"><template #default="{ row }">{{ row.area ?? '—' }}</template></el-table-column>
              <el-table-column label="地块" min-width="130"><template #default="{ row }">{{ row.plotName || row.plotId || '—' }}</template></el-table-column>
              <el-table-column label="历史" min-width="160"><template #default="{ row }">{{ row.historyNote || '—' }}</template></el-table-column>
            </el-table>
            <div v-if="!pendingTasks.length" class="hint" style="padding: 6px 0">后端未返回待派作业</div>

            <div class="panel-title" style="font-size: 13px; margin-top: 14px"><span class="dot" />候选人员（{{ operatorCandidates.length }}）</div>
            <el-alert type="warning" :closable="false" show-icon style="margin: 6px 0">
              <template #title>候选来自后端全量用户评估，未按机手角色过滤，请管理员核实人员身份后再派单。</template>
            </el-alert>
            <el-table :data="operatorCandidates" size="small" style="width: 100%">
              <el-table-column label="姓名" min-width="120"><template #default="{ row }">{{ row.nickName || row.userName || row.userId }}</template></el-table-column>
              <el-table-column label="空闲" width="72"><template #default="{ row }">{{ row.idle ? '是' : '否' }}</template></el-table-column>
              <el-table-column label="完成率" width="88" align="right"><template #default="{ row }">{{ row.completionRate ?? '—' }}%</template></el-table-column>
              <el-table-column label="接单/完成/超时" width="150" align="center"><template #default="{ row }">{{ row.accepted ?? 0 }} / {{ row.completed ?? 0 }} / {{ row.timeout ?? 0 }}</template></el-table-column>
              <el-table-column label="说明" min-width="140"><template #default="{ row }">{{ row.note || '—' }}</template></el-table-column>
            </el-table>
            <div v-if="!operatorCandidates.length" class="hint" style="padding: 6px 0">后端未返回候选人员</div>

            <div class="panel-title" style="font-size: 13px; margin-top: 14px"><span class="dot" />农机信息（{{ machineryOptions.length }}）</div>
            <div v-if="machineryOptions.length" class="hint" style="line-height: 1.9">
              <span v-for="m in machineryOptions" :key="m.machineryId" class="tag violet no-dot" style="margin-right: 6px; margin-bottom: 4px">
                {{ m.name || m.machineryId }}<template v-if="m.location"> · {{ m.location }}</template>
              </span>
            </div>
            <div v-else class="hint" style="padding: 6px 0">后端未返回农机信息</div>

            <div class="panel-title" style="font-size: 13px; margin-top: 14px"><span class="dot" />调度建议（{{ recommendations.length }}）</div>
            <ul v-if="recommendations.length" class="hint" style="margin: 6px 0 0 18px; line-height: 1.9">
              <li v-for="(r, i) in recommendations" :key="i">{{ r }}</li>
            </ul>
            <div v-else class="hint">后端未返回调度建议</div>

            <div class="panel-title" style="font-size: 13px; margin-top: 14px"><span class="dot" />数据缺口（{{ dataGaps.length }}）</div>
            <ul v-if="dataGaps.length" class="hint" style="margin: 6px 0 0 18px; line-height: 1.9">
              <li v-for="(g, i) in dataGaps" :key="i">{{ g }}</li>
            </ul>
            <div v-else class="hint">后端未报告数据缺口</div>
          </template>
        </PanelCard>
      </el-tab-pane>
    </el-tabs>

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

    <!-- 派单 -->
    <el-dialog v-model="assignVisible" :title="assignMode === 'batch' ? '批量派单' : '新建派单'" width="560px" :close-on-click-modal="false">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
        <template #title>
          派单仅管理员可提交；机手候选取自 GET /dispatch/suggestion，<b>后端未按机手角色过滤</b>，请核实身份后再派单。
        </template>
      </el-alert>
      <el-form label-width="92px">
        <el-form-item v-if="assignMode === 'single'" label="作业需求" required>
          <el-select v-model="assignForm.farmWorkId" filterable placeholder="选择待派作业需求" style="width: 100%">
            <el-option v-for="t in assignTaskOptions" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="作业需求" required>
          <el-select v-model="assignForm.farmWorkIds" multiple filterable collapse-tags collapse-tags-tooltip placeholder="可多选待派作业需求" style="width: 100%">
            <el-option v-for="t in assignTaskOptions" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="机手" required>
          <el-select v-model="assignForm.assigneeId" filterable placeholder="选择机手（后端全量用户候选）" style="width: 100%">
            <el-option v-for="o in operatorCandidates" :key="o.userId" :label="candidateLabel(o)" :value="o.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间" required>
          <el-date-picker v-model="assignForm.deadline" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm" placeholder="必须晚于当前时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="assignForm.remark" maxlength="120" show-word-limit placeholder="可选" />
        </el-form-item>
      </el-form>
      <div class="hint-line"><el-icon :size="13"><InfoFilled /></el-icon><span>派单单号由后端生成；同一作业已存在待接单/进行中派单时会被后端拒绝，请以返回信息为准。</span></div>
      <template #footer>
        <el-button :disabled="assignSubmitting" @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignSubmitting" @click="submitAssign">确认派单</el-button>
      </template>
    </el-dialog>

    <!-- 派单详情 -->
    <el-drawer v-model="dispatchDetailVisible" title="派单详情" size="520px">
      <template v-if="dispatchDetail">
        <div class="detail-head">
          <div style="min-width: 0">
            <div class="detail-title">{{ dispatchDetail.farmWorkName || dispatchDetail.dispatchNo || '-' }}</div>
            <div class="detail-sub mono" style="font-size: 10.5px">{{ dispatchDetail.dispatchNo || dispatchDetail.id }}</div>
          </div>
          <div style="margin-left: auto"><StatusTag :status="dispatchDetail.statusLabel || dispatchDetail.status" /></div>
        </div>
        <div class="detail-grid">
          <div class="detail-field"><div class="detail-label">机手</div><div class="detail-value">{{ dispatchDetail.assigneeNick || dispatchDetail.assigneeName || dispatchDetail.assigneeId || '-' }}</div></div>
          <div class="detail-field"><div class="detail-label">作业类型</div><div class="detail-value">{{ dispatchDetail.workType || '—' }}</div></div>
          <div class="detail-field"><div class="detail-label">截止时间</div><div class="detail-value mono">{{ fmtTime(dispatchDetail.deadline) }}</div></div>
          <div class="detail-field"><div class="detail-label">派单时间</div><div class="detail-value mono">{{ fmtTime(dispatchDetail.dispatchTime) }}</div></div>
          <div class="detail-field"><div class="detail-label">接单时间</div><div class="detail-value mono">{{ fmtTime(dispatchDetail.acceptTime) }}</div></div>
          <div class="detail-field"><div class="detail-label">完成时间</div><div class="detail-value mono">{{ fmtTime(dispatchDetail.completeTime) }}</div></div>
          <div class="detail-field"><div class="detail-label">作业 ID</div><div class="detail-value mono" style="font-size: 11px">{{ dispatchDetail.farmWorkId || '-' }}</div></div>
          <div class="detail-field"><div class="detail-label">地块 ID</div><div class="detail-value mono" style="font-size: 11px">{{ dispatchDetail.plotId || '-' }}</div></div>
          <div class="detail-field" style="grid-column: 1 / -1"><div class="detail-label">备注</div><div class="detail-value">{{ dispatchDetail.remark || '—' }}</div></div>
        </div>
        <el-alert type="info" :closable="false" show-icon style="margin-top: 10px">
          <template #title>派单记录中的作业类型后端暂未写入，显示“—”；状态与时间均由后端返回，页面不做推算。</template>
        </el-alert>
      </template>
      <template #footer>
        <el-button @click="dispatchDetailVisible = false">关闭</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, InfoFilled, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  acceptDispatch, cancelDispatch, completeDispatch,
  createTask, dispatchBatch, dispatchOne,
  getDispatchStats, getDispatchSuggestions, listDispatches,
  listLands, listTasks, listWorkTypes, refreshDispatchTimeout,
  revokeTask, updateTask
} from '@/services/api'
import { useUserStore } from '@/stores/user'
import { isAdminUser } from '@/stores/permission'
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

/* ================= 派单调度（第二阶段，复用 services/api 与 adapter） =================
 * 权限：admin 判定复用 isAdminUser()；机手 userId 取自 userStore，列表隔离由服务层强制附加。
 * 副作用：GET /dispatch/list 与 /dispatch/stats 会触发后端 markTimeout()，只在切页签与写操作后按需加载，
 * 页面不做轮询，也不在初始化时无条件请求。
 */
const userStore = useUserStore()
const admin = computed(() => isAdminUser())
const currentUserId = computed(() => toStringId(userStore.userId ?? userStore.user?.userId))

const activeTab = ref('tasks')
const dispatchLoaded = ref(false)
const dispatches = ref([])
const dispatchTotal = ref(null)
const dispatchPage = ref(1)
const dispatchPageSize = ref(10)
const dispatchStatus = ref('')
const dispatchLoading = ref(false)
const timeoutLoading = ref(false)
const stats = ref(emptyStats())
const suggestions = ref(null)
const suggestionsLoading = ref(false)

const DISPATCH_STATUS_OPTIONS = [
  { value: 'DISPATCHED', label: '待接单' },
  { value: 'ACCEPTED', label: '进行中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'TIMEOUT', label: '已超时' },
  { value: 'CANCELLED', label: '已取消' }
]

const pendingTasks = computed(() => suggestions.value?.pendingTasks || [])
const operatorCandidates = computed(() => suggestions.value?.operatorCandidates || [])
const machineryOptions = computed(() => suggestions.value?.machineryOptions || [])
const recommendations = computed(() => suggestions.value?.recommendations || [])
const dataGaps = computed(() => suggestions.value?.dataGaps || [])

function emptyStats() {
  return { total: 0, completed: 0, accepted: 0, dispatched: 0, timeout: 0, cancelled: 0, completionRate: null, timeoutRate: null }
}
function rateText(v) {
  return v === null || v === undefined || v === '' ? '—' : v
}
function errText(error) {
  return error?.msg || error?.payload?.msg || error?.message || '未知错误'
}
function candidateLabel(o) {
  const name = o.nickName || o.userName || o.userId
  const idle = o.idle ? '空闲' : '忙碌'
  const rate = o.completionRate === null || o.completionRate === undefined ? '—' : o.completionRate
  return `${name} · ${idle} · 完成率 ${rate}%`
}

function handleTabChange(name) {
  if (name !== 'dispatch' || dispatchLoaded.value) return
  dispatchLoaded.value = true
  loadDispatchTab()
}
async function loadDispatchTab() {
  await Promise.all([
    loadDispatches(),
    admin.value ? loadStats() : Promise.resolve(),
    admin.value ? loadSuggestions() : Promise.resolve()
  ])
}
async function loadDispatches() {
  dispatchLoading.value = true
  try {
    const res = await listDispatches({
      pageNum: dispatchPage.value,
      pageSize: dispatchPageSize.value,
      status: dispatchStatus.value
    })
    dispatches.value = res.list
    // 后端未返回 total 时保持 null，不用列表长度冒充
    dispatchTotal.value = res.total
  } catch (error) {
    dispatches.value = []
    dispatchTotal.value = null
    ElMessage.error('查询派单失败：' + errText(error))
  } finally {
    dispatchLoading.value = false
  }
}
async function loadStats() {
  try {
    stats.value = await getDispatchStats()
  } catch (error) {
    stats.value = emptyStats()
    ElMessage.error('派单统计加载失败：' + errText(error))
  }
}
async function loadSuggestions() {
  suggestionsLoading.value = true
  try {
    suggestions.value = await getDispatchSuggestions()
  } catch (error) {
    suggestions.value = null
    ElMessage.error('调度建议加载失败：' + errText(error))
  } finally {
    suggestionsLoading.value = false
  }
}

function handleDispatchQuery() {
  dispatchPage.value = 1
  loadDispatches()
}
function handleDispatchPage(page) {
  dispatchPage.value = Number(page)
  loadDispatches()
}
function handleDispatchSize(size) {
  dispatchPageSize.value = Number(size)
  dispatchPage.value = 1
  loadDispatches()
}

/* 按钮显隐只做交互层过滤，权限仍由后端 @PreAuthorize 与本人校验决定 */
function isMine(row) {
  return !!currentUserId.value && row.assigneeId === currentUserId.value
}
function canAccept(row) {
  return !admin.value && row.status === 'DISPATCHED' && isMine(row)
}
function canComplete(row) {
  return !admin.value && row.status === 'ACCEPTED' && isMine(row)
}
function canCancel(row) {
  return admin.value && (row.status === 'DISPATCHED' || row.status === 'ACCEPTED')
}

async function confirmAction(text) {
  try {
    await ElMessageBox.confirm(text, '操作确认', { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' })
    return true
  } catch {
    return false
  }
}
async function afterWrite(message) {
  if (message) ElMessage.success(message)
  await loadDispatches()
  if (admin.value) await loadStats()
}
async function doAccept(row) {
  if (!(await confirmAction(`确认接单「${row.dispatchNo || row.id}」？`))) return
  try {
    await acceptDispatch(row.id)
    await afterWrite('已接单')
  } catch (error) {
    ElMessage.error('接单失败：' + errText(error))
  }
}
async function doComplete(row) {
  if (!(await confirmAction(`确认完成「${row.dispatchNo || row.id}」？`))) return
  try {
    await completeDispatch(row.id)
    await afterWrite('已完成')
  } catch (error) {
    ElMessage.error('完成失败：' + errText(error))
  }
}
async function doCancel(row) {
  if (!(await confirmAction(`确认取消派单「${row.dispatchNo || row.id}」？取消后不可恢复。`))) return
  try {
    await cancelDispatch(row.id)
    await afterWrite('已取消')
  } catch (error) {
    ElMessage.error('取消失败：' + errText(error))
  }
}
async function refreshTimeout() {
  if (!(await confirmAction('刷新超时状态会触发后端 markTimeout() 写库，确认执行？'))) return
  timeoutLoading.value = true
  try {
    const res = await refreshDispatchTimeout()
    ElMessage.success(res.message || `已标记超时派单 ${res.count} 条`)
    await loadDispatches()
    if (admin.value) await loadStats()
  } catch (error) {
    ElMessage.error('刷新超时失败：' + errText(error))
  } finally {
    timeoutLoading.value = false
  }
}

/* ---------------- 派单弹窗 ---------------- */
const assignVisible = ref(false)
const assignMode = ref('single')
const assignSubmitting = ref(false)
const assignForm = reactive({ farmWorkId: '', farmWorkIds: [], assigneeId: '', deadline: '', remark: '' })
const assignTaskOptions = computed(() => {
  const fromSuggestions = pendingTasks.value.map((t) => ({ id: t.farmWorkId, name: t.name || t.farmWorkId }))
  if (fromSuggestions.length) return fromSuggestions
  return tasks.value
    .filter((t) => Number(t.status) === 0)
    .map((t) => ({ id: t.id, name: t.name || t.id }))
})

function openAssign(mode) {
  assignMode.value = mode
  Object.assign(assignForm, { farmWorkId: '', farmWorkIds: [], assigneeId: '', deadline: '', remark: '' })
  assignVisible.value = true
  if (admin.value && !suggestions.value && !suggestionsLoading.value) loadSuggestions()
}

async function submitAssign() {
  if (assignSubmitting.value) return
  if (assignMode.value === 'single' && !assignForm.farmWorkId) return ElMessage.warning('请选择作业需求')
  if (assignMode.value === 'batch' && !assignForm.farmWorkIds.length) return ElMessage.warning('请至少选择一条作业需求')
  if (!assignForm.assigneeId) return ElMessage.warning('请选择机手')
  if (!assignForm.deadline) return ElMessage.warning('请选择截止时间')
  const deadlineTs = parseTime(assignForm.deadline)
  if (deadlineTs === null || deadlineTs <= Date.now()) return ElMessage.warning('截止时间必须晚于当前时间')

  const batch = assignMode.value === 'batch'
  const target = operatorCandidates.value.find((o) => o.userId === assignForm.assigneeId)
  const who = target ? (target.nickName || target.userName || target.userId) : assignForm.assigneeId
  if (!(await confirmAction(batch ? `确认向「${who}」批量派单？` : `确认向「${who}」派单？`))) return

  assignSubmitting.value = true
  try {
    if (batch) {
      const res = await dispatchBatch({
        farmWorkIds: assignForm.farmWorkIds,
        assigneeId: assignForm.assigneeId,
        deadline: assignForm.deadline,
        remark: assignForm.remark
      })
      ElMessage.success(res.message || `批量派单成功，共 ${res.count} 单`)
    } else {
      const res = await dispatchOne({
        farmWorkId: assignForm.farmWorkId,
        assigneeId: assignForm.assigneeId,
        deadline: assignForm.deadline,
        remark: assignForm.remark
      })
      ElMessage.success(res.message || '派单成功')
    }
    assignVisible.value = false
    await loadDispatches()
    if (admin.value) await loadStats()
  } catch (error) {
    if (batch) {
      // 后端批量派单可能部分成功且不回滚：不得提示“全部失败”，必须让管理员刷新核对
      ElMessage.error(`${errText(error)}。后端可能已部分成功，请刷新列表核对实际结果。`)
      assignVisible.value = false
      await loadDispatches()
      if (admin.value) await loadStats()
    } else {
      ElMessage.error('派单失败：' + errText(error))
    }
  } finally {
    assignSubmitting.value = false
  }
}

/* ---------------- 派单详情（直接用列表返回的真实字段，不额外发请求） ---------------- */
const dispatchDetailVisible = ref(false)
const dispatchDetail = ref(null)
function openDispatchDetail(row) {
  dispatchDetail.value = row
  dispatchDetailVisible.value = true
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
