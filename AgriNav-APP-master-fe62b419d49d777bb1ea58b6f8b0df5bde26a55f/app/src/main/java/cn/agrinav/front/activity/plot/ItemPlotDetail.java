package cn.agrinav.front.activity.plot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BaseHoleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.PolygonHoleOptions;
import com.amap.api.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.agrinav.R;
import cn.agrinav.front.database.AbstractPlotDetailDatabase;
import cn.agrinav.front.database.entity.LandPlot;
import cn.agrinav.trajectory.util.DataUtil;
import cn.agrinav.utils.ToastUtil;

/**
 * @author 11230
 */
public class ItemPlotDetail extends AppCompatActivity {

    private MapView mapView;
    private AMap map;
    private long plotInfo;
    private AbstractPlotDetailDatabase instance;
    private LandPlot landPlot;
    private ThreadPoolExecutor threadPoolExecutor;
    private TextView tvArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_item_plot_detail);
        //首先初始化地图组件
        initViews();
        mapView.onCreate(savedInstanceState);
        initData();
        //然后显示地图上的地块信息
    }

    private void initData() {
        Intent intent = getIntent();
        plotInfo = intent.getLongExtra(getString(R.string.font_plot_detail_list_intent_plot_id),0);
        threadPoolExecutor = new ThreadPoolExecutor(
                10,
                20,
                10,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private void initViews() {
        mapView = findViewById(R.id.map_plot_detail);
        tvArea = findViewById(R.id.tv_plot_detail_area);
        if (mapView != null) {
            map = mapView.getMap();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //在这里获取到信息并且重置图像的位置以及绘制地块
        threadPoolExecutor.execute(()->{
            instance = AbstractPlotDetailDatabase.getInstance(this);
            if (landPlot==null) {
                //首先获取点
                landPlot = instance.getLandPlotDao().queryById(plotInfo);
                runOnUiThread(()->{
                    tvArea.setText(String.valueOf(landPlot.getLandArea()));
                });
                if (landPlot!=null) {
                    //解析landPlot中的点
                    new DataUtil(this).asyncLandPlot(landPlot, landPlotBoundaryList -> {
                        if (!landPlotBoundaryList.isEmpty()) {
                        //然后判断有几块内容
                            List<BaseHoleOptions> list = new ArrayList<>();
                            if (landPlotBoundaryList.size()>1){
                                for (int i = 1; i < landPlotBoundaryList.size(); i++){
                                    PolygonHoleOptions polygonHoleOptions = new PolygonHoleOptions();
                                    polygonHoleOptions.addAll(landPlotBoundaryList.get(i));
                                    list.add(polygonHoleOptions);
                                }
                                initMapContent(landPlotBoundaryList.get(0),list);
                            }else {
                                initMapContent(landPlotBoundaryList.get(0));
                            }
                        }else{
                            runOnUiThread(() ->{
                                new ToastUtil().getToast(ItemPlotDetail.this,"该地块信息为空");
                            });
                        }
                    });
                }else{
                    new ToastUtil().getToast(this,"出错了，请重新打开该页面");
                }
            }
        });
    }

    private void initMapContent(ArrayList<LatLng> list) {
//        首先计算大致面积来计算应该有的大小
        // 步骤 1: 计算中心点
        LatLng centerPoint = getLatLng(list);
        // 步骤 2 和 3: 计算每个点到中心点的距离，并找到最大距离
        float maxDistance = 0;
        float distance = 0;
        for (LatLng latLng : list) {
            distance = AMapUtils.calculateLineDistance(latLng,centerPoint);
            if (distance > maxDistance) {
                maxDistance = distance;
            }
        }
        //移动视角到轨迹中心点(伪中心点)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(centerPoint, calculateZoomLevel(maxDistance)));
        map.getUiSettings().setZoomControlsEnabled(true);
        //绘制地块到地图上
        PolygonOptions polygonOptions = new PolygonOptions();
        // 添加 多边形的每个顶点（顺序添加）
        polygonOptions.addAll(list);
        // 多边形的边框
        // 边框颜色
        // 多边形的填充色
        polygonOptions.strokeWidth(getResources().getInteger(R.integer.trajectory_line_size))
                .strokeColor(getColor(R.color.plot))
                .fillColor(getColor(R.color.plot_fill));
        map.addPolygon(polygonOptions);
    }
    private void initMapContent(ArrayList<LatLng> list, List<BaseHoleOptions> listHoleOptions) {
//        首先计算大致面积来计算应该有的大小
        // 步骤 1: 计算中心点
        LatLng centerPoint = getLatLng(list);
        // 步骤 2 和 3: 计算每个点到中心点的距离，并找到最大距离
        float maxDistance = 0;
        float distance = 0;
        for (LatLng latLng : list) {
            distance = AMapUtils.calculateLineDistance(latLng,centerPoint);
            if (distance > maxDistance) {
                maxDistance = distance;
            }
        }
        //移动视角到轨迹中心点(伪中心点)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(centerPoint, calculateZoomLevel(maxDistance)));
        map.getUiSettings().setZoomControlsEnabled(true);
        //绘制地块到地图上
        PolygonOptions polygonOptions = new PolygonOptions();
        // 添加 多边形的每个顶点（顺序添加）
        polygonOptions.addAll(list);
        // 多边形的边框
        // 边框颜色
        // 多边形的填充色
        polygonOptions.strokeWidth(getResources().getInteger(R.integer.trajectory_line_size))
                .strokeColor(getColor(R.color.plot))
                .fillColor(getColor(R.color.plot_fill))
                .setHoleOptions(listHoleOptions);
        map.addPolygon(polygonOptions);
    }
    /**
     * 计算中心点
     * @param list 要计算的点的集合
     * @return 中心带你
     */
    private static @NonNull LatLng getLatLng(ArrayList<LatLng> list) {
        double centerLatitude = 0;
        double centerLongitude = 0;
        for (LatLng latLng : list){
            centerLatitude += latLng.latitude;
            centerLongitude += latLng.longitude;
        }
        centerLatitude /= list.size();
        centerLongitude /= list.size();
        return new LatLng(centerLatitude, centerLongitude);
    }
    /**
     * 根据给定的距离计算缩放级别。
     *
     * @param distance 距离，单位为米（m）或千米（km），需要根据实际情况调整参数类型或单位处理
     * @return 缩放级别，如果距离不在已知范围内，则返回-1表示无效
     */
    public static int calculateZoomLevel(double distance) {
        // 注意：这里假设输入的距离单位是米。如果输入是千米，需要乘以1000。
        if (distance >= 0 && distance < 10) {
            // 注意：由于您给出的数据中最小距离是10m，所以这里实际上不会匹配到任何值，但为了完整性还是保留。
            // 如果需要处理小于10m的情况，请添加相应的逻辑。
            return 20;
        } else if (distance < 25) {
            // 对于10m-25m（不包括25m），缩放级别为19-20，这里取上限20（根据实际需求调整）
            return 20;
        } else if (distance < 50) {
            return 19;
        } else if (distance < 100) {
            return 18;
        } else if (distance < 200) {
            return 17;
        } else if (distance < 500) {
            return 16;
        } else if (distance < 1000) {
            return 15;
        } else if (distance < 2000) {
            return 14;
        } else if (distance < 5000) {
            return 13;
        } else if (distance < 10000) {
            return 12;
        } else if (distance < 20000) {
            return 11;
        } else if (distance < 30000) {
            return 10;
        } else if (distance < 50000) {
            return 9;
        } else if (distance < 100000) {
            return 8;
        } else if (distance < 200000) {
            return 7;
        } else if (distance < 500000) {
            return 6;
        } else if (distance < 1000000) {
            return 5;
        } else {
            // 如果距离超过1000km，根据需求可以返回特定值或继续扩展范围
            // 无效距离（或根据需求返回其他值）
            return 5;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}