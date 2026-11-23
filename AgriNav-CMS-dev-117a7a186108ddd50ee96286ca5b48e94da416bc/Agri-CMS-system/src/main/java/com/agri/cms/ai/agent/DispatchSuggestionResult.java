package com.agri.cms.ai.agent;

import java.util.List;

/**
 * 调度建议结果（A-16 MA-01）。
 *
 * 汇总待派农活、机手候选评估、可用农机运力，以及针对每条农活的
 * 推荐结论与数据缺口，全部基于系统真实数据，不直接派单。
 *
 * @param pendingTasks       待派农活卡片
 * @param operatorCandidates 机手候选评估
 * @param machineryOptions   可用农机运力
 * @param recommendations    针对每条农活的建议结论
 * @param dataGaps           数据缺口说明
 * @author agrinav
 */
public record DispatchSuggestionResult(
        List<PendingTaskCard> pendingTasks,
        List<OperatorCandidate> operatorCandidates,
        List<MachineryOption> machineryOptions,
        List<String> recommendations,
        List<String> dataGaps
) {

    /**
     * 待派农活卡片。
     */
    public record PendingTaskCard(
            String farmWorkId,
            String name,
            java.math.BigDecimal area,
            Long plotId,
            String plotName,
            Double centerLng,
            Double centerLat,
            java.time.LocalDateTime deadline,
            String workType,
            String historyNote
    ) {
    }

    /**
     * 机手候选评估：空闲状态、历史派单统计与建议说明。
     */
    public record OperatorCandidate(
            Long userId,
            String userName,
            String nickName,
            boolean idle,
            long completed,
            long accepted,
            long dispatched,
            long timeout,
            long cancelled,
            double completionRate,
            String note
    ) {
    }

    /**
     * 可用农机运力（未下架）。
     */
    public record MachineryOption(
            String machineryId,
            String name,
            String typeId,
            String location,
            String phoneNumber
    ) {
    }
}
