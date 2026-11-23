package com.agri.cms.ai.agent;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * 任务卡片（A-11，AG-01 任务助手）。
 *
 * 面向农机手的单条任务信息，覆盖 PRD 要求的地块、亩数、单价、距离四个核心字段，
 * 并携带地块中心经纬度供 APP 一键导航，以及认领状态与回执。
 *
 * 单价或定位缺失时，对应字段为 null，由上层明确标注"待定价/需定位"，绝不臆造。
 *
 * @author agrinav
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaskCard(
        String farmWorkId,          // 农活 ID
        String workName,            // 农活名称
        Long workTypeId,            // 农活类型 ID（work_type 表 2001-2008）
        String workTypeName,        // 农活类型名称
        Long landPlotId,            // 地块 ID
        String landPlotName,        // 地块名称
        String landAddress,         // 地块地址
        BigDecimal area,            // 作业亩数（亩）
        BigDecimal unitPrice,       // 单价（元/亩），无对应单价规则时为 null
        BigDecimal estimatedAmount, // 预估金额（单价×亩数），单价缺失时为 null
        Double distanceMeters,      // 距当前定位的距离（米），未提供定位时为 null
        Double centerLongitude,     // 地块中心经度（一键导航用）
        Double centerLatitude,      // 地块中心纬度
        String endTime,             // 农活截止时间
        String phoneNumber,         // 发布方联系电话
        boolean claimed,            // 是否已被认领
        boolean claimedByMe,        // 是否由当前农机手认领
        String receiptNo            // 当前农机手的认领回执（已认领时）
) {
}
