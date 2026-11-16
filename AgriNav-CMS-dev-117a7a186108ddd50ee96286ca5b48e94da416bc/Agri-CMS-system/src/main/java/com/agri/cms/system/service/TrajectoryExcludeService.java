package com.agri.cms.system.service;

import com.agri.cms.system.domain.TrajectoryExclude;
import com.agri.cms.system.domain.TrajectoryInfo;
import com.agri.cms.system.domain.vo.Point;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author 姚锦伦
 */
public interface TrajectoryExcludeService extends IService<TrajectoryExclude> {
    void saveTrajectoryExclude(Map<Long, List<Point>> longListMap, UUID uuid);

    List<TrajectoryExclude> queryTrajectaryExcludes(TrajectoryInfo info);
}
