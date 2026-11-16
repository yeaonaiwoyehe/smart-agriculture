package cn.agrinav.front.entity;


import androidx.annotation.NonNull;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * 地块无用区域实体类
 *
 * @author lya
 */

@Getter
@Setter
public class LandPlotExclude implements Serializable {

    private Long id;

    private Long landPlotId;

    private String boundary;

    private Float area;

    private int isDeleted;


    @NonNull
    @Override
    public String toString() {
        return "LandPlotExclude{" +
                "id=" + id +
                ", landPlotId=" + landPlotId +
                ", boundary=" + boundary +
                ", area=" + area +
                '}';
    }
}