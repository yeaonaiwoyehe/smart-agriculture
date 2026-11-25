package com.agri.cms.ai.routing;

import com.agri.cms.ai.knowledge.KnowledgeAnswer;
import com.agri.cms.ai.tool.ToolConversation;

import java.util.List;

/**
 * 问答路由的统一返回结果（A-07，A-10 扩展会话与工具信息）。
 *
 * 在回答之外显式标注路由类型与来源，并透传业务写操作的确认令牌与回执，
 * 让前端能够区分"业务取数"与"制度知识"并展示来源。
 *
 * @param routeType      路由类型 code
 * @param routeName      路由类型显示名
 * @param status         处理状态：REPLY / NEED_CONFIRM / NEED_PARAMS / FAILED
 * @param answer         回答文本
 * @param source         来源标注
 * @param confirmToken   写操作确认令牌，可空
 * @param receiptNo      回执编号，可空
 * @param toolName       实际命中的工具名，可空（A-10 会话留存用）
 * @param costMs         本轮处理耗时（毫秒），可空（A-10）
 * @param conversationId 所属会话 ID，可空（A-10，前端据此续接对话）
 * @author agrinav
 */
public record RoutedAnswer(
        String routeType,
        String routeName,
        String status,
        String answer,
        String source,
        String confirmToken,
        String receiptNo,
        String toolName,
        Long costMs,
        Long conversationId
) {

    /**
     * 从业务工具调用结果构造路由回答，并按状态生成数据来源标注。
     */
    public static RoutedAnswer fromTool(ToolConversation conv) {
        String source = buildDataSource(conv);
        return new RoutedAnswer(
                QueryRouteType.BUSINESS_DATA.getCode(),
                QueryRouteType.BUSINESS_DATA.getDisplayName(),
                conv.status(),
                conv.reply(),
                source,
                conv.confirmToken(),
                conv.receiptNo(),
                conv.toolName(),
                null,
                null);
    }

    /**
     * 从制度知识问答结果构造路由回答，并标注知识来源。
     */
    public static RoutedAnswer knowledge(KnowledgeAnswer knowledge) {
        String source;
        if (knowledge.retrieved()) {
            List<String> docs = knowledge.sources();
            String docText = (docs == null || docs.isEmpty()) ? "相关文档" : String.join("、", docs);
            source = "知识来源：制度知识库（文档：" + docText + "）";
        } else {
            source = "知识来源：制度知识库（建设中，当前为占位回答）";
        }
        return new RoutedAnswer(
                QueryRouteType.KNOWLEDGE.getCode(),
                QueryRouteType.KNOWLEDGE.getDisplayName(),
                "REPLY",
                knowledge.content(),
                source,
                null,
                null,
                null,
                null,
                null);
    }

    /**
     * 闲聊回答，不涉及取数或检索。
     */
    public static RoutedAnswer chitchat(String answer) {
        return new RoutedAnswer(
                QueryRouteType.CHITCHAT.getCode(),
                QueryRouteType.CHITCHAT.getDisplayName(),
                "REPLY",
                answer,
                "来源：—（闲聊，未取数）",
                null,
                null,
                null,
                null,
                null);
    }

    /**
     * 无法识别时的兜底回答。
     */
    public static RoutedAnswer unknown(String answer) {
        return new RoutedAnswer(
                QueryRouteType.UNKNOWN.getCode(),
                QueryRouteType.UNKNOWN.getDisplayName(),
                "REPLY",
                answer,
                "来源：—",
                null,
                null,
                null,
                null,
                null);
    }

    /**
     * 附上所属会话 ID 与本轮耗时，返回新的结果对象（record 不可变）。
     */
    public RoutedAnswer withConversation(Long conversationId, Long costMs) {
        return new RoutedAnswer(
                routeType, routeName, status, answer, source,
                confirmToken, receiptNo, toolName, costMs, conversationId);
    }

    private static String buildDataSource(ToolConversation conv) {
        return switch (conv.status()) {
            case "REPLY" -> conv.toolName() != null
                    ? "数据来源：业务系统（工具：" + conv.toolName() + "）"
                    : "数据来源：智能助手（未调用业务工具）";
            case "NEED_CONFIRM" -> "数据来源：业务系统（待确认写操作：" + conv.toolName() + "）";
            case "NEED_PARAMS" -> "数据来源：业务系统（待补充参数：" + conv.toolName() + "）";
            default -> "来源：—（处理失败）";
        };
    }
}
