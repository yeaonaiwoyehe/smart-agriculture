package com.agri.cms.system.service.impl;

import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.constant.LandPlotConstants;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.dao.EffectiveTrajectoryDao;
import com.agri.cms.system.domain.LandPlot;
import com.agri.cms.system.domain.LandPlotExclude;
import com.agri.cms.system.domain.TrajectoryExclude;
import com.agri.cms.system.domain.dto.WorkingLandPlotDTO;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.domain.vo.Trajectory;
import com.agri.cms.system.service.LandPlotService;
import com.agri.cms.system.service.WorkingLandPlotService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * @author 姚锦伦
 */
@Service
public class WorkingLandPlotServiceImpl implements WorkingLandPlotService {
    @Resource
    private EffectiveTrajectoryDao effectiveTrajectoryDao;
    @Resource
    private LandPlotService landPlotService;
    @Resource
    private LandPlotExcludeServiceImpl landPlotExcludeService;
    @Resource
    private InfluxDBEffectiveTrajectoryServiceImpl influxDBEffectiveTrajectoryService;
    @Override
    public AjaxResult queryWorkingLandPlot(int pageNum, int pageSize, String address) {
        //查询所有influxdb中的有效轨迹所属地块id
        List<Long> plotIds = effectiveTrajectoryDao.queryAllWorkingLandPlot();
        //根据地块id分页查询所有插花地地块信息
        LambdaQueryWrapper<LandPlot> queryWrapper = new LambdaQueryWrapper<LandPlot>()
                .in(LandPlot::getId, plotIds).
                eq(LandPlot::getIsDeleted, Constants.NOT_DELETED).
                like(address != null, LandPlot::getAddress, address);
        Page<LandPlot> page = new Page<>(pageNum, pageSize);
        Page<LandPlot> result = landPlotService.page(page, queryWrapper);
        AjaxResult ajaxResult = AjaxResult.success();
        ajaxResult.put("data", result.getRecords());
        ajaxResult.put("total", result.getTotal());
        return ajaxResult;
    }

    @Override
    public AjaxResult queryWorkingTrajectoryOfLandPlot(WorkingLandPlotDTO landPlotId) {
        String landType = landPlotId.getLandType();
        //查询该地块中根据手机号分组的所有点集
        Map<String, List<Point>> pointsOfPhoneString = effectiveTrajectoryDao.queryWorkingTrajectoryByPhoneNumber(String.valueOf(landPlotId.getId()));
        //判断地块类型，新增或插花地
        if (landType.equals(LandPlotConstants.LAND_FOR_ARRANGING_FLOWERS)){
            //地块类型为插花地，查询所有点后判断属于插花地应排除区域的轨迹点集
            //先查询插花地关联的所有应该排除的小地块
            LambdaQueryWrapper<LandPlotExclude> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(LandPlotExclude::getLandPlotId, Long.parseLong(landPlotId.getId())).
                    eq(LandPlotExclude::getIsDeleted, Constants.NOT_DELETED);
            List<LandPlotExclude> landPlotExcludes = landPlotExcludeService.list(wrapper);
            
            // 创建轨迹列表
            List<Trajectory> trajectories = new ArrayList<>();
            
            // 遍历每个手机号的点集
            pointsOfPhoneString.forEach((phoneNumber, points) -> {
                // 为每个手机号创建一个轨迹对象
                Trajectory trajectory = new Trajectory();
                trajectory.setPhoneNumber(phoneNumber);
                trajectory.setPoints(points);
                
                // 如果有需要排除的地块，处理排除区域
                if (!landPlotExcludes.isEmpty()) {
                    // 使用influxDBEffectiveTrajectoryService的assignPointsToPlots方法判断点是否在排除区域内
                    Map<Long, List<Point>> excludedPointsMap = influxDBEffectiveTrajectoryService.assignPointsToPlots(points, landPlotExcludes);
                    
                    // 创建TrajectoryExclude列表
                    List<TrajectoryExclude> trajectoryExcludes = new ArrayList<>();
                    
                    // 为每个排除区域创建TrajectoryExclude对象
                    excludedPointsMap.forEach((excludePlotId, excludedPoints) -> {
                        if (!excludedPoints.isEmpty()) {
                            TrajectoryExclude exclude = new TrajectoryExclude();
                            exclude.setId(UUID.randomUUID().toString());
                            exclude.setPointsExclude(excludedPoints);
                            trajectoryExcludes.add(exclude);
                        }
                    });
                    
                    // 设置轨迹的排除区域
                    trajectory.setTrajectoryExclude(trajectoryExcludes);
                }
                
                trajectories.add(trajectory);
            });
            
            return AjaxResult.success(trajectories);
            
        } else if (landType.equals(LandPlotConstants.LAND_FOR_NEW)){
            //地块类型为新增，直接封装Trajectory并返回
            List<Trajectory> trajectories = new ArrayList<>();
            //用流遍历map拿到所有的key和value
            pointsOfPhoneString.forEach((key, value) -> {
                //封装Trajectory对象
                Trajectory trajectory = new Trajectory();
                trajectory.setPhoneNumber(key);
                trajectory.setPoints(value);
                trajectories.add(trajectory);
            });
            return AjaxResult.success(trajectories);
        }
        return null;
    }
}

