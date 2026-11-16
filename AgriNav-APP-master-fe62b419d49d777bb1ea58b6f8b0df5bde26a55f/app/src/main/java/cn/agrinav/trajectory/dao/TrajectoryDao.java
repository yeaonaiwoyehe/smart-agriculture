package cn.agrinav.trajectory.dao;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;
import cn.agrinav.trajectory.entity.Trajectory;

/**
 * @author 11230
 */
@Dao
public interface TrajectoryDao {
    @Insert
    void insert(Trajectory... trajectory);

    /**
     * 查询全部的时候要在里面写sql语句，表名用类名来代替，UserInfo
     * @return List
     */
    @Query("SELECT * FROM Trajectory ORDER BY startTime DESC")
    List<Trajectory> queryAll();

    /**
     * 默认修改以主键为条件的修改
     * @param trajectoryInfoId 轨迹id
     * @param endTime 结束时间
     */
    @Query("UPDATE Trajectory SET endTime =:endTime WHERE trajectoryInfoId = :trajectoryInfoId")
    void updateEndTime(String trajectoryInfoId,String endTime);

    /**
     * 根据轨迹id更新轨迹
     * @param trajectoryInfoId 轨迹id
     * @param isUpload 更新上传状态
     */
    @Query("UPDATE Trajectory SET isUpload =:isUpload WHERE trajectoryInfoId = :trajectoryInfoId")
    void updateState(String trajectoryInfoId,Boolean isUpload);

    /**
     * 删除轨迹
     * @param trajectory 轨迹
     */
    @Delete
    void delete(Trajectory trajectory);
}
