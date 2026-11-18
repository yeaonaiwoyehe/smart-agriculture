package com.agri.cms.system.service;

import com.agri.cms.system.domain.Settlement;
import com.agri.cms.system.domain.SettlementItem;
import com.agri.cms.system.domain.vo.SettlementExportVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 结算单 Service（按周期、按机手汇总，支持导出）
 */
public interface SettlementService {

    /**
     * 查询结算单列表
     */
    List<Settlement> selectList(Settlement query);

    /**
     * 根据ID查询结算单
     */
    Settlement selectById(Long id);

    /**
     * 查询结算单明细
     */
    List<SettlementItem> selectItems(Long settlementId);

    /**
     * 生成结算单及其明细
     */
    Settlement generate(Short periodType, LocalDate periodStart, LocalDate periodEnd,
                        String phoneNumber, String username);

    /**
     * 更新结算单状态（已确认/已付款）
     */
    void updateStatus(Long id, Short status);

    /**
     * 组装导出数据
     */
    List<SettlementExportVO> buildExportData(Long settlementId);
}
