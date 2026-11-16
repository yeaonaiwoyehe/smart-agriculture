package cn.agrinav.trajectory.fragment;

import static cn.agrinav.R.id.mapView_origin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import cn.agrinav.MainActivity;
import cn.agrinav.R;
import cn.agrinav.trajectory.adapter.ItemWorkAdapter;
import cn.agrinav.trajectory.entity.SortMessage;
import cn.agrinav.trajectory.util.DataUtil;
import cn.agrinav.utils.ToastUtil;
import okhttp3.Call;

/**
 * @author 11230
 */
public class FragmentTrajectoryOrigin extends Fragment {
    private final AppCompatActivity activity;
    private final SortMessage sortMessage;
    private MapView mapView;
    private AMap map;
    private ToastUtil toastUtil;
    private AMapLocationClient aMapLocationClient;
    private AMapLocationClientOption aMapLocationClientOption;
    private ArrayList<LatLng> latLngList;
    private Call originTrajectoryData;
    public FragmentTrajectoryOrigin(){
        activity = null;
        sortMessage = null;
    }
    public FragmentTrajectoryOrigin(WeakReference<AppCompatActivity> activityWeakReference, WeakReference<SortMessage> sortMessage) {
        this.activity = activityWeakReference.get();
        this.sortMessage = sortMessage.get();
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trajectory_origin,null);
        initViews(view);
        mapView.onCreate(savedInstanceState);
        initData();
        loadData();
        Log.i("aa","originCreateView");
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (activity==null&&sortMessage==null){
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        super.onCreate(savedInstanceState);
        Log.i("aa","originCreate");
    }

    @Override
    public void onStart() {
        super.onStart();
        if (aMapLocationClient!=null){
            aMapLocationClient.startLocation();
        }
        Log.i("aa","originStart");
    }
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        Log.i("aa","originResume");
    }
    public void loadData(){
        if(!sortMessage.isEmpty()) {
            //非空就加载数据
            new DataUtil(activity).showPlotByOriginTrajectory(sortMessage,map);
            originTrajectoryData = new DataUtil(activity).getOriginTrajectoryData(latLngList, sortMessage, map);
        }else {
            //空着就提示用户
            toastUtil.getToast(activity,"请先进行筛选");
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        map.clear();
        mapView.onPause();
        Log.i("aa","originPause");

    }

    @Override
    public void onStop() {
        super.onStop();
        aMapLocationClient.stopLocation();
        Log.i(getString(R.string.trajectory_tag),"originOnStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDestroy();
        Log.i(getString(R.string.trajectory_tag),"originOnDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (originTrajectoryData != null) {
            originTrajectoryData.cancel();
        }
        Log.i(getString(R.string.trajectory_tag),"originOnDestroy");

    }
    private void initData() {
        //首先获取到一个通知的东西
        toastUtil = new ToastUtil();
        //初始化出来一个用于存放轨迹点的集合
        latLngList = new ArrayList<>();
        //TODO:初始化定位
        initPosition();
    }
    private void initViews(View view){
        mapView = view.findViewById(mapView_origin);
        map = mapView.getMap();
    }
    private void initPosition(){
        try {
            //首先获取定位客户端
            if (aMapLocationClient == null) {
                aMapLocationClient = new AMapLocationClient(activity.getApplicationContext());
            }
            //设置定位的一些相关配置项
            setTargetingOption();
            //配置项加载到客户端中
            aMapLocationClient.setLocationOption(aMapLocationClientOption);
            //设置触发定位层
            map.setMyLocationEnabled(true);
            //设置视野跟随定位
            map.setMyLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
            //设置缩放等级
            CameraUpdate cameraUpdate = CameraUpdateFactory.zoomTo(getResources().getInteger(R.integer.map_camara_size));
            //调整
            map.moveCamera(cameraUpdate);
            //设置可以手势操作
            map.getUiSettings().setZoomGesturesEnabled(true);
            //使用客户端开始定位
            aMapLocationClient.startLocation();
        } catch (Exception e) {
            toastUtil.getToast(activity,"定位失败");
        }
    }
    private void setTargetingOption() {
        aMapLocationClientOption = new AMapLocationClientOption();
        //设置定位模式为高精度模式
        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置北斗定位优先
        aMapLocationClientOption.setBeidouFirst(true);
        aMapLocationClientOption.getGeoLanguage();
        //设置wifi自动刷新
        aMapLocationClientOption.setWifiScan(true);
        //设置定位是否等待wifi列表刷新
        aMapLocationClientOption.setOnceLocationLatest(true);
        //设置开启wifi始终扫描
        AMapLocationClientOption.setOpenAlwaysScanWifi(true);
    }
    public void clearPlot() {
        map.clear();
    }
}
