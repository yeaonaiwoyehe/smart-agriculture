package com.agri.cms.ai.tool;

/**
 * 工具编排的统一返回结果（A-05）。
 *
 * @param status       状态：REPLY（最终回复）、NEED_CONFIRM（等待二次确认）、
 *                     NEED_PARAMS（缺参需补充）、FAILED（失败）
 * @param reply        最终回复或提示文本
 * @param confirmToken 待确认时的确认令牌
 * @param summary      写操作的确认摘要
 * @param receiptNo    写操作成功后的回执编号
 * @author agrinav
 */
public record ToolConversation(
        String status,
        String reply,
        String confirmToken,
        String summary,
        String receiptNo
) {

    public static ToolConversation reply(String reply) {
        return new ToolConversation("REPLY", reply, null, null, null);
    }

    public static ToolConversation needConfirm(String confirmToken, String summary) {
        return new ToolConversation("NEED_CONFIRM", null, confirmToken, summary, null);
    }

    public static ToolConversation needParams(String reply) {
        return new ToolConversation("NEED_PARAMS", reply, null, null, null);
    }

    public static ToolConversation failed(String reply) {
        return new ToolConversation("FAILED", reply, null, null, null);
    }
}
