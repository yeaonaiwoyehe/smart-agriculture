<template>
    <div class="app-container">
        <div class="content-wrapper">
            <div class="main-content">
                <!-- 搜索区域 -->
                <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="68px">
                    <el-form-item label="名称" prop="name">
                        <el-input
                                v-model="queryParams.name"
                                placeholder="请输入名称"
                                clearable
                                @keyup.enter="handleQuery"
                        />
                    </el-form-item>
                    <el-form-item label="状态" prop="status">
                        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 240px">
                            <el-option
                                    v-for="dict in dict.type.agricultural_machinery_status"
                                    :key="dict.value"
                                    :label="dict.label"
                                    :value="dict.value"
                            />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="作物" prop="cropId">
                        <el-select
                                v-model="queryParams.cropId"
                                multiple
                                placeholder="请选择作物"
                                clearable
                                style="width: 240px"
                        >
                            <el-option
                                    v-for="item in cropOptions"
                                    :key="item.id"
                                    :label="item.cropName"
                                    :value="item.id"
                            />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="作业类型" prop="workTypeId">
                        <el-select
                                v-model="queryParams.workTypeId"
                                multiple
                                placeholder="请选择作业类型"
                                clearable
                                style="width: 240px"
                        >
                            <el-option
                                    v-for="item in workTypeOptions"
                                    :key="item.id"
                                    :label="item.workName"
                                    :value="item.id"
                            />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="面积范围">
                        <el-input-number
                                v-model="queryParams.minArea"
                                placeholder="最小"
                                :min="0"
                                :precision="2"
                                size="small"
                                @change="validateArea"
                        />
                        <span class="el-range-separator">-</span>
                        <el-input-number
                                v-model="queryParams.maxArea"
                                placeholder="最大"
                                :min="0"
                                :precision="2"
                                size="small"
                                style="width: 110px"
                                @change="validateArea"
                        />
                    </el-form-item>
                    <el-form-item label="作业区域" prop="location">
                        <el-input
                                v-model="locationInput"
                                placeholder="请输入作业区域，按回车键添加"
                                style="width: 240px"
                                @keyup.enter="handleLocationInputConfirm"
                        >
                            <template #append>
                                <el-button icon="el-icon-plus" @click="handleLocationInputConfirm"></el-button>
                            </template>
                        </el-input>
                        <div style="margin-top: 10px">
                            <el-tag
                                    v-for="(loc, index) in queryParams.location"
                                    :key="index"
                                    closable
                                    :disable-transitions="false"
                                    @close="handleLocationClose(index)"
                                    style="margin-right: 10px; margin-bottom: 5px"
                            >
                                {{ loc }}
                            </el-tag>
                        </div>
                    </el-form-item>
                    <el-form-item label="排序" prop="orderByCreateTimeDesc">
                        <el-switch
                                v-model="queryParams.orderByCreateTimeDesc"
                                active-text="最新发布"
                                inactive-text="最早发布"
                                @change="handleQuery"
                        />
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" icon="el-icon-search" size="small" @click="handleQuery">搜索</el-button>
                        <el-button icon="el-icon-refresh" size="small" @click="resetQuery">重置</el-button>
                    </el-form-item>
                </el-form>

                <!-- 操作按钮区域 -->
                <el-row :gutter="10" class="mb8">
                    <el-col :span="1.5">
                        <el-button
                                type="primary"
                                plain
                                icon="el-icon-plus"
                                size="small"
                                @click="handleAdd"
                        >新增</el-button>
                    </el-col>
                </el-row>

                <!-- 表格区域 -->
                <el-table v-loading="loading" :data="farmWorkList">
                    <el-table-column label="名称" align="center" prop="name" />
                    <el-table-column label="面积(亩)" align="center" prop="area" />
                    <el-table-column label="联系方式" align="center" prop="phoneNumber" />
                    <el-table-column label="创建时间" align="center" prop="createTime" width="180">
                        <template #default="scope">
                            {{ formatDateTime(scope.row.createTime) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="截止时间" align="center" prop="endTime" width="180">
                        <template #default="scope">
                            {{ formatDateTime(scope.row.endTime) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" align="center" prop="status">
                        <template #default="scope">
                            <dict-tag :options="dict.type.agricultural_machinery_status" :value="scope.row.status"/>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                        <template #default="scope">
                            <el-button
                                    size="small"
                                    link
                                    icon="el-icon-view"
                                    @click="handleView(scope.row)"
                            >查看</el-button>
                            <el-button
                                    size="small"
                                    link
                                    icon="el-icon-edit"
                                    @click="handleUpdate(scope.row)"
                            >修改</el-button>
                            <el-button
                                    size="small"
                                    link
                                    icon="el-icon-delete"
                                    @click="handleDelete(scope.row)"
                            >下架</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </div>

            <!-- 分页区域 - 移到 main-content 外面 -->
            <div class="pagination-container">
                <pagination
                        v-show="total>0"
                        :total="total"
                        v-model:page="queryParams.pageNum"
                        v-model:limit="queryParams.pageSize"
                        @pagination="getList"
                />
            </div>
        </div>

        <!-- 添加或修改对话框 -->
        <el-dialog :title="title" v-model="open" width="500px" append-to-body>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
                <el-form-item label="名称" prop="name">
                    <el-input v-model="form.name" placeholder="请输入名称" />
                </el-form-item>
                <el-form-item label="联系方式" prop="phoneNumber">
                    <el-input v-model="form.phoneNumber" placeholder="请输入联系方式" />
                </el-form-item>
                <el-form-item label="截止时间" prop="endTime">
                    <el-date-picker
                            v-model="form.endTime"
                            type="date"
                            placeholder="选择截止时间"
                            value-format="YYYY-MM-DD[T]HH:mm:ss.SSS[Z]"
                    />
                </el-form-item>
                <el-form-item label="作物" prop="cropId">
                    <el-select
                            v-model="form.cropId"
                            multiple
                            placeholder="请选择作物"
                            style="width: 100%"
                    >
                        <el-option
                                v-for="item in cropOptions"
                                :key="item.id"
                                :label="item.cropName"
                                :value="item.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="工作类型" prop="workTypeId">
                    <el-select
                            v-model="form.workTypeId"
                            multiple
                            placeholder="请选择工作类型"
                            style="width: 100%"
                    >
                        <el-option
                                v-for="item in workTypeOptions"
                                :key="item.id"
                                :label="item.workName"
                                :value="item.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="地块" prop="landPlotIds">
                    <div class="land-plot-selector">
                        <el-button type="primary" @click="showLandPlotSelector">选择地块</el-button>
                        <div v-if="form.landPlotIds.length > 0" class="selected-plots">
                            <el-tag
                                    v-for="id in form.landPlotIds"
                                    :key="id"
                                    closable
                                    @close="removeLandPlot(id)"
                                    style="margin-right: 8px; margin-top: 8px"
                            >
                                {{ getLandPlotName(id) }}
                            </el-tag>
                        </div>
                    </div>
                </el-form-item>
                <el-form-item label="面积" prop="area">
                    <el-input-number v-model="form.area" :min="0" :precision="2" placeholder="请输入面积(亩)" />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="submitForm">确 定</el-button>
                    <el-button @click="cancel">取 消</el-button>
                </div>
            </template>
        </el-dialog>

        <!-- 查看详情对话框 -->
        <el-dialog title="查看详情" v-model="viewDialogVisible" width="800px" append-to-body>
            <div class="detail-container">
                <!-- 左侧区域：优先显示图片轮播，如果没有图片则显示地图 -->
                <div class="left-container">
                    <template v-if="viewForm.landPlotImage && viewForm.landPlotImage.length > 0">
                        <el-carousel height="400px" :autoplay="false" trigger="click" indicator-position="outside">
                            <el-carousel-item v-for="(url, index) in viewForm.landPlotImage" :key="index">
                                <el-image
                                        :src="url"
                                        fit="contain"
                                        style="width: 100%; height: 100%"
                                        :preview-src-list="viewForm.landPlotImage"
                                />
                            </el-carousel-item>
                        </el-carousel>
                    </template>
                    <template v-else>
                        <!-- 如果没有图片，示默认图片 -->
                        <div class="map-container">
                            <el-empty
                                    description="暂无地块图片"
                                    :image-size="200">
                            </el-empty>
                        </div>
                    </template>
                </div>

                <!-- 右侧信息描述 -->
                <div class="info-container">
                    <el-descriptions :column="1" border>
                        <el-descriptions-item label="名称">{{ viewForm.name }}</el-descriptions-item>
                        <el-descriptions-item label="面积">{{ viewForm.area }}</el-descriptions-item>
                        <el-descriptions-item label="联系方式">{{ viewForm.phoneNumber }}</el-descriptions-item>
                        <el-descriptions-item label="截止时间">{{ formatDateTime(viewForm.endTime) }}</el-descriptions-item>
                        <el-descriptions-item label="状态">
                            <dict-tag :options="dict.type.agricultural_machinery_status" :value="viewForm.status"/>
                        </el-descriptions-item>
                        <el-descriptions-item label="创建时间">{{ formatDateTime(viewForm.createTime) }}</el-descriptions-item>
                        <el-descriptions-item label="作物">{{ viewForm.cropName ? viewForm.cropName.join('、') : '-' }}</el-descriptions-item>
                        <el-descriptions-item label="作业类型">{{ viewForm.workTypeName ? viewForm.workTypeName.join('、') : '-' }}</el-descriptions-item>
                        <el-descriptions-item label="地块名称">{{ viewForm.landPlotName ? viewForm.landPlotName.join('、') : '-' }}</el-descriptions-item>
                        <el-descriptions-item label="地块地址">{{ viewForm.landAddress ? viewForm.landAddress.join('、') : '-' }}</el-descriptions-item>
                    </el-descriptions>
                </div>
            </div>
        </el-dialog>

        <!-- 添加地块详情对话框 -->
        <el-dialog title="地块详情" v-model="landPlotDetailVisible" width="800px" append-to-body>
            <div class="detail-container">
                <div class="left-container">
                    <el-image
                            v-if="selectedLandPlot.image"
                            :src="selectedLandPlot.image"
                            fit="contain"
                            style="width: 100%; height: 100%"
                            :preview-src-list="[selectedLandPlot.image]"
                    >
                        <template #error>
                            <div class="image-slot">
                                <i class="el-icon-picture-outline"></i>
                            </div>
                        </template>
                    </el-image>
                    <div v-else class="map-container">
                        <el-empty description="暂无地块图片" :image-size="200"></el-empty>
                    </div>
                </div>
                <div class="info-container">
                    <el-descriptions :column="1" border>
                        <el-descriptions-item label="地块名称">{{ selectedLandPlot.landName }}</el-descriptions-item>
                        <el-descriptions-item label="面积">{{ selectedLandPlot.landArea }} 亩</el-descriptions-item>
                        <el-descriptions-item label="位置">{{ selectedLandPlot.address }}</el-descriptions-item>
                    </el-descriptions>
                    <div class="dialog-footer" style="margin-top: 20px; text-align: right;">
                        <el-button type="primary" @click="confirmSelectLandPlot">确认选择</el-button>
                        <el-button @click="landPlotDetailVisible = false">取消</el-button>
                    </div>
                </div>
            </div>
        </el-dialog>

        <!-- 修改地块选择对话框 -->
        <el-dialog title="选择地块" v-model="landPlotSelectorVisible" width="1200px" append-to-body>
            <!-- 搜索表单 -->
            <el-form :model="landPlotQuery" ref="landPlotQueryFormRef" :inline="true" class="search-form">
                <el-form-item label="地块名称" prop="landName">
                    <el-input
                            v-model="landPlotQuery.landName"
                            placeholder="请输入地块名称"
                            clearable
                            size="small"
                    />
                </el-form-item>
                <el-form-item label="地块类型" prop="landType">
                    <el-select v-model="landPlotQuery.landType" placeholder="选择地块类型" clearable size="small">
                        <el-option label="新增" value="新增" />
                        <!-- 加其他地块类型选项 -->
                    </el-select>
                </el-form-item>
                <el-form-item label="面积范围">
                    <el-input-number
                            v-model="landPlotQuery.beginArea"
                            placeholder="最小面积"
                            size="small"
                            style="width: 130px"
                            :min="0"
                    />
                    <span class="el-range-separator">-</span>
                    <el-input-number
                            v-model="landPlotQuery.endArea"
                            placeholder="最大面积"
                            size="small"
                            :min="0"
                    />
                </el-form-item>
                <el-form-item label="地址" prop="address">
                    <el-input
                            v-model="landPlotQuery.address"
                            placeholder="请输入地址"
                            clearable
                            size="small"
                    />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" icon="el-icon-search" size="small" @click="searchLandPlots">搜索</el-button>
                    <el-button icon="el-icon-refresh" size="small" @click="resetLandPlotQuery">重</el-button>
                </el-form-item>
            </el-form>

            <!-- 地块列表 -->
            <div class="land-plot-grid" v-loading="landPlotLoading">
                <!-- 添加空状态显示 -->
                <el-empty
                        v-if="!landPlotLoading && (!landPlotOptions || landPlotOptions.length === 0)"
                        description="暂无地块数据"
                        :image-size="200"
                >
                    <template #description>
                        <p>暂无地块数据，请先添加地块信息</p>
                    </template>
                </el-empty>

                <!-- 有数据时显示地块列表 -->
                <el-row v-else :gutter="20">
                    <el-col :span="8" v-for="item in landPlotOptions" :key="item.id">
                        <el-card
                                class="land-plot-card"
                                :class="{ 'is-selected': form.landPlotIds.includes(item.id) }"
                                @click="toggleLandPlotSelection(item)"
                        >
                            <div class="land-plot-image">
                                <template v-if="item.image">
                                    <el-image
                                            :src="item.image"
                                            fit="cover"
                                    >
                                        <template #error>
                                            <div class="image-slot">
                                                <i class="el-icon-picture-outline"></i>
                                                <span>暂无图片数据</span>
                                            </div>
                                        </template>
                                    </el-image>
                                </template>
                                <div v-else class="image-slot">
                                    <i class="el-icon-picture-outline"></i>
                                    <span>暂无图片数据</span>
                                </div>
                            </div>
                            <div class="land-plot-info">
                                <div>
                                    <h3>{{ item.landName }}</h3>
                                    <p>面积: {{ item.landArea }} 亩</p>
                                    <p>位置: {{ item.address }}</p>
                                </div>
                            </div>
                            <div class="select-indicator">
                                <el-checkbox :model-value="form.landPlotIds.includes(item.id.toString())"></el-checkbox>
                            </div>
                        </el-card>
                    </el-col>
                </el-row>
            </div>

            <!-- 分页器 -->
            <div class="pagination-container" v-if="landPlotTotal > 0">
                <el-pagination
                        background
                        @current-change="handleLandPlotCurrentChange"
                        :current-page="landPlotQuery.current"
                        :page-size="6"
                        :total="landPlotTotal"
                        layout="prev, pager, next, total"
                />
            </div>

            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="confirmLandPlotSelection">确定</el-button>
                    <el-button @click="landPlotSelectorVisible = false">取消</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script>
export default {
    name: 'FarmWork',
    dicts: ['agricultural_machinery_status']
}
</script>

<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listFarmWork, addFarmWork, updateFarmWork, delFarmWork } from '@/api/farmWork/farmWork'
import { getCrops } from '@/api/field/crop'
import { getWorkTypes } from '@/api/field/workType'
import { queryLandPlot } from '@/api/field/square'

const { proxy } = getCurrentInstance()

const formRef = ref(null)
const queryFormRef = ref(null)
const landPlotQueryFormRef = ref(null)

// 自定义验证规则
const validatePhone = (rule, value, callback) => {
    const phoneRegex = /^1[3-9]\d{9}$/
    if (!value) {
        callback(new Error('请输入联系方式'))
    } else if (!phoneRegex.test(value)) {
        callback(new Error('请输入正确的11位手机号码'))
    } else {
        callback()
    }
}

// 遮罩层
const loading = ref(true)
// 显示搜索条件
const showSearch = ref(true)
// 总条数
const total = ref(0)
// 寻找农机表格数据
const farmWorkList = ref([])
// 弹出层标
const title = ref('')
// 是否显示弹出
const open = ref(false)
// 看情框否可见
const viewDialogVisible = ref(false)
// 查看的表单数据
const viewForm = ref({})
// 查询参数
const queryParams = ref({
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    phoneNumber: undefined,
    status: undefined,
    myFarmWork: true, // 添加这个字段
    cropId: [], // 改为cropId以匹配后端DTO
    workTypeId: [], // 改为workTypeId以匹配后端DTO
    minArea: undefined, // 添加最小面积
    maxArea: undefined, // 添加最大面积
    orderByCreateTimeDesc: true, // 添加排序字段
    location: []
})
// 表单参数
const form = ref({
    id: undefined,
    name: undefined,
    area: undefined,
    phoneNumber: undefined,
    endTime: undefined,
    cropId: [], // 这些字段用于表单绑定
    workTypeId: [],
    landPlotIds: [] // 存储字符串类型的ID
})
// 表单校验
const rules = {
    name: [
        { required: true, message: '名称不能为空', trigger: 'blur' }
    ],
    phoneNumber: [
        { required: true, message: '联系方式不能为空', trigger: 'blur' },
        { validator: validatePhone, trigger: 'blur' }
    ],
    endTime: [
        { required: true, message: '截止时间不能为空', trigger: 'blur' }
    ]
}
// 作物选项列表
const cropOptions = ref([])
// 工作类型选项
const workTypeOptions = ref([])
// 地块选项列表
const landPlotOptions = ref([])
// 地块详情对话框是否见
const landPlotDetailVisible = ref(false)
// 选中的地块
const selectedLandPlot = ref({})
// 地块选择器对话框显示状态
const landPlotSelectorVisible = ref(false)
// 地块查询参数
const landPlotQuery = ref({
    landName: undefined,
    landType: undefined,
    address: undefined,
    beginArea: undefined,
    endArea: undefined,
    current: 1
})
// 地块总数
const landPlotTotal = ref(0)
// 地块加载状态
const landPlotLoading = ref(false)
const selectedLocation = ref([])
const addressOptions = ref([])
const addressProps = {
    value: 'id',
    label: 'name',
    children: 'children',
    expandTrigger: 'hover'
}
const locationInput = ref('')

/** 验证面积范围 */
function validateArea() {
    if (queryParams.value.minArea && queryParams.value.maxArea) {
        if (queryParams.value.minArea > queryParams.value.maxArea) {
            ElMessage({
                message: '最小面积不能大于最大面积',
                type: 'warning'
            })
            // 清空最大面积
            queryParams.value.maxArea = undefined
            return false
        }
    }
    return true
}

/** 查询列表 */
function getList() {
    loading.value = true
    const params = {
        pageNum: queryParams.value.pageNum,
        pageSize: queryParams.value.pageSize,
        name: queryParams.value.name,
        phoneNumber: queryParams.value.phoneNumber,
        myFarmWork: queryParams.value.myFarmWork,
        orderByCreateTimeDesc: queryParams.value.orderByCreateTimeDesc
    }

    // 添加面积范围参数
    if (queryParams.value.minArea !== undefined) {
        params.minArea = queryParams.value.minArea
    }
    if (queryParams.value.maxArea !== undefined) {
        params.maxArea = queryParams.value.maxArea
    }

    // 只有当数组有值时才添加到查询参数中
    if (queryParams.value.cropId && queryParams.value.cropId.length > 0) {
        params.cropId = queryParams.value.cropId
    }

    if (queryParams.value.workTypeId && queryParams.value.workTypeId.length > 0) {
        params.workTypeId = queryParams.value.workTypeId
    }

    if (queryParams.value.status !== undefined && queryParams.value.status !== '') {
        params.status = Number(queryParams.value.status)
    }

    // 添加位置查询参数
    if (queryParams.value.location && queryParams.value.location.length > 0) {
        params.location = queryParams.value.location
    }

    listFarmWork(params).then(response => {
        farmWorkList.value = (response.rows || response.data).map(item => ({
            ...item,
            cropNames: item.cropNames || []
        }))
        total.value = response.total
        loading.value = false
    })
}

// 取消按钮
function cancel() {
    open.value = false
    reset()
}

// 表单重置
function reset() {
    form.value = {
        id: undefined,
        name: undefined,
        area: undefined,
        phoneNumber: undefined,
        endTime: undefined,
        cropId: [], // 确保初始化为空数组
        workTypeId: [], // 确保初始化为空数组
        landPlotIds: [] // 确保初始化为空数组
    }
    // 清空地块选项
    landPlotOptions.value = []
    proxy.resetForm('formRef')
}

/** 搜索按钮操作 */
function handleQuery() {
    // 在搜索前验证面积范围
    if (validateArea()) {
        queryParams.value.pageNum = 1
        getList()
    }
}

/** 重置按钮操作 */
function resetQuery() {
    proxy.resetForm('queryFormRef')
    queryParams.value = {
        pageNum: 1,
        pageSize: 10,
        name: undefined,
        phoneNumber: undefined,
        status: undefined,
        myFarmWork: true,
        cropId: [],
        workTypeId: [],
        minArea: undefined,
        maxArea: undefined,
        orderByCreateTimeDesc: true // 添加排序字段
    }
    handleQuery()
}

/** 新增按钮操作 */
function handleAdd() {
    reset()
    open.value = true
    title.value = '添加寻找农机'
}

/** 修改按钮操作 */
function handleUpdate(row) {
    reset()
    landPlotOptions.value = []
    const endTime = row.endTime ? new Date(new Date(row.endTime).getTime() + 8 * 3600 * 1000).toISOString() : undefined

    // 确保landPlotId保持字符串类型
    form.value = {
        id: row.id,
        name: row.name,
        area: row.area,
        phoneNumber: row.phoneNumber,
        endTime: endTime,
        cropId: row.cropId || [],
        workTypeId: row.workTypeId || [],
        landPlotIds: row.landPlotId || [] // 直接使用，不做类型转换
    }

    // 只有当当前行有地块数据时才添加到选项中
    if (row.landPlotId && row.landPlotId.length > 0) {
        const existingPlots = row.landPlotId.map((id, index) => ({
            id: id, // 保持原始类型，不做转换
            landName: row.landPlotName[index],
            landArea: row.area,
            address: '',
            image: row.landPlotImage ? row.landPlotImage[index] : ''
        }))

        landPlotOptions.value = existingPlots
    }

    open.value = true
    title.value = '修改农活信息'
}

/** 查看按钮操作 */
function handleView(row) {
    // 在设置 viewForm 之前处理日期
    const formattedRow = {
        ...row,
        createTime: row.createTime ? new Date(row.createTime).toISOString() : null
    }
    viewForm.value = formattedRow
    viewDialogVisible.value = true
}

/** 提交按钮 */
function submitForm() {
    formRef.value.validate(valid => {
        if (valid) {
            const submitData = {
                ...form.value,
                landPlotId: form.value.landPlotIds || [],
                cropId: form.value.cropId || [],
                workTypeId: form.value.workTypeId || [],
                // 确保时间格式正确
                endTime: form.value.endTime ? new Date(form.value.endTime).toISOString() : undefined
            }

            delete submitData.landPlotIds

            if (form.value.id) {
                updateFarmWork(submitData).then(response => {
                    proxy.$modal.msgSuccess('修改成功')
                    open.value = false
                    getList()
                })
            } else {
                addFarmWork(submitData).then(response => {
                    proxy.$modal.msgSuccess('新增成功')
                    open.value = false
                    getList()
                })
            }
        }
    })
}

/** 删除按钮操作 */
function handleDelete(row) {
    proxy.$modal.confirm('是否确认下架该数据项？').then(() => {
        return delFarmWork(row.id)
    }).then(() => {
        getList()
        proxy.$modal.msgSuccess('下架成功')
    }).catch(() => {})
}

/** 获取作物选项 */
function getCropOptions() {
    getCrops().then(response => {
        if (response.code === 200) {
            // 直接使使用后端返回的数据，不需要重新映射字段
            cropOptions.value = response.data
            console.log('作物选项:', cropOptions.value)
        } else {
            ElMessage.error('获取作物类型失败')
        }
    }).catch(error => {
        console.error('获取作物类型出错:', error)
        ElMessage.error('获取作物类型出错')
    })
}

/** 获取工作类选项 */
function getWorkTypeOptions() {
    getWorkTypes().then(response => {
        if (response.code === 200) {
            // 直接使用后端返回的数据，不需要重映射字段名
            workTypeOptions.value = response.data
            console.log('工作类型选项:', workTypeOptions.value)
        } else {
            ElMessage.error('获取工作类型失败')
        }
    }).catch(error => {
        console.error('获取工作类型出错:', error)
        ElMessage.error('获取工作类型错')
    })
}

/** 获取地块选项 */
function getLandPlotOptions() {
    landPlotLoading.value = true
    queryLandPlot(landPlotQuery.value).then(response => {
        if (response.code === 200) {
            // 直接使用返回的数据，不做ID类型转换
            landPlotOptions.value = response.data || []
            landPlotTotal.value = response.total
        }
        landPlotLoading.value = false
    }).catch(error => {
        console.error('获取地块列表出错:', error)
        landPlotLoading.value = false
    })
}

/** 处理地块点击事件 */
function handleLandPlotClick() {
    // 在这里理地块点击事件
    console.log('地块点击事件')
}

/** 显示地块详情 */
function showLandPlotDetail(item) {
    selectedLandPlot.value = item
    landPlotDetailVisible.value = true
}

/** 确认选择地块 */
function confirmSelectLandPlot() {
    // 在里处理确认选择地块的逻辑
    console.log('确认选择地块')
    landPlotDetailVisible.value = false
}

/** 显示地块选择对话框 */
function showLandPlotSelector() {
    landPlotSelectorVisible.value = true
    // 重置查询条件
    landPlotQuery.value = {
        landName: undefined,
        landType: undefined,
        address: undefined,
        beginArea: undefined,
        endArea: undefined,
        current: 1
    }
    // 立即加载地块数据
    getLandPlotOptions()
}

/** 切换地块选择状态 */
function toggleLandPlotSelection(landPlot) {
    const id = landPlot.id // 直接使用ID，不做类型转换
    const index = form.value.landPlotIds.indexOf(id)

    if (index === -1) {
        form.value.landPlotIds.push(id)
    } else {
        form.value.landPlotIds.splice(index, 1)
    }

    calculateTotalArea()
}

/** 获取地块名称 */
function getLandPlotName(id) {
    const landPlot = landPlotOptions.value.find(item => item.id === id) // 直接比较，不做类型转换
    return landPlot ? landPlot.landName : ''
}

/** 确认地块选择 */
function confirmLandPlotSelection() {
    // 确保在关闭对话框前更新一次总面积
    calculateTotalArea()
    landPlotSelectorVisible.value = false
}

/** 搜索地块 */
function searchLandPlots() {
    landPlotQuery.value.current = 1
    getLandPlotOptions()
}

/** 重置地块查询 */
function resetLandPlotQuery() {
    landPlotQuery.value = {
        landName: undefined,
        landType: undefined,
        address: undefined,
        beginArea: undefined,
        endArea: undefined,
        current: 1
    }
    proxy.resetForm('landPlotQueryFormRef')
    getLandPlotOptions()
}

/** 处理地块大小变化 */
function handleLandPlotSizeChange(newSize) {
    landPlotQuery.value.current = newSize
}

/** 处理地块当前页变化 */
function handleLandPlotCurrentChange(newPage) {
    landPlotQuery.value.current = newPage
    getLandPlotOptions()
}

/** 移除地块标签的处理 */
function removeLandPlot(id) {
    const index = form.value.landPlotIds.indexOf(id)
    if (index !== -1) {
        form.value.landPlotIds.splice(index, 1)
        // 重新计算总面积
        calculateTotalArea()
    }
}

/** 计算选中地块的总面积 */
function calculateTotalArea() {
    const totalArea = landPlotOptions.value
        .filter(plot => form.value.landPlotIds.includes(plot.id)) // 直接比较，不做类型转换
        .reduce((sum, plot) => sum + (Number(plot.landArea) || 0), 0)

    form.value.area = Number(totalArea.toFixed(2))
}

/** 格式化时间 */
function formatDateTime(time) {
    if (!time || time === '0-0-0') {
        return '-'
    }
    const date = new Date(time)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
}

function handleLocationInputConfirm() {
    if (locationInput.value) {
        queryParams.value.location.push(locationInput.value)
        locationInput.value = ''
    }
}

function handleLocationClose(index) {
    queryParams.value.location.splice(index, 1)
}

onMounted(() => {
    getList()
    getCropOptions() // 获取作物选项
    getWorkTypeOptions() // 添加这行
    getLandPlotOptions() // 添加这行
})
</script>


<style scoped>
.app-container {
    height: 100%;
}

.content-wrapper {
    padding: 20px;
    display: flex;
    flex-direction: column;
    min-height: calc(100vh - 84px); /* 减去头��和面包屑的高度 */
}

.main-content {
    flex: 1;
    overflow-y: auto;
}

.pagination-container {
    margin-top: auto; /* 将分页推到底部 */
    padding-top: 15px;
    background-color: #fff;
    border-top: 1px solid #e4e7ed;
}

/* 在小屏幕设备上调整样式 */
@media screen and (max-width: 768px) {
    .content-wrapper {
        padding: 10px;
    }
}

.detail-container {
    display: flex;
    gap: 20px;
}

.left-container {
    width: 400px;
    height: 400px;
    flex-shrink: 0;
    background-color: #f5f7fa;
    border-radius: 4px;
    overflow: hidden;
}

.map-container {
    width: 100%;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    background-color: #f5f7fa;
}

.info-container {
    flex: 1;
    min-width: 0;
}

.land-plot-option {
    display: flex;
    align-items: center;
    padding: 8px 0;
}

.land-plot-info {
    flex: 1;
    margin-right: 10px;
}

.land-name {
    font-weight: bold;
    margin-bottom: 4px;
}

.land-details {
    font-size: 12px;
    color: #666;
}

.land-details span {
    display: block;
    margin-bottom: 2px;
}

.land-image {
    width: 60px;
    height: 60px;
    object-fit: cover;
    border-radius: 4px;
}

.image-slot {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 100%;
    background: #f5f7fa;
    color: #909399;
    font-size: 20px;
}

.land-plot-selector {
    width: 100%;
}

.selected-plots {
    margin-top: 10px;
}

.land-plot-list {
    max-height: 600px;
    overflow-y: auto;
    padding: 10px;
}

.land-plot-card {
    height: 320px;
    margin-bottom: 20px;
    position: relative;
    cursor: pointer;
    transition: all 0.3s;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    /* 添加下面这行，预留边框空间 */
    border: 2px solid transparent;
}

.land-plot-card:hover {
    box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
    transform: translateY(-2px);
}

.land-plot-card.is-selected {
    border-color: #409EFF;
    /* 移除原来可能存在的边框样式 */
    /* border: 2px solid #409EFF; */
}

/* 确保内容区域不受边框影响 */
.el-card__body {
    padding: 0 !important;
    height: 100%;
    display: flex;
    flex-direction: column;
}

.land-plot-image {
    height: 160px;
    background-color: #f5f7fa;
    display: flex;
    justify-content: center;
    align-items: center;
}

.land-plot-image .el-image {
    width: 100%;
    height: 100%;
}

.no-image {
    font-size: 40px;
    color: #909399;
}

.land-plot-info {
    padding: 10px;
}

.land-plot-info h3 {
    margin: 0 0 10px 0;
    font-size: 16px;
}

.land-plot-info p {
    margin: 5px 0;
    font-size: 14px;
    color: #666;
}

.select-indicator {
    position: absolute;
    top: 10px;
    right: 10px;
    background-color: rgba(255,255,255,0.9);
    padding: 5px;
    border-radius: 50%;
}

.search-form {
    margin-bottom: 20px;
    padding: 18px;
    background-color: #f5f7fa;
    border-radius: 4px;
}

.el-range-separator {
    padding: 0 8px;
    line-height: 32px;
    text-align: center;
    color: #909399;
}

/* 调整数字输入框的样式 */
.el-input-number {
    width: 110px;
    margin-right: 5px;
}

.el-input-number:last-child {
    margin-right: 0;
}

/* 可选：调整多选框的样式 */
.el-select {
    width: 240px;
}

/* 可选：调整多选标签的样式 */
.el-select__tags {
    max-width: calc(100% - 30px);
}

/* 添加新的样式 */
.table-container {
    min-height: calc(100vh - 330px); /* 可以根据实际情况调整这个值 */
    display: flex;
    flex-direction: column;
}

.el-table {
    flex: 1;
    margin-bottom: 10px; /* 给分页组件留出一些空间 */
}

.pagination-container {
    margin-top: auto; /* 将分页推到容器底部 */
}
</style>
