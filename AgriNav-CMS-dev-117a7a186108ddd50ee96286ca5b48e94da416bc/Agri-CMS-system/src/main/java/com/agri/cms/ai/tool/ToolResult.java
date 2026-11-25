package com.agri.cms.ai.tool;

/**
 * 工具执行结果（A-05）。
 *
 * @param success      是否执行成功
 * @param content      成功时回传给大模型的结果文本
 * @param receiptNo    写操作生成的回执编号，读操作为 null
 * @param errorMessage 失败原因，success 为 false 时使用
 * @author agrinav
 */
public record ToolResult(
        boolean success,
        String content,
        String receiptNo,
        String errorMessage
) {

    public static ToolResult ok(String content) {
        return new ToolResult(true, content, null, null);
    }

    public static ToolResult ok(String content, String receiptNo) {
        return new ToolResult(true, content, receiptNo, null);
    }

    public static ToolResult fail(String errorMessage) {
        return new ToolResult(false, null, null, errorMessage);
    }
}
