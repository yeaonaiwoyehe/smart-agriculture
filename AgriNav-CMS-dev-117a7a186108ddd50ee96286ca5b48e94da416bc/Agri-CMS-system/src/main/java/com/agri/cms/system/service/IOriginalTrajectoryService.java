package com.agri.cms.system.service;

import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.domain.vo.SortMessage;
import com.agri.cms.system.domain.vo.Trajectory;
import com.baomidou.dynamic.datasource.annotation.DS;

import java.time.LocalDateTime;
import java.util.List;

@DS("pg")
public interface IOriginalTrajectoryService {


    List<Point> queryOriginalPoint(SortMessage sortMessage);

    List<Point> queryOriginalPointByPhoneAndTime(String phone, LocalDateTime startTime, LocalDateTime endTime);

    boolean deleteByUserName(String phone);

    boolean updatePhoneNumber(String oldPhone, String newPhone);
}
