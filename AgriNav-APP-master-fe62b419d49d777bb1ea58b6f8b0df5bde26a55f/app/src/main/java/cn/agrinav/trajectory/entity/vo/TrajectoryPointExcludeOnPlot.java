package cn.agrinav.trajectory.entity.vo;
import java.util.ArrayList;
import cn.agrinav.trajectory.entity.Point;
import lombok.Data;
/**
 * @author 11230
 */
@Data
public class TrajectoryPointExcludeOnPlot {
    private String id;
    private String trajectoryInfoId;
    /**TODO:这里说是不会用到的地方，内容当时看到的是null，说是不会用到直接就用了String*/
    private String points;
    private String times;
    ArrayList<Point> pointsExclude;
}
