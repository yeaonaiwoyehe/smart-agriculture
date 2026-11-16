package com.agri.cms.system.service;

import com.agri.cms.system.domain.TrajectoryInfo;
import com.agri.cms.system.domain.vo.Trajectory;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.UUID;

/**
 * @author 姚锦伦
 */
@DS("pg")
public interface TrajectoryInfoService extends IService<TrajectoryInfo> {
    /**
     * 保存轨迹信息
     *
     * @param trajectory
     * @param uuid
     * @author 姚锦伦
     * @since 2024/10/11
     */
    void saveInfo(Trajectory trajectory, UUID uuid, String userId);
}
