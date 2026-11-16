package com.agri.cms.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * @author 姚锦伦
 */
@TableName("agricultural_machinery_image")
public class AgriculturalMachineryImage {
    /**
     * 农机id
     */
    @TableField(value = "agricultural_machinery_id")
    private String agriculturalMachineryId;
    /**
     * 图片地址
     */
    private String image;

    public String getAgriculturalMachineryId() {
        return agriculturalMachineryId;
    }

    public void setAgriculturalMachineryId(String agriculturalMachineryId) {
        this.agriculturalMachineryId = agriculturalMachineryId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

}
