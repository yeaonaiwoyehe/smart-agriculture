package cn.agrinav.measure.activity;

import static com.amap.api.maps.AMapOptions.ZOOM_POSITION_RIGHT_CENTER;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BaseHoleOptions;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonHoleOptions;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;


import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.front.entity.LandPlotExclude;
import cn.agrinav.measure.entity.LandPlot;
import cn.agrinav.measure.entity.MyPoint;
import cn.agrinav.measure.fragment.FragmentMeasure;
import cn.agrinav.measure.utils.DefaultInfoUtils;
import cn.agrinav.measure.utils.DialogUtils;
import cn.agrinav.measure.utils.PermissionMeasure;
import cn.agrinav.measure.utils.PolygonUtils;
import cn.agrinav.measure.utils.UploadUtils;
import cn.agrinav.user.entity.SysUser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 测量活动类
 * @author 18506
 */
public class MeasureActivity extends AppCompatActivity implements AMap.OnMapClickListener{
    // 结束测量按钮
    private Button buttonEndMeasure;
    // 高德地图对
    private AMap aMap;
    // 处理消息的Handler
    private Handler handler;
    // 插花地
    private LandPlotExclude landPlotExclude ;
    private List<LandPlotExclude> landPlotExcludeList = new ArrayList<>();
    // 自动定位客户端
    private AMapLocationClient autoLocationClient = null;
    // 手动定位客户端
    private AMapLocationClient handLocationClient = null;
    // 定位选项
    private AMapLocationClientOption locationOption;
    // 经纬度对象
    private LatLng latLng;

    // 自定义的点对象
    private MyPoint myPoint;
    // 存储定位的点列表
    private final List<MyPoint> myPoints = new ArrayList<>();
    private boolean auto = false;
    private boolean hand = false;
    private MapView mapView;

    private PolygonOptions polygonOptions = new PolygonOptions();

    private PolylineOptions polylineOptions = new PolylineOptions();

    private Polygon polygon;
    private Polyline polyline;

    private List<BaseHoleOptions> holeOptionsList =new ArrayList<>();

