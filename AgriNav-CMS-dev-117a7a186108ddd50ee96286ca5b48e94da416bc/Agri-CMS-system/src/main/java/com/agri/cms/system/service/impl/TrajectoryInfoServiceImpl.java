package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.TrajectoryInfo;
import com.agri.cms.system.domain.vo.Trajectory;
import com.agri.cms.system.mapper.LandPlotMapper;
import com.agri.cms.system.mapper.TrajectoryInfoMapper;
import com.agri.cms.system.service.TrajectoryInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author 姚锦伦
 */
@Service
public class TrajectoryInfoServiceImpl extends ServiceImpl<TrajectoryInfoMapper, TrajectoryInfo> implements TrajectoryInfoService {
    @Autowired
    private TrajectoryInfoMapper trajectoryInfoMapper;
    @Autowired
    private LandPlotMapper landPlotMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveInfo(Trajectory trajectory, UUID uuid, String userId) {
        TrajectoryInfo trajectoryInfo = new TrajectoryInfo();
        trajectoryInfo.setTrajectoryInfoId(uuid.toString());
        trajectoryInfo.setPhoneNumber(trajectory.getPhoneNumber());
        trajectoryInfo.setWidth(trajectory.getWidth());
        trajectoryInfo.setWorkType(trajectory.getWorkType());
        trajectoryInfo.setStartTime(trajectory.getStartTime());
        trajectoryInfo.setEndTime(trajectory.getEndTime());
        trajectoryInfo.setCreatedBy(userId);
        trajectoryInfo.setCreatedTime(LocalDateTime.now());
        trajectoryInfo.setTrajectoryImg(trajectory.getTrajectoryImg());
        trajectoryInfo.setPlotId(trajectory.getPlotId());
        trajectoryInfo.setArea(trajectory.getArea());
        trajectoryInfoMapper.insert(trajectoryInfo);
    }
}
