package com.agri.cms.system.service;

import com.agri.cms.system.domain.TrajectoryPoints;
import com.agri.cms.system.domain.vo.SortMessage;
import com.agri.cms.system.domain.vo.Trajectory;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.UUID;

/**
 * @Author: 姚锦伦
 * @CreateTime: 2024-09-26
 * @Description:
 * @Version: 1.0
 */

public interface TrajectoryPointsService extends IService<TrajectoryPoints> {
    Boolean savePoints(Trajectory trajectory, UUID uuid);

    Page<Trajectory> queryAll(String username, Integer pageNum, Integer pageSize);

    Page<Trajectory> queryByConditions(SortMessage sortMessage, String phoneNumber, Integer pageNum, Integer pageSize);
}
