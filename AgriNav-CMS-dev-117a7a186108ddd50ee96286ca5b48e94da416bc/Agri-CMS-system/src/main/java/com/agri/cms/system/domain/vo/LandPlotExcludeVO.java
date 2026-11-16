package com.agri.cms.system.domain.vo;

import com.agri.cms.system.domain.LandPlotExclude;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.util.List;

/**
 * @author 姚锦伦
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LandPlotExcludeVO {
    /**
     * 地块id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long landPlotId;
    /**
     * 地块对应的无用区域
     */
    private List<LandPlotExclude> landPlotExcludes;

    public Long getLandPlotId() {
        return landPlotId;
    }

    public void setLandPlotId(Long landPlotId) {
        this.landPlotId = landPlotId;
    }

    public List<LandPlotExclude> getLandPlotExcludes() {
        return landPlotExcludes;
    }

    public void setLandPlotExcludes(List<LandPlotExclude> landPlotExcludes) {
        this.landPlotExcludes = landPlotExcludes;
    }

    @Override
    public String toString() {
        return "LandPlotExcludeVO{" +
                "landPlotId=" + landPlotId +
                ", landPlotExcludes=" + landPlotExcludes +
                '}';
    }
}
