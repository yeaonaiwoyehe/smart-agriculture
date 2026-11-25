package com.agri.cms.ai.tool;

import java.util.Map;

/**
 * 等待用户二次确认的写操作（A-05）。
 *
 * @param toolName  待执行工具名
 * @param arguments 已校验的调用参数
 * @param summary   展示给用户的操作摘要，用于确认弹窗
 * @param createdAt 创建时间戳
 * @author agrinav
 */
public record PendingConfirmation(
        String toolName,
        Map<String, Object> arguments,
        String summary,
        long createdAt
) {

    public static PendingConfirmation of(String toolName,
                                        Map<String, Object> arguments,
                                        String summary) {
        return new PendingConfirmation(toolName, arguments, summary, System.currentTimeMillis());
    }
}
