package cn.agrinav.measure.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.measure.entity.LandPlot;
import cn.agrinav.measure.utils.PolygonUtils;
import cn.agrinav.measure.utils.UploadUtils;
import cn.agrinav.trajectory.util.DataUtil;

/**
 * @author 18506
 */
public class PlotInfoActivity extends AppCompatActivity {

    private ImageButton imageViewBack;
    private Button buttonExport;
    private LandPlot plotInfo;
    private AMap aMap;
    private TextView tvArea;
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_plot_info);
        //先初始化sdk的权限检测
        initSdkPermission();
        //获取点击的地块信息
        initIntent();
        //初始化控件
        initViews(savedInstanceState);
        //点击事件
        initEvents();
    }

    private void initSdkPermission() {
        AMapLocationClient.updatePrivacyAgree(this,true);
        AMapLocationClient.updatePrivacyShow(this,true,true);
    }

    private void initIntent() {
        Intent intent = getIntent();
        plotInfo =(LandPlot) intent.getSerializableExtra("plotInfo");
        Log.i("plotInfo",plotInfo.toString());
    }

    private void initEvents() {
        tvArea.setText(String.valueOf(plotInfo.getLandArea()));
        //返回事件
        imageViewBack.setOnClickListener(v -> {
            Intent intent = new Intent(this, BaseActivity.class);
            startActivity(intent);
        });
        //导出事件
        buttonExport.setOnClickListener(v -> {
            //TODO 导出的逻辑
        });
    }

    private void initViews(Bundle savedInstanceState) {
        tvArea = findViewById(R.id.tv_area);
        mapView = findViewById(R.id.mapViewPlot);
        mapView.onCreate(savedInstanceState);
        //获取返回按钮
        imageViewBack = findViewById(R.id.imageViewBack);
        //获取导出按钮
        buttonExport = findViewById(R.id.buttonExport);
        initMap();
    }

    private void initMap() {
        if (aMap == null) {
            aMap = mapView.getMap();
        }
        
        ArrayList<LatLng> latLngList = new DataUtil(this).decodeLandPlotBoundary(plotInfo.getBoundary());
        LatLng polygonCenterPoint = PolygonUtils.getPolygonCenterPoint(latLngList);
        
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(polygonCenterPoint, 17);
        aMap.moveCamera(cameraUpdate);
        aMap.getUiSettings().setZoomControlsEnabled(false);
        
        aMap.clear(); // 清除之前的所有覆盖物
        
        aMap.addPolyline(new PolylineOptions()
                .addAll(latLngList)
                .width(getResources().getInteger(R.integer.trajectory_line_size))
                .color(getColor(R.color.plot)))
                .setGeodesic(true);

        List<LatLng> latLngs = UploadUtils.reverseTransformation(plotInfo.getBoundary());
        PolygonUtils.adjustCameraToPolygon(aMap, latLngs);
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}
