package com.agri.cms.system.service.impl;

import com.agri.cms.common.constant.OriginalTrajectoryConstants;
import com.agri.cms.common.exception.OriginalTrajectoryException;
import com.agri.cms.system.dao.OriginalTrajectoryDao;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.domain.vo.SortMessage;
import com.agri.cms.system.domain.vo.Trajectory;
import com.agri.cms.system.mapper.LandPlotMapper;
import com.agri.cms.system.service.IOriginalTrajectoryService;
import com.agri.cms.system.utils.PolygonUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;


/**
 * 原始轨迹服务实现类
 *
 * @author mfr
 * @since 2024/10/11
 */
@Service
public class OriginalTrajectoryServiceImpl implements IOriginalTrajectoryService {
    @Resource
    private OriginalTrajectoryDao originalTrajectoryDao;

    @Resource
    private LandPlotMapper landPlotMapper;

    /**
     * 查询原始轨迹
     *
     * @return Trajectory
     * @author mfr
     * @since 2024/9/27
     */
    @Override
    public List<Point> queryOriginalPoint(SortMessage sortMessage) {
        // 从ES查询轨迹点
        List<Point> points = originalTrajectoryDao.queryOriginalPoint(sortMessage);

        // 封装轨迹对象
        Trajectory trajectory = sortMessage.convertToTrajectory(sortMessage);
        trajectory.setPoints(points);

        // 根据地址筛选点集
        trajectory = filterByAddress(trajectory);
        points = trajectory.getPoints();


        return points;

    }


    /**
     * 根据手机号和时间查询轨迹点
     *
     * @return List<Point>
     * @author mfr
     * @since 2024/9/27
     */
    @Override
    public List<Point> queryOriginalPointByPhoneAndTime(String phone, LocalDateTime startTime, LocalDateTime endTime) {
        return originalTrajectoryDao.queryOriginalPointByPhoneAndTime(phone, startTime, endTime);

    }

    /**
     * 删除轨迹
     * @return boolean
     * @author mfr
     * @since 2024/11/12     */
    @Override
    public boolean deleteByUserName(String phone) {
        return originalTrajectoryDao.deleteByUserName(phone);
    }

    /**
     * 更改手机号
     * @return boolean
     * @author mfr
     * @since 2024/11/12     */
    @Override
    public boolean updatePhoneNumber(String oldPhone, String newPhone) {
        return originalTrajectoryDao.updatePhoneNumber(oldPhone, newPhone);
    }

    /**
     * 根据地址筛选轨迹点
     *
     * @return Trajectory
     * @author mfr
     * @since 2024/9/27
     */

    public Trajectory filterByAddress(Trajectory trajectory) {


        // 如果指定了村庄，则进行筛选
        if (trajectory.getLocation() != null && !trajectory.getLocation().isEmpty()) {
            // 先从pg中查询边界信息
            String wkt = landPlotMapper.queryBoundaryByLocation(trajectory.getLocation());
            if (wkt != null) {
                // 遍历trajectory的points使用PolygonUtils判断点是否在边界内
                List<Point> points = trajectory.getPoints();
                List<Point> realPoints = PolygonUtil.filterPointsInPolygon(wkt, points);

                // 按时间排序points
                realPoints.sort(Comparator.comparing(Point::getTime));
                trajectory.setPoints(realPoints);
            } else {
                throw new OriginalTrajectoryException(OriginalTrajectoryConstants.ORIGINAL_TRAJECTORY_QUERY_EXCEPTION+"未找到该村庄！");
            }


        }

        return trajectory;
    }


}
