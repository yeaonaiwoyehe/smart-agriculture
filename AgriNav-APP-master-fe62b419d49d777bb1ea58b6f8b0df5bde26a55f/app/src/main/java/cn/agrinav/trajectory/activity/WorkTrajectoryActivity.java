package cn.agrinav.trajectory.activity;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.health.connect.datatypes.units.Power;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import cn.agrinav.trajectory.dao.TrajectoryDao;
import cn.agrinav.trajectory.dao.TrajectoryPositionDao;
import cn.agrinav.trajectory.database.AbstractTrajectoryDatabase;
import cn.agrinav.trajectory.service.JTService;
import cn.agrinav.trajectory.util.DataUtil;
import cn.agrinav.utils.DialogUtils;
import cn.agrinav.utils.camera.BitmapUtils;
import cn.agrinav.utils.camera.CameraManager;
import cn.agrinav.utils.jtt808.send.JTT808T0003Sender;
import cn.agrinav.utils.netty.NettyClientManager;

import java.lang.ref.WeakReference;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.agrinav.R;
import cn.agrinav.trajectory.entity.TrajectoryPosition;
import cn.agrinav.utils.ToastUtil;
import cn.agrinav.utils.jtt808.send.JTT808T0200Sender;
import cn.agrinav.utils.permission.PermissionUtils;
import okhttp3.Call;

/**
 * @author 11230
 */
public class WorkTrajectoryActivity extends AppCompatActivity {

