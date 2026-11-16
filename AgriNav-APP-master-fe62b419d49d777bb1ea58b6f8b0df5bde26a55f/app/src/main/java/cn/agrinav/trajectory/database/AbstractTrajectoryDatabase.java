package cn.agrinav.trajectory.database;
import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import cn.agrinav.trajectory.dao.TrajectoryDao;
import cn.agrinav.trajectory.dao.TrajectoryPositionDao;
import cn.agrinav.trajectory.dao.WorkTrajectoryPositionDao;
import cn.agrinav.trajectory.entity.Trajectory;
import cn.agrinav.trajectory.entity.TrajectoryPosition;
import cn.agrinav.trajectory.entity.vo.WorkTrajectoryPosition;

/**
 * @author 11230
 * 这个表示的是数据库类，
 * 映射实体是position,里面是一个一个的点
 * 另一个映射实体是Trajectory,里面是一个一个的轨迹
 */
@Database(entities = {TrajectoryPosition.class, Trajectory.class, WorkTrajectoryPosition.class},version = 2,exportSchema = false)
public abstract class AbstractTrajectoryDatabase extends RoomDatabase {
    private static AbstractTrajectoryDatabase instance;
    /**实例化单例模式*/
    public static AbstractTrajectoryDatabase getInstance(Context context) {
        if (instance==null){
            //要初始化数据库实例，并且给这个数据库起个名字
            synchronized (AbstractTrajectoryDatabase.class) {
                instance = Room.databaseBuilder(context.getApplicationContext(),AbstractTrajectoryDatabase.class,"trajectory.db")
                        .addMigrations(MIGRATION_1_2)
                        .build();
            }
        }
        return instance;
    }

    /**
     * 获取坐标dao
     * @return 轨迹坐标dao
     */
    public abstract TrajectoryPositionDao getTrajectoryPositionDao();
    /**
     * 获取轨迹dao
     * @return 轨迹记录dao
     * */
    public abstract TrajectoryDao getTrajectoryDao();
    /**
     * 获取轨迹记录dao
     * @return 作业轨迹记录dao
     * */
    public abstract WorkTrajectoryPositionDao getWorkTrajectoryPositionDao();
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // 在这里定义从版本 1 到版本 2 的迁移逻辑
            // 创建新的表
            database.execSQL("ALTER TABLE `WorkTrajectoryPosition` ADD COLUMN `excludePlotId` TEXT");
            database.execSQL("ALTER TABLE `WorkTrajectoryPosition` ADD COLUMN `inExcludePlot` BOOLEAN DEFAULT FALSE NOT NULL");
        }
    };
}
