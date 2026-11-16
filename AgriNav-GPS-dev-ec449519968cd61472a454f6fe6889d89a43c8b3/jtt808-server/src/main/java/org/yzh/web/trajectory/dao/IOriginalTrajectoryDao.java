package org.yzh.web.trajectory.dao;

import java.time.LocalDateTime;
import java.util.Map;

/**
 *
 * @author mfr
 * @since 2024/9/24 */
public interface IOriginalTrajectoryDao {
    boolean saveOriginalPoint(Map<String, Object> jsonMap);
}
