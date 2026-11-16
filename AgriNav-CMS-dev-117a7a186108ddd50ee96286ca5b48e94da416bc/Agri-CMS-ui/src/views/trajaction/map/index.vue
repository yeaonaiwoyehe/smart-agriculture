<template>
    <div class="map-container">
      <div id="container" class="amap-container"></div>
      <div class="map-controls">
        <el-button class="control-btn" type="primary" plain @click="showOriginalTrajectory">
          <i class="el-icon-view"></i>
          <span>查看原始轨迹</span>
        </el-button>
        <el-button class="control-btn" type="success" plain @click="toggleSatellite">
          <i class="el-icon-picture-outline"></i>
          <span>{{ satelliteLayer ? '关闭卫星图' : '开启卫星图' }}</span>
        </el-button>
        <el-button class="control-btn" type="warning" plain @click="toggleRoadNet">
          <i class="el-icon-share"></i>
          <span>{{ roadNetLayer ? '关闭路网图' : '开启路网图' }}</span>
        </el-button>
        <el-button class="control-btn" type="primary" plain @click="replayTrajectory">
          <i class="el-icon-refresh"></i>
          <span>重播轨迹</span>
        </el-button>
      </div>
      <!-- 添加对话框组件 -->
      <el-dialog
        title="原始轨迹"
        :visible.sync="showOriginalDialog"
        width="80%"
        class="original-trajectory-dialog"
        :modal="false"
        :close-on-click-modal="false"
      >
        <vehicle-trajectory-original
          :trajectory-info-id="trajectoryInfoId"
          :phone-number="phoneNumber"
          :start-time="startTime"
          :end-time="endTime"
        />
      </el-dialog>
    </div>
  </template>

  <script>
  import AMapLoader from '@amap/amap-jsapi-loader'
  import VehicleTrajectoryOriginal from './index_ori.vue'
  import request from '@/utils/request'  // 确保导入了request

  export default {
    name: 'VehicleTrajectory',
    components: {
      VehicleTrajectoryOriginal
    },
    props: {
      trajectory: {
        type: Object,
        required: true
      },
      trajectoryInfoId: {
        type: [String, Number],
        required: true
      }
    },
    data() {
      return {
        map: null,
        path: [], // 存储车辆轨迹坐标
        polyline: null,
        marker: null,
        currentIndex: 0,
        animationInterval: null,
        animationSpeed: 0.01, // 减小动画速度，使动画更平滑
        isAnimationComplete: false,
        showOriginalDialog: false,
        phoneNumber: '',
        startTime: '',
        endTime: '',
        AMap: null,
        isFirstLoad: true, // 新增：用于标记是否是第一次加载
        isDialogOpened: false, // 新增：用于跟踪对话框是否打开
        landPlots: [],  // 用于存储地块数据
        markerIcon: null,
        lastPosition: null,
        satelliteLayer: null,
        roadNetLayer: null,
        completedPath: [], // 新增：用于存储已完成的路径
        completedPolyline: null, // 新增：用于存储已完成路径的polyline
        bluePolyline: null, // 蓝色的完整轨迹
        greenPolyline: null, // 绿色的动画轨迹
        lastTime: 0,
        excludeTimes: new Set(), // 新增：用于存储需要排除的时间点
      }
    },
    watch: {
      trajectory: {
        immediate: true,
        handler(newTrajectory) {
          if (newTrajectory && newTrajectory.points && this.isDialogOpened) {
            this.path = newTrajectory.points.map(point => [point.longitude, point.latitude]);
            this.$nextTick(() => {
              this.resetAndDrawTrajectory();
            });
          }
        }
      }
    },
    mounted() {
      this.initMap()
      this.isDialogOpened = true // 设置对话框为打开状态
    },
    methods: {
      initMap() {
        AMapLoader.load({
          key: '6702779c26dca9d62b2125cd5560da8b',
          version: '2.0',
          plugins: ['AMap.Polyline', 'AMap.Marker', 'AMap.Scale', 'AMap.ToolBar']
        }).then((AMap) => {
          this.AMap = AMap
          this.map = new AMap.Map('container', {
            zoom: 14,
            center: [118.784129, 32.04181],
            viewMode: '3D',
            mapStyle: 'amap://styles/macaron', // 使用马卡龙风格，看起来更加清新
            features: ['bg', 'road', 'building', 'point'],
            showIndoorMap: true,
            showBuildingBlock: true,
            expandZoomRange: true,
            zooms: [3, 20],
            layers: [
              new AMap.TileLayer(),
              new AMap.Buildings({
                zooms: [14, 20],
                zIndex: 10,
                heightFactor: 2
              })
            ]
          })

          this.map.addControl(new AMap.Scale())
          this.map.addControl(new AMap.ToolBar({
            position: 'RB',
            liteStyle: true
          }))

          this.resetAndDrawTrajectory()

          // 在 initMap 方法中，地图初始化后添加以下代码
          const zoomControl = {
            init: (map) => {
              const zoomInButton = document.createElement('button');
              zoomInButton.innerHTML = '+';
              zoomInButton.className = 'custom-zoom-button';
              zoomInButton.onclick = () => map.zoomIn();

              const zoomOutButton = document.createElement('button');
              zoomOutButton.innerHTML = '-';
              zoomOutButton.className = 'custom-zoom-button';
              zoomOutButton.onclick = () => map.zoomOut();

              const container = document.createElement('div');
              container.className = 'custom-zoom-control';
              container.appendChild(zoomInButton);
              container.appendChild(zoomOutButton);

              map.getContainer().appendChild(container);
            }
          };

          zoomControl.init(this.map);
        }).catch(e => {
          this.$message.error('高德地图脚本加载失败')
        })
      },
      resetAndDrawTrajectory() {
        if (!this.AMap) {
          return
        }
        if (this.animationInterval) {
          cancelAnimationFrame(this.animationInterval)
        }
        if (this.polyline) {
          this.map.remove(this.polyline)
        }
        if (this.marker) {
          this.map.remove(this.marker)
        }
        this.currentIndex = 0
        this.isAnimationComplete = false
        this.isFirstLoad = true // 重置为true

        if (this.trajectory && this.trajectory.points && this.trajectory.points.length > 0) {
          this.path = this.trajectory.points.map(point => [point.longitude, point.latitude]);
          this.drawTrajectory();
          this.fetchAndDrawLandPlots();  // 添加这行
        } else {
          this.map.setFitView()
        }
      },
      drawTrajectory() {
        if (!this.AMap || this.path.length === 0) {
          this.$message.warning('轨迹数据为空')
          return
        }

        // 创建标记
        this.markerIcon = new this.AMap.Icon({
          size: new this.AMap.Size(20, 20),
          image: require('@/assets/images/trunk.png'),
          imageSize: new this.AMap.Size(20, 20)
        });

        // 设置初始位置为第一个点
        const initialPosition = this.path[0];

        this.marker = new this.AMap.Marker({
          position: initialPosition,
          icon: this.markerIcon,
          angle: 0,
          anchor: 'center'
        });

        // 修改获取排除点的逻辑
        const excludeTimes = new Set();
        if (this.trajectory.trajectoryExclude) {
          this.trajectory.trajectoryExclude.forEach(exclude => {
            if (exclude.pointsExclude) {
              exclude.pointsExclude.forEach(point => {
                excludeTimes.add(point.time);
              });
            }
          });
        }

        // 修改分段逻辑
        const segments = [];
        let currentSegment = {
          path: [],
          isDashed: false
        };

        this.path.forEach((point, index) => {
          const time = this.trajectory.points[index].time;
          const isExcluded = excludeTimes.has(time);
          const prevTime = index > 0 ? this.trajectory.points[index - 1].time : null;
          const nextTime = index < this.path.length - 1 ? this.trajectory.points[index + 1].time : null;
          const isPrevExcluded = prevTime ? excludeTimes.has(prevTime) : false;
          const isNextExcluded = nextTime ? excludeTimes.has(nextTime) : false;

          // 当前点是排除点的开始（前一个点不是排除点）
          if (isExcluded && !isPrevExcluded) {
            if (currentSegment.path.length > 0) {
              segments.push(currentSegment);
            }
            // 添加一个实线段连接到排除点
            segments.push({
              path: [currentSegment.path[currentSegment.path.length - 1], point],
              isDashed: false
            });
            currentSegment = {
              path: [point],
              isDashed: true
            };
          }
          // 当前点是排除点的结束（下一个点不是��除点）
          else if (isExcluded && !isNextExcluded) {
            currentSegment.path.push(point);
            segments.push(currentSegment);
            currentSegment = {
              path: [point],
              isDashed: false
            };
          }
          // 其他情况
          else {
            if (currentSegment.path.length === 0 || currentSegment.isDashed === isExcluded) {
              currentSegment.path.push(point);
            } else {
              segments.push(currentSegment);
              currentSegment = {
                path: [point],
                isDashed: isExcluded
              };
            }
          }
        });

        // 添加最后一段
        if (currentSegment.path.length > 0) {
          segments.push(currentSegment);
        }

        // 绘制蓝色轨迹（完整轨迹）
        segments.forEach(segment => {
          const polyline = new this.AMap.Polyline({
            path: segment.path,
            strokeColor: "#5DA6CB",
            strokeWeight: 6,
            strokeOpacity: 0.8,
            lineJoin: 'round',
            lineCap: 'round',
            showDir: true,
            strokeStyle: segment.isDashed ? 'dashed' : 'solid',
            strokeDasharray: segment.isDashed ? [10, 5] : null
          });
          if (!this.bluePolyline) {
            this.bluePolyline = [];
          }
          this.bluePolyline.push(polyline);
          this.map.add(polyline);
        });

        // 创建绿色的动画轨迹（初始只有起点）
        this.greenPolyline = new this.AMap.Polyline({
          path: [this.path[0]],
          strokeColor: "#95DDA2",
          strokeWeight: 6,
          strokeOpacity: 0.8,
          lineJoin: 'round',
          lineCap: 'round',
          showDir: true
        });

        this.map.add([this.greenPolyline, this.marker]);

        // 只将地图中心设置到起始点，保持当前缩放级别
        this.map.setCenter(initialPosition);

        // 修改动画逻辑以支持虚线
        this.excludeTimes = excludeTimes; // 保存排除时间供动画使用
        this.animateTrajectory();
      },
      animateTrajectory() {
        let lastAngle = 0;
        const animate = (currentTime) => {
          if (!this.lastTime) this.lastTime = currentTime;
          const deltaTime = currentTime - this.lastTime;
          this.lastTime = currentTime;

          if (this.currentIndex < this.path.length - 1) {
            this.currentIndex += this.animationSpeed * (deltaTime / 16.67);
            const currentPosition = this.interpolatePosition(this.currentIndex);

            // 修改这部分：使用与蓝色轨迹完全相同的分段逻辑
            const completedPath = this.path.slice(0, Math.floor(this.currentIndex) + 1);
            completedPath.push(currentPosition);

            // 移除旧的绿色轨迹
            if (this.greenPolyline) {
              if (Array.isArray(this.greenPolyline)) {
                this.map.remove(this.greenPolyline);
              } else {
                this.map.remove([this.greenPolyline]);
              }
            }

            // 使用与蓝色轨迹完全相同的分段逻辑
            const segments = [];
            let currentSegment = {
              path: [],
              isDashed: false
            };

            completedPath.forEach((point, index) => {
              const time = this.trajectory.points[Math.min(index, this.trajectory.points.length - 1)].time;
              const isExcluded = this.excludeTimes.has(time);
              const prevTime = index > 0 ? this.trajectory.points[Math.min(index - 1, this.trajectory.points.length - 1)].time : null;
              const nextTime = index < completedPath.length - 1 ? this.trajectory.points[Math.min(index + 1, this.trajectory.points.length - 1)].time : null;
              const isPrevExcluded = prevTime ? this.excludeTimes.has(prevTime) : false;
              const isNextExcluded = nextTime ? this.excludeTimes.has(nextTime) : false;

              // 当前点是排除点的开始（前一个点不是排除点）
              if (isExcluded && !isPrevExcluded) {
                if (currentSegment.path.length > 0) {
                  segments.push(currentSegment);
                }
                segments.push({
                  path: [currentSegment.path[currentSegment.path.length - 1], point],
                  isDashed: false
                });
                currentSegment = {
                  path: [point],
                  isDashed: true
                };
              }
              // 当前点是排除点的结束（下一个点不是排除点）
              else if (isExcluded && !isNextExcluded) {
                currentSegment.path.push(point);
                segments.push(currentSegment);
                currentSegment = {
                  path: [point],
                  isDashed: false
                };
              }
              // 其他情况
              else {
                if (currentSegment.path.length === 0 || currentSegment.isDashed === isExcluded) {
                  currentSegment.path.push(point);
                } else {
                  segments.push(currentSegment);
                  currentSegment = {
                    path: [point],
                    isDashed: isExcluded
                  };
                }
              }
            });

            if (currentSegment.path.length > 0) {
              segments.push(currentSegment);
            }

            // 创建新的绿色轨迹
            this.greenPolyline = segments.map(segment => new this.AMap.Polyline({
              path: segment.path,
              strokeColor: "#95DDA2",
              strokeWeight: 6,
              strokeOpacity: 0.8,
              lineJoin: 'round',
              lineCap: 'round',
              showDir: true,
              strokeStyle: segment.isDashed ? 'dashed' : 'solid',
              strokeDasharray: segment.isDashed ? [10, 5] : null
            }));

            this.map.add(this.greenPolyline);

            if (this.lastPosition) {
              const newAngle = this.calculateAngle(this.lastPosition, currentPosition);
              const interpolatedAngle = this.interpolateAngle(lastAngle, newAngle, 0.1);
              this.marker.setAngle(interpolatedAngle);
              lastAngle = interpolatedAngle;
            }
            this.lastPosition = currentPosition;

            this.marker.setPosition(currentPosition);

            const bounds = this.map.getBounds();
            if (!bounds.contains(currentPosition)) {
              this.map.setCenter(currentPosition);
            }

            if (this.map.getZoom() < 14) {
              this.map.setZoom(14);
            }

            this.animationInterval = requestAnimationFrame(animate);
          } else {
            this.isAnimationComplete = true;
            cancelAnimationFrame(this.animationInterval);
          }
        }
        this.animationInterval = requestAnimationFrame(animate);
      },
      interpolatePosition(index) {
        const floorIndex = Math.floor(index)
        const ceilIndex = Math.min(floorIndex + 1, this.path.length - 1)
        const fraction = index - floorIndex

        const start = this.path[floorIndex]
        const end = this.path[ceilIndex]

        return [
          start[0] + (end[0] - start[0]) * fraction,
          start[1] + (end[1] - start[1]) * fraction
        ]
      },
      calculateAngle(start, end) {
        const dx = end[0] - start[0];
        const dy = end[1] - start[1];
        let angle = Math.atan2(dy, dx) * 180 / Math.PI;

        // 调整角度使其与地图坐标系统一致
        angle = 90 - angle;
        if (angle < 0) {
          angle += 360;
        }

        return angle;
      },
      interpolateAngle(startAngle, endAngle, factor) {
        const shortestAngle = ((((endAngle - startAngle) % 360) + 540) % 360) - 180;
        return startAngle + shortestAngle * factor;
      },
      showOriginalTrajectory() {
        // 设置��要的参数
        this.phoneNumber = this.trajectory.phoneNumber || '';
        this.startTime = this.trajectory.startTime || '';
        this.endTime = this.trajectory.endTime || '';

        // 检查是否所有必要的参数都已设���
        if (!this.phoneNumber || !this.startTime || !this.endTime) {
          this.$message.error('缺少必要的参数，无法显示原始轨迹');
          return;
        }

        // 显示对话框
        this.showOriginalDialog = true;
      },
      fetchAndDrawLandPlots() {
        if (this.path.length === 0) return;

        const centerIndex = Math.floor(this.path.length / 2);
        const centerPoint = this.path[centerIndex];

        // 显示加载状态
        const loading = this.$loading({
          lock: true,
          text: '加载地块数据中...',
          spinner: 'el-icon-loading'
        });

        request({
          url: '/landPlot/byCoordinate',
          method: 'get',
          params: {
            longitude: centerPoint[0],
            latitude: centerPoint[1],
            distance: 5000
          }
        })
          .then(response => {
            if (response.code === 200 && response.data) {
              this.landPlots = response.data;
              this.drawLandPlots();
            }
          })
          .catch(error => {
            console.error('获取地块数据失败:', error);
            this.$message.error('获取地块数据失败，请稍后重试');
          })
          .finally(() => {
            loading.close();
          });
      },
      drawLandPlots() {
        if (!this.landPlots.length) return;

        const loading = this.$loading({
          lock: true,
          text: '加载地块数据中...',
          spinner: 'el-icon-loading'
        });

        // 存储所有多边形和与轨迹相关的多边形
        const allPolygons = [];
        const relatedPolygons = [];

        request({
          url: '/landPlotExclude/batchQuery',
          method: 'post',
          data: this.landPlots.map(plot => plot.id.toString())
        })
          .then(response => {
            if (response.code !== 200) {
              throw new Error(response.msg || '获取插花地数据失败');
            }

            const excludeAreasMap = new Map(
              response.data.map(item => [
                item.landPlotId.toString(),
                item.landPlotExcludes || []
              ])
            );

            this.landPlots.forEach(plot => {
              const boundary = plot.boundary;
              if (boundary) {
                const coordinates = this.parseBoundary(boundary);
                const plotIdStr = plot.id.toString();
                const excludeAreas = excludeAreasMap.get(plotIdStr) || [];
                const excludePaths = excludeAreas.map(area => this.parseBoundary(area.boundary));

                // 创建地块多边形
                const plotPolygon = new this.AMap.Polygon({
                  path: [coordinates, ...excludePaths],
                  strokeColor: "#A9DAF3",
                  strokeWeight: 2,
                  strokeOpacity: 0.8,
                  fillColor: '#E0F0DD',
                  fillOpacity: 1,
                  zIndex: 11,
                  extData: { plotId: plotIdStr }
                });

                // 检查地块是否与轨迹相交
                const plotBounds = plotPolygon.getBounds();
                const isRelated = this.path.some(point => plotBounds.contains(point));

                // 如果地块与轨迹相关，添加到相关多边形数组
                if (isRelated) {
                  relatedPolygons.push(plotPolygon);
                }

                allPolygons.push(plotPolygon);

                // 添加插花地边框
                excludeAreas.forEach(excludePlot => {
                  if (excludePlot.boundary) {
                    const excludePolygon = new this.AMap.Polygon({
                      path: this.parseBoundary(excludePlot.boundary),
                      strokeColor: "#A9DAF3",
                      strokeWeight: 2,
                      strokeOpacity: 0.8,
                      fillOpacity: 0,
                      zIndex: 51,
                      extData: { excludeId: excludePlot.id.toString() }
                    });
                    allPolygons.push(excludePolygon);
                  }
                });
              }
            });

            // 添加所有多边形到地图
            this.map.add(allPolygons);

            // 调整视野
            if (relatedPolygons.length > 0) {
              // 如果有相关地块，聚焦到相关地块
              this.map.setFitView(relatedPolygons);
            } else {
              // 如果没有相关地块，聚焦到轨迹路径
              const pathPolygon = new this.AMap.Polygon({
                path: this.path
              });
              this.map.setFitView([pathPolygon]);
            }
          })
          .catch(error => {
            console.error('加载地块数据失败:', error);
            this.$message.error('加载地块数据失败，请稍后重试');
          })
          .finally(() => {
            loading.close();
          });
      },
      parseBoundary(boundary) {
        // 解析 POLYGON 字符串
        const coordsString = boundary.match(/\(\((.*?)\)\)/)[1];
        return coordsString.split(',').map(pair => {
          const [lat, lng] = pair.trim().split(' ');
          return [parseFloat(lng), parseFloat(lat)];
        });
      },
      toggleSatellite() {
        if (this.satelliteLayer) {
          this.map.remove(this.satelliteLayer);
          this.satelliteLayer = null;
        } else {
          this.satelliteLayer = new this.AMap.TileLayer.Satellite();
          this.map.add(this.satelliteLayer);
        }
      },
      toggleRoadNet() {
        if (this.roadNetLayer) {
          this.map.remove(this.roadNetLayer);
          this.roadNetLayer = null;
        } else {
          this.roadNetLayer = new this.AMap.TileLayer.RoadNet();
          this.map.add(this.roadNetLayer);
        }
      },
      replayTrajectory() {
        if (this.isAnimationComplete || this.currentIndex >= this.path.length - 1) {
          // 取消当前动画
          if (this.animationInterval) {
            cancelAnimationFrame(this.animationInterval);
          }

          // 移除旧的绿色轨迹
          if (this.greenPolyline) {
            if (Array.isArray(this.greenPolyline)) {
              this.map.remove(this.greenPolyline);
            } else {
              this.map.remove([this.greenPolyline]);
            }
          }

          // 重置状态
          this.currentIndex = 0;
          this.isAnimationComplete = false;
          this.lastPosition = null;
          this.lastTime = 0;

          // 重置标记位置到起点
          const initialPosition = this.path[0];
          this.marker.setPosition(initialPosition);
          this.marker.setAngle(0);

          // 创建新的绿色轨迹
          this.greenPolyline = new this.AMap.Polyline({
            path: [initialPosition],
            strokeColor: "#95DDA2",
            strokeWeight: 6,
            strokeOpacity: 0.8,
            lineJoin: 'round',
            lineCap: 'round',
            showDir: true
          });

          // 添加新的绿色轨迹到地图
          this.map.add(this.greenPolyline);

          // 重新开始动画
          this.animateTrajectory();
        }
      }
    },
    beforeDestroy() {
      if (this.animationInterval) {
        cancelAnimationFrame(this.animationInterval)
      }
      if (this.map) {
        this.map.destroy()
      }
      this.isDialogOpened = false // 设置对话框为关闭状态
    }
  }
  </script>

  <style scoped>
  .map-container {
    width: 100%;
    height: 600px;
    position: relative;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }

  .amap-container {
    width: 100%;
    height: 100%;
    z-index: 1000;
  }

  .custom-zoom-button {
    width: 30px;
    height: 30px;
    background-color: white;
    border: 1px solid #ccc;
    border-radius: 2px;
    font-size: 18px;
    font-weight: bold;
    color: #666;
    cursor: pointer;
    margin: 2px;
  }

  .custom-zoom-button:hover {
    background-color: #f0f0f0;
  }

  .map-controls {
    position: absolute;
    top: 20px;
    right: 20px;
    z-index: 1000;
    display: flex;
    flex-direction: column;
    gap: 10px;
    width: 170px;
  }

  .control-btn {
    width: 100%;
    height: 40px;
    border-radius: 20px;
    font-size: 14px;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s ease;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }

  .control-btn:first-child {
    margin-left: auto;
    width: 97%; /* 第一个按钮稍微窄一些,实现右移效果 */
  }

  .control-btn i {
    margin-right: 5px;
  }

  /* 移除之前可能影响对齐的样式 */
  .control-btn span {
    display: inline;
  }
  </style>

