package org.yzh.web.trajectory.service.Impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.postgis.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.yzh.web.config.Constants;
import org.yzh.web.mapper.LandPlotMapper;
import org.yzh.web.model.entity.Boundary;
import org.yzh.web.model.entity.EffectivePoint;
import org.yzh.web.model.entity.LandPlot;
import org.yzh.web.trajectory.service.BoundaryService;
import org.yzh.web.trajectory.service.LandPlotService;
import java.util.ArrayList;
import java.util.List;
/**
 * @Author: 姚锦伦
 * @CreateTime: 2024-09-26
 * @Description:
 * @Version: 1.0
 */
@Service
public class CheckPathServiceImpl {
    @Autowired
    private LandPlotService landPlotService;
    @Autowired
    private BoundaryService boundaryService;
    @Autowired
    private LandPlotMapper landPlotMapper;
    public void initAllLandPlot(){
        List<Boundary> boundaries = new ArrayList<>();
        //查询所有地块
        List<LandPlot> landPlots = landPlotService.list();
        landPlots.forEach(landPlot -> {
            //获取POLYGON字符串
            String polygonStr = landPlot.getBoundary().toString();
            //获取地块id
            Long id = landPlot.getId();
            //将字符串转为List<long[]>
            List<long[]> landPlotLongList = parsePolygonLong(polygonStr);
            if (landPlotLongList.size() > 1){
                Boundary boundary = initField(landPlotLongList, id, false);
                boundaries.add(boundary);
            }
        });
        boundaryService.saveBatch(boundaries);
    }
    public List<long[]> parsePolygonLong(String polygonString) {
        List<long[]> coordinates = new ArrayList<>();
        String[] parts = polygonString.split("\\(\\(");
        if(parts.length <= 1) {
            return coordinates;
        } else {
            parts = parts[1].split("\\)\\)");
            parts = parts[0].split(",");
        }
        for(String part : parts){
            String[] pointStrings = part.split(" ");
            long[] point = new long[2];
            //在此对获得的数据的进行精度控制
            point[0] = (long)(Double.parseDouble(pointStrings[0]) * 1000000.0);
            point[1] = (long)(Double.parseDouble(pointStrings[1]) * 1000000.0);
            coordinates.add(point);
        }
        return coordinates;
    }
    public List<EffectivePoint> checkPoint(double testx, double testy){
        double originalX = testx;
        double originalY = testy;
        if(testx < 200)
        {
            testx = testx * 1000000.0;
            testy = testy * 1000000.0;
        }
        List<EffectivePoint> result = new ArrayList<>();
        LambdaQueryWrapper<Boundary> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(Boundary::getLeftDownLongitude, (long)testx)
                .ge(Boundary::getRightTopLongitude, (long)testx)
                .le(Boundary::getLeftDownLatitude, (long)testy)
                .ge(Boundary::getRightTopLatitude, (long)testy)
                .eq(Boundary::getStatus, Constants.notDeleted);
        List<Boundary> boundaries = boundaryService.list(wrapper);
        List<Long> plotIds = boundaries.stream().map(Boundary::getPlotId).toList();
        for(Long plotId : plotIds){
            var landPlot = landPlotMapper.selectOne(new LambdaQueryWrapper<LandPlot>()
                    .eq(LandPlot::getId, plotId)
            );
            if (ObjectUtils.isEmpty(landPlot)){
                continue;
            }
            Polygon boundary = landPlot.getBoundary();
            String str = boundary.toString();
            List<double[]> list = parsePolygonDouble(str);
            if(list.size() >= 3){
                int tmp = 0;
                for(int i = 0,j = list.size() - 1; i < list.size(); j = i++){
                    double vertxi = list.get(i)[0],vertyi = list.get(i)[1];
                    double vertxj = list.get(j)[0],vertyj = list.get(j)[1];
                    if(((vertyi > testy) != (vertyj > testy)) &&
                            (testx < (vertxj - vertxi) * (testy - vertyi) / (vertyj - vertyi) + vertxi)) {
                        tmp = tmp ^ 1;
                    }
                }
                if (tmp == 1) {
                    // 使用原始格式的坐标值（testx=经度，testy=纬度）
                    String formattedX = String.format("%.6f", originalX);
                    String formattedY = String.format("%.6f", originalY);
                    result.add(new EffectivePoint(formattedX, formattedY, String.valueOf(plotId)));
                }
            }
        }
        return result;
    }
    public  List<double[]> parsePolygonDouble(String polygonString) {
        List<double[]> coordinates = new ArrayList<>();
        String[] parts = polygonString.split("\\(\\(");
        if(parts.length <= 1)
        {
            return coordinates;
        }
        else
        {
            parts = parts[1].split("\\)\\)");
            parts = parts[0].split(",");
        }
        for(String part : parts){
            String[] pointStrings = part.split(" ");
            double[] point = new double[2];
            //在此对获得的数据的进行精度控制
            point[0] = Double.parseDouble(pointStrings[0]) * 1000000.0;
            point[1] = Double.parseDouble(pointStrings[1]) * 1000000.0;
            coordinates.add(point);
        }
        return coordinates;
    }
    public  List<int[]> parsePolygonInt(String polygonString) {
        List<int[]> coordinates = new ArrayList<>();
        String[] parts = polygonString.split("\\(\\(");
        if(parts.length <= 1)
        {
            /* 对于POINT 和 LINE 可保留也可不处理 */
            return coordinates;
        }
        else
        {
            parts = parts[1].split("\\)\\)");
            parts = parts[0].split(",");
        }
        for(String part : parts){
            String[] pointStrings = part.split(" ");
            int[] point = new int[2];
            //在此对获得的数据的进行精度控制
            point[0] = (int)(Double.parseDouble(pointStrings[0]) * 1000000.0);
            point[1] = (int)(Double.parseDouble(pointStrings[1]) * 1000000.0);
            coordinates.add(point);
        }
        return coordinates;
    }
    private Boundary initField(List<long[]> list, Long id, boolean flag){
        /* 初始化左下角和右上角 */
        long leftDownX = 180000000L;
        long leftDownY = 180000000L;
        long rightTopX = 0L;
        long rightTopY = 0L;
        /* 获得左下角和右上角范围 */
        for(long[] it : list){
            leftDownX = Math.min(leftDownX, it[0]);
            leftDownY = Math.min(leftDownY, it[1]);
            rightTopX = Math.max(rightTopX, it[0]);
            rightTopY = Math.max(rightTopY, it[1]);
        }
        /* 插入到数据库 */
        Boundary boundary = new Boundary(leftDownX, leftDownY, rightTopX, rightTopY, id);
        if(flag){
            //将田块的左上角和右下角插入到field_after_init表中
            boundaryService.save(boundary);
        }
        return boundary;
    }
}