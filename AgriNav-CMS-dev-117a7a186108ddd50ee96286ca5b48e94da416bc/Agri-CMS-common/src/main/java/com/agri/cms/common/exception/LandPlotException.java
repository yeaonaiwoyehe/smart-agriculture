package com.agri.cms.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 自定义异常类 LandPlotException
 * @author mfr
 * @since 2024/11/25 */
public class LandPlotException extends RuntimeException {
    private static final Logger logger = LoggerFactory.getLogger(LandPlotException.class);

    public LandPlotException() {
        super();
    }

    public LandPlotException(String message) {
        super(message);
        logger.error("LandPlotException: {}", message);
    }

    public LandPlotException(String message, Throwable cause) {
        super(message, cause);
        logger.error("LandPlotException: {}", message, cause);
    }

    public LandPlotException(Throwable cause) {
        super(cause);
        logger.error("LandPlotException occurred", cause);
    }
}