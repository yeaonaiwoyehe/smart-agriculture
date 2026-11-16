package cn.agrinav.front.entity;

import lombok.Getter;
import lombok.Setter;

// 热力图数据模型类
@Getter
@Setter
public class HeatmapData {
    private Long id;
    private String latitude;
    private String longitude;
    private String weight;
}