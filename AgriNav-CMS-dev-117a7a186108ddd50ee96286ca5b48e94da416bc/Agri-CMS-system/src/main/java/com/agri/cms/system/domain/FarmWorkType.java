package com.agri.cms.system.domain;

/**
 * @author 姚锦伦
 */
public class FarmWorkType {
    /**
     * 农活id
     */
    private String farmWorkId;
    /**
     * 工作类型id
     */
    private Long workTypeId;

    public String getFarmWorkId() {
        return farmWorkId;
    }

    public void setFarmWorkId(String farmWorkId) {
        this.farmWorkId = farmWorkId;
    }

    public Long getWorkTypeId() {
        return workTypeId;
    }

    public void setWorkTypeId(Long workTypeId) {
        this.workTypeId = workTypeId;
    }
}
