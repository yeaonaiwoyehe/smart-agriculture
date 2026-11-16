package org.yzh.web.trajectory.dao;

import org.yzh.protocol.t808.T0200;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author 姚锦伦
 *  @since 2024/9/23
 */
public interface IEffectiveTrajectoryDao {

    boolean insert(T0200 t0200,List<Long> plots);

    boolean insertBatch(Map<T0200, List<String>> t0200ToPlotIdsMap);
}