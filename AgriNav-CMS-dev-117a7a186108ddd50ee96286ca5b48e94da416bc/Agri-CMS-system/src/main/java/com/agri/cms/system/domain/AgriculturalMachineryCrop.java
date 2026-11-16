package com.agri.cms.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * @author 姚锦伦
 */
@TableName("agricultural_machinery_crop")
public class AgriculturalMachineryCrop {
    /**
     * 农机id
     */
    private String agriculturalMachineryId;
    /**
     * 农作物id
     */
    private long cropId;

    public String getAgriculturalMachineryId() {
        return agriculturalMachineryId;
    }

    public void setAgriculturalMachineryId(String agriculturalMachineryId) {
        this.agriculturalMachineryId = agriculturalMachineryId;
    }

    public long getCropId() {
        return cropId;
    }

    public void setCropId(long cropId) {
        this.cropId = cropId;
    }

}
