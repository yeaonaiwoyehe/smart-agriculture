package com.agri.cms.system.domain.vo;

/**
 * @author 李鸿麟
 * 大屏百分比VO类
 */
public class ScreenPercentVO {
    private double percent;

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        this.percent = percent;
    }

    @Override
    public String toString() {
        return "ScreenPercentVO{" +
                "percent='" + percent + '\'' +
                '}';
    }
}
