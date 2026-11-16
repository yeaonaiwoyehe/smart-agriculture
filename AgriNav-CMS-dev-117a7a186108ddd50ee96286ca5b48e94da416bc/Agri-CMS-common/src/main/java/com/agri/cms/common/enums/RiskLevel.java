package com.agri.cms.common.enums;


/**
 * @author 李鸿麟
 * 风险等级枚举
 */
public enum RiskLevel {
    /**
     * 高危
     */
    HIGH("高危"),
    /**
     * 中危
     */
    MEDIUM("中危"),
    /**
     * 低危
     */
    LOW("低危");

    private final String description;

    RiskLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }


}
