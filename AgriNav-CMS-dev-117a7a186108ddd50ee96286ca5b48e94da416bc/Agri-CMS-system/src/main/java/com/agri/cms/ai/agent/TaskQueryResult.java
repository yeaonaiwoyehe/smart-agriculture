package com.agri.cms.ai.agent;

import java.util.List;

/**
 * 今日任务查询结果（A-11）。
 *
 * 按"可认领任务"和"我的任务"两组返回，供大模型组织成清晰的回复，
 * 也便于 APP 分区渲染卡片。
 *
 * @author agrinav
 */
public record TaskQueryResult(
        List<TaskCard> availableTasks,   // 可认领任务（尚未被任何人认领）
        List<TaskCard> myTasks,          // 当前农机手已认领的任务
        boolean locationProvided,        // 本次查询是否提供了有效定位（决定距离是否可信）
        int total                        // 本次返回的卡片总数
) {
}