    private List<LatLng> holePoints = new ArrayList<>();
    /**
     * 自动定位的监听器
     */
    public AMapLocationListener mLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {
            if (aMapLocation != null) {
                if (aMapLocation.getErrorCode() == 0) {
                    myPoint = new MyPoint(aMapLocation.getLatitude(), aMapLocation.getLongitude(), LocalDateTime.now());
                    latLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
                   if (!isInterspersed){
                       polygonOptions.add(latLng).strokeColor(getColor(R.color.plot)).strokeWidth(20).fillColor(getColor(R.color.plot_fill));
                       //设置打点样式
                       if(polygonOptions.getPoints().size()>3){
                           if (polygon!=null){
                               polygon.remove();
                           }
                           if (polyline!=null) {
                               polyline.remove();
                           }
                           polygon = aMap.addPolygon(polygonOptions);
                       }else {
                           polylineOptions.add(latLng).width(20).color(getColor(R.color.plot));
                           polyline = aMap.addPolyline(polylineOptions);
                       }
                       Marker marker = aMap.addMarker(new MarkerOptions().position(latLng));
                       setMarkerStyle(marker);
                       marker.showInfoWindow();
                       myPoints.add(myPoint);
                   }else{
                       //判断之前是否传过
                       if (polygonOptions.getHoleOptions()!=null){
                           polygonOptions.setHoleOptions(null);
                       }
                       PolygonHoleOptions holeOptions = new PolygonHoleOptions();
                       holePoints.add(latLng);
                       holeOptions.addAll(holePoints);
                       holeOptionsList.add(holeOptions);
                       polygonOptions.setHoleOptions(holeOptionsList);
                       aMap.addPolygon(polygonOptions);
                   }
                }
            }
        }
    };
    /**
     * 手动定位的监听器
     */
    public AMapLocationListener locationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {
            if (aMapLocation != null) {
                if (aMapLocation.getErrorCode() == 0) {
                   if (!isInterspersed){
                       myPoint = new MyPoint(aMapLocation.getLatitude(), aMapLocation.getLongitude(), LocalDateTime.now());
                       latLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
                       //设置打点样式
                       polygonOptions.add(latLng).strokeWidth(20).strokeColor(getColor(R.color.plot)).fillColor(getColor(R.color.plot_fill));
                       if (polygonOptions.getPoints().size()>=3){
                           if (polyline!=null){
                               polyline.remove();
                           }
                           if (polygon!=null){
                               polygon.remove();
                           }
                           polygon  = aMap.addPolygon(polygonOptions);
                       }else{
                           polylineOptions.add(latLng).width(20).color(getColor(R.color.plot));
                           polyline = aMap.addPolyline(polylineOptions);
                       }
                       Marker marker = aMap.addMarker(new MarkerOptions().position(latLng));
                       setMarkerStyle(marker);
                       marker.showInfoWindow();
                       myPoints.add(myPoint);
                   }
                }
            }
        }
    };
    private Marker centerMarker;

    private void setMarkerStyle(Marker marker) {
        Drawable drawable = ContextCompat.getDrawable(MeasureActivity.this, R.mipmap.marker);
        if (drawable!= null) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.marker);
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
            marker.setIcon(bitmapDescriptor);
        }
    }
    private Button buttonPause;
    // 圈地边界
    private String boundary;
    // 土地地块对象
    private LandPlot landPlot;
    // 用户对象
    private SysUser user;
    // 手动开始按钮
    private Button buttonHandStart;

    private boolean isInterspersed = false;

    /**
     * 活动创建时调用
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_measure);
        initSdkPermission();
        initData();
        initHandler();
        initViews(savedInstanceState);
        server();
    }

    private void initData() {
        Intent intent = getIntent();
        landPlot = (LandPlot) intent.getSerializableExtra("LandPlot");
        user = DefaultInfoUtils.getUserData(this);
    }


    /**
     * 服务器相关操作
     */
    private void server() {
        boolean allowTargeting = PermissionMeasure.isAllowTargeting(this);
        boolean allowWifi = PermissionMeasure.isAllowWifi(this);
        if (allowTargeting && allowWifi) {
            checkTargetingPermissions();
        } else {
            if (!allowTargeting) {
                Toast.makeText(this, "未开启定位功能", Toast.LENGTH_SHORT).show();
            }
            if (!allowWifi) {
                Toast.makeText(this, "未开启wifi功能", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 检查定位权限
     */
    private void checkTargetingPermissions() {
        ArrayList<String> permissions = null;
        for (String permission : PermissionMeasure.PERMISSIONS_LOCATION) {
            if (ContextCompat.checkSelfPermission(this, permission)!= PackageManager.PERMISSION_GRANTED) {
                if (permissions == null) {
                    permissions = new ArrayList<>();
                }
                permissions.add(permission);
            }
        }
        if (permissions!= null) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), getResources().getInteger(R.integer.permission_trajectory_request_code));
        } else {
            ifHavePermission();
        }
    }

    /**
     * 处理已拥有权限的情况
     */
    private void ifHavePermission() {
        try {
            initPosition();
        } catch (Exception e) {
            Log.e(getResources().getString(R.string.trajectory_tag), e.toString());
        }
    }

    /**
     * 初始化位置相关
     */
    private void initPosition() {
        try {
            autoLocationClient = new AMapLocationClient(getApplicationContext());
            handLocationClient = new AMapLocationClient(getApplicationContext());
            initEvents();
            setMap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置地图相关
     */
    private void setMap() {
        initMyLocationStyle();
    }

    /**
     * 初始化自动定位的选项
     */
    private void initLocationOptions() {
        locationOption = new AMapLocationClientOption();
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        if (autoLocationClient!= null) {
            initContinueLocationOptions(locationOption);
            autoLocationClient.setLocationOption(locationOption);
            autoLocationClient.stopLocation();
            autoLocationClient.startLocation();
        }
    }

    /**
     * 初始化手动定位的选项
     */
    private void initMaLocationOptions() {
        locationOption = new AMapLocationClientOption();
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        if (handLocationClient!= null) {
            initSingleLocationOptions(locationOption);
            handLocationClient.setLocationOption(locationOption);
            handLocationClient.stopLocation();
            handLocationClient.startLocation();
        }
    }

    /**
     * 初始化持续定位选项
     * @param mLocationOption 定位选项对象
     */
    private void initContinueLocationOptions(AMapLocationClientOption mLocationOption) {
        mLocationOption.setInterval(5000);
        mLocationOption.setHttpTimeOut(30000);
    }

    /**
     * 初始化单次定位选项
     * @param mLocationOption 定位选项对象
     */
    private void initSingleLocationOptions(AMapLocationClientOption mLocationOption) {
        mLocationOption.setOnceLocation(true);
        mLocationOption.setLocationCacheEnable(false);
    }

    /**
     * 初始化事件
     */
    private void initEvents() {
        if ("圈地".equals(landPlot.getMeasurementMethod())) {
            initEnclosureMode();
            Toast.makeText(this, "已开启圈地模式", Toast.LENGTH_SHORT).show();
            switch (landPlot.getLandType()) {
                case "新增":
                    handleNormalMeasurement();
                    break;
                case "复测":
                    handleRemeasurement();
                    break;
                case "插花地":
                    handleEnclaveMeasurement();
                    break;
                default:
                    handleNormalMeasurement();
                    break;
            }
        } else if ("手动".equals(landPlot.getMeasurementMethod())) {
            initManualMode();
            Toast.makeText(this, "请按开始按钮打点", Toast.LENGTH_SHORT).show();
        } else if ("自动".equals(landPlot.getMeasurementMethod())) {
            initAutomaticMode();
            Toast.makeText(this, "已开启自动打点模式", Toast.LENGTH_SHORT).show();
        }
        buttonHandStart.setOnClickListener(v -> {
            handLocationClient.setLocationListener(locationListener);
            handLocationClient.startLocation();
        });

    }

    private void initEnclosureMode() {
        // 停止自动定位
        autoLocationClient.stopLocation();
        // 停止手动定位
        handLocationClient.stopLocation();
        // 设置手动按钮模式
        buttonHandStart.setVisibility(View.INVISIBLE);
        buttonHandStart.setEnabled(false);
        aMap.setOnMapClickListener(this);

        // 停止位置更新并禁用我的位置图层
        if (autoLocationClient != null) {
            autoLocationClient.stopLocation();
        }
        if (handLocationClient != null) {
            handLocationClient.stopLocation();
        }
        aMap.setMyLocationEnabled(false);

        // 允许用户自由移动地图
        aMap.getUiSettings().setAllGesturesEnabled(true);

        // 获取一次位置并移动相机
        getLocationOnce();
    }

    private void getLocationOnce() {
        try {
            AMapLocationClient locationClient = new AMapLocationClient(getApplicationContext());
            AMapLocationClientOption locationOption = new AMapLocationClientOption();
            locationOption.setOnceLocation(true);
            locationOption.setNeedAddress(false);
            locationClient.setLocationOption(locationOption);
            
            locationClient.setLocationListener(aMapLocation -> {
                if (aMapLocation != null && aMapLocation.getErrorCode() == 0) {
                    LatLng latLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
                    aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 19));
                }
                locationClient.onDestroy();
            });
            
            locationClient.startLocation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initManualMode() {
        buttonHandStart.setVisibility(View.VISIBLE);
        buttonHandStart.setEnabled(true);
        initMaLocationOptions();
        hand = true;
        initMyLocationStyle();
    }

    private void initAutomaticMode() {
        buttonHandStart.setVisibility(View.INVISIBLE);
        buttonHandStart.setEnabled(false);

        initLocationOptions();
        auto = true;
        initMyLocationStyle();
        autoLocationClient.setLocationListener(mLocationListener);
        autoLocationClient.startLocation();
    }

    /**
     * 初始化我的位置样式
     */
    private void initMyLocationStyle() {
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        // 在圈地模式下，不使用跟随模式
        if (!"圈地".equals(landPlot.getMeasurementMethod())) {
            myLocationStyle.interval(500);
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW);
            aMap.setMyLocationEnabled(true);
        } else {
            // 圈地模式下不启用定位图层
            aMap.setMyLocationEnabled(false);
        }
        myLocationStyle.showMyLocation(true);
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);
    }

    /**
     * 初始化SDK权限
     */
    private void initSdkPermission() {
        AMapLocationClient.updatePrivacyAgree(this, true);
        AMapLocationClient.updatePrivacyShow(this, true, true);
    }

    /**
     * 初始化Handler
     */
    private void initHandler() {
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what >= 200 && msg.what <= 299) {
                    Toast.makeText(MeasureActivity.this, getResources().getString(R.string.success_upload), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MeasureActivity.this, BaseActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(getString(R.string.fragment_index), 1);
                    startActivity(intent);
                } else {
                    Toast.makeText(MeasureActivity.this, getResources().getString(R.string.error_upload)+","+msg.obj.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    /**
     * 初始化视图
     * @param savedInstanceState 保存的实例状态
     */
    private void initViews(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        buttonEndMeasure = findViewById(R.id.buttonEndMeasure);
        buttonHandStart = findViewById(R.id.buttonHandStart);
        buttonPause = findViewById(R.id.btn_pause);
        try {
            if (aMap == null) {
                aMap = mapView.getMap();
                if (aMap != null) {
                    CameraUpdate cameraUpdate = CameraUpdateFactory.zoomTo(19);
                    aMap.moveCamera(cameraUpdate);
                    aMap.getUiSettings().setZoomPosition(ZOOM_POSITION_RIGHT_CENTER);
                    aMap.getUiSettings().setScaleControlsEnabled(true);
                } else {
                    Toast.makeText(this, "地图初始化失败，请重试", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "地图初始化错误，请重试", Toast.LENGTH_SHORT).show();
        }
    }
    //圈地
    @Override
    public void onMapClick(LatLng latLng) {
        MyPoint myPoint = new MyPoint(latLng.latitude, latLng.longitude, LocalDateTime.now());
        if (!isInterspersed){
            //获取我所需要的点的信息
            polygonOptions.add(latLng).strokeColor(getColor(R.color.plot)).strokeWidth(20).fillColor(getColor(R.color.plot_fill));
            myPoints.add(myPoint);
            //添加到中间
            MarkerOptions position = new MarkerOptions().position(latLng);
            Marker marker = aMap.addMarker(position);
            //设置样式
            setMarkerStyle(marker);
            if (polygonOptions.getPoints().size()>=3){
                if (polygon!=null){
                    polygon.remove();
                }
                if (polyline!=null){
                    polyline.remove();
                }
                polygon = aMap.addPolygon(polygonOptions);
            }else {
                polylineOptions.add(latLng).width(20).color(getColor(R.color.plot));
                polyline = aMap.addPolyline(polylineOptions);
            }
        }else{
            //绘制插花地
            holePoints.add(latLng);
            
            //添加标记点
            MarkerOptions position = new MarkerOptions().position(latLng);
            Marker marker = aMap.addMarker(position);
            setMarkerStyle(marker);
            
            //实时显示插花地轮廓
            if (holePoints.size() >= 2) {
                List<LatLng> tempHolePoints = new ArrayList<>(holePoints);
                tempHolePoints.add(tempHolePoints.get(0)); //临时闭合
                
                if (polygon != null) {
                    polygon.remove();
                }
                
                PolygonHoleOptions tempHoleOptions = new PolygonHoleOptions();
                tempHoleOptions.addAll(tempHolePoints);
                
                List<BaseHoleOptions> tempHoleOptionsList = new ArrayList<>(holeOptionsList);
                tempHoleOptionsList.add(tempHoleOptions);
                polygonOptions.setHoleOptions(tempHoleOptionsList);
                polygon = aMap.addPolygon(polygonOptions);
            }
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
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (autoLocationClient != null) {
            autoLocationClient.onDestroy();
        }
        if (handLocationClient != null) {
            handLocationClient.onDestroy();
        }
    }

    /**
     * 新增
     */
    private void handleNormalMeasurement() {
        //将插花地的暂停按钮置为不可见，并且不可以使用
        buttonPause.setVisibility(View.INVISIBLE);
        buttonPause.setEnabled(false);
        //新增的完成逻辑
        buttonEndMeasure.setOnClickListener(v -> {
            //无论是手动测量还是自动测量，都需要先暂停定好
            stopLocation();
            // 上传数据
            if (polygon!=null){
                polygon.remove();
            }
            beforeUpload();
            DialogUtils.uploadDialog(MeasureActivity.this, landPlot,
                    getResources().getString(R.string.server_url)+getResources().getString(R.string.measure_url) ,
                    aMap, handler, user.getUserId());
        });
    }

    /**
     * 复测
     */

    private void handleRemeasurement() {
        //将插花地的暂停按钮置为不可见，并且不可以使用
        buttonPause.setVisibility(View.INVISIBLE);
        buttonPause.setEnabled(false);
        // 处理复测逻辑
        buttonEndMeasure.setOnClickListener(v -> {
            stopLocation();
            beforeUpload();
            DialogUtils.uploadDialog(MeasureActivity.this, landPlot,
                    getResources().getString(R.string.server_url)+getResources().getString(R.string.measure_url) ,
                    aMap, handler, user.getUserId());
        });
    }

    /**
     * 插花地
     */
    private void handleEnclaveMeasurement() {
        // 修改暂停按钮的逻辑
        buttonPause.setOnClickListener(v -> {
            stopLocation();
            if (polygonOptions.getPoints().size() < 3) {
                Toast.makeText(MeasureActivity.this, "请至少绘制3个点再暂停", Toast.LENGTH_SHORT).show();
                return;
            }
            // 处理外边界数据
            beforeUpload();
            
            // 准备绘制插花地
            isInterspersed = true;
            holePoints = new ArrayList<>(); // 重置空洞点列表
            Toast.makeText(MeasureActivity.this, "请开始绘制插花地", Toast.LENGTH_SHORT).show();
        });

        // 修改完成按钮的逻辑
        buttonEndMeasure.setOnClickListener(v -> {
            if (isInterspersed && holePoints.size() < 3) {
                Toast.makeText(MeasureActivity.this, "插花地至少需要3个点", Toast.LENGTH_SHORT).show();
                return;
            }
            myPoints.clear();
            // 如果正在绘制插花地，添加最后一个空洞
            if (isInterspersed && holePoints.size() >= 3) {
                // 闭合空洞
                PolygonHoleOptions holeOptions = new PolygonHoleOptions();
                holeOptions.addAll(holePoints);
                holeOptionsList.add(holeOptions);
                for(int i = 0; i < holePoints.size(); i++){
                    MyPoint myPoint = new MyPoint(holePoints.get(i).latitude, holePoints.get(i).longitude);
                    myPoints.add(myPoint);
                }
                // 计算插花地面积
                float holeArea = PolygonUtils.calculateArea(holePoints);
                landPlotExclude = new LandPlotExclude();
                landPlotExclude.setBoundary(UploadUtils.transformation(myPoints));
                landPlotExclude.setArea(holeArea);
                landPlotExcludeList.add(landPlotExclude);
            }

            // 更新多边形显示
            polygonOptions.setHoleOptions(holeOptionsList);
            polygon = aMap.addPolygon(polygonOptions);
            //调整相机位置
            PolygonUtils.adjustCameraToPolygon(aMap, polygonOptions.getPoints());
            // 上传数据
            landPlot.setLandPlotExcludes(landPlotExcludeList);
            DialogUtils.uploadDialog(MeasureActivity.this, landPlot,
                    getResources().getString(R.string.server_url)+getResources().getString(R.string.measure_url),
                    aMap, handler, user.getUserId());
        });
    }

    private void resetMeasurementState() {
        // 重置数据结构
        polygonOptions = new PolygonOptions();
        polylineOptions = new PolylineOptions();
        myPoints.clear();
        polygon = null;
        polyline = null;
        centerMarker = null;
        // 重新初始化地图状态
        initMyLocationStyle();
        Toast.makeText(this, "请开始新的插花地测量", Toast.LENGTH_SHORT).show();
    }


    /**
     * 上传前的逻辑
     * 肯定是上传第一个父级地块
     */
    private void beforeUpload() {
        if (polygonOptions.getPoints().size() > 2) {
            //添加第一个点，闭合
            polygonOptions.getPoints().add(polygonOptions.getPoints().get(0));
            LatLng polygonCenterPoint = PolygonUtils.getPolygonCenterPoint(polygonOptions.getPoints());
            MyPoint aPoint = new MyPoint(polygonCenterPoint.latitude, polygonCenterPoint.longitude);
            
            if (!isInterspersed) {
                // 处理主地块的逻辑
                float area = PolygonUtils.calculateArea(polygonOptions.getPoints());
                landPlot.setCenterPoint(UploadUtils.transformationCenter(aPoint));
                if (centerMarker == null) {
                    centerMarker = aMap.addMarker(new MarkerOptions()
                            .position(polygonCenterPoint)
                            .snippet(area + getResources().getString(R.string.areaUnit)));
                    setMarkerStyle(centerMarker);
                    centerMarker.showInfoWindow();
                }
                
                // 关闭定位图层
                aMap.setMyLocationEnabled(false);
                // 获取当前地图的缩放比例
                float currentZoomLevel = aMap.getCameraPosition().zoom;
                // 根据当前缩放比例进行一些调整，例如增加或减少一定比例
                float finalZoomLevel = currentZoomLevel - 6;
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(polygonCenterPoint, finalZoomLevel));
                boundary = UploadUtils.transformation(myPoints);
                //传统上传
                landPlot.setBoundary(boundary);
                landPlot.setLandArea(area);
                PolygonUtils.adjustCameraToPolygon(aMap, polygonOptions.getPoints());
            } else {
                // 处理插花地的逻辑
                if (holePoints.size() >= 3) {
                    List<MyPoint> currentHolePoints = new ArrayList<>();
                    // 将当前空洞的点转换为MyPoint对象
                    for (LatLng point : holePoints) {
                        currentHolePoints.add(new MyPoint(point.latitude, point.longitude));
                    }
                    
                    // 创建新的插花地对象
                    LandPlotExclude currentExclude = new LandPlotExclude();
                    float holeArea = PolygonUtils.calculateArea(holePoints);
                    currentExclude.setBoundary(UploadUtils.transformation(currentHolePoints));
                    currentExclude.setArea(holeArea);
                    landPlotExcludeList.add(currentExclude);
                    
                    // 更新多边形显示
                    PolygonHoleOptions holeOptions = new PolygonHoleOptions();
                    holeOptions.addAll(holePoints);
                    holeOptionsList.add(holeOptions);
                    
                    // 更新主多边形的所有空洞
                    if (polygon != null) {
                        polygon.remove();
                    }
                    polygonOptions.setHoleOptions(holeOptionsList);
                    polygon = aMap.addPolygon(polygonOptions);
                }
            }
        } else {
            Toast.makeText(this, "点数不足，请打三个点以上", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 停止定位
     */
    private void stopLocation(){
        if ("自动".equals(landPlot.getMeasurementMethod())) {
            autoLocationClient.stopLocation();
        } else {
            handLocationClient.stopLocation();
        }
    }
}
