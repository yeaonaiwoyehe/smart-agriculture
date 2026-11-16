package cn.agrinav.trajectory.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BaseHoleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.PolygonHoleOptions;
import com.amap.api.maps.model.PolygonOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.front.activity.plot.ItemPlotDetail;
import cn.agrinav.front.database.entity.LandPlot;
import cn.agrinav.trajectory.dao.TrajectoryDao;
import cn.agrinav.trajectory.dao.TrajectoryPositionDao;
import cn.agrinav.trajectory.database.AbstractTrajectoryDatabase;
import cn.agrinav.trajectory.entity.ResultLandPlot;
import cn.agrinav.trajectory.entity.Trajectory;
import cn.agrinav.trajectory.permission.PermissionTrajectory;
import cn.agrinav.trajectory.util.DataUtil;
import cn.agrinav.utils.DialogUtils;
import cn.agrinav.utils.OkHttpUtils;
import cn.agrinav.utils.Result;
import cn.agrinav.utils.ToastUtil;
import cn.agrinav.utils.jtt808.send.JTT808T0002Sender;
import cn.agrinav.utils.jtt808.send.JTT808T0003Sender;
import cn.agrinav.utils.jtt808.send.JTT808T0100Sender;
import cn.agrinav.utils.jtt808.send.JTT808T0102Sender;
import cn.agrinav.utils.jtt808.receive.JTT808RegisterResponse;
import cn.agrinav.utils.jtt808.receive.JTT808T0100Receiver;
import cn.agrinav.utils.netty.NettyClientManager;
import cn.agrinav.utils.netty.exception.NettyConnectionException;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/**
 * @author 11230
 */
