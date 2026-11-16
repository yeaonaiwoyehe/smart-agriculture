package com.agri.cms.system.domain.vo;


/**
 * @author 李鸿麟
 * @descrpiton 给CMS返回的地块总面积
 */
public class LandPlotAreaVO extends ProvinceVO{
    private Float landArea;

    public Float getLandArea() {
        return landArea;
    }

    public void setLandArea(Float landArea) {
        this.landArea = landArea;
    }

    @Override
    public String toString() {
        return "LandPlotAreaVO{" +
                "landArea=" + landArea +
                '}';
    }
}
