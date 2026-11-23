package com.agri.cms.ai.tool;

/**
 * 工具编排的统一返回结果（A-05，A-06 扩展工具信息）。
 *
 * @param status        状态：REPLY（最终回复）、NEED_CONFIRM（等待二次确认）、
 *                      NEED_PARAMS（缺参需补充）、FAILED（失败）
 * @param reply         最终回复或提示文本
 * @param confirmToken  待确认时的确认令牌
 * @param summary       写操作的确认摘要
 * @param receiptNo     写操作成功后的回执编号
 * @param toolName      本次实际命中的工具名，未调用工具为 null（A-06 审计用）
 * @param toolArguments 工具参数 JSON 字符串，未调用工具为 null（A-06 审计用）
 * @author agrinav
 */
public record ToolConversation(
        String status,
        String reply,
        String confirmToken,
        String summary,
        String receiptNo,
        String toolName,
        String toolArguments
) {

    public static ToolConversation reply(String reply) {
        return new ToolConversation("REPLY", reply, null, null, null, null, null);
    }

    public static ToolConversation needConfirm(String confirmToken, String summary) {
        return new ToolConversation("NEED_CONFIRM", null, confirmToken, summary, null, null, null);
    }

    public static ToolConversation needParams(String reply) {
        return new ToolConversation("NEED_PARAMS", reply, null, null, null, null, null);
    }

    public static ToolConversation failed(String reply) {
        return new ToolConversation("FAILED", reply, null, null, null, null, null);
    }

    /**
     * 附上本次命中的工具名与参数，返回新的结果对象（record 不可变）。
     */
    public ToolConversation withTool(String toolName, String toolArguments) {
        return new ToolConversation(status, reply, confirmToken, summary,
                receiptNo, toolName, toolArguments);
    }
}
