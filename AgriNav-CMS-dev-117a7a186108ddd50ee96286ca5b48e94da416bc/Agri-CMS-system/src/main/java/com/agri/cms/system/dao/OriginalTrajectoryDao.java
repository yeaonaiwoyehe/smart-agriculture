package com.agri.cms.system.dao;

import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.domain.vo.SortMessage;
import com.agri.cms.system.domain.vo.Trajectory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 原始轨迹数据访问接口
 *
 * @author mfr
 * @since 2024/9/26
 */
public interface OriginalTrajectoryDao {

    List<Point> queryOriginalPoint(SortMessage sortMessage);

    List<Point> queryOriginalPointByPhoneAndTime(String phone, LocalDateTime startTime, LocalDateTime endTime);

    List<Point> queryAllPoints();

    boolean deleteByUserName(String phone);
    boolean updatePhoneNumber(String oldPhone, String newPhone);
}
