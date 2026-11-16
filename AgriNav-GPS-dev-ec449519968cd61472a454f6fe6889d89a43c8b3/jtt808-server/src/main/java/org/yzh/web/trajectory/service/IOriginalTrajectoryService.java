package org.yzh.web.trajectory.service;

import org.yzh.protocol.t808.T0200;

import java.util.List;

/**
 *
 * @author mfr
 * @since 2024/9/24 */
public interface IOriginalTrajectoryService {
    public boolean saveOriginalPoints(List<T0200> t0200List);
}
