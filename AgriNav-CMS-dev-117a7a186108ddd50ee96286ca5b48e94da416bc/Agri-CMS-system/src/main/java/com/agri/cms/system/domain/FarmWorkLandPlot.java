package com.agri.cms.system.domain;

/**
 * @author 姚锦伦
 */
public class FarmWorkLandPlot {
    /**
     * 农活id
     */
    private String farmWorkId;
    /**
     * 地块id
     */
    private Long landPlotId;

    public String getFarmWorkId() {
        return farmWorkId;
    }

    public void setFarmWorkId(String farmWorkId) {
        this.farmWorkId = farmWorkId;
    }

    public Long getLandPlotId() {
        return landPlotId;
    }

    public void setLandPlotId(Long landPlotId) {
        this.landPlotId = landPlotId;
    }
}
