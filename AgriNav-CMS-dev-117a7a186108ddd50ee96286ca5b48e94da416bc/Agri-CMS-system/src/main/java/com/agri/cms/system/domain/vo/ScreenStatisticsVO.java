package com.agri.cms.system.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author 李鸿麟
 */
public class ScreenStatisticsVO {
    private String workName;

    private BigDecimal totalArea;

    public String getWorkName() {
        return workName;
    }

    public void setWorkName(String workName) {
        this.workName = workName;
    }

    public BigDecimal getTotalArea() {
        return totalArea;
    }

    public void setTotalArea(BigDecimal totalArea) {
        this.totalArea = totalArea;
    }

    @Override
    public String toString() {
        return "ScreenStatisticsVO{" +
                "workName='" + workName + '\'' +
                ", totalArea=" + totalArea +
                '}';
    }
}
