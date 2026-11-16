package cn.agrinav.front.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.databinding.ActivityPlotDetailListBinding;
import cn.agrinav.front.adapter.PlotDetailListAdapter;
import cn.agrinav.front.database.AbstractPlotDetailDatabase;
import cn.agrinav.front.database.dao.LandPlotDao;
import cn.agrinav.front.database.entity.LandPlot;
import cn.agrinav.front.entity.SortPlotDetail;
import cn.agrinav.measure.adapter.GsonLocalDateTimeAdapter;
import cn.agrinav.measure.entity.Result;
import cn.agrinav.trajectory.permission.PermissionTrajectory;
import cn.agrinav.utils.DialogUtils;
import cn.agrinav.utils.OkHttpUtils;
import cn.agrinav.utils.ToastUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author 11230
 */
public class PlotDetailListActivity extends AppCompatActivity {

    private ActivityPlotDetailListBinding viewDataBinding;
    private SortPlotDetail sortMessage;
    private ThreadPoolExecutor threadPoolExecutor;
    private SharedPreferences sharedPreferences;
    private ArrayList<LandPlot> landPlotList;
    private PlotDetailListAdapter plotDetailListAdapter;
    private Integer currentPage = 1;
    private Handler handler;
    private LandPlotDao landPlotDao;
    private AMapLocationClient aMapLocationClient;
    private ActivityResultLauncher lancher;
    private AlertDialog dialog;
    private Snackbar snackbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewDataBinding = DataBindingUtil.setContentView(this,R.layout.activity_plot_detail_list);
        sortMessage = new SortPlotDetail();
        viewDataBinding.setSortMessage(sortMessage);
        lancher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->{
            checkTargetingPermissions();
        });
        initEvents();
        initData();
        initHandler();
        checkTargetingPermissions();
        viewDataBinding.gvPlotDetail.setHorizontalSpacing(30);
    }
    private void checkTargetingPermissions() {
        ArrayList<String> permissions = null;
        //在这里把每一个权限都进行一次检验
        for(int i = 0; i< PermissionTrajectory.PERMISSIONS_LOCATION.length; i++) {
            //判断是否拥有某项权限
            if (ContextCompat.checkSelfPermission(this, PermissionTrajectory.PERMISSIONS_LOCATION[i]) != PackageManager.PERMISSION_GRANTED) {
                //判断是否有未获得的权限已经创建了集合，如果不是，现在就开始新建集合
                if(permissions==null){
                    permissions = new ArrayList<>();
                }
                //把欠缺的权限加入到欠缺集合中
                permissions.add(PermissionTrajectory.PERMISSIONS_LOCATION[i]);
            }
        }//判断是否有欠缺权限,如果有就去申请，没有就开始正常操作
        //判断是否有缺少的权限
        if(permissions!=null) {
            String[] strings = new String[permissions.size()];
            for (short j=0;j<strings.length;j++){
                strings[j]=permissions.get(j);
            }
            //在这里设置去请求用户权限
            snackbar = Snackbar.make(findViewById(R.id.refreshLayout_plot_detail_list), "需要定位权限来获取地块信息", Snackbar.LENGTH_INDEFINITE).setAnchorView(R.id.refreshLayout_plot_detail_list);
            snackbar.setBackgroundTint(Color.WHITE);
            snackbar.setTextColor(Color.BLACK);
            snackbar.show();
            ActivityCompat.requestPermissions(this,strings, getResources().getInteger(R.integer.permission_trajectory_request_code));
            Log.i(getResources().getString(R.string.trajectory_tag),"已经去申请权限了");
        }else {
            //如果不缺少定位的所有权限就直接初始化这个页面
            setTargetingOption();
        }
    }
    /**这个方法用于让用户自己去授权
     * 如果授权了就让haveTargetingPermission为true
     * 反之会让他为false*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //如果回调方法中的是trajectory的内容状态码就执行以下逻辑
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        snackbar.dismiss();
        if (requestCode == getResources().getInteger(R.integer.permission_trajectory_request_code)) {
            if (PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                Toast.makeText(this, "用户已授权定位权限", Toast.LENGTH_SHORT).show();
                //用户如果给予了定位权限就直接让他操作了
                setTargetingOption();
            } else {
                //如果没有给权限就提示他，并且让他去给予权限，重新授权，不给权限不让用轨迹功能
                DialogUtils dialogUtils = new DialogUtils(this);
                dialog = dialogUtils.getDialog();
                dialogUtils.setTitle("温馨提示")
                        .setContent("该页面需要使用定位权限来获取周围地块,请授予地块权限")
                        .setPositiveText("去授权")
                        .setNegativeText("取消")
                        .setPositiveListener(v -> {
                             //如果要重新授权就直接让他重新给权限重新调用一遍直到他给我权限为止
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            lancher.launch(intent);
                            dialog.cancel();
                        })
                        .setNegativeListener(v -> {
                            //不给权限不让用这个功能给他个提示顺便窗口不可以关闭
                            DialogUtils dialogUtilsExit = new DialogUtils(this);
                            AlertDialog dialogExit = dialogUtilsExit.getDialog();
                            dialogUtilsExit.setTitle("温馨提示")
                                    .setContent("未授予定位权限,无法获取周围地块即将退出该界面,是否退出")
                                    .setPositiveText("退出")
                                    .setNegativeText("重新授权")
                                    .setPositiveListener(v1 -> {
                                        finish();
                                    })
                                    .setNegativeListener(v1 -> {
                                        //不给权限不让用这个功能给他个提示顺便窗口不可以关闭
                                        //如果要重新授权就直接让他重新给权限重新调用一遍直到他给我权限为止
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        intent.setData(Uri.parse("package:" + getPackageName()));
                                        lancher.launch(intent);
                                        dialogExit.cancel();
                                    });
                            View viewExit = dialogUtilsExit.getView();
                            dialogExit.setContentView(viewExit);
                            dialogExit.show();
                            dialog.cancel();
                        });
                View view = dialogUtils.getView();
                dialog.setContentView(view);
                dialog.show();
            }
        }
    }
    private void initHandler() {
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(android.os.Message msg) {
                super.handleMessage(msg);
            }
        };
    }

    @Override
    protected void onStart() {
        landPlotDao = AbstractPlotDetailDatabase.getInstance(this).getLandPlotDao();
        super.onStart();
    }

    private void initData() {
        threadPoolExecutor = new ThreadPoolExecutor(
                10,
                20,
                10,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        //获取到SharedPreferences
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        //初始化一个地块列表
        landPlotList = new ArrayList<>();
        //初始化适配器
        plotDetailListAdapter = new PlotDetailListAdapter(this, R.layout.item_plot_detail_list, landPlotList);
        viewDataBinding.gvPlotDetail.setAdapter(plotDetailListAdapter);
    }

    /**
     * 刷新逻辑
     */
    private void refresh(){
        if (aMapLocationClient!=null) {
            if (aMapLocationClient.getLastKnownLocation()!=null) {
                threadPoolExecutor.execute(() -> {
                    //先清空
                    landPlotList.clear();
                    //然后获取数据
                    AMapLocation lastKnownLocation = aMapLocationClient.getLastKnownLocation();
                    StringBuilder sb = new StringBuilder();
                    sb.append(lastKnownLocation.getProvince());
                    sb.append(lastKnownLocation.getCity());
                    sb.append(lastKnownLocation.getDistrict());
                    if (sortMessage.getMaxArea().isEmpty() && sortMessage.getMinArea().isEmpty()) {
                        getLandPlotListNoneCondition(1, getResources().getInteger(R.integer.page_size),sb.toString());
                    } else {
                        //按照地块大小查询地块列表
                        getLandPlotListByCondition(1, getResources().getInteger(R.integer.page_size),sb.toString());
                    }
                });
                viewDataBinding.gridFooterPlotDetail.setNoMoreData(false);
                currentPage = 1;
                return;
            }else {
                showTips("正在定位中，请稍后再试");
            }
        }else{
            showTips("请先允许定位权限");
        }
        viewDataBinding.refreshLayoutPlotDetailList.finishRefresh();
    }

    /**
     * 加载逻辑
     */
    private void loadMore() {
        if (aMapLocationClient!=null) {
            if (aMapLocationClient.getLastKnownLocation()!=null) {
                threadPoolExecutor.execute(()->{
                    //加载更多首先判断条件
                    currentPage+=1;
                    AMapLocation lastKnownLocation = aMapLocationClient.getLastKnownLocation();
                    StringBuilder sb = new StringBuilder();
                    sb.append(lastKnownLocation.getProvince());
                    sb.append(lastKnownLocation.getCity());
                    sb.append(lastKnownLocation.getDistrict());
                    if (sortMessage.getMaxArea().isEmpty()&&sortMessage.getMinArea().isEmpty()) {
                        getLandPlotListNoneCondition(currentPage,getResources().getInteger(R.integer.page_size),sb.toString());
                    }else {
                        //按照地块大小查询地块列表
                        getLandPlotListByCondition(currentPage,getResources().getInteger(R.integer.page_size),sb.toString());
                    }
                });
                return;
            }else {
                showTips("正在定位中，请稍后再试");
            }
        }else{
            showTips("请先允许定位权限");
        }
        viewDataBinding.refreshLayoutPlotDetailList.finishLoadMore();
    }
    private void initEvents() {
        viewDataBinding.imageBtnPlotDetailListReset.setOnClickListener(v -> {
            sortMessage.setMaxArea("");
            sortMessage.setMinArea("");
            refresh();
        });
        viewDataBinding.imageBtnPlotDetailListSearch.setOnClickListener(v -> {
            search();
        });
        viewDataBinding.refreshLayoutPlotDetailList.setOnRefreshListener(refreshLayout -> {
            refresh();
        });
        viewDataBinding.refreshLayoutPlotDetailList.setOnLoadMoreListener(refreshLayout -> {
            loadMore();
        });
    }

    private void search() {
        //查询就是按条件刷新
        if (sortMessage.getMaxArea()!=null&&sortMessage.getMinArea()!=null){
            if (sortMessage.getMaxArea().isEmpty()&&sortMessage.getMinArea().isEmpty()){
                showTips("请输入查询的面积条件");
            }else {
                refresh();
            }
        }else {
            Log.e(getString(R.string.front_plot_detail_list_log_tag_activity_item), "面积筛选内容为空");
        }
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
        if (aMapLocationClient != null) {
            if (aMapLocationClient.isStarted()){
                aMapLocationClient.stopLocation();
            }
            aMapLocationClient.onDestroy();
        }
    }

    private void getLandPlotListByCondition(int pageNum, int pageSize,String address) {
        if (address!=null) {
            HashMap<String, Object> pageMap = new HashMap<>();
            pageMap.put(getString(R.string.font_plot_detail_list_request_get_plot_by_condition_param_current),pageNum);
            pageMap.put(getString(R.string.font_plot_detail_list_request_get_plot_by_condition_param_size),pageSize);
            pageMap.put(getString(R.string.font_plot_detail_list_request_get_plot_by_condition_param_address),address);
            pageMap.put("isCurrent",false);
            Integer areaMin = null;
            Integer areaMax = null;
            //判断面积合法性
            if(sortMessage.getMinArea()!=null&&!sortMessage.getMinArea().isEmpty()){
                if (sortMessage.getMinArea().length()<10) {
                    areaMin = Integer.valueOf(sortMessage.getMinArea());
                    pageMap.put(getString(R.string.font_plot_detail_list_request_get_plot_by_condition_param_area_min), areaMin);
                }else{
                    handler.post(()->{
                        showTips("输入的最小值面积过大");
                    });
                    return;
                }
            }else {
                areaMin = 0;
                pageMap.put(getString(R.string.font_plot_detail_list_request_get_plot_by_condition_param_area_min),areaMin);
            }
            if (sortMessage.getMaxArea()!=null&&!sortMessage.getMaxArea().isEmpty()){
                if (sortMessage.getMaxArea().length()<10) {
                    areaMax = Integer.valueOf(sortMessage.getMaxArea());
                    pageMap.put(getString(R.string.font_plot_detail_list_request_get_plot_by_condition_param_area_max),areaMax);
                }else{
                    handler.post(()->{
                        showTips("输入的最大值面积过大");
                    });
                    return;
                }
            }else {
                areaMax = 1800000000;
                pageMap.put(getString(R.string.font_plot_detail_list_request_get_plot_by_condition_param_area_max),areaMax);
            }
            //判断最大值最小值的合法性(
            if (areaMax<areaMin){
                handler.post(()->{
                    new ToastUtil().getToast(this,"最大值小于最小值");
                    return;
                });
            }
            //访问后端
            OkHttpUtils okHttpUtils = new OkHttpUtils();
            Request request = okHttpUtils.createBuilder(getResources().getString(R.string.server_url), getResources().getString(R.string.font_plot_detail_list_request_get_plot_by_condition), pageMap)
                    .addHeader(getString(R.string.request_header_key_token), sharedPreferences.getString(getString(R.string.user_pref_key_token), ""))
                    .get()
                    .build();
            okHttpUtils.sendRequest(request, new LandPlotListCallBack());
        }
    }
    private void getLandPlotListNoneCondition(int pageNum,int pageSize,String address) {
        if (address!=null) {
            HashMap<String, Object> pageMap = new HashMap<>();
            pageMap.put(getString(R.string.font_plot_detail_list_request_get_plot_by_condition_param_current),pageNum);
            pageMap.put(getString(R.string.font_plot_detail_list_request_get_plot_by_condition_param_size),pageSize);
            pageMap.put(getString(R.string.font_plot_detail_list_request_get_plot_by_condition_param_address),address);
            pageMap.put("isCurrent",false);
            //访问后端
            OkHttpUtils okHttpUtils = new OkHttpUtils();
            Request request= okHttpUtils.createBuilder(getResources().getString(R.string.server_url),getResources().getString(R.string.font_plot_detail_list_request_get_plot_by_condition),pageMap)
                .addHeader(getString(R.string.request_header_key_token),sharedPreferences.getString(getString(R.string.user_pref_key_token),""))
                .get()
                .build();
            okHttpUtils.sendRequest(request,new LandPlotListCallBack());
        }
    }
    private class LandPlotListCallBack implements Callback{
        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
            showToast(getString(R.string.toast_err_net_or_server));
            // 添加日志记录
            Log.e(getString(R.string.front_plot_detail_list_log_tag_activity_item), "网络请求失败", e);
        }
        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            if (response.isSuccessful()) {
                if (response.body() != null){
                    String json = response.body().string();
                    //添加适配器
                    GsonBuilder gsonBuilder = new GsonBuilder();
                    //适配localDateTime
                    gsonBuilder.registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTimeAdapter());
                    Gson gson = gsonBuilder.create();
                    Result<ArrayList<LandPlot>> result = gson.fromJson(json,new TypeToken<Result<List<LandPlot>>>() {}.getType());
                    if (result.getCode() == getResources().getInteger(R.integer.request_success)) {
                        //这里是正常的获取到了数据的处理方式
                        if (!result.getData().isEmpty()){
                            landPlotList.addAll(result.getData());
                            landPlotDao.insert(result.getData());
                        }else {
                            notifyNullData();
                        }
                        handler.post(()->{
                            plotDetailListAdapter.notifyDataSetChanged(landPlotList);
                        });
                    } else {
                        showToast(result.getMsg());
                    }
                }else {
                    notifyNullData();
                }
                handler.post(()->{
                    //更改加载栏的状态
                    if(viewDataBinding.refreshLayoutPlotDetailList.isRefreshing()){
                        viewDataBinding.refreshLayoutPlotDetailList.finishRefresh();
                    }
                    if (viewDataBinding.refreshLayoutPlotDetailList.isLoading()){
                        viewDataBinding.refreshLayoutPlotDetailList.finishLoadMore();
                    }
                });
            } else {
                showToast(getString(R.string.toast_err_request));
            }
        }
        private void notifyNullData(){
            handler.post(()->{
                if (viewDataBinding.refreshLayoutPlotDetailList.isLoading()){
                    viewDataBinding.gridFooterPlotDetail.setNoMoreData(true);
                    currentPage-=1;
                }
            });
        }
        private void showToast(String message) {
            // 使用 Handler 在主线程中显示 Toast
            handler.post(() -> new ToastUtil().getToast(PlotDetailListActivity.this, message));
        }
    }
    /**
     * 设置定位参数并且开始定位
     */
    private void setTargetingOption() {
        //首先获取定位客户端
        aMapLocationClient = null;
        try {
            aMapLocationClient = new AMapLocationClient(this);
        } catch (Exception e) {
            showTips(getString(R.string.toast_trajectory_location_error));
        }
        AMapLocationClientOption aMapLocationClientOption = new AMapLocationClientOption();
        //设置定位模式为高精度模式
        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置北斗定位优先
        aMapLocationClientOption.setBeidouFirst(true);
        aMapLocationClientOption.getGeoLanguage();
        //设置wifi自动刷新
        aMapLocationClientOption.setWifiScan(true);
        //设置开启wifi始终扫描
        AMapLocationClientOption.setOpenAlwaysScanWifi(true);
        aMapLocationClientOption.setInterval(2000);
        //配置项加载到客户端中
        aMapLocationClient.setLocationOption(aMapLocationClientOption);
        //使用客户端开始定位
        aMapLocationClient.startLocation();

        refresh();
    }
    /**
     * 显示提示信息
     * @param content 提示信息的内容
     */
    private void showTips(String content){
        new ToastUtil().getToast(this,content);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent(this, BaseActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(getString(R.string.fragment_index), getResources().getInteger(R.integer.fragment_index_front));
            startActivity(intent);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}