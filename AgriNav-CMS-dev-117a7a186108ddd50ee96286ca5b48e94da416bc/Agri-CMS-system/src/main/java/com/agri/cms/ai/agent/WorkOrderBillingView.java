package com.agri.cms.ai.agent;

import com.agri.cms.system.domain.ReviewRequest;
import com.agri.cms.system.domain.Settlement;
import com.agri.cms.system.domain.SettlementItem;

/**
 * 作业单结算视图（A-13）。
 *
 * 承载某条作业单的结算明细、所属结算单与复核（扣减）记录。
 * settled=false 表示该作业单尚未进入结算（item 与 settlement 为 null）。
 *
 * @author agrinav
 */
public record WorkOrderBillingView(
        String trajectoryInfoId,
        boolean settled,
        SettlementItem item,
        Settlement settlement,
        ReviewRequest reviewRequest
) {
}
