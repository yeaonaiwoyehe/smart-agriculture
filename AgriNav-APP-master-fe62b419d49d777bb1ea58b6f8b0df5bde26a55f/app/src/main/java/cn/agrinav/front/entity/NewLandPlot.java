package cn.agrinav.front.entity;


import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author simon
 * @since 2024/9/20 */
@Getter
@Setter
public class NewLandPlot implements Serializable {

    //id
    private String id;
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
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NewLandPlot landPlot = (NewLandPlot) o;
        return Objects.equals(getId(), landPlot.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}

