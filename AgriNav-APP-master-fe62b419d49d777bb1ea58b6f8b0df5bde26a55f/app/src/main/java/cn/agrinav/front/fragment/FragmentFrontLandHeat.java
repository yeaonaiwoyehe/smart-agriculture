package cn.agrinav.front.fragment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.Gradient;
import com.amap.api.maps.model.HeatmapTileProvider;
import com.amap.api.maps.model.TileOverlay;
import com.amap.api.maps.model.TileOverlayOptions;
import com.amap.api.maps.model.LatLng;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import cn.agrinav.R;
import cn.agrinav.front.entity.HeatmapData;
import cn.agrinav.front.util.CommonUtils;
import cn.agrinav.front.util.QueryUtils;
import cn.agrinav.measure.utils.DefaultInfoUtils;
import cn.agrinav.measure.utils.ThreadPoolUtils;

/**
 * @author 18506
 */
public class FragmentFrontLandHeat extends MyFragment {
    private AppCompatActivity activity;
    private SharedPreferences sharedPreferences;
    private AMap aMap;
    private TileOverlay heatmapOverlay;
    private Handler handler = new Handler(Looper.getMainLooper());

    private SmartRefreshLayout refreshLayout;

    public FragmentFrontLandHeat(){

    }
    public FragmentFrontLandHeat(SoftReference<AppCompatActivity> activityWeakReference, SharedPreferences sharedPreferences){
        this.sharedPreferences = sharedPreferences;
        this.activity = activityWeakReference.get();
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.fragment_land_heat, null);
        ThreadPoolUtils.initThreadPool();
        initSdkPermission();
        MapView mapView = view.findViewById(R.id.map_land_plot);
        mapView.onCreate(savedInstanceState);

        // 初始化地图
        aMap = mapView.getMap();
        // 设置为普通地图（白色背景）
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);
        // 设置缩放级别
        aMap.moveCamera(CameraUpdateFactory.zoomTo(3));
        // 获取热力图数据并显示
        fetchHeatmapData();
        return view;
    }
    private void initSdkPermission() {
        AMapLocationClient.updatePrivacyAgree(activity,true);
        AMapLocationClient.updatePrivacyShow(activity,true,true);
    }
    //不写loadMore方法
    @Override
    public void onLoadMore(SmartRefreshLayout layout) {
        refreshLayout = layout;
        refreshLayout.finishLoadMore();
        refreshLayout.setEnableLoadMore(false);
    }

    private void fetchHeatmapData() {
        // TODO: 实现网络请求获取热力图数据
        // 这里应该调用你的API获取数据
        // 获取数据后调用 createHeatmap(data)
        ThreadPoolUtils.threadPoolExecutor.execute(() -> {
            List<HeatmapData> heatmapDataList = QueryUtils.getHeatMap(getString(R.string.server_url), getString(R.string.measure_url)+getString(R.string.heatmap), DefaultInfoUtils.getToken(activity));
            CommonUtils.waitDataGetting(heatmapDataList, activity);
            handler.post(()->{
                createHeatmap(heatmapDataList);
            });
        });
    }

    private void createHeatmap(List<HeatmapData> data) {
        // 转换数据格式
        List<LatLng> heatmapData = new ArrayList<>();
        for (HeatmapData item : data) {
            heatmapData.add(new LatLng(
                Double.parseDouble(item.getLatitude()),
                Double.parseDouble(item.getLongitude())
            ));
        }
        // 创建热力图Provider
        HeatmapTileProvider.Builder builder = new HeatmapTileProvider.Builder();
        builder.data(heatmapData)
               .gradient(createGradient())
               .radius(25);
        // 添加热力图到地图
        if (heatmapOverlay != null) {
            heatmapOverlay.remove();
        }
        heatmapOverlay = aMap.addTileOverlay(new TileOverlayOptions()
                .tileProvider(builder.build()));
    }

    private Gradient createGradient() {
        // 定义渐变色数组，使用 ARGB 格式来控制透明度
        int[] colors = new int[] {
            Color.parseColor("#FFFFFF"),  // 白色
            Color.parseColor("#000080"),  // 深蓝色
            Color.parseColor("#006600"),  // 深绿色
            Color.parseColor("#B2FF66"),  // 亮绿色
            Color.parseColor("#FFFF00"),  // 黄色
            Color.parseColor("#FF8000"),  // 橙色
            Color.parseColor("#FF0000")   // 红色
        };
        // 定义渐变开始位置
        float[] startPoints = new float[] {
            0f, 0.2f, 0.4f, 0.6f, 0.7f, 0.8f, 1.0f
        };
        
        return new Gradient(colors, startPoints);
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}

