package com.agri.cms.ai.routing;

import com.agri.cms.ai.intent.IntentType;

/**
 * 问答路由类型（A-07）。
 *
 * 把用户问题按回答方式分成不同链路：
 * BUSINESS_DATA 走业务工具取数，KNOWLEDGE 走制度知识库（RAG），
 * CHITCHAT 直接对话，UNKNOWN 走兜底。
 *
 * @author agrinav
 */
public enum QueryRouteType {

    /** 业务数据问答：通过工具调用从业务系统取数或执行操作 */
    BUSINESS_DATA("BUSINESS_DATA", "业务数据问答"),

    /** 制度知识问答：从制度知识库检索（RAG，A-08 建成后接入） */
    KNOWLEDGE("KNOWLEDGE", "制度知识问答"),

    /** 闲聊问候：不涉及取数或检索，直接对话 */
    CHITCHAT("CHITCHAT", "闲聊"),

    /** 无法识别：走兜底回复 */
    UNKNOWN("UNKNOWN", "无法识别");

    private final String code;

    private final String displayName;

    QueryRouteType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * 把识别意图映射到路由类型。
     *
     * 常见问题、制度口径走知识库；闲聊与未知单独处理；
     * 其余意图默认按业务数据处理，保证新增业务意图不会误走知识库。
     */
    public static QueryRouteType fromIntent(IntentType intent) {
        if (intent == null) {
            return UNKNOWN;
        }
        return switch (intent) {
            case FAQ, POLICY_QA -> KNOWLEDGE;
            case CHITCHAT -> CHITCHAT;
            case UNKNOWN -> UNKNOWN;
            default -> BUSINESS_DATA;
        };
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}
