package cn.agrinav.front.database.entity;


import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * @author 11230
 */
@Data
@Entity
public class LandPlot implements Serializable {
    /**
     *  id
     */
    @PrimaryKey
    private Long id;
    /**
     * 地块名
     */
    private String landName;
    /**
     * 地块类型
     */
    private String landType;
    /**
     * 地块位置
     */
    private String address;
    /**
     * 点集
     */
    private String boundary;
    /**
     * 上传面积
     */
    private Float landArea;
    /**
     * 缩略图地址
     */
    private String image;
    /**
     * 上传时间
     */
    @Ignore
    private LocalDateTime createTime;
    @Ignore
    private Bitmap bitmap;
    private String centerPoint;
    @Ignore
    private String measurementMethod;
}

