package cn.agrinav.front.database.dao;

import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.ArrayList;
import java.util.List;


import cn.agrinav.front.database.entity.LandPlot;

/**
 * @author 11230
 */
@Dao
public interface LandPlotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ArrayList<LandPlot> landPlots);
    @Delete
    void delete(LandPlot landPlot);
    @Query("SELECT * FROM LandPlot")
    List<LandPlot> queryAll();
    @Query("SELECT * FROM LandPlot WHERE id = :id")
    LandPlot queryById(Long id);
    @Update
    void update(LandPlot... landPlot);

}
