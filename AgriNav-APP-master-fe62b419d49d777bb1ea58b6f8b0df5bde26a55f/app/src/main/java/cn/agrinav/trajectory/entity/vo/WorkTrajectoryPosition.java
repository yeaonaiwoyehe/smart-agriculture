package cn.agrinav.trajectory.entity.vo;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.Insert;
import androidx.room.PrimaryKey;

import lombok.Data;

/**
 * @author 11230
 */
@Data
@Entity(indices = { @Index(value = {"trajectoryId","latitude","longitude","time"}, unique = true)})
public class WorkTrajectoryPosition {
    @PrimaryKey(autoGenerate = true)
    private Integer positionId;
    private Double latitude;
    private Double longitude;
    private String time;
    private String trajectoryId;
    private Long plotId;
    private Double area;
    private String excludePlotId;
    private Boolean inExcludePlot;
}
