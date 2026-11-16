package cn.agrinav.trajectory.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.LocalDate;
import java.util.Date;

import lombok.Data;

/**
 * @author 11230
 * 这个是用户存入本地数据库的映射实体
 */
@Data
@Entity
public class TrajectoryPosition {
    @PrimaryKey(autoGenerate = true)
    private Integer positionId;
    private Double latitude;
    private Double longitude;
    private String localDate;
    private String trajectoryId;
}
