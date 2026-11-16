package com.agri.cms.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.system.domain.TrajectoryExclude;
import com.agri.cms.system.domain.TrajectoryInfo;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.mapper.TrajectoryExcludeMapper;
import com.agri.cms.system.service.TrajectoryExcludeService;
import com.agri.cms.system.utils.LineStringUtils;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.postgis.LineString;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author 姚锦伦
 */
@Service
public class TrajectoryExcludeServiceImpl extends ServiceImpl<TrajectoryExcludeMapper, TrajectoryExclude> implements TrajectoryExcludeService {
    @Override
    @DS("pg")
    public void saveTrajectoryExclude(Map<Long, List<Point>> longListMap, UUID uuid) {
        if (ObjectUtil.isEmpty(longListMap)){
            return;
        }

        List<TrajectoryExclude> trajectoryExcludes = new ArrayList<>();

        // 遍历Map
        longListMap.forEach((plotId, points) -> {
            if (CollectionUtil.isNotEmpty(points)) {
                TrajectoryExclude trajectoryExclude = new TrajectoryExclude();
                // 设置UUID
                trajectoryExclude.setId(UUID.randomUUID().toString());
                trajectoryExclude.setTrajectoryInfoId(uuid.toString());
                //如果点的个数等于1，再往points里面放一个重复的点(能够构造LineString正常)
                if (points.size() == 1) {
                    points.add(points.get(0));
                }
                // 构造LineString
                org.postgis.Point[] linePoints = points.stream()
                    .map(p -> {
                        org.postgis.Point point = new org.postgis.Point(
                            p.getLongitude(),
                            p.getLatitude()
                        );
                        point.setSrid(Constants.WGS84_SRID);
                        return point;
                    })
                    .toArray(org.postgis.Point[]::new);

                    LineString lineString = new LineString(linePoints);
                    lineString.setSrid(Constants.WGS84_SRID);
                    trajectoryExclude.setPoints(lineString);

                    // 构造时间字符串
                    String timeStr = points.stream()
                        .map(p -> p.getTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .collect(Collectors.joining(","));

                    trajectoryExclude.setTimes(timeStr);

                    trajectoryExcludes.add(trajectoryExclude);
            }
        });

        // 批量保存
        if (!trajectoryExcludes.isEmpty()) {
            this.saveBatch(trajectoryExcludes);
        }
    }

    @Override
    @DS("pg")
    public List<TrajectoryExclude> queryTrajectaryExcludes(TrajectoryInfo info) {

         LambdaQueryWrapper<TrajectoryExclude> excludeQueryWrapper = new LambdaQueryWrapper<>();
                excludeQueryWrapper.eq(TrajectoryExclude::getTrajectoryInfoId, info.getTrajectoryInfoId());
                List<TrajectoryExclude> processedTrajectoryExcludes = list(excludeQueryWrapper)
                    .stream()
                    .map(trajectoryExclude -> {
                        TrajectoryExclude newTrajectoryExclude = new TrajectoryExclude();
                        newTrajectoryExclude.setId(trajectoryExclude.getId());

                        // 使用 LineStringUtils 将 LineString 和时间字符串转换为带时间的 Point 列表
                        List<Point> pointsWithTime = LineStringUtils.lineStringToPointsWithTime(
                            trajectoryExclude.getPoints(),
                            trajectoryExclude.getTimes()
                        );

                        newTrajectoryExclude.setPointsExclude(pointsWithTime);

                        return newTrajectoryExclude;
                    })
                    .toList();
                return processedTrajectoryExcludes;
    }
}
