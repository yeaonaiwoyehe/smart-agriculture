package com.agri.cms.ai.agent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 异常研判结果（A-17 MA-02）。
 *
 * 汇总异常工单、超时派单、面积争议与作业停滞四类异常，每条都区分
 * 「数据事实」与「可能原因」，并给出可追溯线索，不直接修改业务数据。
 *
 * @param overview          异常数量摘要
 * @param alertAnomalies    异常工单研判
 * @param timeoutAnomalies  超时派单研判
 * @param disputeAnomalies  面积争议研判
 * @param stagnationAnomalies 作业停滞研判
 * @param dataGaps          数据缺口说明
 * @author agrinav
 */
public record AnomalyInsightResult(
        Overview overview,
        List<AlertAnomaly> alertAnomalies,
        List<TimeoutAnomaly> timeoutAnomalies,
        List<DisputeAnomaly> disputeAnomalies,
        List<StagnationAnomaly> stagnationAnomalies,
        List<String> dataGaps
) {

    /**
     * 异常数量摘要。
     */
    public record Overview(
            long pendingAlerts,
            long timeoutDispatches,
            long areaDisputes,
            long stagnations,
            long total
    ) {
    }

    /**
     * 异常工单研判。
     */
    public record AlertAnomaly(
            String alertNo,
            String alertType,
            String description,
            String status,
            String handlerName,
            LocalDateTime deadline,
            boolean overdue,
            String dataFact,
            List<String> possibleCauses
    ) {
    }

    /**
     * 超时派单研判。
     */
    public record TimeoutAnomaly(
            String dispatchNo,
            String farmWorkId,
            String assigneeId,
            LocalDateTime deadline,
            long overdueDays,
            String dataFact,
            List<String> possibleCauses
    ) {
    }

    /**
     * 面积争议研判。
     */
    public record DisputeAnomaly(
            String reviewNo,
            String trajectoryInfoId,
            java.math.BigDecimal systemArea,
            java.math.BigDecimal claimedArea,
            java.math.BigDecimal gap,
            short status,
            String finalComment,
            String dataFact,
            List<String> possibleCauses
    ) {
    }

    /**
     * 作业停滞研判。
     */
    public record StagnationAnomaly(
            String phoneNumber,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long durationSeconds,
            double longitude,
            double latitude,
            String dataFact,
            List<String> possibleCauses
    ) {
    }
}
