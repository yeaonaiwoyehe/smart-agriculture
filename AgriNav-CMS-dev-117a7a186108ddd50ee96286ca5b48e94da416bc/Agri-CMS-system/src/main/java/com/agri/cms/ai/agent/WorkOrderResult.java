package com.agri.cms.ai.agent;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 作业单操作结果（A-12，开始/结束作业）。
 *
 * @param success         是否成功
 * @param message         面向用户的结果说明
 * @param workOrderId     作业单主键（trajectory_info_id）
 * @param workOrderNo     业务作业单号（WO- 开头，可核对）
 * @param statusCode      作业单状态码（0 进行中 / 1 已完成）
 * @param statusText      状态文字
 * @param receiptNo       写操作回执编号
 * @param boundaryWarning 超界提醒，定位点在地块外时给出，无则为 null
 * @author agrinav
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkOrderResult(
        boolean success,
        String message,
        String workOrderId,
        String workOrderNo,
        Short statusCode,
        String statusText,
        String receiptNo,
        String boundaryWarning
) {
}
