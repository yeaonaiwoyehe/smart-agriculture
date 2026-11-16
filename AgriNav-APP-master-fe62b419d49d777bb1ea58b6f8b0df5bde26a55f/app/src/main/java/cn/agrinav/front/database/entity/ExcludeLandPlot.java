package cn.agrinav.front.database.entity;

import lombok.Data;

/**
 * @author 11230
 */
@Data
public class ExcludeLandPlot {
    private String id;
    private String landPlotId;
    private String boundary;
    private Double area;
    private int isDelete ;
}