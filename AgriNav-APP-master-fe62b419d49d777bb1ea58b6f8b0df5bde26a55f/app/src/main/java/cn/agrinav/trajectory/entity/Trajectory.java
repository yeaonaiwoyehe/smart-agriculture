package cn.agrinav.trajectory.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Data;
import lombok.NonNull;

/**
 * @author 11230
 */
@Entity
@Data
public class Trajectory {
    /**
     * 轨迹Id
     * 用于和轨迹位置表关联
     * 这个id仅用于和位置表关联
     * 同时是查询本机自己的作业轨迹的时候使用的
     * 在用户上传轨迹的时候通过轨迹id去寻找轨迹位置
     */
    @PrimaryKey(autoGenerate = false)
    @NonNull
    @androidx.annotation.NonNull
    private String trajectoryInfoId;
    /**轨迹的开始时间*/
    private String startTime;
    /**轨迹的结束时间*/
    private String endTime;
    /**作物*/
    private String crop;
    /**作业类型*/
    private String workType;
    /**宽度*/
    private int width;
    /** 是否已上传 */
    private Boolean isUpload;
}
