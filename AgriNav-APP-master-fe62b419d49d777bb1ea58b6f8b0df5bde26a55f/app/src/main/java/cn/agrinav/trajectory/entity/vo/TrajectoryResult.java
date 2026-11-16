package cn.agrinav.trajectory.entity.vo;

import java.util.ArrayList;
import java.util.List;

import cn.agrinav.trajectory.entity.Point;
import lombok.Data;

/**
 * @author 11230
 */
@Data
public class TrajectoryResult {
    private String trajectoryImg;
    private String trajectoryInfoId;
    /**手机号 */
    private String phoneNumber;
    /**作业类型 */
    private String workType;
    /**作物*/
    private String crop;
    /**位置 */
    private String location;
    /**幅宽*/
    private int width;
    /**地块id */
    private long plotId;
    /**点集 */
    private List<Point> points;
    //开始时间
    private String startTime;
    //结束时间
    private String endTime;
    private Double area;
    private ArrayList<TrajectoryPointExcludeOnPlot> trajectoryExclude;
}
