package cn.agrinav.front.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import cn.agrinav.front.database.dao.LandPlotDao;
import cn.agrinav.front.database.entity.LandPlot;

/**
 * @author 11230
 */
@Database(entities = {LandPlot.class},version = 1,exportSchema = false)
public abstract class AbstractPlotDetailDatabase extends RoomDatabase {
    private static AbstractPlotDetailDatabase instance;
    /**实例化单例模式*/
    public static AbstractPlotDetailDatabase getInstance(Context context) {
        if (instance==null){
            //要初始化数据库实例，并且给这个数据库起个名字
            instance = Room.databaseBuilder(context.getApplicationContext(),AbstractPlotDetailDatabase.class,"landPlotDetail.db").build();
        }
        return instance;
    }

    /**
     * 获取坐标dao
     * @return 轨迹坐标dao
     */
    public abstract LandPlotDao getLandPlotDao();

}
