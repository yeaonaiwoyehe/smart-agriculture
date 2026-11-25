package com.agri.cms.ai.intent;

import java.util.List;

/**
 * 用户意图枚举（A-04）。
 *
 * 覆盖 App 端（农机手）与管理端（调度/管理人员）的可识别意图，
 * 每个意图声明执行该意图所必需的关键参数；缺失必填参数时由识别服务
 * 触发一次追问，而不是默认取最近一条记录。
 *
 * 意图编号会作为大模型输出 JSON 的标准取值，命名保持稳定，便于后续
 * 工具调用框架（A-05）按意图分发。
 *
 * @author agrinav
 */
public enum IntentType {

    // ===== App 端（农机手）=====
    QUERY_TODAY_TASKS("QUERY_TODAY_TASKS", "查询今日任务", "ag",
            List.of(), "查询今天分配给我的任务，如：今天有啥活"),
    CLAIM_TASK("CLAIM_TASK", "认领任务", "ag",
            List.of(IntentParams.TASK_ID), "认领或接下某个任务，如：这块地我去"),
    NAVIGATE("NAVIGATE", "导航到地块", "ag",
            List.of(IntentParams.TASK_ID), "导航前往某块地，如：这块地在哪"),
    START_WORK("START_WORK", "开始作业", "ag",
            List.of(IntentParams.WORK_ORDER_NO), "开始某作业单的作业，如：开始作业"),
    END_WORK("END_WORK", "结束作业", "ag",
            List.of(IntentParams.WORK_ORDER_NO), "结束某作业单的作业，如：结束作业"),
    QUERY_WORK_ORDER("QUERY_WORK_ORDER", "查询作业单/越界", "ag",
            List.of(IntentParams.WORK_ORDER_NO), "查询作业单状态或是否越界，如：超界了没"),
    QUERY_SETTLEMENT("QUERY_SETTLEMENT", "查询结算明细", "ag",
            List.of(), "查询本单或本月结算，如：这月做了多少、这单为啥少算"),
    REPORT_REVIEW("REPORT_REVIEW", "提交复核", "ag",
            List.of(IntentParams.WORK_ORDER_NO), "对面积或金额提交复核，如：我要复核"),
    REPORT_ALERT("REPORT_ALERT", "异常上报", "ag",
            List.of(IntentParams.DESCRIPTION), "上报设备或作业异常，如：机器陷车了"),
    FAQ("FAQ", "常见问题", "ag",
            List.of(), "结算方式、到账时间等常见问题，如：怎么结算、多久到账"),

    // ===== 管理端（调度 / 管理人员）=====
    DISPATCH_SUGGEST("DISPATCH_SUGGEST", "调度建议", "ma",
            List.of(IntentParams.TASK_ID), "获取派单建议，人工确认后才执行"),
    ANALYZE_ALERT("ANALYZE_ALERT", "异常研判", "ma",
            List.of(IntentParams.WORK_ORDER_NO), "分析解释某条作业异常"),
    POLICY_QA("POLICY_QA", "制度口径问答", "ma",
            List.of(), "制度、规则、流程口径问答"),

    // ===== 通用 =====
    CHITCHAT("CHITCHAT", "闲聊问候", "common",
            List.of(), "打招呼或无业务含义的闲聊"),
    UNKNOWN("UNKNOWN", "无法识别", "common",
            List.of(), "无法理解或不在职责范围内的输入");

    /** 意图编号，同时作为大模型 JSON 输出的标准值 */
    private final String code;

    /** 意图显示名称 */
    private final String displayName;

    /** 所属端：ag = App 端，ma = 管理端，common = 通用 */
    private final String side;

    /** 执行该意图必需的关键参数，全部满足才允许执行 */
    private final List<String> requiredParams;

    /** 意图说明与示例，用于拼装识别提示词 */
    private final String description;

    IntentType(String code, String displayName, String side,
               List<String> requiredParams, String description) {
        this.code = code;
        this.displayName = displayName;
        this.side = side;
        this.requiredParams = requiredParams;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSide() {
        return side;
    }

    public List<String> getRequiredParams() {
        return requiredParams;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 按编号（大小写不敏感）查找意图，找不到返回 UNKNOWN。
     */
    public static IntentType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return UNKNOWN;
        }
        String normalized = code.trim().toUpperCase();
        for (IntentType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
