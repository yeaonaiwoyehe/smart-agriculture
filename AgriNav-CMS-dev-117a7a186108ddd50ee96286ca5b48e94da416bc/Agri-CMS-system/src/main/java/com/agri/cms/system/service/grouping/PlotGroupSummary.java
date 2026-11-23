package com.agri.cms.system.service.grouping;

import java.math.BigDecimal;

/** 分组数量和面积仅统计当前用户未删除的地块；字典可在团队内共用。 */
public record PlotGroupSummary(String groupType, String groupCode, String groupName,
                               long plotCount, BigDecimal totalArea) { }
