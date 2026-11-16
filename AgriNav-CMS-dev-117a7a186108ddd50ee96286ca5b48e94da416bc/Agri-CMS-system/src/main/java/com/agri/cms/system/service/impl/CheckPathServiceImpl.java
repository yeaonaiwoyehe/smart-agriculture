package com.agri.cms.system.service.impl;

import com.agri.cms.common.constant.Constants;
import com.agri.cms.system.domain.Boundary;
import com.agri.cms.system.domain.EffectivePoint;
import com.agri.cms.system.domain.LandPlot;
import com.agri.cms.system.mapper.LandPlotMapper;
import com.agri.cms.system.service.BoundaryService;
import com.agri.cms.system.service.LandPlotService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.postgis.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: 姚锦伦
 * @CreateTime: 2024-09-26
 * @Description:
 * @Version: 1.0
 */
@Service
@DS("pg")
public class CheckPathServiceImpl {
    @Autowired
    @Lazy
    private LandPlotService landPlotService;
    @Autowired
    private BoundaryService boundaryService;
    @Autowired
    private LandPlotMapper landPlotMapper;
    private static final int LOW_PRECISION_DEGREE = 200;

    public void initAllLandPlot() {
        List<Boundary> boundaries = new ArrayList<>();
        //查询所有未删除的地块
        LambdaQueryWrapper<LandPlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LandPlot::getIsDeleted, Constants.NOT_DELETED);
        List<LandPlot> landPlots = landPlotService.list();
        landPlots.forEach(landPlot -> {
            //获取POLYGON字符串
            String polygonStr = landPlot.getBoundary().toString();
            //获取地块id
            Long id = landPlot.getId();
            //将字符串转为List<long[]>
            List<long[]> landPlotLongList = parsePolygonLong(polygonStr);
            if (landPlotLongList.size() > 1) {
                Boundary boundary = initField(landPlotLongList, id, false);
                boundary.setStatus(Constants.NOT_DELETED);
                boundaries.add(boundary);
            }
        });
        boundaryService.saveBatch(boundaries);
    }

    public void initSingleLandPlot(Long landPlotId) {
        LandPlot landPlot = landPlotService.getById(landPlotId);
        if (landPlot != null) {
            String polygonStr = landPlot.getBoundary().toString();
            List<long[]> landPlotLongList = parsePolygonLong(polygonStr);
            if (landPlotLongList.size() > 1) {
                Boundary boundary = initField(landPlotLongList, landPlotId, false);
                //设置状态为未删除
                boundary.setStatus(Constants.NOT_DELETED);
                boundaryService.save(boundary);
            }
        }
    }

    public List<long[]> parsePolygonLong(String polygonString) {
        List<long[]> coordinates = new ArrayList<>();
        String[] parts = polygonString.split("\\(\\(");
        if (parts.length <= 1) {
            return coordinates;
        } else {
            parts = parts[1].split("\\)\\)");
            parts = parts[0].split(",");
        }

        for (String part : parts) {
            String[] pointStrings = part.split(" ");
            long[] point = new long[2];
            //在此对获得的数据的进行精度控制
            point[0] = (long) (Double.parseDouble(pointStrings[0]) * 1000000.0);
            point[1] = (long) (Double.parseDouble(pointStrings[1]) * 1000000.0);
            coordinates.add(point);
        }
        return coordinates;
    }

    public List<EffectivePoint> checkPoint(double testx, double testy) {
        double originalX = testx;
        double originalY = testy;
        if (testx < LOW_PRECISION_DEGREE) {
            testx = testx * 1000000.0;
            testy = testy * 1000000.0;
        }
        List<EffectivePoint> result = new ArrayList<>();
        LambdaQueryWrapper<Boundary> wrapper = new LambdaQueryWrapper<>();
        //未删除的边界
        wrapper.eq(Boundary::getStatus, Constants.NOT_DELETED);
        wrapper.le(Boundary::getLeftDownLongitude, (int) testx)
                .ge(Boundary::getRightTopLongitude, (int) testx)
                .le(Boundary::getLeftDownLatitude, (int) testy)
                .ge(Boundary::getRightTopLatitude, (int) testy);
        List<Boundary> boundaries = boundaryService.list(wrapper);

        List<Long> plotIds = boundaries.stream().map(Boundary::getPlotId).toList();
        for (Long plotId : plotIds) {
            LandPlot landPlot = landPlotMapper.selectById(plotId);
            Polygon boundary = landPlot.getBoundary();
            String str = boundary.toString();
            List<double[]> list = parsePolygonDouble(str);
            if (list.size() >= 3) {
                int tmp = 0;
                for (int i = 0, j = list.size() - 1; i < list.size(); j = i++) {
                    double vertxi = list.get(i)[0], vertyi = list.get(i)[1];
                    double vertxj = list.get(j)[0], vertyj = list.get(j)[1];
                    if (((vertyi > testy) != (vertyj > testy)) &&
                            (testx < (vertxj - vertxi) * (testy - vertyi) / (vertyj - vertyi) + vertxi)) {
                        tmp = tmp ^ 1;
                    }
                }
                if (tmp == 1) {
                    // 使用原始格式的坐标值
                    String formattedX = String.format("%.6f", originalX);
                    String formattedY = String.format("%.6f", originalY);
                    result.add(new EffectivePoint(formattedY, formattedX, String.valueOf(plotId)));
                }
            }
        }
        return result;
    }
    public int rayMethod(List<double[]> list, double testx, double testy){
        int tmp = 0;
        if (list.size() >= 3) {
                for (int i = 0, j = list.size() - 1; i < list.size(); j = i++) {
                    double vertxi = list.get(i)[0], vertyi = list.get(i)[1];
                    double vertxj = list.get(j)[0], vertyj = list.get(j)[1];
                    if (((vertyi > testy) != (vertyj > testy)) &&
                            (testx < (vertxj - vertxi) * (testy - vertyi) / (vertyj - vertyi) + vertxi)) {
                        tmp = tmp ^ 1;
                    }
                }
        }
        return tmp;
    }
    public List<double[]> parsePolygonDouble(String polygonString) {
        List<double[]> coordinates = new ArrayList<>();
        String[] parts = polygonString.split("\\(\\(");
        if (parts.length <= 1) {
            return coordinates;
        } else {
            parts = parts[1].split("\\)\\)");
            parts = parts[0].split(",");
        }


        for (String part : parts) {
            String[] pointStrings = part.split(" ");
            double[] point = new double[2];
            //在此对获得的数据的进行精度控制
            point[0] = Double.parseDouble(pointStrings[0]) * 1000000.0;
            point[1] = Double.parseDouble(pointStrings[1]) * 1000000.0;
            coordinates.add(point);
        }
        return coordinates;
    }

    public List<int[]> parsePolygonInt(String polygonString) {
        List<int[]> coordinates = new ArrayList<>();
        String[] parts = polygonString.split("\\(\\(");
        if (parts.length <= 1) {
            /* 对于POINT 和 LINE 可保留也可不处理 */
            return coordinates;
        } else {
            parts = parts[1].split("\\)\\)");
            parts = parts[0].split(",");
        }


        for (String part : parts) {
            String[] pointStrings = part.split(" ");
            int[] point = new int[2];
            //在此对获得的数据的进行精度控制
            point[0] = (int) (Double.parseDouble(pointStrings[0]) * 1000000.0);
            point[1] = (int) (Double.parseDouble(pointStrings[1]) * 1000000.0);
            coordinates.add(point);
        }
        return coordinates;
    }

    public Boundary initField(List<long[]> list, Long id, boolean flag) {
        /* 初始化左下角和右上角 */
        long leftDownX = 180000000L;
        long leftDownY = 180000000L;
        long rightTopX = 0L;
        long rightTopY = 0L;

        /* 获得左下角和右上角范围 */
        for (long[] it : list) {
            leftDownX = Math.min(leftDownX, it[0]);
            leftDownY = Math.min(leftDownY, it[1]);
            rightTopX = Math.max(rightTopX, it[0]);
            rightTopY = Math.max(rightTopY, it[1]);
        }
        /* 插入到数据库 */
        Boundary boundary = new Boundary(leftDownX, leftDownY, rightTopX, rightTopY, id);
        if (flag) {
            //将田块的左上角和右下角插入到field_after_init表中
            boundaryService.save(boundary);
        }
        return boundary;
    }

    public void deleteBoundary(Long longId) {
        LambdaUpdateWrapper<Boundary> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Boundary::getPlotId, longId);
        wrapper.set(Boundary::getStatus, Constants.DELETED);
        boundaryService.update(wrapper);
    }
}
