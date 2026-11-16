package org.yzh.web.trajectory.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yzh.protocol.t808.T0200;
import org.yzh.web.model.entity.EffectivePoint;
import org.yzh.web.service.RpcService;
import org.yzh.web.trajectory.service.Impl.CheckPathServiceImpl;
import org.yzh.web.trajectory.dao.IEffectiveTrajectoryDao;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 姚锦伦
 *  @since 2024/9/23
 */
@Service
public class EffectiveTrajectoryServiceImpl implements IEffectiveTrajectoryService{
    @Autowired
    private IEffectiveTrajectoryDao effectiveTrajectoryDao;
    @Autowired
    private CheckPathServiceImpl checkPathService;
    @Override
    public boolean checkEffectPointAndInsertBatch(List<T0200> t0200List) {
        return true;
    }

    @Override
    public boolean insertBatch(List<T0200> t0200List) {
        if (t0200List == null || t0200List.isEmpty()) {
            //废弃
            return false;
        }
        //每个点和所对应的地块id的映射
        Map<T0200, List<String>> t0200ToPlotIdsMap = new HashMap<>();
        t0200List.forEach(t0200 -> {
             //拿到原始经纬度
            t0200.transform();
            //判断该点所属哪个地块
            List<EffectivePoint> effectivePoints = checkPathService.checkPoint(t0200.getLat(), t0200.getLng());
            List<String> plotIds = t0200ToPlotIdsMap.get(t0200);
            if (plotIds == null){
                plotIds = effectivePoints.stream()
                        .map(EffectivePoint::getPlotId)
                        .distinct()
                        .collect(Collectors.toList());
                t0200ToPlotIdsMap.put(t0200, plotIds);
            }
        });
        //打印map信息
        t0200ToPlotIdsMap.forEach((t0200, plotIds) -> {
            plotIds.forEach(System.out::println);
        });
        return effectiveTrajectoryDao.insertBatch(t0200ToPlotIdsMap);
    }
    @Override
    public boolean insert(T0200 t0200) {
        List<Long> plotIds = new ArrayList<>();
        t0200.transform();
        List<EffectivePoint> effectivePoints = checkPathService.checkPoint(t0200.getLat(), t0200.getLng());
        effectivePoints.forEach(point -> {
            if (point.getPlotId() != null){
                plotIds.add(Long.parseLong(point.getPlotId()));
            }
        });
        return effectiveTrajectoryDao.insert(t0200,plotIds);
    }
}