package com.agri.cms.system.domain.dto;
/**
 * @author 姚锦伦
 * 工作地块DTO
 */
public class WorkingLandPlotDTO {
    private String id;
    private String landType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLandType() {
        return landType;
    }

    public void setLandType(String landType) {
        this.landType = landType;
    }

    @Override
    public String toString() {
        return "WorkingLandPlotDTO{" +
                "id='" + id + '\'' +
                ", landType='" + landType + '\'' +
                '}';
    }
}
