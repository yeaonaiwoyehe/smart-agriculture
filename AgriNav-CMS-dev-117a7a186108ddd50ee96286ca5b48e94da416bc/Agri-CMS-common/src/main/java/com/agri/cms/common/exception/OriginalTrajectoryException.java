package com.agri.cms.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 原始轨迹异常
 *
 * @author mfr
 * @since 2024/11/25
 */
public class OriginalTrajectoryException extends RuntimeException {
    private static final Logger logger = LoggerFactory.getLogger(OriginalTrajectoryException.class);

    public OriginalTrajectoryException(){
        super();
    }

    public OriginalTrajectoryException(String message){
        super(message);
        logger.error("OriginalPointException: {}", message);
    }

    public OriginalTrajectoryException(String message, Throwable cause){
        super(message, cause);
        logger.error("OriginalPointException: {}", message, cause);
    }

    public OriginalTrajectoryException(Throwable cause){
        super(cause);
        logger.error("OriginalPointException  occurred", cause);
    }
}
