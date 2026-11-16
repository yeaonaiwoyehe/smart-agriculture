package cn.agrinav.trajectory.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Upsert;

import java.util.ArrayList;
import java.util.List;

import cn.agrinav.trajectory.entity.TrajectoryPosition;
import cn.agrinav.trajectory.entity.vo.WorkTrajectoryPosition;

/**
 * @author 11230
 */
@Dao
public interface WorkTrajectoryPositionDao {
    @Upsert
    void insert(ArrayList<WorkTrajectoryPosition> workTrajectoryPositions);
    @Insert
    void insert(WorkTrajectoryPosition workTrajectoryPosition);
    /**查询全部的时候要在里面写sql语句，表名用类名来代替，UserInfo*/
    @Query("SELECT * FROM WorkTrajectoryPosition")
    List<WorkTrajectoryPosition> queryAll();
    @Query("SELECT * FROM WorkTrajectoryPosition WHERE trajectoryId = :trajectoryId order by time")
    List<WorkTrajectoryPosition>queryByTrajectoryId(String trajectoryId);
    /**默认修改以主键为条件的修改*/
    @Update
    void update(WorkTrajectoryPosition... workTrajectoryPosition);
    /**删除*/
    @Delete
    void delete(WorkTrajectoryPosition workTrajectoryPosition);

    /**与其说带参数的query是用于查询的不如说是执行sql语句的*/
    @Query("DELETE FROM WorkTrajectoryPosition")
    void deleteAll();
}
