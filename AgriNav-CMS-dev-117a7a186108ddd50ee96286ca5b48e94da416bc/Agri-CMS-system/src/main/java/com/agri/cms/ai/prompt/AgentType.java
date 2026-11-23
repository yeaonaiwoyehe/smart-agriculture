package com.agri.cms.ai.prompt;

/**
 * 智能体类型枚举（A-03）。
 *
 * 集中登记平台的全部智能体，按所属端分为两类：
 * AG：App 端智能体，面向机手（AG-01 ~ AG-05）；
 * MA：管理端智能体，面向调度与管理人员（MA-01 ~ MA-03）。
 *
 * 每个枚举值绑定该智能体系统提示词模板在 classpath 下的位置，
 * 新增智能体时在此登记即可，调用方无需关心具体文件路径。
 *
 * @author agrinav
 */
public enum AgentType {

    AG_01("AG-01", "任务助手", "ag", "prompts/ag/ag-01-task.txt"),
    AG_02("AG-02", "作业助手", "ag", "prompts/ag/ag-02-work.txt"),
    AG_03("AG-03", "结算助手", "ag", "prompts/ag/ag-03-settlement.txt"),
    AG_04("AG-04", "异常上报", "ag", "prompts/ag/ag-04-alert.txt"),
    AG_05("AG-05", "常见问题", "ag", "prompts/ag/ag-05-faq.txt"),

    MA_01("MA-01", "调度建议", "ma", "prompts/ma/ma-01-dispatch.txt"),
    MA_02("MA-02", "异常研判", "ma", "prompts/ma/ma-02-insight.txt"),
    MA_03("MA-03", "制度口径问答", "ma", "prompts/ma/ma-03-policy.txt");

    /** 智能体编号，如 AG-01 */
    private final String code;

    /** 智能体显示名称，如 任务助手 */
    private final String displayName;

    /** 所属端：ag = App 端，ma = 管理端 */
    private final String side;

    /** 系统提示词模板的 classpath 路径 */
    private final String templatePath;

    AgentType(String code, String displayName, String side, String templatePath) {
        this.code = code;
        this.displayName = displayName;
        this.side = side;
        this.templatePath = templatePath;
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

    public String getTemplatePath() {
        return templatePath;
    }

    /**
     * 按编号（如 AG-01、ma-03，大小写不敏感）查找智能体，找不到返回 null。
     */
    public static AgentType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toUpperCase();
        for (AgentType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
