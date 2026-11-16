package com.agri.cms.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * @author 姚锦伦
 */
@TableName("agricultural_machinery_location")
public class AgriculturalMachineryLocation {
    /**
     * 农机id
     */
    @TableField(value = "agriculture_machinery_id")
    private String agriculturalMachineryId;
    /**
     * 位置
     */
    private String location;

    public String getAgriculturalMachineryId() {
        return agriculturalMachineryId;
    }

    public void setAgriculturalMachineryId(String agriculturalMachineryId) {
        this.agriculturalMachineryId = agriculturalMachineryId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