public class TrajectoryActivity extends AppCompatActivity {
    //这个表示的是该地图的所需权限
    /**这个表示地图视图本身*/
    private MapView mapView;
    /**这个表示的是地图的控制器*/
    private AMap amap;
    /**这个表示的是启用定位功能的控制*/
    private AMapLocationClient aMapLocationClient;
    private TextView tvCorn;
    private TextView tvCornType;
    private TextView tvState;
    private EditText editWidth;
    private Button btnBegin;
    private LinearLayout bottomLayout;
    private RelativeLayout cornLayout;
    private RelativeLayout cornTypeLayout;
    private ThreadPoolExecutor threadPoolExecutor;
    private ToastUtil toastUtil;
    private PopupMenu popupMenuCorn;
    private PopupMenu popupMenuWork;
    private boolean haveCorn;
    private boolean haveWorkType;
    private boolean isConnected;
    private TrajectoryPositionDao trajectoryPositionDao;
    private TrajectoryDao trajectoryDao;
    private SharedPreferences sharedPreferences;
    private DataUtil dataUtil;
    private Handler handler;
    private final int handlerError = 1;
    private String url;
    private Trajectory trajectory;
    /**维护两个hashmap分别存储作物和作业类型的名字与id*/
    ConcurrentHashMap<String,Long> cornEntities = new ConcurrentHashMap<>();
    ConcurrentHashMap<String,Long> workTypeEntities = new ConcurrentHashMap<>();
    /**netty客户端*/
    private NettyClientManager manager;
    /**是否发送心跳*/
    private Boolean isHeartSend = true;
    private JTT808T0100Sender jtt808T0100Sender;
    private JTT808T0002Sender jtt808T0002Sender;
    private JTT808T0102Sender jtt808T0102Sender;
    private String phone;
    private ActivityResultLauncher lancher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(getResources().getString(R.string.trajectory_tag), "创建地图页");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trajectory);
        //sdk权限检测和定位权限检测已经在前面拿到了，这里一定有没不需要再查验
        //初始化地图上的视图组件
        initView();
        //初始化基础数据:获取作物，建立连接
        initData();
        //初始化地图上的数据
        initMap(savedInstanceState);
        changeCanUseMap();
        initEvents();

    }
    private void initMap(Bundle savedInstanceState) {
        //首先获取地图视图
        mapView = findViewById(R.id.map_trajectory);
        //在这里创建视图
        mapView.onCreate(savedInstanceState);
        //获取地图控制器
        if (amap==null) {
            amap = mapView.getMap();
        }
    }
    private void initData() {
        //工具类
        dataUtil = new DataUtil(this);
        //初始化一个toast
        toastUtil = new ToastUtil();
        //获取数据
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), Context.MODE_PRIVATE);
        url = sharedPreferences.getString(getString(R.string.user_pref_key_server),getString(R.string.server_url));
        handler = initHandler();
        //初始化线程池
        threadPoolExecutor = new ThreadPoolExecutor(
                10,
                20,
                10,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        //获取用户当前手机号
        phone = sharedPreferences.getString(getString(R.string.user_pref_key_phone),"");
        //获取菜单里面的值
        threadPoolExecutor.execute(()-> new DataUtil(this).getCornData(popupMenuCorn,cornEntities));
        //在这里要发起注册设备的请求
        manager = NettyClientManager.getInstance();
        // 传递上下文
        NettyClientManager.setContext(this);
        try {
            manager.connect();
            jtt808T0100Sender = new JTT808T0100Sender(manager);
            jtt808T0102Sender = new JTT808T0102Sender(manager);
            jtt808T0002Sender = new JTT808T0002Sender(manager);
            threadPoolExecutor.execute(()->{registerDevice(phone);});
        } catch (Exception e) {
            toastUtil.getToast(this,"连接失败,请检查网络连接");
            tvState.setText("连接失败");
            tvState.setTextColor(Color.RED);
        }
        lancher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> changeCanUseMap());
    }
    private Handler initHandler(){
        return handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if(msg.what==handlerError){
                    //这个是有异常的时候打印，我传递过来的消息是错误信息
                    toastUtil.getToast(TrajectoryActivity.this, (String)msg.obj);
                }
            }
        };
    }

    private void initEvents() {
        //设置菜单选项的点击事件
        popupMenuCorn.setOnMenuItemClickListener(item -> {
            tvCorn.setText(item.getTitle());
            //获取工作类型数据
            threadPoolExecutor.execute(()-> new DataUtil(this).getCornTypeData(popupMenuWork,workTypeEntities,cornEntities.get(item.getTitle())));
            return false;
        });
        popupMenuWork.setOnMenuItemClickListener(item -> {
            tvCornType.setText(item.getTitle());
            return false;
        });
        //两个菜单按钮的点击事件
        cornLayout.setOnClickListener(v->{
            //弹出作物菜单
            if (popupMenuCorn.getMenu().size()>0) {
                popupMenuCorn.show();
            }else {
                showTips("作物正在加载,请稍后");
            }
        });
        cornTypeLayout.setOnClickListener(v->{
            //弹出作业类型菜单
            if (popupMenuWork.getMenu().size()>0) {
                popupMenuWork.show();
            }else{
                showTips("请先选择作物");
            }
        });

        //开始作业和结束作业按钮的点击事件
        btnBegin.setOnClickListener(v->{
            //能执行到这里就说明肯定是有权限的了，只需要重新检测wifi模块和定位模块是否开启了就行
            boolean isAllowTargeting = PermissionTrajectory.isAllowTargeting(this);
            boolean isAllowWifi = PermissionTrajectory.isAllowWifi(this);
            if (!isAllowTargeting){
                toastUtil.getToast(this,"未开启定位功能");
            }else if (!isAllowWifi){
                toastUtil.getToast(this,"未开启wifi功能");
            }else {
                //TODO:先判断通用状态是否正确 现在下面这个判断条之间是有问题的，其实应该是正确的分析这个按钮的状态然后再执行下列内容
                if (tvState.getText().equals("连接正常")) {
                    //要先保证不缺少幅宽作物作业类型
                    if (tvCorn.getText().toString().isEmpty() || tvCornType.getText().toString().isEmpty() || editWidth.getText().toString().isEmpty()) {
                        toastUtil.getToast(this,"请先选择作物作业类型和幅宽");
                    } else {
                        if (editWidth.getText().length()>6) {
                            toastUtil.getToast(this,"幅宽不能超过6位");
                            return;
                        }
                        threadPoolExecutor.execute(()-> {
                            //首先新建出来对象（请求体）
                            trajectory = new Trajectory(UUID.randomUUID().toString());
                            trajectory.setCrop(tvCorn.getText().toString());
                            trajectory.setWorkType(tvCornType.getText().toString());
                            trajectory.setWidth(Integer.parseInt(editWidth.getText().toString()));
                            trajectory.setStartTime(LocalDateTime.now().toString());
                            //存入本地数据库
                            trajectoryDao.insert(trajectory);
                            //ui元素，要在主线程当中
                            runOnUiThread(() -> {
                                //首先就让线程池停止发送心跳
                                isHeartSend = false;
                                //然后再跳转
                                Intent intent = new Intent(TrajectoryActivity.this, WorkTrajectoryActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("workTypeId",Integer.valueOf(String.valueOf(workTypeEntities.get(tvCornType.getText().toString()))));
                                intent.putExtra("width",Integer.valueOf(editWidth.getText().toString()));
                                intent.putExtra("trajectoryId",trajectory.getTrajectoryInfoId());
                                intent.putExtra("phone",phone);
                                intent.putExtra("startTime",trajectory.getStartTime());
                                startActivity(intent);
                            });
                        });
                    }
                } else {
                    //TODO：这里进行状态不正确时候的处理
                    toastUtil.getToast(this, "通讯异常，稍后再试");
                }
            }
        });
    }
    /**
     * 显示提示信息
     * @param content 提示信息的内容
     */
    private void showTips(String content){
        new ToastUtil().getToast(this,content);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(getResources().getString(R.string.trajectory_tag),"onDestroy");
        if (aMapLocationClient!=null&&aMapLocationClient.isStarted()) {
            aMapLocationClient.stopLocation();
            aMapLocationClient.onDestroy();
        }
        //TODO:在销毁的时候要给后台发送一个断联请求
        mapView.onDestroy();
    }
    @Override
    public void onStart() {
        super.onStart();
        //在这个页面实例化出来轨迹的操作Dao和坐标的Dao
        trajectoryDao = AbstractTrajectoryDatabase.getInstance(this).getTrajectoryDao();
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(getResources().getString(R.string.trajectory_tag),"onPause");
        mapView.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(getResources().getString(R.string.trajectory_tag),"onResume");
        //当授予权限之后会直接进行这一步，然后
        mapView.onResume();
    }
    private void initView() {
        //初始化底部弹窗内部的东西
        bottomLayout = findViewById(R.id.ll_trajectory_begin_bottom);
        cornLayout = findViewById(R.id.rl_trajectory_begin_corn);
        cornTypeLayout = findViewById(R.id.rl_trajectory_begin_corn_type);
        //这是底部弹窗的文本控件
        tvCorn = findViewById(R.id.tv_trajectory_begin_corn);
        tvCornType = findViewById(R.id.tv_trajectory_begin_corn_type);
        tvState = findViewById(R.id.tv_trajectory_begin_state);
        editWidth = findViewById(R.id.tv_trajectory_begin_width);
        //初始化开始按钮
        btnBegin = findViewById(R.id.btn_trajectory_begin);
        popupMenuCorn = new PopupMenu(this,cornLayout);
        popupMenuWork = new PopupMenu(this,cornTypeLayout);
    }

    /**这个是用户使用地图服务之前的检查操作
     * 包含权限检查和能力检查
     * 如果检查成功了就会正常进入不成功不让进*/
    private void changeCanUseMap() {
        //是否开启定位
        boolean allowTargeting= PermissionTrajectory.isAllowTargeting(this);
        //是否开启wifi
        boolean allowWifi = PermissionTrajectory.isAllowWifi(this);
        //先检查wifi和定位
        if (allowTargeting && allowWifi){
            //执行定位权限的检查，如果检查成功了会让haveTargetingPermission为true,否则会让他为false
            initPosition();
        }else {
            if (!allowTargeting) {
                //在这里弹出一个弹窗让用户打开定位
                DialogUtils dialogUtils = new DialogUtils(this);
                AlertDialog dialog = dialogUtils.getDialog();
                dialogUtils.setTitle("温馨提示")
                        .setContent("该页面需要使用定位功能来查询周围地块信息,请打开定位功能")
                        .setPositiveText("去开启")
                        .setNegativeText("取消")
                        .setPositiveListener(v -> {
                            //如果要重新授权就直接让他重新给权限重新调用一遍直到他给我权限为止
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            lancher.launch(intent);
                            dialog.cancel();
                        })
                        .setNegativeListener(v -> {
                            //不给权限不让用这个功能，结束这个页面
                            end808(new JTT808T0003Sender(manager));
                            Intent intent = new Intent(this, BaseActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra(getString(R.string.fragment_index), getResources().getInteger(R.integer.fragment_index_trajectory));
                            dialog.cancel();
                            startActivity(intent);
                            finish();
                        });
                View view = dialogUtils.getView();
                dialog.setContentView(view);
                dialog.show();
            }
            if(!allowWifi){
                //在这里弹出来一个弹窗让用户打开wifi功能
                DialogUtils dialogUtils = new DialogUtils(this);
                AlertDialog dialog = dialogUtils.getDialog();
                dialogUtils.setTitle("温馨提示")
                        .setContent("该页面需要使用wifi功能来查询周围地块信息,请打开wifi功能")
                        .setPositiveText("去开启")
                        .setNegativeText("取消")
                        .setPositiveListener(v -> {
                            //如果要重新授权就直接让他重新给权限重新调用一遍直到他给我权限为止
                            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                            lancher.launch(intent);
                            dialog.cancel();
                        })
                        .setNegativeListener(v -> {
                            //不给权限不让用这个功能，结束这个页面
                            end808(new JTT808T0003Sender(manager));
                            Intent intent = new Intent(this, BaseActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra(getString(R.string.fragment_index), getResources().getInteger(R.integer.fragment_index_trajectory));
                            dialog.cancel();
                            startActivity(intent);
                            finish();
                        });
                View view = dialogUtils.getView();
                dialog.setContentView(view);
                dialog.show();
            }
//            Intent intent = new Intent(this, BaseActivity.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            startActivity(intent);
        }
    }
    /**
     * 定位一次获取周围地块
     * */
    private void initPosition(){
        try {
            //首先获取定位客户端
            if (aMapLocationClient == null) {
                aMapLocationClient = new AMapLocationClient(this.getApplicationContext());
            }
            AMapLocationClientOption aMapLocationClientOption = new AMapLocationClientOption();
            //设置定位的一些相关配置项
            setTargetingOption(aMapLocationClientOption);
            //配置项加载到客户端中
            aMapLocationClient.setLocationOption(aMapLocationClientOption);
            aMapLocationClient.startLocation();
            //设置定位跟随视野
            amap.setMyLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW);
            //设置出发定位层
            amap.setMyLocationEnabled(true);
            //设置缩放等级
            CameraUpdate cameraUpdate = CameraUpdateFactory.zoomTo(getResources().getInteger(R.integer.map_camara_size));
            //调整
            amap.moveCamera(cameraUpdate);
            //设置可以手势操作
            amap.getUiSettings().setZoomGesturesEnabled(true);
            //在这里给后端发送消息通知现在的位置
            AMapLocation lastKnownLocation = aMapLocationClient.getLastKnownLocation();
            if (lastKnownLocation != null) {
                threadPoolExecutor.execute(()-> getLandPlotInfo(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude(),getResources().getInteger(R.integer.distance_round_plot_trajectory)));
            }
        } catch (Exception e) {
            toastUtil.getToast(this,e.getMessage());
        }
    }
    /**
     * 设置定位参数
     */
    private void setTargetingOption(AMapLocationClientOption aMapLocationClientOption) {
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
        //五秒定位一次
        aMapLocationClientOption.setOnceLocation(true);
    }
    /**
     * 获取周边的地块
     * @param latitude 维度
     * @param longitude 精度
     * @param distance 距离
     */
    private void getLandPlotInfo(double latitude, double longitude,double distance) {
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        HashMap<String,Double> params = new HashMap<>();
        params.put(getString(R.string.trajectory_request_param_latitude),latitude);
        params.put(getString(R.string.trajectory_request_param_longitude),longitude);
        params.put(getString(R.string.trajectory_request_param_distance),distance);
        Request request = okHttpUtils.createBuilder(url, getString(R.string.trajectory_request_get_land_plot), params)
                .addHeader(getString(R.string.request_header_key_token), Objects.requireNonNull(sharedPreferences.getString(getString(R.string.user_pref_key_token), null)))
                .build();
        try(Response response = okHttpUtils.sendRequest(request)){
            if (response.code()==getResources().getInteger(R.integer.request_success)) {
                if (response.body() != null) {
                    //获取到结果
                    Gson gson = new Gson();
                    String responseJson = response.body().string();
                    //把字符串解析出来
                    if (!responseJson.isEmpty()) {
                        Result<ArrayList<ResultLandPlot>> resultLandPlot = gson.fromJson(responseJson, new TypeToken<Result<ArrayList<ResultLandPlot>>>() {}.getType());
                        //如果不空我就解析出来他
                        if (resultLandPlot.getCode()==getResources().getInteger(R.integer.request_success)) {
                            ArrayList<ResultLandPlot> landPlots = resultLandPlot.getData();
                            //再解析还要考虑周围有没有地块,有才绘制，没有就不绘制
                            if (!landPlots.isEmpty()) {
                                for (ResultLandPlot aResultLandPlot : landPlots) {
                                    LandPlot landPlot = new LandPlot();
                                    landPlot.setId(aResultLandPlot.getId());
                                    landPlot.setBoundary(aResultLandPlot.getBoundary());
                                    landPlot.setLandType(aResultLandPlot.getLandType());
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
                                                handler.sendMessage(handler.obtainMessage(handlerError, "该地块信息为空"));
                                        }
                                    });
                                }
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
    private void initMapContent(ArrayList<LatLng> landPlotBoundaryList, List<BaseHoleOptions> listHoleOptions){
        //把地块的数据直接绘制出来
        PolygonOptions polygonOptions = new PolygonOptions();
        // 添加 多边形的每个顶点（顺序添加）
        polygonOptions.addAll(landPlotBoundaryList);
        // 多边形的边框
        // 边框颜色
        // 多边形的填充色
        polygonOptions.strokeWidth(getResources().getInteger(R.integer.trajectory_line_size))
                .strokeColor(getColor(R.color.plot))
                .fillColor(getColor(R.color.plot_fill))
                .setHoleOptions(listHoleOptions);
        amap.addPolygon(polygonOptions);
    }
    private void initMapContent(ArrayList<LatLng> landPlotBoundaryList){
        //把地块的数据直接绘制出来
        PolygonOptions polygonOptions = new PolygonOptions();
        // 添加 多边形的每个顶点（顺序添加）
        polygonOptions.addAll(landPlotBoundaryList);
        // 多边形的边框
        // 边框颜色
        // 多边形的填充色
        polygonOptions.strokeWidth(getResources().getInteger(R.integer.trajectory_line_size))
                .strokeColor(getColor(R.color.plot))
                .fillColor(getColor(R.color.plot_fill));
        amap.addPolygon(polygonOptions);
    }
    /**
     * 注册设备
     * */
    private void registerDevice(String phone) {
        String terminalId = "0".concat(phone);
        String hexResponse = jtt808T0100Sender.sendRegistrationMessage(terminalId, "0", "0", "0", "0", phone, "0", "");
        JTT808T0100Receiver receiver = new JTT808T0100Receiver();
        JTT808RegisterResponse response = receiver.receiveRegistrationResponse(hexResponse);
        if (response.getResult() == 0) {
            String authCode = response.getAuthCode();
            //鉴权码存下来
            sharedPreferences.edit().putString("authCode", authCode).apply();
            //发送心跳包
            jtt808T0102Sender.sendAuthenticationMessage(terminalId, authCode);
            if (!authCode.isEmpty()) {
                // 每5秒发送一次心跳
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        sendHeartBeat(terminalId);
                    }
                }, 0, 5000);
            }
        }
    }
    /**
     * 发送心跳
     */
    private void sendHeartBeat(String terminalId) {
        //进入页面就需要发送心跳
        while(isHeartSend) {
            String s = jtt808T0002Sender.sendHeartbeatMessage(terminalId);
            Log.i("000",s);
            runOnUiThread(()->{
                tvState.setText("连接正常");
                tvState.setTextColor(getColor(R.color.green));
            });
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Log.i("heart", "sleep error");
            }
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            end808(new JTT808T0003Sender(manager));
            Intent intent = new Intent(this, BaseActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(getString(R.string.fragment_index), getResources().getInteger(R.integer.fragment_index_trajectory));
            startActivity(intent);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
    /**
     * 结束808
     */
    private void end808(JTT808T0003Sender jtt808T0003Sender){
        isHeartSend = false;
        //注销
        jtt808T0003Sender.sendLogoutMessage("0".concat(phone));
        //断开连接
        manager.close();
    }
}
