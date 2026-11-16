package com.agri.cms.system.domain;

/**
 * @author 姚锦伦
 */
public class FarmWorkCrop {
    /**
     * 农活id
     */
    private String farmWorkId;
    /**
     * 作物id
     */
    private Long cropId;

    public String getFarmWorkId() {
        return farmWorkId;
    }

    public void setFarmWorkId(String farmWorkId) {
        this.farmWorkId = farmWorkId;
    }

    public Long getCropId() {
        return cropId;
    }

    public void setCropId(Long cropId) {
        this.cropId = cropId;
    }
}

