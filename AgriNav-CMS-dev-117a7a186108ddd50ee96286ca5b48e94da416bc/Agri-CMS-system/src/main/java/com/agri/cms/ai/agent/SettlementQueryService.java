package com.agri.cms.ai.agent;

import com.agri.cms.system.domain.PricingRule;
import com.agri.cms.system.domain.ReviewRequest;
import com.agri.cms.system.domain.Settlement;
import com.agri.cms.system.domain.SettlementItem;
import com.agri.cms.system.mapper.PricingRuleMapper;
import com.agri.cms.system.mapper.ReviewRequestMapper;
import com.agri.cms.system.mapper.SettlementItemMapper;
import com.agri.cms.system.mapper.SettlementMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 结算助手查询服务（A-13，AG-03）。
 *
 * 面向智能体的只读取数：作业单结算视图、月度结算视图、单价规则。
 * 全部数据来自数据库已有记录，不做任何估算或推算；不存在的记录如实返回 null/空。
 *
 * @author agrinav
 */
@Service
@DS("pg")
public class SettlementQueryService {

    private final SettlementMapper settlementMapper;
    private final SettlementItemMapper settlementItemMapper;
    private final ReviewRequestMapper reviewRequestMapper;
    private final PricingRuleMapper pricingRuleMapper;

    public SettlementQueryService(SettlementMapper settlementMapper,
                                  SettlementItemMapper settlementItemMapper,
                                  ReviewRequestMapper reviewRequestMapper,
                                  PricingRuleMapper pricingRuleMapper) {
        this.settlementMapper = settlementMapper;
        this.settlementItemMapper = settlementItemMapper;
        this.reviewRequestMapper = reviewRequestMapper;
        this.pricingRuleMapper = pricingRuleMapper;
    }

    /**
     * 查某条作业单的结算明细、所属结算单与复核（扣减）记录。
     */
    public WorkOrderBillingView getWorkOrderBilling(String trajectoryInfoId) {
        // 一条作业单在结算明细中至多出现一次
        SettlementItem item = settlementItemMapper.selectOne(
                new LambdaQueryWrapper<SettlementItem>()
                        .eq(SettlementItem::getTrajectoryInfoId, trajectoryInfoId)
                        .last("limit 1"));

        Settlement settlement = null;
        if (item != null && item.getSettlementId() != null) {
            settlement = settlementMapper.selectById(item.getSettlementId());
        }

        ReviewRequest review = reviewRequestMapper.selectOne(
                new LambdaQueryWrapper<ReviewRequest>()
                        .eq(ReviewRequest::getTrajectoryInfoId, trajectoryInfoId)
                        .orderByDesc(ReviewRequest::getCreateTime)
                        .last("limit 1"));

        return new WorkOrderBillingView(trajectoryInfoId, item != null, item, settlement, review);
    }

    /**
     * 查某机手指定年月的结算单及其全部明细；无结算单时 settlement 为 null。
     */
    public MonthlySettlementView getMonthlySettlement(String phoneNumber, int year, int month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        Settlement settlement = settlementMapper.selectOne(
                new LambdaQueryWrapper<Settlement>()
                        .eq(Settlement::getPhoneNumber, phoneNumber)
                        .ge(Settlement::getPeriodStart, monthStart)
                        .le(Settlement::getPeriodStart, monthEnd)
                        .orderByDesc(Settlement::getCreateTime)
                        .last("limit 1"));

        if (settlement == null) {
            return new MonthlySettlementView(null, List.of());
        }

        List<SettlementItem> items = settlementItemMapper.selectList(
                new LambdaQueryWrapper<SettlementItem>()
                        .eq(SettlementItem::getSettlementId, settlement.getId())
                        .orderByAsc(SettlementItem::getId));
        return new MonthlySettlementView(settlement, items);
    }

    /**
     * 查单价规则；workType 为空时返回全部启用规则。
     */
    public List<PricingRule> listPricingRules(Long workType) {
        LambdaQueryWrapper<PricingRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PricingRule::getStatus, (short) 0);
        if (workType != null) {
            wrapper.eq(PricingRule::getWorkType, workType);
        }
        wrapper.orderByAsc(PricingRule::getWorkType)
                .orderByAsc(PricingRule::getMinArea);
        return pricingRuleMapper.selectList(wrapper);
    }
}
