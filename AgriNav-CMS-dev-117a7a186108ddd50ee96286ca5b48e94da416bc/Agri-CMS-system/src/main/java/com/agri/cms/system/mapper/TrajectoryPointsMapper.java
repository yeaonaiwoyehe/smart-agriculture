package com.agri.cms.system.mapper;

import com.agri.cms.system.domain.TrajectoryPoints;
import com.agri.cms.system.domain.vo.Trajectory;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.postgis.Polygon;
import org.springframework.stereotype.Service;

/**
 * @Author: 姚锦伦
 * @CreateTime: 2024-09-26
 * @Description:
 * @Version: 1.0
 */
@DS("pg")
public interface TrajectoryPointsMapper extends BaseMapper<TrajectoryPoints> {

    @Select("SELECT ST_NPoints(points::geometry) FROM trajectory_points WHERE points = #{points}")
    int countPoints(Polygon points);
}
