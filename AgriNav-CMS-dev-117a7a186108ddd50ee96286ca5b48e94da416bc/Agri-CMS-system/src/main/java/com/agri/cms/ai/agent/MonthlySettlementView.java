package com.agri.cms.ai.agent;

import com.agri.cms.system.domain.Settlement;
import com.agri.cms.system.domain.SettlementItem;

import java.util.List;

/**
 * 月度（周期）结算视图（A-13）。
 *
 * 承载某机手某周期的结算单及其全部明细。settlement 为 null 表示该周期暂无结算单。
 *
 * @author agrinav
 */
public record MonthlySettlementView(
        Settlement settlement,
        List<SettlementItem> items
) {
}
