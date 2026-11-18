<template>
    <div class="map-container" v-if="isMapVisible">
      <div id="map-container-ori" class="amap-container"></div>
      <div class="map-controls">
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
    </div>
  </template>

  <script>
  import { loadAMap } from '@/utils/amap'
  import request from '@/utils/request'

  export default {
    name: 'VehicleTrajectoryOriginal',
    props: {
      trajectoryInfoId: {
        type: [String, Number],
        required: true
      },
      phoneNumber: {
        type: String,
        required: true
      },
      startTime: {
        type: String,
        required: true
      },
      endTime: {
        type: String,
        required: true
      }
    },
    data() {
      return {
        map: null,
        path: [],
        AMap: null,
        isMapVisible: true,
        isMapInitialized: false,
        satelliteLayer: null,
        roadNetLayer: null,
        bluePolyline: null,
        greenPolyline: null,
        marker: null,
        markerIcon: null,
        currentIndex: 0,
        animationInterval: null,
        animationSpeed: 0.01,
        isAnimationComplete: false,
        lastPosition: null,
        lastTime: 0,
        landPlots: [],
        animationFrame: null,
        lastAngle: 0
      }
    },
    mounted() {
      this.initMap()
    },
    beforeDestroy() {
      if (this.map) {
        this.map.destroy()
        this.map = null
      }
    },
    methods: {
      initMap() {
        loadAMap(['AMap.Polyline', 'AMap.Marker', 'AMap.Scale', 'AMap.ToolBar']).then((AMap) => {
          this.AMap = AMap
          this.map = new AMap.Map('map-container-ori', {
            zoom: 14,
            center: [118.784129, 32.04181],
            viewMode: '3D',
            mapStyle: 'amap://styles/macaron',
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

          this.fetchOriginalTrajectory()
          this.map.on('complete', () => {
            this.isMapInitialized = true
            this.resetAndDrawTrajectory()
          })
        }).catch(e => {
          console.error('Failed to load AMap API:', e)
        })
      },
      fetchOriginalTrajectory() {
        const params = {
          phone: this.phoneNumber,
          startTime: this.startTime,
          endTime: this.endTime
        }
        request({
          url: '/originalTrajectory/queryOriginalTrajectoryByPhoneAndTime',
          method: 'get',
          params: params
        }).then(response => {
          if (response.code === 200 && response.data) {
            this.path = response.data.map(point => [point.longitude, point.latitude])
            console.log('轨迹数据:', this.path) // 添加日志
            this.$nextTick(() => {
              this.resetAndDrawTrajectory()
              this.fetchAndDrawLandPlots()
            })
          } else {
            this.$message.error('获取轨迹数据失败')
          }
        }).catch(error => {
          console.error('获取轨迹数据失败', error)
          this.$message.error('获取轨迹数据失败')
        })
      },
      resetAndDrawTrajectory() {
        if (!this.AMap || !this.isMapInitialized) {
          return
        }
        this.clearMap()
        this.currentIndex = 0
        this.isAnimationComplete = false
        this.lastPosition = null
        this.lastTime = 0

        if (this.path.length > 0) {
          this.drawTrajectory()
          this.fetchAndDrawLandPlots()
        } else {
          this.map.setFitView()
        }
      },
      clearMap() {
        if (this.animationInterval) {
          cancelAnimationFrame(this.animationInterval)
        }
        if (this.bluePolyline) {
          this.map.remove(this.bluePolyline)
        }
        if (this.greenPolyline) {
          this.map.remove(this.greenPolyline)
        }
        if (this.marker) {
          this.map.remove(this.marker)
        }
      },
      drawTrajectory() {
        if (!this.AMap || this.path.length === 0) {
          this.$message.warning('轨迹数据为空')
          return
        }

        // 创建标记和轨迹线
        this.markerIcon = new this.AMap.Icon({
          size: new this.AMap.Size(20, 20),
          image: require('@/assets/images/trunk.png'),
          imageSize: new this.AMap.Size(20, 20)
        })

        const initialPosition = this.path[0];
        this.marker = new this.AMap.Marker({
          position: initialPosition,
          icon: this.markerIcon,
          angle: 0,
          anchor: 'center'
        })

        this.bluePolyline = new this.AMap.Polyline({
          path: this.path,
          strokeColor: "#5DA6CB",
          strokeWeight: 6,
          strokeOpacity: 0.8,
          lineJoin: 'round',
          lineCap: 'round',
          showDir: true
        })

        this.greenPolyline = new this.AMap.Polyline({
          path: [this.path[0]],
          strokeColor: "#95DDA2",
          strokeWeight: 6,
          strokeOpacity: 0.8,
          lineJoin: 'round',
          lineCap: 'round',
          showDir: true
        })

        this.map.add([this.bluePolyline, this.greenPolyline, this.marker])

        // 只将地图中心设置到起始点，保持当前缩放级别
        this.map.setCenter(initialPosition)

        // 确保最小缩放级别为14
        if (this.map.getZoom() < 14) {
          this.map.setZoom(14)
        }

        this.animateTrajectory()
      },
      animateTrajectory() {
        const animate = (currentTime) => {
          if (!this.lastTime) this.lastTime = currentTime;
          const deltaTime = currentTime - this.lastTime;
          this.lastTime = currentTime;

          if (this.currentIndex < this.path.length - 1) {
            this.currentIndex += this.animationSpeed * (deltaTime / 16.67);
            const currentPosition = this.interpolatePosition(this.currentIndex);

            const completedPath = this.path.slice(0, Math.floor(this.currentIndex) + 1);
            completedPath.push(currentPosition);
            this.greenPolyline.setPath(completedPath);

            if (this.lastPosition) {
              const newAngle = this.calculateAngle(this.lastPosition, currentPosition);
              // 使用插值函数平滑旋转
              const interpolatedAngle = this.interpolateAngle(this.lastAngle, newAngle, 0.1);
              this.marker.setAngle(interpolatedAngle);
              this.lastAngle = interpolatedAngle;
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

            this.animationFrame = requestAnimationFrame(animate);
          } else {
            console.log('动画完成') // 添加日志
            this.isAnimationComplete = true;
            cancelAnimationFrame(this.animationFrame);
          }
        };
        this.animationFrame = requestAnimationFrame(animate);
      },
      interpolatePosition(index) {
        const floorIndex = Math.floor(index);
        const ceilIndex = Math.min(floorIndex + 1, this.path.length - 1);
        const fraction = index - floorIndex;

        const start = this.path[floorIndex];
        const end = this.path[ceilIndex];

        const interpolated = [
          start[0] + (end[0] - start[0]) * fraction,
          start[1] + (end[1] - start[1]) * fraction
        ];

        console.log('插值计算:', {floorIndex, ceilIndex, fraction, start, end, interpolated}) // 添加日志

        return interpolated;
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
        console.log('重播轨迹') // 添加日志
        this.currentIndex = 0;
        this.isAnimationComplete = false;
        this.lastPosition = null;
        if (this.path.length > 0) {
          this.greenPolyline.setPath([this.path[0]]);
          this.marker.setPosition(this.path[0]);
          this.animateTrajectory();
        } else {
          console.log('轨迹数据为空，无法重播') // 添加日志
        }
      },
      fetchAndDrawLandPlots() {
        if (this.path.length === 0) return;

        const centerIndex = Math.floor(this.path.length / 2);
        const centerPoint = this.path[centerIndex];

        request({
          url: '/landPlot/byCoordinate',
          method: 'get',
          params: {
            longitude: centerPoint[0],
            latitude: centerPoint[1],
            distance: 5000
          }
        }).then(response => {
          if (response.code === 200 && response.data) {
            this.landPlots = response.data;
            this.drawLandPlots();
          }
        }).catch(error => {
          console.error('获取地块数据失败', error);
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
                  strokeColor: "#237CBE",
                  strokeWeight: 2,
                  strokeOpacity: 0.8,
                  fillColor: '#E0F0DD',
                  fillOpacity: 0.8,
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
        const coordsString = boundary.match(/\(\((.*?)\)\)/)[1];
        return coordsString.split(',').map(pair => {
          // WKT 每对为“经度 纬度”，高德需要 [经度, 纬度]
          const [lng, lat] = pair.trim().split(' ');
          return [parseFloat(lng), parseFloat(lat)];
        });
      },
      showMap() {
        this.isMapVisible = true;
        this.$nextTick(() => {
          this.initMap();
        });
      },
      hideMap() {
        this.isMapVisible = false;
        if (this.map) {
          this.map.destroy();
          this.map = null;
        }
      }
    }
  }
  </script>

  <style scoped>
  .map-container {
    width: 100%;
    height: 100%;
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
    width: 97%;
  }

  .control-btn i {
    margin-right: 5px;
  }

  .control-btn span {
    display: inline;
  }
  </style>
