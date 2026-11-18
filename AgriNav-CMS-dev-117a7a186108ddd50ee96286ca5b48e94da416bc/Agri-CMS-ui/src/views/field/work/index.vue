<template>
  <div class="app-container">
    <div class="content-wrapper">
      <div class="main-content">
        <!-- 搜索区域 -->
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" class="search-form">
          <el-form-item label="地址" prop="address">
            <el-input
              v-model="queryParams.address"
              placeholder="请输入地址"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <!-- 表格区域 -->
        <el-table v-loading="loading" :data="workingLandPlots">
          <el-table-column label="地块名称" align="center" prop="landName" />
          <el-table-column label="地块类型" align="center" prop="landType">
            <template slot-scope="scope">
              <el-tag type="primary">{{ scope.row.landType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="面积(亩)" align="center" prop="landArea">
            <template slot-scope="scope">
              {{ scope.row.landArea.toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="地块地址" align="center" prop="address" />
          <el-table-column label="所有者" align="center" prop="ownerName" />
          <el-table-column label="联系电话" align="center" prop="ownerPhone" />
          <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
          <el-table-column label="地块图片" align="center" width="100">
            <template slot-scope="scope">
              <el-image
                v-if="scope.row.image"
                :src="scope.row.image"
                :preview-src-list="[scope.row.image]"
                fit="cover"
                style="width: 50px; height: 50px"
              >
                <div slot="error" class="image-slot">
                  <i class="el-icon-picture-outline"></i>
                </div>
              </el-image>
              <i v-else class="el-icon-picture-outline" style="font-size: 20px; color: #909399"></i>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="120">
            <template slot-scope="scope">
              <el-button
                type="text"
                size="small"
                icon="el-icon-map-location"
                @click="viewTrajectory(scope.row)"
              >查看轨迹</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页区域 -->
        <pagination
          v-show="total > 0"
          :total="total"
          :page.sync="queryParams.pageNum"
          :limit.sync="queryParams.pageSize"
          @pagination="getList"
        />

        <!-- 地块边界对话框 -->
        <el-dialog
          title="地块边界"
          :visible.sync="boundaryDialogVisible"
          width="80%"
          @closed="handleDialogClosed"
        >
          <div id="boundaryMap" style="width: 100%; height: 500px;"></div>
        </el-dialog>
      </div>
    </div>
  </div>
</template>

<script>
import { queryWorkingLandPlot } from "@/api/field/work";
import { loadAMap } from '@/utils/amap';
import request from '@/utils/request';

export default {
  name: "WorkingLandPlot",
  components: {
    Pagination: () => import("@/components/Pagination")
  },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 工作地块列表
      workingLandPlots: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        address: undefined,
      },
      map: null,
      AMap: null,
      boundaryDialogVisible: false,
      polygon: null,
      excludePolygons: [],
      path: [], // 存储轨迹坐标
      marker: null,
      polyline: null,
      currentIndex: 0,
      animationInterval: null,
      animationSpeed: 0.01,
      isAnimationComplete: false,
      markerIcon: null,
      lastPosition: null,
      lastTime: 0,
      bluePolyline: null, // 蓝色完整轨迹
      greenPolyline: null, // 绿色动态轨迹
      excludeTimes: new Set(), // 存储需要排除的时间点
      trajectory: null, // 用存储轨迹数据
      trajectorySegments: [], // 存储完整的轨迹段信息
      greenSegments: [], // 存储绿色轨迹的段
      websocket: null,
      wsUrl: process.env.VUE_APP_BASE_API.replace('http', 'ws') + '/websocket/workingLandPlot',
      wsTimer: null,
      currentLandPlot: null,
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询工作地块列表 */
    getList() {
      this.loading = true;
      queryWorkingLandPlot(this.queryParams).then(response => {
        this.workingLandPlots = response.data;
        this.total = response.total;
        this.loading = false;
      }).catch(error => {
        console.error('获取工作地块列表失败:', error);
        this.$message.error('获取工作地块列表失败');
        this.loading = false;
      });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 查看轨迹按钮操作 */
    async viewTrajectory(row) {
      this.boundaryDialogVisible = true;
      this.currentLandPlot = row; // 保存当前地块信息
      await this.$nextTick();

      try {
        // 初始化地图（如果还没有初始化）
        if (!this.map) {
          const AMap = await loadAMap(['AMap.Polygon', 'AMap.Polyline', 'AMap.Marker']);

          this.AMap = AMap;
          this.map = new AMap.Map('boundaryMap', {
            zoom: 14,
            viewMode: '3D',
            pitch: 0,
            mapStyle: 'amap://styles/normal',
            features: ['bg', 'road', 'building']
          });
        }

        // 清除之前的图层
        this.clearMapLayers();

        // 绘制地块边界
        if (row.landType === '新增') {
          this.drawBoundary(row);
        } else if (row.landType === '插花地') {
          this.drawBoundaryWithExcludes(row);
        }

        // 初始化 WebSocket 连接
        this.initWebSocket();

      } catch (error) {
        console.error('初始化失败:', error);
        this.$message.error('初始化失败，请稍后重试');
      }
    },
    /** 绘制轨迹 */
    drawTrajectory() {
      if (!this.AMap || this.path.length === 0) {
        this.$message.warning('轨迹数据为空');
        return;
      }

      // 创建标记
      this.markerIcon = new this.AMap.Icon({
        size: new this.AMap.Size(20, 20),
        image: require('@/assets/images/trunk.png'),
        imageSize: new this.AMap.Size(20, 20)
      });

      // 设置初始位置为倒数第二个点
      const initialPosition = this.path[this.path.length - 2] || this.path[0];
      this.currentIndex = this.path.length - 2;

      this.marker = new this.AMap.Marker({
        position: initialPosition,
        icon: this.markerIcon,
        angle: 0,
        anchor: 'center',
        zIndex: 120
      });

      // 构建初始轨迹段
      const initialPath = this.path.slice(0, this.path.length - 1);
      const segments = this.buildTrajectorySegments(initialPath);
      this.drawGreenSegments(segments);

      this.map.add(this.marker);
      this.map.setFitView([...this.greenSegments, this.marker]);

      // 计算初始角度
      if (this.path.length >= 2) {
        const lastPoint = this.path[this.path.length - 2];
        const endPoint = this.path[this.path.length - 1];
        const initialAngle = this.calculateAngle(lastPoint, endPoint);
        this.marker.setAngle(initialAngle);
      }

      // 开始动画
      this.animateTrajectory();
    },
    /** 轨迹动画 */
    animateTrajectory() {
      const animate = (currentTime) => {
        if (!this.lastTime) this.lastTime = currentTime;
        const deltaTime = currentTime - this.lastTime;
        this.lastTime = currentTime;

        if (this.currentIndex < this.path.length - 1) {
          const lastIndex = this.currentIndex;
          this.currentIndex += this.animationSpeed * (deltaTime / 16.67);
          const currentPosition = this.interpolatePosition(this.currentIndex);

          // 计算运动方向的角度
          const nextIndex = Math.min(Math.ceil(this.currentIndex), this.path.length - 1);
          const angle = this.calculateAngle(
            this.interpolatePosition(lastIndex),
            this.interpolatePosition(nextIndex)
          );

          // 更新小车位置和角度
          this.marker.setPosition(currentPosition);
          this.marker.setAngle(angle);
          this.lastPosition = currentPosition;

          // 更新绿色轨迹
          const completedPath = this.path.slice(0, Math.floor(this.currentIndex) + 1);
          completedPath.push(currentPosition);

          // 清除现有的绿色轨迹段
          if (this.greenSegments.length) {
            this.map.remove(this.greenSegments);
          }

          // 重新构建并绘制轨迹段
          const segments = this.buildTrajectorySegments(completedPath);
          this.drawGreenSegments(segments);

          this.animationInterval = requestAnimationFrame(animate);
        } else {
          this.isAnimationComplete = true;
          this.animationInterval = requestAnimationFrame(animate);
        }
      };

      this.animationInterval = requestAnimationFrame(animate);
    },
    /** 位置插值计算 */
    interpolatePosition(index) {
      const floorIndex = Math.floor(index);
      const ceilIndex = Math.min(floorIndex + 1, this.path.length - 1);
      const fraction = index - floorIndex;

      const start = this.path[floorIndex];
      const end = this.path[ceilIndex];

      return [
        start[0] + (end[0] - start[0]) * fraction,
        start[1] + (end[1] - start[1]) * fraction
      ];
    },
    /** 计算角度 */
    calculateAngle(start, end) {
      const dx = end[0] - start[0];
      const dy = end[1] - start[1];
      let angle = Math.atan2(dy, dx) * 180 / Math.PI;

      // 将角度转换为与正北方向的夹角
      angle = 90 - angle;
      if (angle < 0) {
        angle += 360;
      }

      return angle;
    },
    /** 清除地图图层 */
    clearMapLayers() {
      if (this.animationInterval) {
        cancelAnimationFrame(this.animationInterval);
      }

      if (this.greenSegments.length) {
        this.map.remove(this.greenSegments);
        this.greenSegments = [];
      }

      if (this.marker) {
        this.map.remove(this.marker);
        this.marker = null;
      }

      if (this.polygon) {
        this.map.remove(this.polygon);
        this.polygon = null;
      }
    },
    /** 解析边界坐标 */
    parseBoundary(boundary) {
      const coordsString = boundary.match(/\(\((.*?)\)\)/)[1];
      return coordsString.split(',').map(pair => {
        // WKT 每对为“经度 纬度”，高德需要 [经度, 纬度]
        const [lng, lat] = pair.trim().split(' ');
        return [parseFloat(lng), parseFloat(lat)];
      });
    },
    /** 绘制地块边界 */
    drawBoundary(landPlot) {
      const coordinates = this.parseBoundary(landPlot.boundary);
      this.polygon = new this.AMap.Polygon({
        path: coordinates,
        strokeColor: "#237CBE",
        strokeWeight: 2,
        strokeOpacity: 0.8,
        fillColor: '#E0F0DD',
        fillOpacity: 0.8,
        zIndex: 50
      });

      this.map.add(this.polygon);
      this.map.setFitView([this.polygon]);
    },
    /** 绘制带无用区域的地块边界 */
    drawBoundaryWithExcludes(landPlot) {
      // 获取无用区域数据
      request({
        url: '/landPlotExclude/queryByLandPlotId',
        method: 'get',
        params: {
          landPlotId: landPlot.id
        }
      }).then(response => {
        if (response.code === 200) {
          const excludes = response.data;

          // 获得主地块的坐标
          const mainPath = this.parseBoundary(landPlot.boundary);
          // 获取所有无用区域的坐标
          const excludePaths = excludes.map(exclude => this.parseBoundary(exclude.boundary));

          // 创建包含镂空区域的多边形
          this.polygon = new this.AMap.Polygon({
            path: [mainPath, ...excludePaths], // 第一个路径是外部轮廓，后续的路径会形成镂空区域
            strokeColor: "#237CBE",
            strokeWeight: 2,
            strokeOpacity: 0.8,
            fillColor: '#E0F0DD',
            fillOpacity: 0.8,
            zIndex: 50
          });

          // 添加到地图并调整视野
          this.map.add(this.polygon);
          this.map.setFitView([this.polygon]);
        }
      }).catch(error => {
        console.error('获取无用区域数据失败:', error);
        this.$message.error('获取无用区域据失败');
      });
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams = {
        pageNum: 1,
        pageSize: 10,
        address: undefined,
      };
      this.handleQuery();
    },
    handleDialogClosed() {
      // 关闭 WebSocket 连接
      if (this.websocket) {
        this.websocket.close();
      }
      if (this.wsTimer) {
        clearInterval(this.wsTimer);
      }

      // 清理动画和图层
      if (this.animationInterval) {
        cancelAnimationFrame(this.animationInterval);
      }
      if (this.map) {
        this.clearMapLayers();
      }

      // 重置轨迹相关数据，但保留地图实例
      this.path = [];
      this.currentIndex = 0;
      this.lastPosition = null;
      this.lastTime = 0;
      this.isAnimationComplete = false;
      this.trajectory = null;
      this.currentLandPlot = null;
    },
    initWebSocket() {
      // 关闭已存在的连接
      if (this.websocket) {
        this.websocket.close();
      }
      if (this.wsTimer) {
        clearInterval(this.wsTimer);
      }

      // 创建 WebSocket 连接
      this.websocket = new WebSocket(this.wsUrl);

      this.websocket.onopen = () => {
        console.log('WebSocket连接已建立');
        // 连接建立后立即发送第一次请求
        this.sendTrajectoryRequest();
        // 设置定时器，每5秒发送一次请求
        this.wsTimer = setInterval(() => {
          this.sendTrajectoryRequest();
        }, 5000);
      };

      this.websocket.onmessage = (event) => {
        try {
          const response = JSON.parse(event.data);
          if (response.code === 200 && response.data && response.data.length > 0) {
            const newTrajectory = response.data[0];

            // 如果是第一次接收数据
            if (!this.trajectory) {
              this.trajectory = newTrajectory;
              this.path = this.trajectory.points.map(point => [point.longitude, point.latitude]);
              this.drawTrajectory();
            } else {
              // 更新轨迹数据
              const newPoints = newTrajectory.points;
              const oldPoints = this.trajectory.points;

              // 只添加新的点
              if (newPoints.length > oldPoints.length) {
                this.trajectory = newTrajectory;
                this.path = this.trajectory.points.map(point => [point.longitude, point.latitude]);

                // 如果动画已完成，则重新开始动画
                if (this.isAnimationComplete) {
                  this.continueAnimation();
                }
              }
            }
          }
        } catch (error) {
          console.error('处理WebSocket消息失败:', error);
        }
      };

      this.websocket.onerror = (error) => {
        console.error('WebSocket错误:', error);
        this.$message.error('WebSocket连接错误');
      };

      this.websocket.onclose = () => {
        console.log('WebSocket连接已关闭');
        if (this.wsTimer) {
          clearInterval(this.wsTimer);
        }
      };
    },

    sendTrajectoryRequest() {
      if (this.websocket && this.websocket.readyState === WebSocket.OPEN && this.currentLandPlot) {
        const request = {
          id: this.currentLandPlot.id,
          landType: this.currentLandPlot.landType
        };
        this.websocket.send(JSON.stringify(request));
      }
    },

    updateTrajectoryDisplay() {
      // 清除现有的轨迹显示
      if (this.greenSegments.length) {
        this.map.remove(this.greenSegments);
        this.greenSegments = [];
      }
      if (this.bluePolyline) {
        this.map.remove(this.bluePolyline);
      }
      if (this.marker) {
        this.map.remove(this.marker);
      }

      // 重新绘制轨迹
      this.drawTrajectory();
      // 开始动画
      this.animateTrajectory();
    },
    continueAnimation() {
      // 重置动画状态
      this.isAnimationComplete = false;

      // 保持当前位置
      const currentPosition = this.path[Math.floor(this.currentIndex)];

      // 清除现有的绿色轨迹
      if (this.greenSegments.length) {
        this.map.remove(this.greenSegments);
        this.greenSegments = [];
      }

      // 重新开始动画
      this.animateTrajectory();
    },
    buildTrajectorySegments(completedPath) {
      const segments = [];
      let currentSegment = {
        path: [],
        isDashed: false
      };

      // 辅助函数：检查点是否在排除区域内
      const isPointExcluded = (point, index) => {
        if (index >= this.trajectory.points.length) return false;
        const time = this.trajectory.points[index].time;
        return this.trajectory?.trajectoryExclude?.some(exclude =>
          exclude.pointsExclude?.some(p =>
            p.time === time && p.longitude === point[0] && p.latitude === point[1]
          )
        );
      };

      completedPath.forEach((point, index) => {
        const currentPointExcluded = isPointExcluded(point, index);
        const prevPointExcluded = index > 0 ? isPointExcluded(completedPath[index - 1], index - 1) : false;

        // 开始新的段落的条件：
        // 1. 第一个点
        // 2. 当前点和前一个点的排除状态不同，且不是边界连接点
        if (index === 0) {
          currentSegment.path.push(point);
          currentSegment.isDashed = currentPointExcluded;
        } else {
          // 处理排除区域的边界点
          if (currentPointExcluded !== prevPointExcluded) {
            // 结束当前段落
            currentSegment.path.push(point);
            segments.push({...currentSegment});

            // 开始新的段落
            currentSegment = {
              path: [point],
              isDashed: currentPointExcluded
            };
          } else {
            // 继续当前段落
            currentSegment.path.push(point);
          }
        }
      });

      // 添加最后一个段落
      if (currentSegment.path.length > 0) {
        segments.push(currentSegment);
      }

      return segments;
    },

    drawGreenSegments(segments) {
      this.greenSegments = segments.map(segment => new this.AMap.Polyline({
        path: segment.path,
        strokeColor: "#95DDA2",
        strokeWeight: 6,
        strokeOpacity: 0.8,
        lineJoin: 'round',
        lineCap: 'round',
        showDir: true,
        strokeStyle: segment.isDashed ? 'dashed' : 'solid',
        strokeDasharray: segment.isDashed ? [10, 5] : null,
        zIndex: 111
      }));

      if (this.greenSegments.length > 0) {
        this.map.add(this.greenSegments);
      }
    },
  },
  beforeDestroy() {
    if (this.websocket) {
      this.websocket.close();
    }
    if (this.wsTimer) {
      clearInterval(this.wsTimer);
    }
    if (this.animationInterval) {
      cancelAnimationFrame(this.animationInterval);
    }
    // 只在组件销毁时才销毁地图
    if (this.map) {
      this.map.destroy();
      this.map = null;
      this.AMap = null;
    }
  }
};
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
}

.content-wrapper {
  background-color: #fff;
  padding: 20px;
  border-radius: 4px;
}

.search-form {
  margin-bottom: 20px;
  padding: 18px;
  background-color: #f5f7fa;
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

#boundaryMap {
  width: 100%;
  height: 500px;
  background: #f5f5f5;
}

.el-dialog__body {
  padding: 10px 20px;
}
</style>
