package org.yzh.web.trajectory.service;

import org.yzh.protocol.t808.T0200;

import java.util.List;

/**
 * @author 姚锦伦
 *  @since 2024/9/23
 */
public interface IEffectiveTrajectoryService {
    boolean checkEffectPointAndInsertBatch(List<T0200> t0200List);
    boolean insertBatch(List<T0200> t0200List);
    boolean insert(T0200 t0200);
}