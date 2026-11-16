<template>
  <div class="app-container">
    <!-- 将整个内容包裹在一个flex容器中 -->
    <div class="content-wrapper">
      <!-- 主要内容区域 -->
      <div class="main-content">
        <!-- 原有的表单、表格等内容 -->
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="名称" prop="name">
            <el-input
              v-model="queryParams.name"
              placeholder="请输入农机找活名称"
              clearable
              style="width: 240px"
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item label="农机类型" prop="typeId">
            <el-select v-model="queryParams.typeId" placeholder="请选择农机类型" clearable style="width: 240px">
              <el-option
                v-for="item in typeOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="农机找活状态" clearable style="width: 240px">
              <el-option
                v-for="dict in dict.type.agricultural_machinery_status"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="作物类型" prop="cropId">
            <el-select
              v-model="queryParams.cropId"
              multiple
              placeholder="请选择作物类"
              clearable
              style="width: 240px"
            >
              <el-option
                v-for="item in cropOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
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

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button
              type="primary"
              plain
              icon="el-icon-plus"
              size="mini"
              @click="handleAdd"
              v-hasPermi="['home:agriculturalMachinery:add']"
            >新增
            </el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table
          v-loading="loading"
          :data="agriculturalMachineryList"
        >
          <el-table-column label="名称" align="center" prop="name"/>
          <el-table-column label="农机类型" align="center" prop="typeName"/>
          <el-table-column label="联系方式" align="center" prop="phoneNumber"/>
          <el-table-column label="运粮车" align="center">
            <template slot-scope="scope">
              <el-tag :type="scope.row.havingGrainCarrier ? 'success' : 'info'">
                {{ scope.row.havingGrainCarrier ? '有' : '无' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center">
            <template slot-scope="scope">
              <dict-tag :options="dict.type.agricultural_machinery_status" :value="scope.row.status"/>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template slot-scope="scope">
              <span>{{ formatDate(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="截止时间" align="center" prop="endTime" width="180">
            <template slot-scope="scope">
              <span>{{ formatDate(scope.row.endTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="200">
            <template slot-scope="scope">
              <el-button
                size="mini"
                type="text"
                icon="el-icon-view"
                @click="handleView(scope.row)"
              >查看详情
              </el-button>
              <el-button
                size="mini"
                type="text"
                icon="el-icon-edit"
                @click="handleUpdate(scope.row)"
              >修改
              </el-button>
              <el-button
                size="mini"
                type="text"
                icon="el-icon-delete"
                @click="handleDelete(scope.row)"
              >下架
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页区域 -->
      <div class="pagination-container">
        <pagination
          v-show="total>0"
          :total="total"
          :page.sync="pageNum"
          :limit.sync="pageSize"
          @pagination="getList"
        />
      </div>
    </div>

    <!-- 添加或修改农机找活对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入农机找活名称"/>
        </el-form-item>
        <el-form-item label="农机类型" prop="typeId">
          <el-select v-model="form.typeId" placeholder="请选择农机类型">
            <el-option
              v-for="item in typeOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="联系方式" prop="phoneNumber">
          <el-input v-model="form.phoneNumber" placeholder="请输联系方式"/>
        </el-form-item>
        <el-form-item label="作物类型" prop="cropId">
          <el-select v-model="form.cropId" multiple placeholder="请选择作物类型">
            <el-option
              v-for="item in cropOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="作业区域" prop="locations">
          <div v-for="(location, index) in form.locations" :key="index" style="margin-bottom: 10px">
            <el-cascader
              v-model="location.codes"
              :options="addressOptions"
              :props="addressProps"
              @change="(value) => handleAddressChange(value, index)"
              :change-on-select="true"
              placeholder="请选择作业区域"
              style="width: 85%"
            ></el-cascader>
            <el-button type="text" @click="removeLocation(index)" style="margin-left: 10px">删除</el-button>
          </div>
          <el-button type="text" @click="addLocation" icon="el-icon-plus">添加作业区域</el-button>
        </el-form-item>
        <el-form-item label="运粮车" prop="havingGrainCarrier">
          <el-switch v-model="form.havingGrainCarrier"></el-switch>
        </el-form-item>
        <el-form-item label="图片" prop="image">
          <el-upload
            class="upload-demo"
            action="#"
            :http-request="uploadImage"
            :on-preview="handlePreview"
            :on-remove="handleRemove"
            :file-list="fileList"
            list-type="picture">
            <el-button size="small" type="primary">点击上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="截止日期" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="date"
            placeholder="选择截止日期"
            value-format="yyyy-MM-dd"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 添加查看详情的对话框 -->
    <el-dialog title="查看详情" :visible.sync="viewDialogVisible" width="700px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="名称">{{ viewForm.name }}</el-descriptions-item>
        <el-descriptions-item label="农机类型">{{ viewForm.typeName }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ viewForm.phoneNumber }}</el-descriptions-item>
        <el-descriptions-item label="运粮车">
          <el-tag :type="viewForm.havingGrainCarrier ? 'success' : 'info'">
            {{ viewForm.havingGrainCarrier ? '有' : '无' }}
          </el-tag>
        </el-descriptions-item>
        <!-- 添加创建时间和截止时间的显示 -->
        <el-descriptions-item label="创建时间">{{ formatDate(viewForm.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="截止时间">{{ formatDate(viewForm.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="作物类型" :span="2">
          <el-tag
            v-for="(crop, index) in viewForm.crop"
            :key="index"
            size="mini"
            type="success"
            style="margin-right: 5px"
          >{{ crop }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="作业区域" :span="2">
          <el-tag
            v-for="(location, index) in viewForm.location"
            :key="index"
            size="mini"
            type="info"
            style="margin-right: 5px"
          >{{ location }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 图片轮播 -->
      <div class="carousel-container" v-if="viewForm.image && viewForm.image.length > 0">
        <h4 class="carousel-title">图片展示</h4>
        <el-carousel :height="carouselHeight" trigger="click" indicator-position="outside">
          <el-carousel-item v-for="(url, index) in viewForm.image" :key="index">
            <div class="carousel-image-container">
              <el-image
                :src="url"
                fit="contain"
                :preview-src-list="viewForm.image"
              >
                <div slot="error" class="image-slot">
                  <i class="el-icon-picture-outline"></i>
                </div>
              </el-image>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>

      <div slot="footer" class="dialog-footer">
        <el-button @click="viewDialogVisible = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listAgriculturalMachinery,
  getAgriculturalMachinery,
  delAgriculturalMachinery,
  addAgriculturalMachinery,
  updateAgriculturalMachinery
} from "@/api/agriculturalMachinery/agriculturalMachinery";
import {getToken} from "@/utils/auth";
import {parseTime} from "@/utils";
import {listAgriculturalMachineryTypes} from "@/api/agriculturalMachinery/agriculturalMachineryType";
import {getCrops} from "@/api/field/crop"; // 请确保导入路径正确
import {getProvinces, getCities, getDistricts, getVillages, getHamlets} from "@/api/field/landAddress";

export default {
  name: "AgriculturalMachinery",
  // 声明要使用的字典
  dicts: ['agricultural_machinery_status'],
  data() {
    // 自定义验证规则
    const validatePhone = (rule, value, callback) => {
      if (value && !/^1[3-9]\d{9}$/.test(value)) {
        callback(new Error('请输入正确的11位手机号码'));
      } else {
        callback();
      }
    };

    return {
      // 遮罩层
      loading: true,
      // 显示搜索
      showSearch: true,
      // 总条数
      total: 0,
      // 农机找活表格数据
      agriculturalMachineryList: [],
      // 弹出层标
      title: "",
      // 是否显示弹出层
      open: false,
      // 状态数据字典
      statusOptions: [],
      // 查询参数
      queryParams: {
        name: null,
        typeId: null,
        status: null,
        cropId: [],
        location: [],
        myAgriculturalMachinery: true,
        orderByCreateTimeDesc: true  // 默认按最新发布排序
      },
      pageNum: 1,
      pageSize: 10,
      // 表单参数
      form: {
        id: null,
        name: null,
        typeId: null,
        phoneNumber: null,
        cropId: [],
        locations: [{codes: [], address: ''}], // 修改为数组形式存储多个地址
        havingGrainCarrier: false,
        image: [],
        endTime: '',
      },
      // 表单校验
      rules: {
        name: [
          {required: true, message: "农活名称不能为", trigger: "blur"}
        ],
        typeId: [
          {required: true, message: "农机类型不能为空", trigger: "change"}
        ],
        phoneNumber: [
          {required: true, message: "联系方式不能为空", trigger: "blur"},
          {validator: validatePhone, trigger: "blur"}
        ],
        cropId: [
          {required: true, message: "作物类型不能为空", trigger: "change"}
        ],
        locations: [
          {required: true, message: "至少选择一个作业区域", trigger: "change"}
        ],
        endTime: [
          {required: true, message: "截止日期不能为空", trigger: "change"}
        ],
      },
      typeOptions: [],
      cropOptions: [],
      addressOptions: [],
      addressProps: {
        value: 'code',
        label: 'name',
        children: 'children',
        lazy: true,
        lazyLoad: this.lazyLoadAddress
      },
      fileList: [],
      // 存储地址数据的缓存
      addressCache: new Map(),
      viewDialogVisible: false,
      viewForm: {
        name: '',
        typeName: '',
        phoneNumber: '',
        havingGrainCarrier: false,
        crop: [],
        location: [],
        image: []
      },
      carouselHeight: '300px', // 轮播图高度
      locationInput: '' // 添加作业区域输入框
    };
  },
  created() {
    this.getList();
    this.getDicts("agricultural_machinery_status").then(response => {
      this.statusOptions = response.data;
    });
    this.getTypeOptions();
    this.getCropOptions();
    this.getProvinceOptions();
  },
  methods: {
    /** 查询农找活列表 */
    getList() {
      this.loading = true;
      const params = {
        ...this.queryParams,
      };
      listAgriculturalMachinery(params, this.pageNum, this.pageSize).then(response => {
        this.agriculturalMachineryList = response.data;
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
        id: null,
        name: null,
        typeId: null,
        phoneNumber: null,
        cropId: [],
        locations: [{codes: [], address: ''}],
        havingGrainCarrier: false,
        image: [],
        endTime: '',
      };
      this.fileList = [];
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams.cropId = [];
      this.queryParams.location = [];
      this.queryParams.myAgriculturalMachinery = true;
      this.queryParams.orderByCreateTimeDesc = true; // 重置为默认排序
      this.handleQuery();
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加农机找活";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();

      // 设置表单数据
      this.form = {
        id: row.id,
        name: row.name,
        // 直接使用 typeId，这样如果不修改就会保持原值
        typeId: row.typeId,
        phoneNumber: row.phoneNumber,
        // 直接使用 cropId 数组，这样如果不修改就会保持原值
        cropId: row.cropId || [],
        havingGrainCarrier: row.havingGrainCarrier,
        image: row.image || [],
        endTime: row.endTime,
        locations: []
      };

      // 处理图片列表
      if (this.form.image && this.form.image.length > 0) {
        this.fileList = this.form.image.map(url => ({
          name: url.substring(url.lastIndexOf('/') + 1),
          url: url
        }));
      }

      // 处理作业区域
      if (row.location) {
        const locationArray = typeof row.location === 'string'
          ? row.location.split('、')
          : Array.isArray(row.location) ? row.location : [];

        this.form.locations = locationArray.map(addr => ({
          codes: [], // 这里会在后续异步填充
          address: addr
        }));

        // 异步获取每个地址的完整代码
        this.form.locations.forEach((location, index) => {
          this.getAddressCodesFromName(location.address, index);
        });
      }

      // 确保至少有一个作业区域输入框
      if (this.form.locations.length === 0) {
        this.form.locations = [{codes: [], address: ''}];
      }

      this.open = true;
      this.title = "修改农机找活";
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          const formData = new FormData();

          // 处理表单数据
          const formDataObj = {...this.form};
          // 确保使用原始的 typeId 和 cropId
          formDataObj.typeId = this.form.typeId;
          formDataObj.cropId = this.form.cropId;

          // 处理地址
          formDataObj.location = formDataObj.locations.map(loc => loc.address);
          delete formDataObj.locations;

          // 处理图片
          // 1. 处理新上传的图片文件
          this.fileList.forEach(file => {
            if (file.raw) {
              formData.append('image', file.raw);
            }
          });

          // 2. 收集所有已有的图片URL
          const existingImages = this.fileList
            .filter(file => !file.raw)
            .map(file => file.url);

          // 3. 设置图片URL到表单数据
          formDataObj.image = existingImages;

          // 将对象转换为JSON字符串，并添加到FormData中
          formData.append('agriculturalMachinery', JSON.stringify(formDataObj));

          if (this.form.id) {
            updateAgriculturalMachinery(formData).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addAgriculturalMachinery(formData).then(response => {
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
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确下架该数据项').then(function () {
        return delAgriculturalMachinery(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("下架成功");
      }).catch(() => {
      });
    },
    /** 获取农机类型选项 */
    getTypeOptions() {
      listAgriculturalMachineryTypes().then(response => {
        this.typeOptions = response.data.map(item => ({
          id: item.id,
          name: item.name
        }));
      });
    },
    /** 获取作物类型选项 */
    getCropOptions() {
      getCrops().then(response => {
        if (response.code === 200) {
          this.cropOptions = response.data.map(item => ({
            id: item.id,
            name: item.cropName
          }));
        } else {
          this.$message.error("获取作物类型失败");
        }
      }).catch(error => {
        console.error("获取作物类型出错:", error);
        this.$message.error("获取作物类型出错");
      });
    },
    /** 获取省份选项 */
    getProvinceOptions() {
      getProvinces().then(response => {
        if (response.code === 200) {
          this.addressOptions = response.data.map(item => ({
            code: item.code,
            name: item.name,
            children: []
          }));
        } else {
          this.$message.error("获取省份列表失败");
        }
      });
    },
    /** 懒加载地选项 */
    lazyLoadAddress(node, resolve) {
      const {level} = node;
      const parentCode = node.data ? node.data.code : '';

      // 如果已经有缓存的数据，直接使用缓存
      if (this.addressCache.has(parentCode)) {
        resolve(this.addressCache.get(parentCode));
        return;
      }

      if (level === 0) {
        // 加载省份
        getProvinces().then(response => {
          if (response.code === 200) {
            const provinces = response.data.map(item => ({
              code: item.code,
              name: item.name,
              leaf: false
            }));
            this.addressCache.set(parentCode, provinces);
            resolve(provinces);
          } else {
            this.$message.error("获取省份列表失败");
            resolve([]);
          }
        });
      } else {
        // 加载下级地址
        const {code, name} = node.data;
        const params = {
          code: code,
          name: name,
          level: level,
          pcode: level > 1 ? node.parent.data.code : 0,
          category: 0
        };

        let apiMethod;
        switch (level) {
          case 1:
            apiMethod = getCities;
            break;
          case 2:
            apiMethod = getDistricts;
            break;
          case 3:
            apiMethod = getVillages;
            break;
          case 4:
            apiMethod = getHamlets;
            break;
          default:
            resolve([]);
            return;
        }

        apiMethod(params).then(response => {
          if (response.code === 200) {
            const children = response.data.map(item => ({
              code: item.code,
              name: item.name,
              leaf: level === 4
            }));
            this.addressCache.set(code, children);
            resolve(children);
          } else {
            this.$message.error(`获取${['城市', '区县', '乡镇', '村镇'][level - 1]}列表失败`);
            resolve([]);
          }
        }).catch(error => {
          console.error(`获取${['城市', '区县', '乡镇', '村镇'][level - 1]}列表出错:`, error);
          this.$message.error(`获取${['城市', '区县', '乡镇', '村镇'][level - 1]}列表出错`);
          resolve([]);
        });
      }
    },
    /** 处理地址变化 */
    async handleAddressChange(value, index) {
      if (!value || value.length === 0) {
        this.form.locations[index].address = '';
        return;
      }

      let addressNames = [];
      let currentNode = {children: this.addressOptions};

      for (let i = 0; i < value.length; i++) {
        const code = value[i];

        if (!currentNode.children || currentNode.children.length === 0) {
          const cachedChildren = this.addressCache.get(currentNode.code);
          if (cachedChildren) {
            currentNode.children = cachedChildren;
          }
        }

        const matchedNode = currentNode.children.find(node => node.code === code);
        if (matchedNode) {
          addressNames.push(matchedNode.name);
          currentNode = matchedNode;
        }
      }

      this.form.locations[index].address = addressNames.join('');
    },
    /** 添加地址选择器 */
    addLocation() {
      this.form.locations.push({codes: [], address: ''});
    },
    /** 移除地址选择器 */
    removeLocation(index) {
      if (this.form.locations.length > 1) {
        this.form.locations.splice(index, 1);
      } else {
        this.$message.warning('至少保留一个作业区域');
      }
    },
    /** 图片上传 */
    uploadImage(param) {
      this.fileList.push({
        name: param.file.name,
        url: URL.createObjectURL(param.file),
        raw: param.file  // 保存原始文件对象
      });
    },
    /** 图片预览 */
    handlePreview(file) {
      // 实现图片预览逻辑
    },
    /** 图片移除 */
    handleRemove(file, fileList) {
      this.fileList = fileList;
    },
    /** 格式化日期（使用/分隔符） */
    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}/${month}/${day}`;
    },
    /** 格式化作物名称 */
    formatCropNames(cropNames) {
      if (!cropNames) return '-';
      return Array.isArray(cropNames) ? cropNames.join('、') : cropNames;
    },

    /** 格式化作业区域 */
    formatLocations(locations) {
      if (!locations) return '';
      return Array.isArray(locations) ? locations.join('、') : locations;
    },

    /** 格式化单个地址（只显示最后一级） */
    formatSingleLocation(location) {
      if (!location) return '';
      const parts = location.split(',');
      return parts[parts.length - 1] || location;
    },
    /** 查看详情按钮操作 */
    handleView(row) {
      this.viewForm = {...row};
      this.viewDialogVisible = true;
    },
    // 添加一个方法用于根据地址名称获取完整的地址代码
    async getAddressCodesFromName(address, index) {
      // 这里需要实现一方法来根据地址名称查询完整的地址代码
      // 可能需要调用后端API或使用缓存的地址数据
      try {
        // 示例：假设有一个API可以根据地址名称返回完整的地址代码
        // const response = await getAddressCodesByName(address);
        // this.form.locations[index].codes = response.data;

        // 暂时保持原样
        console.log(`需要为地址 ${address} 查询完整的地址代码`);
      } catch (error) {
        console.error('获取地址代码败:', error);
      }
    },
    /** 处理作业区域输入框的输入 */
    handleLocationInputConfirm() {
      if (this.locationInput.trim() !== '') {
        this.queryParams.location.push(this.locationInput.trim());
        this.locationInput = '';
      }
    },
    /** 处理作业区域输入框的关闭 */
    handleLocationClose(index) {
      this.queryParams.location.splice(index, 1);
    }
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
  min-height: calc(100vh - 84px); /* 减去头部和面包屑的高度 */
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

/* 保留原有样式 */
.detail-table {
  padding: 20px;
}

.detail-table .el-form-item {
  margin-bottom: 10px;
  margin-right: 20px;
}

.ellipsis {
  display: inline-block;
  width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
}

.el-descriptions {
  padding: 20px;
  margin-bottom: 20px;
}

.carousel-container {
  width: 100%;
  padding: 0 20px;
}

.carousel-title {
  margin: 10px 0;
  color: #606266;
}

.carousel-image-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f7fa;
}

.carousel-image-container .el-image {
  max-width: 100%;
  max-height: 100%;
}

/* 自定义轮播图指示器样式 */
.el-carousel ::v-deep .el-carousel__indicators {
  bottom: -20px;
}

.el-carousel ::v-deep .el-carousel__indicator {
  padding: 12px 4px;
}

.el-carousel ::v-deep .el-carousel__button {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

/* 自定义轮播图按钮样式 */
.el-carousel ::v-deep .el-carousel__arrow {
  background-color: rgba(31, 45, 61, 0.5);
  border-radius: 50%;
}
</style>

