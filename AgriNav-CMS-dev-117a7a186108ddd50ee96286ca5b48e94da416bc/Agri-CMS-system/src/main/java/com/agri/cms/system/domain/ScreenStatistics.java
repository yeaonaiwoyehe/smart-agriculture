package com.agri.cms.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("screen_statistics")
public class ScreenStatistics {
    @TableId(value = "id")
    private String id;

    private String cropName;

    private String workName;

    private BigDecimal totalArea;

    private Integer workCount;

    private LocalDateTime lastUpdateTime;


    @TableField(exist = false)
    private Long cropId;

    @TableField(exist = false)
    private Long workId;


    // Getters and Setters


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public String getWorkName() {
        return workName;
    }

    public void setWorkName(String workName) {
        this.workName = workName;
    }

    public BigDecimal getTotalArea() {
        return totalArea;
    }

    public void setTotalArea(BigDecimal totalArea) {
        this.totalArea = totalArea;
    }

    public Integer getWorkCount() {
        return workCount;
    }

    public void setWorkCount(Integer workCount) {
        this.workCount = workCount;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
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

    @Override
    public String toString() {
        return "WorkStatistics{" +
                "id=" + id +
                ", cropName='" + cropName + '\'' +
                ", workName='" + workName + '\'' +
                ", totalArea=" + totalArea +
                ", workCount=" + workCount +
                ", lastUpdateTime=" + lastUpdateTime +
                '}';
    }
}
