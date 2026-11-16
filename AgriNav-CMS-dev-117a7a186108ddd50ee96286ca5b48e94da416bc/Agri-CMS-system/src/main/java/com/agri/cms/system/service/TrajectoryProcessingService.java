package com.agri.cms.system.service;

import com.agri.cms.system.domain.vo.Trajectory;
import com.influxdb.query.FluxTable;

import java.util.List;
import java.util.Map;

/**
 * @author 姚锦伦
 */
public interface TrajectoryProcessingService {
    /**
     * 处理轨迹数据
     *
     * @param result
     * @param baseTrajectory
     * @return Map<Long, Trajectory>
     * @author lt
     * @since 2024/10/11
     */
    Map<Long, Trajectory> processTrajectoryData(List<FluxTable> result, Trajectory baseTrajectory);

    /**
     * 通过位置查询地块id
     *
     * @param trajectoryList
     * @return List<Trajectory>
     * @author lt
     * @since 2024/10/11
     */
    List<Trajectory> queryLocationByPlotId(List<Trajectory> trajectoryList);
}
