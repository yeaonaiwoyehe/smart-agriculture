package com.agri.cms.ai.agent;

import java.time.LocalDateTime;

/**
 * 异常工单提交结果（A-14）。
 *
 * @param success     是否成功
 * @param message     提示信息（成功/失败原因）
 * @param alertNo     异常工单编号（成功时）
 * @param handlerName 处理人（成功时）
 * @param deadline    处理时限（成功时）
 * @author agrinav
 */
public record AlertSubmitResult(
        boolean success,
        String message,
        String alertNo,
        String handlerName,
        LocalDateTime deadline
) {
    public static AlertSubmitResult fail(String message) {
        return new AlertSubmitResult(false, message, null, null, null);
    }
}
