package cn.agrinav.trajectory.activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.GridView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import cn.agrinav.R;
import cn.agrinav.trajectory.adapter.ReloadAdapter;
import cn.agrinav.trajectory.dao.TrajectoryDao;
import cn.agrinav.trajectory.dao.TrajectoryPositionDao;
import cn.agrinav.trajectory.database.AbstractTrajectoryDatabase;
import cn.agrinav.trajectory.entity.Trajectory;

/**
 * @author 11230
 */
public class TrajectoryReuploadActivity extends AppCompatActivity {

    private GridView gridView;
    private TrajectoryDao trajectoryDao;
    private ThreadPoolExecutor threadPoolExecutor;
    private Handler handler;
    private final int DELETE_POSITION = 1;
    private TrajectoryPositionDao trajectoryPositionDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trajectory_reupload);
        initViews();
        threadPoolExecutor = new ThreadPoolExecutor(
                 10,
                 20,
                 10,
                 TimeUnit.SECONDS,
                 new ArrayBlockingQueue<>(20),
                 new ThreadPoolExecutor.CallerRunsPolicy()
         );
        threadPoolExecutor.execute(()->{
            Looper.prepare();
            handler = new Handler(Looper.myLooper()){
                @Override
                public void handleMessage(@NonNull Message msg) {
                   if (msg.what==DELETE_POSITION){
                       //删除这个轨迹
                       trajectoryDao.delete((Trajectory) msg.obj);
                       trajectoryPositionDao.deleteByTrajectoryId(((Trajectory) msg.obj).getTrajectoryInfoId());
                   }
                }
            };
            Looper.loop();
        });
        trajectoryDao = AbstractTrajectoryDatabase.getInstance(this).getTrajectoryDao();
        trajectoryPositionDao = AbstractTrajectoryDatabase.getInstance(this).getTrajectoryPositionDao();
    }

    @Override
    protected void onStart() {
        super.onStart();
        threadPoolExecutor.execute(()->{
            //获取当前数据库中的所有trajectory
            List<Trajectory> trajectoryList = trajectoryDao.queryAll();
            List<Trajectory> trajectories = new ArrayList<>();
            //获取到三天内的数据
            if (trajectoryList!=null){
                for (Trajectory trajectory : trajectoryList) {
                    //判断是否超过三天
                    if(trajectory.getEndTime()!=null) {
                        if ((System.currentTimeMillis() - (LocalDateTime.parse(trajectory.getEndTime()).toInstant(ZoneOffset.of("+8")).toEpochMilli()) >= 1000 * 60 * 60 * 24 * 3)) {
                            //超过三天发送给一个子线程处理这个事情
                            handler.sendMessage(handler.obtainMessage(DELETE_POSITION, trajectory));
                        } else {
                            trajectories.add(trajectory);
                        }
                    }
                }
            }
            runOnUiThread(()->{
                ReloadAdapter reloadAdapter = new ReloadAdapter(R.layout.item_reload_trajectory,trajectories,this);
                gridView.setAdapter(reloadAdapter);
            });
        });
    }

    public void initViews(){
        gridView = findViewById(R.id.list_trajectory_reload);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().setStatusBarColor(Color.rgb(9,204,125));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}