package com.agri.cms.system.dao.Impl;

import com.agri.cms.system.dao.EffectiveTrajectoryDao;
import com.agri.cms.system.domain.EffectivePointBuffer;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.mapper.EffectivePointBufferMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 有效作业点数据访问实现（方案 B：PostgreSQL 暂存表 effective_point_buffer）
 *
 * @Author: 姚锦伦
 * @CreateTime: 2024-09-25
 */
@Repository
public class EffectiveTrajectoryDaoImpl implements EffectiveTrajectoryDao {

    @Autowired
    private EffectivePointBufferMapper bufferMapper;

    @Override
    public List<EffectivePointBuffer> queryByPhoneNumber(String phoneNumber, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<EffectivePointBuffer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EffectivePointBuffer::getPhoneNumber, phoneNumber)
                .ge(EffectivePointBuffer::getDeviceTime, startTime)
                .le(EffectivePointBuffer::getDeviceTime, endTime)
                .orderByAsc(EffectivePointBuffer::getDeviceTime);
        return bufferMapper.selectList(wrapper);
    }

    @Override
    public void deleteByPhoneNumber(String phoneNumber, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<EffectivePointBuffer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EffectivePointBuffer::getPhoneNumber, phoneNumber)
                .ge(EffectivePointBuffer::getDeviceTime, startTime)
                .le(EffectivePointBuffer::getDeviceTime, endTime);
        bufferMapper.delete(wrapper);
    }

    @Override
    public List<Long> queryAllWorkingLandPlot() {
        List<EffectivePointBuffer> all = bufferMapper.selectList(
                new LambdaQueryWrapper<EffectivePointBuffer>()
                        .select(EffectivePointBuffer::getPlotId));
        return all.stream()
                .map(EffectivePointBuffer::getPlotId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<Point>> queryWorkingTrajectoryByPhoneNumber(String plotId) {
        LambdaQueryWrapper<EffectivePointBuffer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EffectivePointBuffer::getPlotId, Long.parseLong(plotId))
                .orderByAsc(EffectivePointBuffer::getDeviceTime);
        List<EffectivePointBuffer> rows = bufferMapper.selectList(wrapper);

        Map<String, List<Point>> trajectoryMap = new HashMap<>();
        for (EffectivePointBuffer row : rows) {
            Point point = new Point(row.getLongitude(), row.getLatitude(), row.getDeviceTime());
            trajectoryMap.computeIfAbsent(row.getPhoneNumber(), k -> new ArrayList<>()).add(point);
        }
        return trajectoryMap;
    }
}
