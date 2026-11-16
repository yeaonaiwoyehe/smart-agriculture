package cn.agrinav.trajectory.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import cn.agrinav.trajectory.entity.TrajectoryPosition;

/**
 * @author 11230
 */
@Dao
public interface TrajectoryPositionDao {
    @Insert
    void insert(TrajectoryPosition trajectoryPositions);
    /**查询全部的时候要在里面写sql语句，表名用类名来代替，UserInfo*/
    @Query("SELECT * FROM TrajectoryPosition")
    List<TrajectoryPosition> queryAll();
    @Query("SELECT * FROM TrajectoryPosition WHERE trajectoryId = :trajectoryId order by localDate")
    List<TrajectoryPosition>queryById(String trajectoryId);
    /**默认修改以主键为条件的修改*/
    @Update
    void update(TrajectoryPosition... trajectoryPosition);
    /**删除*/
    @Delete
    void delete(TrajectoryPosition trajectoryPosition);

    /**与其说带参数的query是用于查询的不如说是执行sql语句的*/
    @Query("DELETE FROM TrajectoryPosition")
    void deleteAll();

    /**
     * 删除指定轨迹下的轨迹点
     * @param trajectoryId 轨迹id
     */
    @Query("DELETE FROM TrajectoryPosition WHERE trajectoryId = :trajectoryId")
    void deleteByTrajectoryId(String trajectoryId);
}
