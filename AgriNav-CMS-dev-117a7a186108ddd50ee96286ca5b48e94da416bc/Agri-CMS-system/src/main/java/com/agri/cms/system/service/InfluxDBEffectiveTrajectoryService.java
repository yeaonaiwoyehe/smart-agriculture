package com.agri.cms.system.service;

import com.agri.cms.system.domain.vo.Trajectory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author 姚锦伦
 */
public interface InfluxDBEffectiveTrajectoryService {
    /**
     * 查询该用户的轨迹
     *
     * @param phoneNumber
     * @param startTime
     * @param endTime
     * @return List<Trajectory>
     * @author 姚锦伦
     * @since 2024/10/10
     */
    List<Trajectory> queryByPhoneNumber(String phoneNumber, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 删除轨迹
     *
     * @param phoneNumber
     * @param startTime
     * @param endTime
     * @author 姚锦伦
     * @since 2024/10/10
     */
    void deleteByPhoneNumber(String phoneNumber, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 生成轨迹
     *
     * @param phoneNumber
     * @param startTime
     * @param endTime
     * @author 姚锦伦
     * @since 2024/10/10
     */
    void createTrajectory(String phoneNumber, LocalDateTime startTime, LocalDateTime endTime);

    void loginOut(String username);
}
