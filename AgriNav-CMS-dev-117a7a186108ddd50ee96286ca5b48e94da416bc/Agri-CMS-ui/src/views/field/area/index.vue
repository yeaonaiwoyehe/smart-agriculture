<template>
  <div class="app-container">
    <el-card>
      <div class="area-statistics">
        <!-- 加载动画 -->
        <div v-if="loading" class="loading-container">
          <el-image
            style="width: 200px; opacity: 0.6"
            :src="require('@/assets/images/3.gif')"
          />
          <p class="loading-text">数据加载中，请稍候...</p>
        </div>

        <template v-else>
          <!-- 工具栏 -->
          <div class="toolbar">
            <el-select v-model="sortType" placeholder="请选择排序方式" @change="handleSortChange">
              <el-option label="默认排序" value="default"/>
              <el-option label="面积升序" value="areaAsc"/>
              <el-option label="面积降序" value="areaDesc"/>
              <el-option label="名称升序" value="nameAsc"/>
              <el-option label="名称降序" value="nameDesc"/>
            </el-select>
          </div>

          <!-- 修改面包屑导航部分 -->
          <div class="breadcrumb">
            <el-breadcrumb separator="/">
              <!-- 添加"全国"固定选项 -->
              <el-breadcrumb-item
                @click.native="getProvinceArea()"
                :class="{ clickable: currentLevel !== 1 }"
              >
                全国
              </el-breadcrumb-item>
              <!-- 其他导航项保持不变 -->
              <el-breadcrumb-item
                v-for="(item, index) in addressPath"
                :key="item.code"
                @click.native="navigateToLevel(item, index)"
                :class="{ clickable: index !== addressPath.length - 1 }"
              >
                {{ item.name }}
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>

          <!-- 地区列表 -->
          <el-table :data="sortedAreaData" style="width: 100%">
            <el-table-column prop="name" label="地区名称"/>
            <el-table-column prop="area" label="总面积(亩)">
              <template #default="scope">
                <el-button
                  type="text"
                  @click="drillDown(scope.row)"
                  v-if="canDrillDown(scope.row.level)"
                >
                  {{ scope.row.area }}
                </el-button>
                <span v-else>{{ scope.row.area }}</span>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-card>
  </div>
</template>

<script>
import {getAreaStatistics} from '@/api/field/square'  // 更新引入名称

export default {
  name: 'AreaStatistics',
  data() {
    return {
      loading: false,
      sortType: 'default',
      areaData: [],
      addressPath: [], // 存储导航路径
      currentLevel: 1  // 当前级别: 1-省, 2-市, 3-区, 4-乡, 5-村
    }
  },
  computed: {
    sortedAreaData() {
      const data = Object.values(this.areaData)
      switch (this.sortType) {
        case 'areaAsc':
          return data.sort((a, b) => a.area - b.area)
        case 'areaDesc':
          return data.sort((a, b) => b.area - a.area)
        case 'nameAsc':
          return data.sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
        case 'nameDesc':
          return data.sort((a, b) => b.name.localeCompare(a.name, 'zh-CN'))
        default:
          return data
      }
    }
  },
  created() {
    this.getProvinceArea()
  },
  methods: {
    // 获取省级数据
    async getProvinceArea() {
      if (this.loading) return

      this.loading = true
      try {
        const res = await getAreaStatistics(0, 1) // 更新方法名
        this.areaData = res.data
        this.addressPath = [] // 清空导航路径
        this.currentLevel = 1
      } catch (error) {
        console.error('获取省份面积数据失败:', error)
        this.$message.error('获取数据失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },

    // 下钻查询
    async drillDown(address) {
      if (!this.canDrillDown(address.level)) return

      this.loading = true
      try {
        const res = await getAreaStatistics(address.code, address.level + 1) // 更新方法名
        this.areaData = res.data

        // 更新导航路径
        const pathIndex = this.addressPath.findIndex(item => item.code === address.code)
        if (pathIndex === -1) {
          this.addressPath.push({
            code: address.code,
            name: address.name,
            level: address.level
          })
        } else {
          this.addressPath = this.addressPath.slice(0, pathIndex + 1)
        }

        this.currentLevel = address.level + 1
      } catch (error) {
        console.error('获取下级面积数据失败:', error)
        this.$message.error('获取数据失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },

    // 导航到指定层级
    async navigateToLevel(address, index) {
      if (index === this.addressPath.length - 1) return

      this.loading = true
      try {
        const res = await getAreaStatistics(address.code, address.level + 1) // 更新方法名
        this.areaData = res.data
        this.addressPath = this.addressPath.slice(0, index + 1)
        this.currentLevel = address.level + 1
      } catch (error) {
        console.error('导航查询失败:', error)
        this.$message.error('获取数据失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },

    // 判断是否可以继续下钻
    canDrillDown(level) {
      return level < 5 // 村级(level=5)不可再下钻
    },

    handleSortChange() {
      // 排序变化时不需要额外操作，computed属性会自动处理
    }
  }
}
</script>

<style scoped>
.area-statistics {
  padding: 20px;
}

.toolbar {
  margin-bottom: 20px;
  text-align: right;
}

.toolbar :deep(.el-select) {
  width: 140px;
}

.breadcrumb {
  margin-bottom: 20px;
  min-height: 22px; /* 保持最小高度，避免闪烁 */
}

.clickable {
  cursor: pointer;
  color: #409EFF;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}

.loading-text {
  margin-top: 20px;
  color: #909399;
  font-size: 14px;
}

/* 当前级别项的样式 */
.el-breadcrumb__item:last-child .el-breadcrumb__inner {
  color: #606266;
  cursor: default;
}
</style>
