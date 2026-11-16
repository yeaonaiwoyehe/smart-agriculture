package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.LandPlot;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.domain.vo.Trajectory;
import com.agri.cms.system.mapper.LandPlotMapper;
import com.agri.cms.system.service.TrajectoryProcessingService;
import com.influxdb.query.FluxTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 姚锦伦
 */
@Service
public class TrajectoryProcessingServiceImpl implements TrajectoryProcessingService {

    @Autowired
    private LandPlotMapper landPlotMapper;


    @Override
    public Map<Long, Trajectory> processTrajectoryData(List<FluxTable> result, Trajectory baseTrajectory) {
        //地块ID和轨迹对象映射
        Map<Long, Trajectory> trajectoryMap = new HashMap<>();

        result.forEach(table -> {
            table.getRecords().forEach(record -> {
                double latitude = record.getValueByKey("latitude") != null ? Double.parseDouble(String.valueOf(record.getValueByKey("latitude"))) : 0.0;
                double longitude = record.getValueByKey("longitude") != null ? Double.parseDouble(String.valueOf(record.getValueByKey("longitude"))) : 0.0;
                LocalDateTime deviceTime = LocalDateTime.parse(String.valueOf(record.getValueByKey("deviceTime")));

                Point point = new Point();
                point.setLatitude(latitude);
                point.setLongitude(longitude);
                point.setTime(deviceTime);

                long plotId = record.getValueByKey("plotId") != null ? Long.parseLong(String.valueOf(record.getValueByKey("plotId"))) : 0L;
                //没有该地块对应的轨迹对象，则创建一个新对象
                if (!trajectoryMap.containsKey(plotId)) {
                    Trajectory newTrajectory = new Trajectory();
                    //轨迹ID
                    newTrajectory.setTrajectoryInfoId(baseTrajectory.getTrajectoryInfoId());
                    newTrajectory.setWorkType(baseTrajectory.getWorkType());
                    newTrajectory.setCrop(baseTrajectory.getCrop());
                    newTrajectory.setPhoneNumber(baseTrajectory.getPhoneNumber());
                    newTrajectory.setStartTime(baseTrajectory.getStartTime());
                    newTrajectory.setEndTime(baseTrajectory.getEndTime());
                    //初始化一个点集合
                    newTrajectory.setPoints(new ArrayList<>());
                    newTrajectory.setPlotId(plotId);
                    trajectoryMap.put(plotId, newTrajectory);
                }
                //将轨迹点添加到轨迹对象中
                Trajectory currentTrajectory = trajectoryMap.get(plotId);
                currentTrajectory.getPoints().add(point);
            });
        });

        return trajectoryMap;
    }

    @Override
    public List<Trajectory> queryLocationByPlotId(List<Trajectory> trajectoryList) {
        Map<Long, String> plotIdLocationMap = new HashMap<>();
        //获取轨迹中包含的地块ID
        Set<Long> plotIds = trajectoryList.stream()
                .map(Trajectory::getPlotId)
                .collect(Collectors.toSet());

        if (plotIds.isEmpty()) {
            throw new RuntimeException("轨迹所属地块为空");
        }

        List<LandPlot> landPlots = landPlotMapper.selectBatchIds(plotIds);
        //地块ID和地址的映射
        landPlots.forEach(landPlot -> plotIdLocationMap.put(landPlot.getId(), landPlot.getAddress()));
        //给每个轨迹设置请求的地址
        trajectoryList.forEach(trajectory -> {
            trajectory.setLocation(plotIdLocationMap.get(trajectory.getPlotId()));
        });
        trajectoryList.forEach(trajectory -> {
            trajectory.getPoints().sort(Comparator.comparing(Point::getTime));
        });
        return trajectoryList;
    }
}