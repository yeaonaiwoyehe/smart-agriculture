package com.agri.cms.system.mapper;

import com.agri.cms.system.domain.SettlementItem;
import com.agri.cms.system.domain.vo.SettlementExportVO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 结算明细 Mapper
 */
@DS("pg")
public interface SettlementItemMapper extends BaseMapper<SettlementItem> {

    /**
     * 关联结算单、作业类型表，查询某结算单的导出数据
     */
    @Select("SELECT s.settlement_no AS settlementNo, " +
            "s.phone_number AS phoneNumber, " +
            "i.plot_id AS plotId, " +
            "w.work_name AS workType, " +
            "i.area AS area, " +
            "i.unit_price AS unitPrice, " +
            "i.amount AS amount " +
            "FROM settlement_item i " +
            "JOIN settlement s ON s.id = i.settlement_id " +
            "LEFT JOIN work w ON w.id = CAST(i.work_type AS bigint) " +
            "WHERE i.settlement_id = #{settlementId} " +
            "ORDER BY i.id")
    List<SettlementExportVO> selectExportData(@Param("settlementId") Long settlementId);
}
