package com.agri.cms.system.dao;

import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.domain.vo.Trajectory;
import com.influxdb.query.FluxTable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author: 姚锦伦
 * @CreateTime: 2024-09-25
 * @Description:
 * @Version: 1.0
 */
public interface EffectiveTrajectoryDao {
    List<FluxTable> queryByPhoneNumber(String phoneNumber, LocalDateTime startTime, LocalDateTime endTime);

    void deleteByPhoneNumber(String phoneNumber, LocalDateTime startTime, LocalDateTime endTime);

    List<Long> queryAllWorkingLandPlot();

    Map<String, List<Point>> queryWorkingTrajectoryByPhoneNumber(String s);
}
