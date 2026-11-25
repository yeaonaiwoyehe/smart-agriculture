package com.agri.cms.ai.agent;

/**
 * 认领结果（A-11）。
 *
 * @param success       是否成功（含幂等命中）
 * @param message       面向用户的结果说明
 * @param farmWorkId    农活 ID
 * @param workName      农活名称
 * @param receiptNo     认领回执编号
 * @param alreadyClaimed 是否为幂等命中（该用户此前已认领，直接返回原回执）
 * @author agrinav
 */
public record ClaimResult(
        boolean success,
        String message,
        String farmWorkId,
        String workName,
        String receiptNo,
        boolean alreadyClaimed
) {
}
