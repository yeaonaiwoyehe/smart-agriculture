package org.yzh.web.trajectory.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.yzh.protocol.commons.transform.AttributeKey;
import org.yzh.protocol.t808.T0200;
import org.yzh.web.mapper.EffectivePointBufferMapper;
import org.yzh.web.model.entity.EffectivePointBuffer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 有效轨迹点数据访问实现（方案 B：去 InfluxDB，直接写入 PostgreSQL 暂存表）
 */
@Repository
public class EffectiveTrajectoryDaoImpl implements IEffectiveTrajectoryDao {

    @Autowired
    private EffectivePointBufferMapper effectivePointBufferMapper;

    /**
     * 单个位置点写入（每个所属地块一条记录）
     */
    @Override
    public boolean insert(T0200 t0200, List<Long> plotIds) {
        if (plotIds == null || plotIds.isEmpty()) {
            return true;
        }
        t0200.transform();
        List<EffectivePointBuffer> rows = new ArrayList<>(plotIds.size());
        for (Long plotId : plotIds) {
            rows.add(toEntity(t0200, String.valueOf(plotId)));
        }
        effectivePointBufferMapper.insert(rows.get(0));
        for (int i = 1; i < rows.size(); i++) {
            effectivePointBufferMapper.insert(rows.get(i));
        }
        return true;
    }

    /**
     * 批量位置点写入
     */
    @Override
    public boolean insertBatch(Map<T0200, List<String>> t0200ToPlotIdsMap) {
        List<EffectivePointBuffer> rows = new ArrayList<>();
        for (Map.Entry<T0200, List<String>> entry : t0200ToPlotIdsMap.entrySet()) {
            T0200 t0200 = entry.getKey();
            List<String> plotIds = entry.getValue();
            t0200.transform();
            for (String plotId : plotIds) {
                rows.add(toEntity(t0200, plotId));
            }
        }
        for (EffectivePointBuffer row : rows) {
            effectivePointBufferMapper.insert(row);
        }
        return true;
    }

    /**
     * 把 T0200 与所属地块组装成暂存记录
     */
    private EffectivePointBuffer toEntity(T0200 t0200, String plotId) {
        Map<Integer, Object> attributes = t0200.getAttributes();
        int workType = -1;
        int width = -1;
        if (attributes != null) {
            Object wt = attributes.get(AttributeKey.WorkType);
            if (wt != null) {
                workType = ((Number) wt).intValue();
            }
            Object wd = attributes.get(AttributeKey.Width);
            if (wd != null) {
                width = ((Number) wd).intValue();
            }
        }

        EffectivePointBuffer entity = new EffectivePointBuffer();
        entity.setPhoneNumber(t0200.getClientId());
        entity.setPlotId(Long.parseLong(plotId));
        entity.setWorkType(workType);
        entity.setWidth(width);
        entity.setLongitude(t0200.getLng());
        entity.setLatitude(t0200.getLat());
        entity.setDeviceTime(t0200.getDeviceTime());
        entity.setCreateTime(LocalDateTime.now());
        return entity;
    }
}
