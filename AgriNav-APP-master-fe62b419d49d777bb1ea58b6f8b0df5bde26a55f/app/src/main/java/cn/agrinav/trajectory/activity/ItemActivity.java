package cn.agrinav.trajectory.activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BaseHoleOptions;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolygonHoleOptions;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.utils.overlay.MovingPointOverlay;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.front.activity.plot.ItemPlotDetail;
import cn.agrinav.front.database.entity.LandPlot;
import cn.agrinav.trajectory.component.SwitchView;
import cn.agrinav.trajectory.dao.WorkTrajectoryPositionDao;
import cn.agrinav.trajectory.database.AbstractTrajectoryDatabase;
import cn.agrinav.trajectory.entity.Point;
import cn.agrinav.trajectory.entity.ResultLandPlot;
import cn.agrinav.trajectory.entity.vo.WorkTrajectoryPosition;
import cn.agrinav.trajectory.util.DataUtil;
import cn.agrinav.utils.OkHttpUtils;
import cn.agrinav.utils.Result;
import cn.agrinav.utils.ToastUtil;
import cn.agrinav.utils.jtt808.send.JTT808T0003Sender;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author 11230
 */
public class ItemActivity extends AppCompatActivity {

    private MapView mapView;
    private AMap aMap;
    private Handler handler;
    private ToastUtil toastUtil;
    private final int handlerError=0;
    private DataUtil dataUtil;
    private SharedPreferences sharedPreferences;
    private String url;
    private ThreadPoolExecutor executor;
    private String trajectoryId;
    private String startTime;
    private String endTime;
    private WorkTrajectoryPositionDao workTrajectoryPositionDao;
    private List<WorkTrajectoryPosition> workTrajectoryPositions;
    private ArrayList<LatLng> originLatLngList;
    private String phone;
    private Call callGetOriginTrajectory;
    private SwitchView switchView;
    private MovingPointOverlay movingPointOverlay;
    private Polyline originPolyline;
    private ArrayList<Polyline> workingPolylineList = new ArrayList<>();
    private ArrayList<LatLng> workLngList;
    private Polyline polylineGreen;
    private ArrayList<Polyline> polylineGreens = new ArrayList<>();
    private ArrayList<Integer> boundaryPointsIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_item);
        //进入到这个页面的时候一定有定位权限不过有没有都无所谓了我只是展示一下坐标
        initViews(savedInstanceState);
        initHandler();
        initData();
        initEvents();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent(this, BaseActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(getString(R.string.fragment_index),getResources().getInteger(R.integer.fragment_index_trajectory));
            startActivity(intent);
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initData() {
        workTrajectoryPositionDao = AbstractTrajectoryDatabase.getInstance(this).getWorkTrajectoryPositionDao();
        //获取主页面携带的轨迹信息
        trajectoryId = getIntent().getStringExtra(getString(R.string.item_extra_key));
        startTime = getIntent().getStringExtra(getString(R.string.item_extra_begin_time));
        endTime = getIntent().getStringExtra(getString(R.string.item_extra_end_time));
        phone = getIntent().getStringExtra(getString(R.string.item_extra_phone));
        //初始化线程池
        executor = new ThreadPoolExecutor(10, 20, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(20), new ThreadPoolExecutor.CallerRunsPolicy());
        //创建小车
        initCar();
        //初次绘制轨迹
        initTrajectory();
    }

    private void initCar() {
        //一个小车的标记
        Marker marker = aMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                        .decodeResource(getResources(),R.drawable.trajectory_car))));
        //设置这个小车滑动
        movingPointOverlay = new MovingPointOverlay(aMap, marker);
    }

    private void initEvents(){
        switchView.setOnSwitchListener(new SwitchView.OnSwitchListener() {
            @Override
            public void onCheck(SwitchView sv, boolean checkLeft, boolean checkRight) {
                //判断按钮当前所处位置
                if (checkLeft) {
                    for (Polyline polyline:workingPolylineList){
                        polyline.setVisible(true);
                    }
//                    workPolyline.setVisible(true);
                    originPolyline.setVisible(false);
                    movingPointOverlay.stopMove();
                    //删除掉这些线段
                    if(polylineGreens!=null) {
                        for (Polyline polyline:polylineGreens){
                            polyline.remove();
                        }
                    }
//                    if (polylineGreen!=null){
//                        polylineGreen.remove();
//                    }
                    setCarMove(workLngList);
                }else {
                    if(originPolyline==null){
                        PolylineOptions polylineOptions = new PolylineOptions()
                                .addAll(originLatLngList)
                                .width(getResources().getInteger(R.integer.trajectory_line_size))
                                .setCustomTexture(BitmapDescriptorFactory.fromResource(R.drawable.trajectory_no))
                                .color(getColor(R.color.trajectory_no));
                        originPolyline = aMap.addPolyline(polylineOptions);
                    }else {
                        originPolyline.setVisible(true);
                        for (Polyline polyline:workingPolylineList){
                            polyline.setVisible(false);
                        }
//                        workPolyline.setVisible(false);
                    }
                    movingPointOverlay.stopMove();
                    //删除掉这些线段
                    if(polylineGreens!=null) {
                        for (Polyline polyline:polylineGreens){
                            polyline.remove();
                        }
                    }
//                    if (polylineGreen!=null){
//                        polylineGreen.remove();
//                    }
                    setCarMove(originLatLngList,workTrajectoryPositions);
                }
            }
        });
    }
    private void initTrajectory() {
        if (trajectoryId==null||startTime==null||endTime==null) {
            new ToastUtil().getToast(this,getString(R.string.toast_err_data));
            finish();
        }else {
            //获取原始轨迹信息
            originLatLngList = new ArrayList<>();
            //获取原始轨迹的数据
            callGetOriginTrajectory =getOriginTrajectoryFromWorkTrajectoryData(phone,startTime,endTime,originLatLngList);
            //获取工作轨迹信息
            executor.execute(()->{
                //加载数据库当中的作业轨迹数据
                workTrajectoryPositions = workTrajectoryPositionDao.queryByTrajectoryId(trajectoryId);
                //根据id查询轨迹值
                workLngList = new ArrayList<>();
                for (WorkTrajectoryPosition workTrajectoryPosition : workTrajectoryPositions) {
                    workLngList.add(new LatLng(workTrajectoryPosition.getLatitude(), workTrajectoryPosition.getLongitude()));
                }
                if (!workLngList.isEmpty()) {
                    //获取地块信息,地块就获取这一次就够了
                    getLandPlotInfo(workTrajectoryPositions.get(0));
                    //绘制作业轨迹
                    printWorkTrajectoryNo(workLngList,workTrajectoryPositions,workingPolylineList);
                    setCarMove(workLngList,workTrajectoryPositions);
                }else {
                    runOnUiThread(() -> Toast.makeText(this, "没有轨迹数据", Toast.LENGTH_SHORT).show());
                }
            });

        }
    }
    private void setCarMove(ArrayList<LatLng> workLngList){
        //设置小车的路径
        movingPointOverlay.setPoints(workLngList);
        movingPointOverlay.setTotalDuration(workLngList.size());
        //小车开始滑动
        movingPointOverlay.startSmoothMove();
        //监听轨迹变化
        movingPointOverlay.setMoveListener(new MovingPointOverlay.MoveListener() {
            //起点的点
            final LatLng firstLatLng = new LatLng(workLngList.get(0).latitude,workLngList.get(0).longitude);
            /**
             * 这是个已经过路线的配置信息
             */
            final PolylineOptions polylineYes = new PolylineOptions()
                    .add(firstLatLng)
                    .width(getResources().getInteger(R.integer.trajectory_line_size))
                    .setCustomTexture(BitmapDescriptorFactory.fromResource(R.drawable.trajectory_yes))
                    .color(getColor(R.color.trajectory_yes));
            //这个是已经过的路线的点集
            final ArrayList<LatLng> polylineList = new ArrayList<>();
            @Override
            public void move(double v) {
                if (polylineList.isEmpty()){
                    polylineList.add(firstLatLng);
                }
                //添加点
                polylineList.add(movingPointOverlay.getPosition());
                polylineYes.getPoints().clear();
                polylineYes.addAll(polylineList);
                if (polylineGreen!=null) {
                    polylineGreen.remove();
                }
                polylineGreen = aMap.addPolyline(polylineYes);
                if (v<=0){
                    //删除掉这些绿色线段
                    polylineGreen.remove();
                    polylineYes.getPoints().clear();
                    polylineList.clear();
                    //然后让小车回到最初的位置重新开始绘制
                    movingPointOverlay.setPoints(workLngList);
                    movingPointOverlay.startSmoothMove();
                }
            }
        });
    }
    private void setCarMove(ArrayList<LatLng> workLngList,List<WorkTrajectoryPosition> workTrajectoryPositions){
        //设置小车的路径
        movingPointOverlay.setPoints(workLngList);
        movingPointOverlay.setTotalDuration(workLngList.size());
        //小车开始滑动
        movingPointOverlay.startSmoothMove();
        //监听轨迹变化
        //创建一个已经历边界点的数
        movingPointOverlay.setMoveListener(new MovingPointOverlay.MoveListener() {
            /**
             * 这是个已经过路线的配置信息
             */
            final PolylineOptions polylineYes = new PolylineOptions()
                    .width(getResources().getInteger(R.integer.trajectory_line_size))
                    .color(getColor(R.color.trajectory_yes));
            //这个是已经过的路线的点集
            final ArrayList<LatLng> polylineList = new ArrayList<>();
            int printIndex = 0;
            int hasGoBoundaryNum = 0;
            @Override
            public void move(double v) {
                //每两个点连出来一条线段
                polylineYes.getPoints().clear();
                polylineYes.add(workLngList.get(printIndex));
                polylineYes.add(movingPointOverlay.getPosition());
                if(boundaryPointsIndex.contains(printIndex)){
                    hasGoBoundaryNum++;
                }
                polylineYes.setDottedLine(hasGoBoundaryNum%2!=0);
                polylineGreens.add(aMap.addPolyline(polylineYes));
                printIndex++;
                if (v==0){
                    //重新开始
                    //删除掉这些绿色线段
                    polylineGreens.forEach(Polyline::remove);
                    polylineYes.getPoints().clear();
                    polylineList.clear();
                    //然后让小车回到最初的位置重新开始绘制
                    movingPointOverlay.setPoints(workLngList);
                    movingPointOverlay.startSmoothMove();
                    printIndex=0;
                    hasGoBoundaryNum = 0;
                }
            }
        });
    }
    private void printWorkTrajectoryNo(ArrayList<LatLng> latLngList,List<WorkTrajectoryPosition> workTrajectoryPositions,List<Polyline> polylines) {
        ArrayList<ArrayList<WorkTrajectoryPosition>> result = new ArrayList<>();
        boundaryPointsIndex = new ArrayList<>();
        //遍历整个轨迹进行分段
        for (int i = 0; i < workTrajectoryPositions.size(); i++){
            //在这里要找出所有的边界点
            if (i<workTrajectoryPositions.size()-1){
                Boolean currentInExcludePlot = workTrajectoryPositions.get(i).getInExcludePlot();
                Boolean nextInExcludePlot = workTrajectoryPositions.get(i + 1).getInExcludePlot();
                if(i==0|| !currentInExcludePlot.equals(nextInExcludePlot)){
                    if (!currentInExcludePlot.equals(nextInExcludePlot)){
                        boundaryPointsIndex.add(i);
                    }
                    ArrayList<WorkTrajectoryPosition> aPolylineList = new ArrayList<>();
                    result.add(aPolylineList);
                }
            }
            //这里说明不论是边界点还是边界点往后的点都要加入到对应的集合当中
            result.get(result.size()-1).add(workTrajectoryPositions.get(i));
        }
        //到这里的时候就已经出来所有的分段结果了
        int index = 0;
        for (int i = 0; i < result.size(); i++) {
            //首先要知道分段是什么点的集合
            ArrayList<WorkTrajectoryPosition> list = result.get(i);
            WorkTrajectoryPosition workTrajectoryPosition = list.get(list.size()- 1);
            //计算出来这次要用的点的集合
            if (workTrajectoryPosition.getInExcludePlot()){
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(latLngList.subList(index,Math.min(index+list.size()+1,latLngList.size())))
                        .width(getResources().getInteger(R.integer.trajectory_line_size))
                        .color(getColor(R.color.trajectory_no))
                        .setDottedLine(true);
                polylines.add(aMap.addPolyline(polylineOptions));
            }else {
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(latLngList.subList(index,Math.min(index+list.size()+1,latLngList.size())))
                        .width(getResources().getInteger(R.integer.trajectory_line_size))
                        .setCustomTexture(BitmapDescriptorFactory.fromResource(R.drawable.trajectory_no))
                        .color(getColor(R.color.trajectory_no))
                        .setDottedLine(false);
                polylines.add(aMap.addPolyline(polylineOptions));
            }
            index += list.size();
        }
//        PolylineOptions polylineOptions = new PolylineOptions()
//                .addAll(latLngList)
//                .width(getResources().getInteger(R.integer.trajectory_line_size))
//                .setCustomTexture(BitmapDescriptorFactory.fromResource(R.drawable.trajectory_no))
//                .color(getColor(R.color.trajectory_no));
//        workPolyline = aMap.addPolyline(polylineOptions);
    }
    private void printWorkTrajectoryYes(ArrayList<LatLng> latLngList,List<WorkTrajectoryPosition> workTrajectoryPositions,List<Polyline> polylines) {
        ArrayList<ArrayList<WorkTrajectoryPosition>> result = new ArrayList<>();
        //遍历整个轨迹进行分段
        for (int i = 0; i < workTrajectoryPositions.size(); i++){
            //在这里要找出所有的边界点
            if (i<workTrajectoryPositions.size()-1){
                Boolean currentInExcludePlot = workTrajectoryPositions.get(i).getInExcludePlot();
                Boolean nextInExcludePlot = workTrajectoryPositions.get(i + 1).getInExcludePlot();
                if(i==0|| !currentInExcludePlot.equals(nextInExcludePlot)){
                    ArrayList<WorkTrajectoryPosition> aPolylineList = new ArrayList<>();
                    result.add(aPolylineList);
                }
            }
            //这里说明不论是边界点还是边界点往后的点都要加入到对应的集合当中
            result.get(result.size()-1).add(workTrajectoryPositions.get(i));
        }
        //到这里的时候就已经出来所有的分段结果了
        int index = 0;
        for (int i = 0; i < result.size(); i++) {
            //首先要知道分段是什么点的集合
            ArrayList<WorkTrajectoryPosition> list = result.get(i);
            WorkTrajectoryPosition workTrajectoryPosition = list.get(list.size()- 1);
            //计算出来这次要用的点的集合
            if (workTrajectoryPosition.getInExcludePlot()){
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(latLngList.subList(index,Math.min(index+list.size()+1,latLngList.size()-1)))
                        .width(getResources().getInteger(R.integer.trajectory_line_size))
                        .color(getColor(R.color.trajectory_yes))
                        .setDottedLine(true);
                polylines.add(aMap.addPolyline(polylineOptions));
            }else {
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(latLngList.subList(index,Math.min(index+list.size()+1,latLngList.size()-1)))
                        .setCustomTexture(BitmapDescriptorFactory.fromResource(R.drawable.trajectory_yes))
                        .color(getColor(R.color.trajectory_yes))
                        .setDottedLine(false);
                polylines.add(aMap.addPolyline(polylineOptions));
            }
            index += list.size();
        }
    }
    public void initViews(Bundle savedInstanceState){
        mapView = findViewById(R.id.mapView_item);
        mapView.onCreate(savedInstanceState);
        if (aMap ==null){
            aMap = mapView.getMap();
        }
        switchView = findViewById(R.id.switch_choose_view);
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();
        aMap.clear();
        mapView.onDestroy();
        callGetOriginTrajectory.cancel();

    }
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }
    private void initHandler(){
        dataUtil = new DataUtil(this);
        toastUtil = new ToastUtil();
        url = getString(R.string.server_url);
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), Context.MODE_PRIVATE);
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == handlerError) {
                    //这个是有异常的时候打印，我传递过来的消息是错误信息
                    toastUtil.getToast(ItemActivity.this, (String) msg.obj);
                }
            }
        };
    }
    /**
     * 获取周边的地块
     */
    private void getLandPlotInfo(WorkTrajectoryPosition workTrajectoryPosition) {
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        HashMap<String,Long> params = new HashMap<>();
        params.put(getString(R.string.trajectory_request_param_plot_id), workTrajectoryPosition.getPlotId());
        Request request = okHttpUtils.createBuilder(url, getString(R.string.trajectory_request_get_plot_of_work)+"/"+workTrajectoryPosition.getPlotId())
                .addHeader(getString(R.string.request_header_key_token), Objects.requireNonNull(sharedPreferences.getString(getString(R.string.user_pref_key_token), "")))
                .build();
        try(Response response = okHttpUtils.sendRequest(request)){
            if (response.code()==getResources().getInteger(R.integer.request_success)) {
                if (response.body() != null) {
                    //获取到结果
                    Gson gson = new Gson();
                    String responseJson = response.body().string();
                    //把字符串解析出来
                    if (!responseJson.isEmpty()) {
                        Result<ResultLandPlot> resultLandPlot = gson.fromJson(responseJson, new TypeToken<Result<ResultLandPlot>>() {}.getType());
                        //如果不空我就解析出来他
                        if (resultLandPlot.getCode()==getResources().getInteger(R.integer.request_success)) {
                            ResultLandPlot aResultLandPlot = resultLandPlot.getData();
                            LandPlot landPlot = new LandPlot();
                            landPlot.setId(aResultLandPlot.getId());
                            landPlot.setBoundary(aResultLandPlot.getBoundary());
                            landPlot.setLandType(aResultLandPlot.getLandType());
                            //再解析还要考虑周围有没有地块,有才绘制，没有就不绘制
                            if (landPlot!=null) {
                                //把地块的数据直接绘制出来
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
                                            new ToastUtil().getToast(ItemActivity.this,"该地块信息为空");
                                        });
                                    }
                                });
                                ArrayList<LatLng> list = dataUtil.decodeLandPlotBoundary(landPlot.getBoundary());

                            }
                        }else {
                            handler.sendMessage(handler.obtainMessage(handlerError, resultLandPlot.getMsg()));
                        }
                        //遍历每个地块
                    } else {
                        //这个说明的是返回的请求体中的数据为空，后端如果没写错，这里不会调用,这里其实可以不写这一行但是需要在if中return，但是我懒得改了
                        handler.sendMessage(handler.obtainMessage(handlerError, getString(R.string.toast_first_error_verification_code_request_false_server_text)));
                    }
                } else {
                    //这里说明是服务器没有返回body数据，后端如果没写错，这里不会调用
                    handler.sendMessage(handler.obtainMessage(handlerError, getString(R.string.toast_first_error_verification_code_request_false_server_text)));
                }
            }else {
                //说明访问服务器这个地址的时候出了问题了
                handler.sendMessage(handler.obtainMessage(handlerError, getString(R.string.toast_first_error_verification_code_request_false_text)));
            }
        }catch (IOException e){
            //这里说明发送消息都有问题
            handler.sendMessage(handler.obtainMessage(handlerError, getString(R.string.toast_first_error_verification_code_request_false_text)));
        }
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

    public Call getOriginTrajectoryFromWorkTrajectoryData(String phone, String startTime, String endTime, ArrayList<LatLng> latLngList) {
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        HashMap<String, String> params = new HashMap<>();
        params.put(getString(R.string.trajectory_request_param_phone),phone);
        params.put(getString(R.string.trajectory_request_param_start_time),startTime);
        params.put(getString(R.string.trajectory_request_param_end_time),endTime);
        Request request = okHttpUtils.createBuilder(url,getString(R.string.trajectory_request_get_origin_by_phone),params)
                .addHeader(getString(R.string.request_header_key_token), Objects.requireNonNull(getSharedPreferences(getString(R.string.user_pref_name),Context.MODE_PRIVATE).getString(getString(R.string.user_pref_key_token), null)))
                .build();
        Callback callback =getOriginTrajectoryByPhoneAndTime(latLngList);
        return okHttpUtils.sendRequest(request, callback);
    }
    public Callback getOriginTrajectoryByPhoneAndTime(ArrayList<LatLng> latLngList) {
        // 创建一个 Handler，用于在主线程中处理 UI 更新
        Handler handler = new Handler(Looper.getMainLooper());
        return new Callback() {

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                //获取到activity
                AppCompatActivity activityRef = ItemActivity.this;
                // 检测是否回收了 activity
                //这里获取response状态码
                if (response.code() == getResources().getInteger(R.integer.request_success)) {
                    //这里说明后端可以收到请求并且进行处理
                    if (response.body() != null) {
                        String responseString = response.body().string();
                        Gson gson = new Gson();
                        try {
                            Result<ArrayList<Point>> result = gson.fromJson(responseString, new TypeToken<Result<ArrayList<Point>>>() {}.getType());
                            if (result.getCode() == getResources().getInteger(R.integer.request_success)) {
                                handler.post(() -> {
                                    //在主线程操作ui和数据
                                    if (result.getData() != null &&!result.getData().isEmpty()) {
                                        //清楚原有数据，覆盖掉
                                        result.getData().forEach(point -> {
                                            LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
                                            latLngList.add(latLng);
                                        });
                                    } else {
                                        showToast(activityRef, activityRef.getString(R.string.toast_err_data));
                                    }
                                });
                            } else {
                                showToast(activityRef, result.getMsg());
                            }
                        } catch (JsonSyntaxException e) {
                            showToast(activityRef, activityRef.getString(R.string.toast_err_json));
                            Log.e("getWorkCallBack", "JSON解析错误", e);
                        }
                    } else {
                        // 响应体为空就提示用户，一般是后端有了问题
                        showToast(activityRef, activityRef.getString(R.string.toast_err_server));
                    }
                } else {
                    //这里说明状态码都有问题，那么我就需要修改请求
                    showToast(activityRef, activityRef.getString(R.string.toast_err_request));
                }
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppCompatActivity activityRef = ItemActivity.this;
                // 检测是否回收了 activity
                //告知用户网络异常或者服务器状态异常
                showToast(activityRef, activityRef.getString(R.string.toast_err_net_or_server));
                // 添加日志记录
                Log.e("getCornCallBack", "网络请求失败", e);
            }
            private boolean isActivityValid(AppCompatActivity activity) {
                // 检查 Activity 是否有效
                return activity == null;
            }
            private void showToast(AppCompatActivity activity, String message) {
                // 使用 Handler 在主线程中显示 Toast
                handler.post(() -> toastUtil.getToast(activity, message));
            }
        };
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
    private void initMapContent(ArrayList<LatLng> list) {
        PolygonOptions polygonOptions = new PolygonOptions();
        // 添加 多边形的每个顶点（顺序添加）
        polygonOptions.addAll(list);
        // 多边形的边框
        // 边框颜色
        // 多边形的填充色
        polygonOptions.strokeWidth(getResources().getInteger(R.integer.trajectory_line_size))
                .strokeColor(getColor(R.color.plot))
                .fillColor(getColor(R.color.plot_fill));
        aMap.addPolygon(polygonOptions);
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
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centerPoint, calculateZoomLevel(maxDistance)));
        aMap.getUiSettings().setZoomControlsEnabled(true);
    }
    private void initMapContent(ArrayList<LatLng> list, List<BaseHoleOptions> listHoleOptions) {
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
        aMap.addPolygon(polygonOptions);
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
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centerPoint, calculateZoomLevel(maxDistance)));
        aMap.getUiSettings().setZoomControlsEnabled(true);
    }
}