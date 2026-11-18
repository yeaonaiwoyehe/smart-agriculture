package com.agri.cms.system.domain.vo;

import com.agri.cms.common.annotation.Excel;

import java.math.BigDecimal;

/**
 * 结算明细导出 VO（对接线下付款）
 */
public class SettlementExportVO {

    @Excel(name = "结算单号")
    private String settlementNo;

    @Excel(name = "机手手机号")
    private String phoneNumber;

    @Excel(name = "地块编号", cellType = Excel.ColumnType.NUMERIC)
    private Long plotId;

    @Excel(name = "作业类型")
    private String workType;

    @Excel(name = "作业面积(亩)", cellType = Excel.ColumnType.NUMERIC)
    private BigDecimal area;

    @Excel(name = "单价(元/亩)", cellType = Excel.ColumnType.NUMERIC)
    private BigDecimal unitPrice;

    @Excel(name = "金额(元)", cellType = Excel.ColumnType.NUMERIC)
    private BigDecimal amount;

    public String getSettlementNo() {
        return settlementNo;
    }

    public void setSettlementNo(String settlementNo) {
        this.settlementNo = settlementNo;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Long getPlotId() {
        return plotId;
    }

    public void setPlotId(Long plotId) {
        this.plotId = plotId;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public BigDecimal getArea() {
        return area;
    }

    public void setArea(BigDecimal area) {
        this.area = area;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