    private MapView mapView;
    private AMap map;
    private MaterialButton btnEnd;
    private MaterialButton btnPhoto;
    private ToastUtil toastUtil;
    private boolean isHaveInternet;
    private AMapLocationClient aMapLocationClient;
    private AMapLocationClientOption aMapLocationClientOption;
    private ArrayList<TrajectoryPosition> trajectoryPositions;
    private String trajectoryId;
    private NettyClientManager manager;
    private JTT808T0200Sender jtt808T0200Sender;
    private final int POSITION_SAVE=0;
    private final int POSITION_END=1;
    private final int TAG_CAMERA = 2;
    private boolean takePhoto= false;
    private CameraManager cameraManager;
    private ActivityResultLauncher lancher;
    /**这个字段用于判断在本次轨迹绘制的时候是否有上传过信息*/
    private boolean isUpload;
    /**
     * 这个是work类型的id
     */
    private int workTypeId;
    /**
     *轨迹宽度
     */
    private int width;
    /**
     *手机号
     */
    private String phone;
    /**
     *线程池
     */
    private ThreadPoolExecutor threadPoolExecutor;
    /**
     * 数据库
     */
    private TrajectoryPositionDao trajectoryPositionsDao;
    /**
     * 注销实体
     */
    private JTT808T0003Sender jtt808T0003Sender;
    private Handler handlerSave;
    private Call endTrajectoryCall;
    private NotificationChannel channel;
    private String startTime;
    private TrajectoryDao trajectoryDao;
    private static final String NOTIFICATION_CHANNEL_NAME = "BackgroundLocation";
    private NotificationManager notificationManager = null;
    boolean isCreateChannel = false;
    private Snackbar snackbar;
    private final int HANDLER_TAKE_PHOTO = 9;
    private Matrix matrix;
    private Handler handler;
    private Notification buildNotification(String content) {

        Notification.Builder builder = null;
        Notification notification = null;
        //Android O上对Notification进行了修改，如果设置的targetSDKVersion>=26建议使用此种方式创建通知栏
        if (null == notificationManager) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        String channelId = getPackageName();
        if(!isCreateChannel) {
            NotificationChannel notificationChannel = new NotificationChannel(channelId,
                    NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            //是否在桌面icon右上角展示小圆点
            notificationChannel.enableLights(true);
            //小圆点颜色
            notificationChannel.setLightColor(Color.BLUE);
            //是否在久按桌面图标时显示此渠道的通知
            notificationChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(notificationChannel);
            isCreateChannel = true;
        }
        builder = new Notification.Builder(getApplicationContext(), channelId);
        builder.setSmallIcon(R.mipmap.logo)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(content)
                .setWhen(System.currentTimeMillis());

        notification = builder.build();
        return notification;
    }

    /**
     * 进入这个页面应该拥有的数据
     * 作业类型id
     * 定位权限，如果没有定位权限他前边就别想用
     * 上一个页面返还过来的地块坐标ArrayList
     * /
     /**
     * 需要做的事情
     * 检测网络状态
     * 检测wifi和定位是否开启
     * 向用户展示地图
     * 开启定位
     * 五秒监听一次用户的位置
     * 给服务器发送用户定位
     * 在结束之后要把所有的点存入本地数据库三天
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_work_trajectory);
        initViews(savedInstanceState);
        initData();
        initEvents();
    }



    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        mapView.onSaveInstanceState(outState);
    }
    @Override
    protected void onStop() {
        super.onStop();
        mapView.onPause();
    }
    @Override
    protected void onDestroy() {
//        back();
        mapView.onDestroy();
        notificationManager.notify(1, buildNotification("轨迹上传完成"));
        super.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        mapView.onResume();
    }
    @Override
    protected void onStart() {
        super.onStart();
        //打开数据库
        trajectoryPositionsDao = AbstractTrajectoryDatabase.getInstance(this).getTrajectoryPositionDao();
        trajectoryDao = AbstractTrajectoryDatabase.getInstance(this).getTrajectoryDao();
        PolylineOptions rePrint = new PolylineOptions()
                .width(getResources().getInteger(R.integer.trajectory_line_size))
                .setCustomTexture(BitmapDescriptorFactory.fromResource(R.drawable.trajectory_no))
                .color(getColor(R.color.trajectory_no));
        //清除原来所有的轨迹
        map.clear();
        threadPoolExecutor.execute(()->{
            ArrayList<LatLng> currentLocations = new ArrayList<>();
            trajectoryPositionsDao.queryById(trajectoryId).forEach(trajectoryPosition -> {
                currentLocations.add(new LatLng(trajectoryPosition.getLatitude(), trajectoryPosition.getLongitude()));
            });
            rePrint.addAll(currentLocations);
            map.addPolyline(rePrint);
        });
    }
    /**
     * 初始化所有组件
     */
    private void initViews(Bundle savedInstanceState) {
        //首先初始化组件
        mapView = findViewById(R.id.map_work);
        mapView.onCreate(savedInstanceState);
        //获取地图控制器
        if (map==null) {
            map = mapView.getMap();
        }
        //获取结束按钮
        btnEnd = findViewById(R.id.btn_trajectory_work_end);
        btnPhoto = findViewById(R.id.btn_trajectory_work_take_photo);
        cameraManager = new CameraManager(this, findViewById(R.id.trajectory_work_preview));
        cameraManager.initCamera();
    }
    /**
     * 初始化一些基础变量
     */
    private void initData() {
        //首先获取到一个通知的东西
        matrix = new Matrix();
        toastUtil = new ToastUtil();
        //初始化出来一个用于存放轨迹点的集合
        trajectoryPositions = new ArrayList<>();
        //初始化线程池
        threadPoolExecutor = new ThreadPoolExecutor(
                10,
                20,
                10,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        lancher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->{
            beginOrEndPhoto();
        });
        //TODO:获取需要传递给后端的附加信息
        getExtraDataToBackend();
        //初始化出来一个存入本地数据库点坐标的子线程
        threadPoolExecutor.execute(()->{
            //handler的创建
            Looper.prepare();
            handlerSave = new Handler(Looper.myLooper()){
                @Override
                public void handleMessage(@NonNull Message msg) {
                    if (msg.what==POSITION_SAVE){
                        //每次插入一条数据到数据库
                        AMapLocation aMapLocation = (AMapLocation)msg.obj;
                        TrajectoryPosition trajectoryPosition = new TrajectoryPosition();
                        trajectoryPosition.setLatitude(aMapLocation.getLatitude());
                        trajectoryPosition.setLongitude(aMapLocation.getLongitude());
                        trajectoryPosition.setLocalDate(LocalDateTime.now().toString());
                        trajectoryPosition.setTrajectoryId(trajectoryId);
                        trajectoryPositionsDao.insert(trajectoryPosition);
                        Log.i("aaa","插入成功");
                    }else if(msg.what==POSITION_END){
                        //结束的时候设置结束时间
                        trajectoryDao.updateEndTime(trajectoryId,(String)msg.obj);
                    }
                }
            };
            Looper.loop();
        });
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what==HANDLER_TAKE_PHOTO){
                    cameraManager.takePhotoChangeForBitmap(data -> {
                        if (cameraManager.currentType != CameraManager.CAMERA_TYPE_BACK) {
                            matrix.postRotate(90);
                        }else {
                            matrix.postRotate(270);
                        }
                        if (data != null) {
                            BitmapFactory.Options options = new BitmapFactory.Options();
//                                Bitmap bitmap = null;
                            //压缩并且得到图片的字节数组
                            byte[] byteArray = BitmapUtils.compressBitmap(WorkTrajectoryActivity.this,data, 150, 200, matrix);
                            if (byteArray!=null) {
                                Intent intent = new Intent(WorkTrajectoryActivity.this, JTService.class);
                                intent.putExtra("phone",phone);
                                intent.putExtra(getString(R.string.trajectory_work_service_extra_data_latitude),aMapLocationClient.getLastKnownLocation().getLatitude());
                                intent.putExtra(getString(R.string.trajectory_work_service_extra_data_longitude),aMapLocationClient.getLastKnownLocation().getLongitude());
                                intent.putExtra("workTypeId",workTypeId);
                                intent.putExtra("width",width);
                                //这个是传入的数据
                                intent.putExtra(getString(R.string.trajectory_work_service_extra_data_media),byteArray);
                                //这里是类型标识
                                intent.putExtra(getString(R.string.trajectory_work_service_media_type_key),getString(R.string.trajectory_work_service_media_type_value));
                                startService(intent);
//                                    // 再次显示图片大小
//                                    Toast.makeText(WorkTrajectoryActivity.this, "压缩后" + byteArray.length / 1000000f + "MB", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        };
        //TODO:初始化定位
        initPosition();
    }

    /**
     * 初始化事件
     */
    private void initEvents() {
        btnEnd.setOnClickListener(v->{
            back();
        });
        btnPhoto.setOnClickListener(v->{
            beginOrEndPhoto();
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * 获取附加信息
     */
    private void getExtraDataToBackend() {
        workTypeId = getIntent().getIntExtra("workTypeId",0);
        width = getIntent().getIntExtra("width",0);
        trajectoryId = getIntent().getStringExtra("trajectoryId");
        phone = getIntent().getStringExtra("phone");
        startTime = getIntent().getStringExtra("startTime");
//        construct808();
    }
    /**
     * 初始化坐标
     */
    private void initPosition(){
        try {
            //首先获取定位客户端
            if (aMapLocationClient == null) {
                aMapLocationClient = new AMapLocationClient(this.getApplicationContext());
            }
            //设置定位的一些相关配置项
            setTargetingOption();
            //配置项加载到客户端中
            aMapLocationClient.setLocationOption(aMapLocationClientOption);
            //开启定位
            aMapLocationClient.enableBackgroundLocation(1,buildNotification("后台服务开启"));
            //设置触发定位层
            map.setMyLocationEnabled(true);
            //设置定位回调方法
            setPositionListener();
            //设置视野跟随定位
            map.setMyLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW);
            //设置缩放等级
            CameraUpdate cameraUpdate = CameraUpdateFactory.zoomTo(getResources().getInteger(R.integer.map_camara_size));
            //调整
            map.moveCamera(cameraUpdate);
            //设置可以手势操作
            map.getUiSettings().setZoomGesturesEnabled(true);
            //使用客户端开始定位
            aMapLocationClient.startLocation();
        } catch (Exception e) {
            Log.e(getResources().getString(R.string.trajectory_tag),e.toString());
        }
    }
    /**
     * 设置一些定位的相关配置
     */
    private void setTargetingOption() {
        aMapLocationClientOption = new AMapLocationClientOption();
        //设置定位模式为高精度模式
        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置北斗定位优先
        aMapLocationClientOption.setBeidouFirst(true);
        aMapLocationClientOption.getGeoLanguage();
        //设置wifi自动刷新
        aMapLocationClientOption.setWifiScan(true);
        //设置开启wifi始终扫描
        AMapLocationClientOption.setOpenAlwaysScanWifi(true);
        //五秒定位一次
        aMapLocationClientOption.setInterval(5000);
    }
    /**
     * 设置定位的回调方法
     */
    private void setPositionListener(){
        aMapLocationClient.setLocationListener(new AMapLocationListener() {
            private List<LatLng> positions = new ArrayList<>();
            private final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            //这玩意实在主线程发生的事情
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                //发送给后端
                Intent intent = new Intent(WorkTrajectoryActivity.this, JTService.class);
                intent.putExtra("phone",phone);
                intent.putExtra("latitude",aMapLocation.getLatitude());
                intent.putExtra("longitude",aMapLocation.getLongitude());
                intent.putExtra("workTypeId",workTypeId);
                intent.putExtra("width",width);
                startService(intent);
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                     notificationManager.notify(1,buildNotification("正在定位"));
                }
                //构造轨迹点实体存入轨迹点实体集合
                handlerSave.sendMessage(handlerSave.obtainMessage(POSITION_SAVE,aMapLocation));
                //绘制轨迹
                printTrajectory(aMapLocation);
            }
            /**
             * 绘制轨迹的方法
             * @param aMapLocation 坐标点
             * 逻辑：存入当前点->绘制一条线->清空所有坐标->重新添加当前点
             */
            private void printTrajectory(AMapLocation aMapLocation) {
                //把这次的点存入集合
                positions.add(new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude()));
                //在这里设置轨迹样式
                map.addPolyline(new PolylineOptions()
                        .addAll(positions)
                        .width(getResources().getInteger(R.integer.trajectory_line_size))
                        .setCustomTexture(BitmapDescriptorFactory.fromResource(R.drawable.trajectory_no))
                        .color(getColor(R.color.trajectory_no)))
                        .setGeodesic(true);
                //把所有的点清空然后再次存入这次的点,这样下一次执行这个回调方法的时候绘制出来的线就是这个点和下一个点之间的连线，随时就可以释放掉内存了
                positions.clear();
                positions.add(new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude()));
            }
        });
    }
    /**
     * 退出这个页面
     */
    private void back(){
        if (takePhoto){
            btnPhoto.performClick();
        }
        toastUtil.getToast(this,"上传需要时间处理,请刷新查看最新轨迹");
        //然后终止定位器
        if (aMapLocationClient!=null&&aMapLocationClient.isStarted()) {
            aMapLocationClient.stopLocation();
        }
        Intent intent = new Intent(this, JTService.class);
        stopService(intent);
        //结束轨迹
        endTrajectory(startTime);
        //发送请求
    }
    /**
     * 结束轨迹
     */
    private void endTrajectory(String startTime){
        //结束时间
        String endTime = LocalDateTime.now().toString();
        //修改数据库结束时间
        handlerSave.sendMessage(handlerSave.obtainMessage(POSITION_END,endTime));
        //给后端发送结束轨迹的请求
        endTrajectoryCall = new DataUtil(this).endTrajectory(new WeakReference<>(this),startTime,endTime,trajectoryId);
    }
    /**首先关于权限的问题*/
    private void beginOrEndPhoto(){
        //第一件事情就是检验到底开始拍照还是结束拍照
        if (takePhoto){
            endPhoto();
        }else{
            beginPhoto();
        }
    }
    private void endPhoto() {
        takePhoto = false;
        btnPhoto.setText(getString(R.string.trajectory_work_btn_text_take_photo));
    }
    private void beginPhoto() {
        takePhoto = true;
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        snackbar = PermissionUtils.checkPermission(findViewById(R.id.snack_bar_container_work), this, permissions, new PermissionUtils.CheckPermissionListener() {
            @Override
            public void onHavePermission() {
                haveCameraPermission();
            }

            @Override
            public void noHavePermission(Snackbar snackbar) {

            }
        }, TAG_CAMERA,"需要获取摄像头权限来拍照");
    }
    private void haveCameraPermission(){
        //首先更改按钮的状态
        btnPhoto.setText(getString(R.string.trajectory_work_btn_text_cancel_photo));
        //然后开始拍照
        takePicture();
    }
    private void takePicture() {
        threadPoolExecutor.execute(()->{
            while (takePhoto) {
                try {
                    handler.sendMessage(Message.obtain(handler,HANDLER_TAKE_PHOTO));
                    Thread.sleep(getResources().getInteger(R.integer.take_photo_interval));
//                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Log.e("TAG", "线程睡眠异常", e);
                }
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == TAG_CAMERA){
            //首先关闭snackBar
            if(snackbar!=null) {
                snackbar.dismiss();
            }
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //这里表示用户重新授权了
                haveCameraPermission();
            }else{
                //这里说明用户没有授权
                DialogUtils dialogUtils = new DialogUtils(WorkTrajectoryActivity.this);
                dialogUtils.setContent("没有权限无法开启相机");
                AlertDialog dialog = dialogUtils.getDialog();
                dialog.show();
                dialogUtils.setNegativeListener(v -> {
                    btnPhoto.performClick();
                    dialog.cancel();
                });
                dialogUtils.setPositiveListener(v -> {
                    //如果要重新授权就直接让他重新给权限重新调用一遍直到他给我权限为止
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    lancher.launch(intent);
                    dialog.cancel();
                });
            }
        }
    }
}