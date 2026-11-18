package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.PricingRule;
import com.agri.cms.system.domain.Settlement;
import com.agri.cms.system.domain.SettlementItem;
import com.agri.cms.system.domain.TrajectoryInfo;
import com.agri.cms.system.domain.vo.SettlementExportVO;
import com.agri.cms.system.mapper.PricingRuleMapper;
import com.agri.cms.system.mapper.SettlementItemMapper;
import com.agri.cms.system.mapper.SettlementMapper;
import com.agri.cms.system.mapper.TrajectoryInfoMapper;
import com.agri.cms.system.service.SettlementService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 结算单 Service 实现
 */
@Service
@DS("pg")
public class SettlementServiceImpl implements SettlementService {

    @Resource
    private SettlementMapper settlementMapper;
    @Resource
    private SettlementItemMapper settlementItemMapper;
    @Resource
    private TrajectoryInfoMapper trajectoryInfoMapper;
    @Resource
    private PricingRuleMapper pricingRuleMapper;

    @Override
    public List<Settlement> selectList(Settlement query) {
        LambdaQueryWrapper<Settlement> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getSettlementNo() != null && !query.getSettlementNo().isEmpty()) {
                wrapper.like(Settlement::getSettlementNo, query.getSettlementNo());
            }
            if (query.getPhoneNumber() != null && !query.getPhoneNumber().isEmpty()) {
                wrapper.eq(Settlement::getPhoneNumber, query.getPhoneNumber());
            }
            if (query.getPeriodType() != null) {
                wrapper.eq(Settlement::getPeriodType, query.getPeriodType());
            }
            if (query.getStatus() != null) {
                wrapper.eq(Settlement::getStatus, query.getStatus());
            }
        }
        wrapper.orderByDesc(Settlement::getCreateTime);
        return settlementMapper.selectList(wrapper);
    }

    @Override
    public Settlement selectById(Long id) {
        return settlementMapper.selectById(id);
    }

    @Override
    public List<SettlementItem> selectItems(Long settlementId) {
        LambdaQueryWrapper<SettlementItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SettlementItem::getSettlementId, settlementId)
                .orderByAsc(SettlementItem::getId);
        return settlementItemMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Settlement generate(Short periodType, LocalDate periodStart, LocalDate periodEnd,
                               String phoneNumber, String username) {
        if (periodStart == null || periodEnd == null || periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("结算周期不合法");
        }
        // 防止同一机手、同一周期重复生成
        LambdaQueryWrapper<Settlement> dup = new LambdaQueryWrapper<>();
        dup.eq(Settlement::getPhoneNumber, phoneNumber)
                .eq(Settlement::getPeriodStart, periodStart)
                .eq(Settlement::getPeriodEnd, periodEnd);
        if (settlementMapper.selectCount(dup) > 0) {
            throw new IllegalStateException("该机手在该周期已存在结算单，请勿重复生成");
        }

        // 取周期内该机手的正常作业单（status=0）
        LambdaQueryWrapper<TrajectoryInfo> tw = new LambdaQueryWrapper<>();
        tw.eq(TrajectoryInfo::getPhoneNumber, phoneNumber)
                .eq(TrajectoryInfo::getStatus, (short) 0)
                .ge(TrajectoryInfo::getStartTime, periodStart.atStartOfDay())
                .le(TrajectoryInfo::getStartTime, periodEnd.atTime(23, 59, 59))
                .orderByAsc(TrajectoryInfo::getStartTime);
        List<TrajectoryInfo> workList = trajectoryInfoMapper.selectList(tw);
        if (workList.isEmpty()) {
            throw new IllegalStateException("该周期内没有可结算的作业单");
        }

        Settlement settlement = new Settlement();
        settlement.setSettlementNo(generateSettlementNo());
        settlement.setPeriodType(periodType);
        settlement.setPeriodStart(periodStart);
        settlement.setPeriodEnd(periodEnd);
        settlement.setPhoneNumber(phoneNumber);
        settlement.setStatus((short) 0);
        settlement.setCreateBy(username);
        settlement.setCreateTime(LocalDateTime.now());

        BigDecimal totalArea = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 先插入结算单，拿到雪花ID
        settlementMapper.insert(settlement);

        for (TrajectoryInfo info : workList) {
            BigDecimal area = info.getArea() == null ? BigDecimal.ZERO : info.getArea();
            BigDecimal unitPrice = resolveUnitPrice(info.getWorkType(), area);
            if (unitPrice == null) {
                throw new IllegalStateException("作业类型 " + info.getWorkType() + " 未配置启用单价，无法结算");
            }
            BigDecimal amount = area.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

            SettlementItem item = new SettlementItem();
            item.setSettlementId(settlement.getId());
            item.setTrajectoryInfoId(info.getTrajectoryInfoId());
            item.setPlotId(info.getPlotId());
            item.setWorkType(info.getWorkType());
            item.setArea(area);
            item.setUnitPrice(unitPrice);
            item.setAmount(amount);
            item.setCreateTime(LocalDateTime.now());
            settlementItemMapper.insert(item);

            totalArea = totalArea.add(area);
            totalAmount = totalAmount.add(amount);
        }

        settlement.setTotalArea(totalArea);
        settlement.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        settlementMapper.updateById(settlement);
        return settlement;
    }

    @Override
    public void updateStatus(Long id, Short status) {
        Settlement settlement = settlementMapper.selectById(id);
        if (settlement == null) {
            throw new IllegalArgumentException("结算单不存在");
        }
        settlement.setStatus(status);
        settlement.setUpdateTime(LocalDateTime.now());
        settlementMapper.updateById(settlement);
    }

    @Override
    public List<SettlementExportVO> buildExportData(Long settlementId) {
        return settlementItemMapper.selectExportData(settlementId);
    }

    /**
     * 按作业类型与面积匹配启用单价：支持阶梯，取满足条件中 min_area 最大（最具体）的规则
     */
    private BigDecimal resolveUnitPrice(String workType, BigDecimal area) {
        Long wt = Long.valueOf(workType);
        LambdaQueryWrapper<PricingRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PricingRule::getWorkType, wt)
                .eq(PricingRule::getStatus, (short) 0)
                .orderByAsc(PricingRule::getMinArea);
        List<PricingRule> rules = pricingRuleMapper.selectList(wrapper);
        BigDecimal matched = null;
        for (PricingRule rule : rules) {
            BigDecimal minArea = rule.getMinArea() == null ? BigDecimal.ZERO : rule.getMinArea();
            boolean geMin = area.compareTo(minArea) >= 0;
            boolean leMax = rule.getMaxArea() == null || area.compareTo(rule.getMaxArea()) <= 0;
            if (geMin && leMax) {
                matched = rule.getUnitPrice();
            }
        }
        return matched;
    }

    private String generateSettlementNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = ThreadLocalRandom.current().nextInt(100, 1000);
        return "ST-" + date + "-" + seq;
    }
}
