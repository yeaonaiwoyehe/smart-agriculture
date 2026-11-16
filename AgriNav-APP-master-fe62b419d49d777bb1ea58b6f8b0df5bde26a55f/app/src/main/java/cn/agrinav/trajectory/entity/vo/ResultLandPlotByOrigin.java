package cn.agrinav.trajectory.entity.vo;

import java.util.ArrayList;

import cn.agrinav.trajectory.entity.ResultLandPlot;
import lombok.Data;

/**
 * @author 11230
 */
@Data
public class ResultLandPlotByOrigin {
    private Integer total;
    private ArrayList<ResultLandPlot> data;
}
