package com.agri.cms.system.dao;

import com.agri.cms.system.domain.EffectivePointBuffer;
import com.agri.cms.system.domain.vo.Point;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 有效作业点数据访问（方案 B：已改为 PostgreSQL 暂存表 effective_point_buffer）
 *
 * @Author: 姚锦伦
 * @CreateTime: 2024-09-25
 */
public interface EffectiveTrajectoryDao {

    /**
     * 查询某手机号在时间范围内的全部暂存点
     */
    List<EffectivePointBuffer> queryByPhoneNumber(String phoneNumber, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 删除某手机号在时间范围内已处理的暂存点
     */
    void deleteByPhoneNumber(String phoneNumber, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询所有存在暂存点的地块 ID
     */
    List<Long> queryAllWorkingLandPlot();

    /**
     * 查询某地块内按手机号分组的点集
     */
    Map<String, List<Point>> queryWorkingTrajectoryByPhoneNumber(String plotId);
}
