package com.agri.cms.common.enums;

/**
 * @author 李鸿麟
 * 用户活跃度枚举
 */
public enum ActivityLevel {
    /**
     * 高活跃用户
     */
    HIGH("高活跃"),

    /**
     * 中等活跃用户
     */
    MEDIUM("中等活跃"),

    /**
     * 低活跃用户
     */
    LOW("低活跃");

    private final String description;

    ActivityLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
