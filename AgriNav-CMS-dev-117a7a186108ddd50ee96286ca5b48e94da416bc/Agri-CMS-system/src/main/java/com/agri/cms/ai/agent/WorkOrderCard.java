package com.agri.cms.ai.agent;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * 我的作业单卡片（A-12）。
 *
 * 展示一条作业单的完整信息：单号、状态、地块、作业类型、幅宽、起止时间、面积。
 * 面积在轨迹核算完成前为 null 且 areaPending=true，由上层标注"待核算"，不估算。
 *
 * @author agrinav
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkOrderCard(
        String workOrderNo,          // 作业单号
        Short statusCode,            // 状态码（0 进行中 / 1 已完成）
        String statusText,           // 状态文字
        Long plotId,                 // 地块 ID
        String plotName,             // 地块名称
        String workTypeId,           // 作业类型 ID（4001-4006 字符串）
        String workTypeName,         // 作业类型名称
        Integer width,               // 作业幅宽（厘米）
        String startTime,            // 开始时间
        String endTime,              // 结束时间（进行中为 null）
        BigDecimal area,             // 核算面积（待核算时为 null）
        boolean areaPending          // 是否待核算
) {
}
