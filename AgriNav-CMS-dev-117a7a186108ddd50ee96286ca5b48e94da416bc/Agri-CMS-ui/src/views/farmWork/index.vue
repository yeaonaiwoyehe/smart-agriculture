<template>
    <div class="app-container">
        <div class="content-wrapper">
            <div class="main-content">
                <!-- 搜索区域 -->
                <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
                    <el-form-item label="名称" prop="name">
                        <el-input
                                v-model="queryParams.name"
                                placeholder="请输入名称"
                                clearable
                                @keyup.enter.native="handleQuery"
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
                                @keyup.enter.native="handleLocationInputConfirm"
                        >
                            <el-button slot="append" icon="el-icon-plus" @click="handleLocationInputConfirm"></el-button>
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
                        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
                        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
                    </el-form-item>
                </el-form>

                <!-- 操作按钮区域 -->
                <el-row :gutter="10" class="mb8">
                    <el-col :span="1.5">
                        <el-button
                                type="primary"
                                plain
                                icon="el-icon-plus"
                                size="mini"
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
                        <template slot-scope="scope">
                            {{ formatDateTime(scope.row.createTime) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="截止时间" align="center" prop="endTime" width="180">
                        <template slot-scope="scope">
                            {{ formatDateTime(scope.row.endTime) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" align="center" prop="status">
                        <template slot-scope="scope">
                            <dict-tag :options="dict.type.agricultural_machinery_status" :value="scope.row.status"/>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                        <template slot-scope="scope">
                            <el-button
                                    size="mini"
                                    type="text"
                                    icon="el-icon-view"
                                    @click="handleView(scope.row)"
                            >查看</el-button>
                            <el-button
                                    size="mini"
                                    type="text"
                                    icon="el-icon-edit"
                                    @click="handleUpdate(scope.row)"
                            >修改</el-button>
                            <el-button
                                    size="mini"
                                    type="text"
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
                        :page.sync="queryParams.pageNum"
                        :limit.sync="queryParams.pageSize"
                        @pagination="getList"
                />
            </div>
        </div>

        <!-- 添加或修改对话框 -->
        <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
            <el-form ref="form" :model="form" :rules="rules" label-width="80px">
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
                            value-format="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
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
            <div slot="footer" class="dialog-footer">
                <el-button type="primary" @click="submitForm">确 定</el-button>
                <el-button @click="cancel">取 消</el-button>
            </div>
        </el-dialog>

        <!-- 查看详情对话框 -->
        <el-dialog title="查看详情" :visible.sync="viewDialogVisible" width="800px" append-to-body>
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
        <el-dialog title="地块详情" :visible.sync="landPlotDetailVisible" width="800px" append-to-body>
            <div class="detail-container">
                <div class="left-container">
                    <el-image
                            v-if="selectedLandPlot.image"
                            :src="selectedLandPlot.image"
                            fit="contain"
                            style="width: 100%; height: 100%"
                            :preview-src-list="[selectedLandPlot.image]"
                    >
                        <div slot="error" class="image-slot">
                            <i class="el-icon-picture-outline"></i>
                        </div>
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
        <el-dialog title="选择地块" :visible.sync="landPlotSelectorVisible" width="1200px" append-to-body>
            <!-- 搜索表单 -->
            <el-form :model="landPlotQuery" ref="landPlotQueryForm" :inline="true" class="search-form">
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
                                @click.native="toggleLandPlotSelection(item)"
                        >
                            <div class="land-plot-image">
                                <template v-if="item.image">
                                    <el-image
                                            :src="item.image"
                                            fit="cover"
                                    >
                                        <div slot="error" class="image-slot">
                                            <i class="el-icon-picture-outline"></i>
                                            <span>暂无图片数据</span>
                                        </div>
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
                                <el-checkbox :value="form.landPlotIds.includes(item.id.toString())"></el-checkbox>
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

            <div slot="footer" class="dialog-footer">
                <el-button type="primary" @click="confirmLandPlotSelection">确定</el-button>
                <el-button @click="landPlotSelectorVisible = false">取消</el-button>
            </div>
        </el-dialog>
    </div>
</template>

<script>
import { listFarmWork, addFarmWork, updateFarmWork, delFarmWork } from "@/api/farmWork/farmWork";
import { getCrops } from '@/api/field/crop';
import { getWorkTypes } from '@/api/field/workType'
import { queryLandPlot } from "@/api/field/square";

export default {
    name: "FarmWork",
    dicts: ['agricultural_machinery_status'],
    data() {
        // 自定义验证规则
        const validatePhone = (rule, value, callback) => {
            const phoneRegex = /^1[3-9]\d{9}$/;
            if (!value) {
                callback(new Error('请输入联系方式'));
            } else if (!phoneRegex.test(value)) {
                callback(new Error('请输入正确的11位手机号码'));
            } else {
                callback();
            }
        };

        return {
            // 遮罩层
            loading: true,
            // 显示搜索条件
            showSearch: true,
            // 总条数
            total: 0,
            // 寻找农机表格数据
            farmWorkList: [],
            // 弹出层标
            title: "",
            // 是否显示弹出
            open: false,
            // 看情框否可见
            viewDialogVisible: false,
            // 查看的表单数据
            viewForm: {},
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                name: undefined,
                phoneNumber: undefined,
                status: undefined,
                myFarmWork: true,  // 添加这个字段
                cropId: [],    // 改为cropId以匹配后端DTO
                workTypeId: [], // 改为workTypeId以匹配后端DTO
                minArea: undefined, // 添加最小面积
                maxArea: undefined,  // 添加最大面积
                orderByCreateTimeDesc: true, // 添加排序字段
                location: [],
            },
            // 表单参数
            form: {
                id: undefined,
                name: undefined,
                area: undefined,
                phoneNumber: undefined,
                endTime: undefined,
                cropId: [], // 这些字段用于表单绑定
                workTypeId: [],
                landPlotIds: [], // 存储字符串类型的ID
            },
            // 表单校验
            rules: {
                name: [
                    { required: true, message: "名称不能为空", trigger: "blur" }
                ],
                phoneNumber: [
                    { required: true, message: "联系方式不能为空", trigger: "blur" },
                    { validator: validatePhone, trigger: 'blur' }
                ],
                endTime: [
                    { required: true, message: "截止时间不能为空", trigger: "blur" }
                ]
            },
            // 作物选项列表
            cropOptions: [],
            // 工作类型选项
            workTypeOptions: [],
            // 地块选项列表
            landPlotOptions: [],
            // 地块详情对话框是否见
            landPlotDetailVisible: false,
            // 选中的地块
            selectedLandPlot: {},
            // 地块选择器对话框显示状态
            landPlotSelectorVisible: false,
            // 地块查询参数
            landPlotQuery: {
                landName: undefined,
                landType: undefined,
                address: undefined,
                beginArea: undefined,
                endArea: undefined,
                current: 1
            },
            // 地块总数
            landPlotTotal: 0,
            // 地块加载状态
            landPlotLoading: false,
            selectedLocation: [],
            addressOptions: [],
            addressProps: {
                value: 'id',
                label: 'name',
                children: 'children',
                expandTrigger: 'hover'
            },
            locationInput: '',
        };
    },
    created() {
        this.getList();
        this.getCropOptions(); // 获取作物选项
        this.getWorkTypeOptions(); // 添加这行
        this.getLandPlotOptions(); // 添加这行
    },
    methods: {
        /** 验证面积范围 */
        validateArea() {
            if (this.queryParams.minArea && this.queryParams.maxArea) {
                if (this.queryParams.minArea > this.queryParams.maxArea) {
                    this.$message({
                        message: '最小面积不能大于最大面积',
                        type: 'warning'
                    });
                    // 清空最大面积
                    this.queryParams.maxArea = undefined;
                    return false;
                }
            }
            return true;
        },

        /** 查询列表 */
        getList() {
            this.loading = true;
            const params = {
                pageNum: this.queryParams.pageNum,
                pageSize: this.queryParams.pageSize,
                name: this.queryParams.name,
                phoneNumber: this.queryParams.phoneNumber,
                myFarmWork: this.queryParams.myFarmWork,
                orderByCreateTimeDesc: this.queryParams.orderByCreateTimeDesc
            };

            // 添加面积范围参数
            if (this.queryParams.minArea !== undefined) {
                params.minArea = this.queryParams.minArea;
            }
            if (this.queryParams.maxArea !== undefined) {
                params.maxArea = this.queryParams.maxArea;
            }

            // 只有当数组有值时才添加到查询参数中
            if (this.queryParams.cropId && this.queryParams.cropId.length > 0) {
                params.cropId = this.queryParams.cropId;
            }

            if (this.queryParams.workTypeId && this.queryParams.workTypeId.length > 0) {
                params.workTypeId = this.queryParams.workTypeId;
            }

            if (this.queryParams.status !== undefined && this.queryParams.status !== '') {
                params.status = Number(this.queryParams.status);
            }

            // 添加位置查询参数
            if (this.queryParams.location && this.queryParams.location.length > 0) {
                params.location = this.queryParams.location;
            }

            listFarmWork(params).then(response => {
                this.farmWorkList = (response.rows || response.data).map(item => ({
                    ...item,
                    cropNames: item.cropNames || []
                }));
                this.total = response.total;
                this.loading = false;
            });
        },
        // 取消按钮
        cancel() {
            this.open = false;
            this.reset();
        },
        // 表单重置
        reset() {
            this.form = {
                id: undefined,
                name: undefined,
                area: undefined,
                phoneNumber: undefined,
                endTime: undefined,
                cropId: [], // 确保初始化为空数组
                workTypeId: [], // 确保初始化为空数组
                landPlotIds: [] // 确保初始化为空数组
            };
            // 清空地块选项
            this.landPlotOptions = [];
            this.resetForm("form");
        },
        /** 搜索按钮操作 */
        handleQuery() {
            // 在搜索前验证面积范围
            if (this.validateArea()) {
                this.queryParams.pageNum = 1;
                this.getList();
            }
        },
        /** 重置按钮操作 */
        resetQuery() {
            this.resetForm("queryForm");
            this.queryParams = {
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
                orderByCreateTimeDesc: true, // 添加排序字段
            };
            this.handleQuery();
        },
        /** 新增按钮操作 */
        handleAdd() {
            this.reset();
            this.open = true;
            this.title = "添加寻找农机";
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.reset();
            this.landPlotOptions = []; 
            const endTime = row.endTime ? new Date(new Date(row.endTime).getTime() + 8 * 3600 * 1000).toISOString() : undefined;

            // 确保landPlotId保持字符串类型
            this.form = {
                id: row.id,
                name: row.name,
                area: row.area,
                phoneNumber: row.phoneNumber,
                endTime: endTime,
                cropId: row.cropId || [],
                workTypeId: row.workTypeId || [],
                landPlotIds: row.landPlotId || [] // 直接使用，不做类型转换
            };

            // 只有当当前行有地块数据时才添加到选项中
            if (row.landPlotId && row.landPlotId.length > 0) {
                const existingPlots = row.landPlotId.map((id, index) => ({
                    id: id, // 保持原始类型，不做转换
                    landName: row.landPlotName[index],
                    landArea: row.area,
                    address: '',
                    image: row.landPlotImage ? row.landPlotImage[index] : ''
                }));

                this.landPlotOptions = existingPlots;
            }

            this.open = true;
            this.title = "修改农活信息";
        },
        /** 查看按钮操作 */
        handleView(row) {
            // 在设置 viewForm 之前处理日期
            const formattedRow = {
                ...row,
                createTime: row.createTime ? new Date(row.createTime).toISOString() : null
            };
            this.viewForm = formattedRow;
            this.viewDialogVisible = true;
        },
        /** 提交按钮 */
        submitForm() {
            this.$refs["form"].validate(valid => {
                if (valid) {
                    const submitData = {
                        ...this.form,
                        landPlotId: this.form.landPlotIds || [],
                        cropId: this.form.cropId || [],
                        workTypeId: this.form.workTypeId || [],
                        // 确保时间格式正确
                        endTime: this.form.endTime ? new Date(this.form.endTime).toISOString() : undefined
                    };

                    delete submitData.landPlotIds;

                    if (this.form.id) {
                        updateFarmWork(submitData).then(response => {
                            this.$modal.msgSuccess("修改成功");
                            this.open = false;
                            this.getList();
                        });
                    } else {
                        addFarmWork(submitData).then(response => {
                            this.$modal.msgSuccess("新增成功");
                            this.open = false;
                            this.getList();
                        });
                    }
                }
            });
        },
        /** 删除按钮操作 */
        handleDelete(row) {
            this.$modal.confirm('是否确认下架该数据项？').then(() => {
                return delFarmWork(row.id);
            }).then(() => {
                this.getList();
                this.$modal.msgSuccess("下架成功");
            }).catch(() => {});
        },
        /** 获取作物选项 */
        getCropOptions() {
            getCrops().then(response => {
                if (response.code === 200) {
                    // 直接使使用后端返回的数据，不需要重新映射字段
                    this.cropOptions = response.data;
                    console.log('作物选项:', this.cropOptions);
                } else {
                    this.$message.error("获取作物类型失败");
                }
            }).catch(error => {
                console.error("获取作物类型出错:", error);
                this.$message.error("获取作物类型出错");
            });
        },
        /** 获取工作类选项 */
        getWorkTypeOptions() {
            getWorkTypes().then(response => {
                if (response.code === 200) {
                    // 直接使用后端返回的数据，不需要重映射字段名
                    this.workTypeOptions = response.data;
                    console.log('工作类型选项:', this.workTypeOptions);
                } else {
                    this.$message.error("获取工作类型失败");
                }
            }).catch(error => {
                console.error("获取工作类型出错:", error);
                this.$message.error("获取工作类型错");
            });
        },
        /** 获取地块选项 */
        getLandPlotOptions() {
            this.landPlotLoading = true;
            queryLandPlot(this.landPlotQuery).then(response => {
                if (response.code === 200) {
                    // 直接使用返回的数据，不做ID类型转换
                    this.landPlotOptions = response.data || [];
                    this.landPlotTotal = response.total;
                }
                this.landPlotLoading = false;
            }).catch(error => {
                console.error("获取地块列表出错:", error);
                this.landPlotLoading = false;
            });
        },
        /** 处理地块点击事件 */
        handleLandPlotClick() {
            // 在这里理地块点击事件
            console.log('地块点击事件');
        },
        /** 显示地块详情 */
        showLandPlotDetail(item) {
            this.selectedLandPlot = item;
            this.landPlotDetailVisible = true;
        },
        /** 确认选择地块 */
        confirmSelectLandPlot() {
            // 在里处理确认选择地块的逻辑
            console.log('确认选择地块');
            this.landPlotDetailVisible = false;
        },
        /** 显示地块选择对话框 */
        showLandPlotSelector() {
            this.landPlotSelectorVisible = true;
            // 重置查询条件
            this.landPlotQuery = {
                landName: undefined,
                landType: undefined,
                address: undefined,
                beginArea: undefined,
                endArea: undefined,
                current: 1
            };
            // 立即加载地块数据
            this.getLandPlotOptions();
        },
        /** 切换地块选择状态 */
        toggleLandPlotSelection(landPlot) {
            const id = landPlot.id; // 直接使用ID，不做类型转换
            const index = this.form.landPlotIds.indexOf(id);

            if (index === -1) {
                this.form.landPlotIds.push(id);
            } else {
                this.form.landPlotIds.splice(index, 1);
            }

            this.calculateTotalArea();
        },
        /** 获取地块名称 */
        getLandPlotName(id) {
            const landPlot = this.landPlotOptions.find(item => item.id === id); // 直接比较，不做类型转换
            return landPlot ? landPlot.landName : '';
        },
        /** 确认地块选择 */
        confirmLandPlotSelection() {
            // 确保在关闭对话框前更新一次总面积
            this.calculateTotalArea();
            this.landPlotSelectorVisible = false;
        },
        /** 搜索地块 */
        searchLandPlots() {
            this.landPlotQuery.current = 1;
            this.getLandPlotOptions();
        },
        /** 重置地块查询 */
        resetLandPlotQuery() {
            this.landPlotQuery = {
                landName: undefined,
                landType: undefined,
                address: undefined,
                beginArea: undefined,
                endArea: undefined,
                current: 1
            };
            this.resetForm("landPlotQueryForm");
            this.getLandPlotOptions();
        },
        /** 处理地块大小变化 */
        handleLandPlotSizeChange(newSize) {
            this.landPlotQuery.current = newSize;
        },
        /** 处理地块当前页变化 */
        handleLandPlotCurrentChange(newPage) {
            this.landPlotQuery.current = newPage;
            this.getLandPlotOptions();
        },
        /** 移除地块标签的处理 */
        removeLandPlot(id) {
            const index = this.form.landPlotIds.indexOf(id);
            if (index !== -1) {
                this.form.landPlotIds.splice(index, 1);
                // 重新计算总面积
                this.calculateTotalArea();
            }
        },
        /** 计算选中地块的总面积 */
        calculateTotalArea() {
            const totalArea = this.landPlotOptions
                .filter(plot => this.form.landPlotIds.includes(plot.id)) // 直接比较，不做类型转换
                .reduce((sum, plot) => sum + (Number(plot.landArea) || 0), 0);

            this.form.area = Number(totalArea.toFixed(2));
        },
        /** 格式化时间 */
        formatDateTime(time) {
            if (!time || time === '0-0-0') {
                return '-';
            }
            const date = new Date(time);
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            return `${year}-${month}-${day}`;
        },
        handleLocationInputConfirm() {
            if (this.locationInput) {
                this.queryParams.location.push(this.locationInput);
                this.locationInput = '';
            }
        },
        handleLocationClose(index) {
            this.queryParams.location.splice(index, 1);
        },
    }
};
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
