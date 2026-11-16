package cn.agrinav.trajectory.entity;

import android.graphics.Bitmap;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * @author 11230
 */
@Data
public class ResultLandPlot implements Serializable {
    /**id*/
    private Long id;
    /**地块名*/
    private String landName;
    /**地块类型*/
    private String landType;
    /**上传面积*/
    private Float landArea;
    /**是否删除*/
    private int isDeleted;
    /**地址*/
    private String address;
    /**创建人*/
    private String createBy;
    /**上传时间*/
    private String createTime;
    /**创建人*/
    private String updateBy;
    /**更新时间*/
    private String updateTime;
    /**缩略图地址*/
    private String image;
    /**点集*/
    private String boundary;
    private String centerPoint;

}
