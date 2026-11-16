package com.agri.cms.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * @author 李鸿麟
 * @description: crop_work表持久化对象
 */

@TableName("crop_work")
public class CropWork {
    //作物id
    private Long cropId;
    //作业类型id
    private Long workId;
    //该作物该作业类型是否可用
    private Boolean isAvailable;

    public CropWork(Long cropId, Long workId, Boolean isAvailable) {
        this.cropId = cropId;
        this.workId = workId;
        this.isAvailable = isAvailable;
    }

    public CropWork() {
    }

    public Long getCropId() {
        return cropId;
    }

    public void setCropId(Long cropId) {
        this.cropId = cropId;
    }

    public Long getWorkId() {
        return workId;
    }

    public void setWorkId(Long workId) {
        this.workId = workId;
    }

    public Boolean getAvailable() {
        return isAvailable;
    }

    public void setAvailable(Boolean available) {
        isAvailable = available;
    }
}
