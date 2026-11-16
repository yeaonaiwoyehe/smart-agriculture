package cn.agrinav.measure.entity;


import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import cn.agrinav.front.entity.LandPlotExclude;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author simon
 * @since 2024/9/20 */
@Getter
@Setter
public class LandPlot implements Serializable {
    //id
    private Long id;
    //地块名
    private String landName;
    //地块类型
    private String landType;
    //地块位置
    private String address;
    //点集
    private String boundary;
    //上传面积
    private Float landArea;
    //缩略图地址
    private String image;
    //上传时间
    private LocalDateTime createTime;
    private Bitmap bitmap;
    private String centerPoint;
    private String measurementMethod;
    private String createdBy;

    private String ownerName;
    private String ownerPhone;

    private List<LandPlotExclude> landPlotExcludes;

    @NonNull
    @Override
    public String toString() {
        return "LandPlot{" +
                "id=" + id +
                ", landName='" + landName + '\'' +
                ", landType='" + landType + '\'' +
                ", address='" + address + '\'' +
                ", boundary='" + boundary + '\'' +
                ", landArea=" + landArea +
                ", image='" + image + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}

