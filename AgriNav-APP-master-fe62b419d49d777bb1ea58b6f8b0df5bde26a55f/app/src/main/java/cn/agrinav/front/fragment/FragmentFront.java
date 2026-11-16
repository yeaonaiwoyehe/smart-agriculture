package cn.agrinav.front.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.weather.LocalDayWeatherForecast;
import com.amap.api.services.weather.LocalWeatherForecast;
import com.amap.api.services.weather.LocalWeatherForecastResult;
import com.amap.api.services.weather.LocalWeatherLive;
import com.amap.api.services.weather.LocalWeatherLiveResult;
import com.amap.api.services.weather.WeatherSearch;
import com.amap.api.services.weather.WeatherSearchQuery;
import com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout;
import com.donkingliang.consecutivescroller.ConsecutiveViewPager2;
import com.google.android.material.search.SearchBar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshFooter;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.scwang.smart.refresh.layout.simple.SimpleMultiListener;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.agrinav.R;
import cn.agrinav.front.activity.PlotDetailListActivity;
import cn.agrinav.front.activity.ScanCodeActivity;
import cn.agrinav.front.activity.findfarm.FindFarmActivity;
import cn.agrinav.front.activity.findjob.FindJobActivity;
import cn.agrinav.front.activity.sysmessage.ReceiveSysMessageActivity;
import cn.agrinav.front.activity.sysmessage.utils.SysMessageUtils;
import cn.agrinav.front.adapter.ViewPagerAdapter;
import cn.agrinav.front.util.CommonUtils;
import cn.agrinav.front.util.QueryUtils;
import cn.agrinav.front.util.TabStyleUtil;
import cn.agrinav.measure.utils.DefaultInfoUtils;
import cn.agrinav.measure.utils.PermissionMeasure;
import cn.agrinav.measure.utils.ThreadPoolUtils;

/**
 * @author 18506
 */
public class FragmentFront extends Fragment implements WeatherSearch.OnWeatherSearchListener {
    // 页面
    private AppCompatActivity activity;
    private SharedPreferences sharedPreferences;
    //滑动页面
    //滑动页面
    private List<cn.agrinav.front.fragment.MyFragment> fragmentList;
    //搜索组件
    private SearchBar search;
    //客服
    private ImageButton customService;
    //消息站
    private ImageButton message;
    //更多
    private ImageButton more;
    //搜索的文本
    private String text;
    private ImageButton findJob;
    private ImageButton findMachine;
    private ImageButton collectedLand;
    private ImageButton myLand;
    private TextView textPlotNumber;
    private TextView textPlotArea;
    private TextView enterDetailSum, enterDetailArea;
    private TabLayout tabLayout;
    private ConsecutiveViewPager2 pager;
    private AMapLocationClient aMapLocationClient;
    private TextView textFrontAddress;
    private SmartRefreshLayout rlRefresh;
    private ViewPager2 pager2;
    private ConsecutiveScrollerLayout scrollerLayout;
    private TextView weather;

    private ActivityResultLauncher<Intent> intentActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

    });
    private View messageRedDot;

    /**这是构造方法*/
    public FragmentFront() {
    }
    /**这是含参构造方法*/
    public FragmentFront(AppCompatActivity activity, SharedPreferences sharedPreferences) {
        this.activity = activity;
        this.sharedPreferences = sharedPreferences;
        ThreadPoolUtils.initThreadPool();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i(getString(R.string.trajectory_tag),"fragmentTrajectory_onCreate");
        super.onCreate(savedInstanceState);
        setTargetingOption();
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.fragment_front, null);
        AMapLocationClient.updatePrivacyShow(activity, true, true);
        AMapLocationClient.updatePrivacyAgree(activity, true);
        initViews(view);
        // 检查是否有未读消息并显示红点
        updateMessageDotStatus();
        initEvents(view);
        return view;
    }

    private void updateMessageDotStatus() {
        if (SysMessageUtils.hasUnreadMessages(requireContext())) {
            messageRedDot.setVisibility(View.VISIBLE);
        } else {
            messageRedDot.setVisibility(View.GONE);
        }
    }

    private void checkTargetingPermissions(Context context) {
        ArrayList<String> permissions = null;
        
        // 检查定位权限
        for(String permission : PermissionMeasure.PERMISSIONS_LOCATION) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                if(permissions == null) {
                    permissions = new ArrayList<>();
                }
                permissions.add(permission);
            }
        }
        
        if(permissions != null) {
            // 显示自定义对话框
            showPermissionDialog();
        } else {
            // 有权限，重新初始化定位
            if (aMapLocationClient != null) {
                aMapLocationClient.stopLocation();
                aMapLocationClient.onDestroy();
            }
            setTargetingOption();
            updateLocationDisplay();
        }
    }

    private void showPermissionDialog() {
        // 检查是否已经存在显示的对话框
        if(activity.getWindow().getDecorView().findViewWithTag("permission_dialog") != null) {
            return;
        }
        
        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle("需要定位权限")
            .setMessage("请在设置中开启定位权限，以便获取您的位置信息")
            .setPositiveButton("去设置", (dialogInterface, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
                dialogInterface.dismiss();
            })
            .setNegativeButton("取消", (dialogInterface, which) -> {
                dialogInterface.dismiss();
            })
            .create();
        
        // 给对话框设置tag，用于判断是否已经显示
        Objects.requireNonNull(dialog.getWindow()).getDecorView().setTag("permission_dialog");
        dialog.show();
    }

    private void updateLocationDisplay() {
        if (aMapLocationClient != null && aMapLocationClient.getLastKnownLocation() != null) {
            String district = aMapLocationClient.getLastKnownLocation().getDistrict();
            textFrontAddress.setText(district);
            // 更新天气信息
            initWeather();
            getData(); // 重新获取数据
        } else {
            textFrontAddress.setText("未授权");
        }
    }

    /**
     * 设置定位参数并且开始定位
     */
    private void setTargetingOption() {
        //首先获取定位客户端
        aMapLocationClient = null;
        try {
            aMapLocationClient = new AMapLocationClient(activity.getApplicationContext());
            // 添加定位监听器
            aMapLocationClient.setLocationListener(location -> {
                if (location != null) {
                    String district = location.getDistrict();
                    activity.runOnUiThread(() -> {
                        textFrontAddress.setText(district);
                        initWeather();
                        getData(); // 位置更新时重新获取数据
                    });
                }
            });
        } catch (Exception e) {
            Log.e("FragmentFront", "Error initializing location client: " + e.getMessage());
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
    }

    private void initEvents(View view) {
        //刷新和加载更多传给Fragment
        initRefreshFragment(rlRefresh);
        //搜索
        //TODO 其他功能
        customService.setOnClickListener(v -> {

        });
        message.setOnClickListener(v -> {
            Intent intent = new Intent(activity, ReceiveSysMessageActivity.class);
            startActivity(intent);
        });
        more.setOnClickListener(v -> {
            Intent intent = new Intent(activity, ScanCodeActivity.class);
            intentActivityResultLauncher.launch(intent);
        });
        //农机找活
        findJob.setOnClickListener(v -> {
            Intent intent = new Intent(activity, FindJobActivity.class);
            startActivity(intent);
        });
        //寻找农机
        findMachine.setOnClickListener(v -> {
            Intent intent = new Intent(activity, FindFarmActivity.class);
            startActivity(intent);
        });
        //已收土地 TODO
        collectedLand.setOnClickListener(v -> {
//            Intent intent = new Intent();
//            startActivity(intent);
        });
        //我的土地 TODO
        myLand.setOnClickListener(v -> {
//            Intent intent = new Intent();
//            startActivity(intent);
        });
        //进入详情页
        enterDetailSum.setOnClickListener(v -> {
            Intent intent = new Intent(activity, PlotDetailListActivity.class);
            startActivity(intent);
        });
        //进入详情页
        enterDetailArea.setOnClickListener(v -> {
            Intent intent = new Intent(activity, PlotDetailListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }

    private void initRefreshFragment(SmartRefreshLayout rlRefresh) {
        //将刷新和加载更多传给Fragment
        rlRefresh.setOnRefreshLoadMoreListener(new OnRefreshLoadMoreListener(){
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                if (pager2.getCurrentItem()==0){
                    FragmentFrontFreeAgriculturalMachinery fragment = (FragmentFrontFreeAgriculturalMachinery) fragmentList.get(0);
                    fragment.onLoadMore((SmartRefreshLayout) refreshLayout);
                } else if (pager2.getCurrentItem()==1) {
                    FragmentFrontDemandFarmWork fragment = (FragmentFrontDemandFarmWork) fragmentList.get(1);
                    fragment.onLoadMore((SmartRefreshLayout) refreshLayout);
                }else{
                    FragmentFrontLandHeat fragment = (FragmentFrontLandHeat) fragmentList.get(2);
                    fragment.onLoadMore((SmartRefreshLayout) refreshLayout);
                }
            }
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                rlRefresh.finishRefresh();
            }
        });
        //固定顶部
        rlRefresh.setOnMultiListener(new SimpleMultiListener() {
            @Override
            public void onFooterMoving(RefreshFooter footer, boolean isDragging, float percent, int offset, int footerHeight, int maxDragHeight) {
                scrollerLayout.setStickyOffset(offset);
            }
        });
    }

    private void initViews(View view) {
        //刷新布局
        rlRefresh = view.findViewById(R.id.rl_refresh);
        //滑动布局
        scrollerLayout = view.findViewById(R.id.scrollerLayout);
        //定位
        textFrontAddress = view.findViewById(R.id.tv_front_address);
        weather = view.findViewById(R.id.tv_weather);
        //搜索栏
//        search = view.findViewById(R.id.front_search);
        //客服
        customService = view.findViewById(R.id.image_button_custom_service);
        //消息站
        message = view.findViewById(R.id.image_button_message);
        messageRedDot = view.findViewById(R.id.message_red_dot);
        //更多
        more = view.findViewById(R.id.image_button_more);
        //农机找活
        findJob = view.findViewById(R.id.image_button_find_job);
        //寻找农机
        findMachine = view.findViewById(R.id.image_button_find_machinery);
        //已收土地
        collectedLand = view.findViewById(R.id.image_button_collected_land);
        //我的土地
        myLand = view.findViewById(R.id.image_button_my_land);
        //地块面积
        textPlotArea = view.findViewById(R.id.tv_plot_area);
        //地块数
        textPlotNumber = view.findViewById(R.id.tv_plot_sum);
        //详情按钮
        enterDetailSum = view.findViewById(R.id.tv_enter1);
        enterDetailArea = view.findViewById(R.id.tv_enter2);
        //滑动栏
        tabLayout = view.findViewById(R.id.tabLayout);
        //滑动页面
        pager = view.findViewById(R.id.pager);
        pager2 = pager.getViewPager2();
        pager2.setUserInputEnabled(false);
        //设置页面
        initFragments();
        //绑定页面和滑动栏
        setViewPager(pager2);
    }

    private void getData() {
        // 获取地块总面积数据
        ThreadPoolUtils.threadPoolExecutor.execute(() -> {
            try {
                Float landPlotAreaSum = QueryUtils.getLandPlotAreaSum(
                        getString(R.string.server_url),
                        getString(R.string.measure_url)+getString(R.string.areaByAddress),
                    textFrontAddress.getText().toString(),
                    DefaultInfoUtils.getToken(activity)
                );
                CommonUtils.waitDataGetting(landPlotAreaSum, activity);

                activity.runOnUiThread(() -> {
                    if (landPlotAreaSum!=-0.1f){
                        String areaText = String.format("%.2f", landPlotAreaSum);
                        textPlotArea.setText(areaText);
                    }else{
                        Toast.makeText(activity, "获取信息失败", Toast.LENGTH_SHORT).show();
                        textPlotArea.setText("0");
                    }
                });
            } catch (Exception e) {
                Log.e("FragmentFront", "Error getting land plot area: " + e.getMessage());
            }
        });


        //获取地块总数量
        ThreadPoolUtils.threadPoolExecutor.execute(() -> {
            try {
                Integer landPlotNum = QueryUtils.getLandPlotNum(getString(R.string.server_url),
                        getString(R.string.measure_url)+getString(R.string.query_url),
                        textFrontAddress.getText().toString(),
                        DefaultInfoUtils.getToken(activity)
                );
                CommonUtils.waitDataGetting(landPlotNum, activity);

                activity.runOnUiThread(() ->{
                    if (landPlotNum!=-1){
                        textPlotNumber.setText(landPlotNum.toString());
                    }else{
                        Toast.makeText(activity, "获取信息失败", Toast.LENGTH_SHORT).show();
                        textPlotNumber.setText("0");
                    }
                });
            } catch (Exception e) {
                Log.e("FragmentFront", "Error getting land plot number: " + e.getMessage());
            }
        });
    }

    /**
     * 实现天气实况
     */
    private void initWeather() {
        try {
            // 创建天气查询
            WeatherSearchQuery query = new WeatherSearchQuery(
                    textFrontAddress.getText().toString(),
                    WeatherSearchQuery.WEATHER_TYPE_FORECAST
            );

            WeatherSearch weatherSearch = new WeatherSearch(activity);
            weatherSearch.setOnWeatherSearchListener(this);
            weatherSearch.setQuery(query);

            // 添加错误处理和日志
            weatherSearch.searchWeatherAsyn();
        } catch (AMapException e) {
            weather.setText("获取天气失败");
        }
    }

    private void initFragments() {
        fragmentList = new ArrayList<>();
        fragmentList.add(new FragmentFrontFreeAgriculturalMachinery(new SoftReference<>(activity),sharedPreferences));
        fragmentList.add(new FragmentFrontDemandFarmWork(new SoftReference<>(activity),sharedPreferences));
        fragmentList.add(new FragmentFrontLandHeat(new SoftReference<>(activity),sharedPreferences));
    }

    private void setViewPager(ViewPager2 pager) {
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(fragmentList,activity);
        pager.setAdapter(viewPagerAdapter);
        //关联起来
        TabLayoutMediator tabLayoutMediator = new TabLayoutMediator(
                tabLayout,
                pager,
                (tab, position) -> {
                    //todo
                    // 根据位置设置对应的标题
                    switch (position) {
                        case 0:
                            tab.setText(R.string.idle_agricultural_machinery);
                            break;
                        case 1:
                            tab.setText(R.string.agricultural_work_demand);
                            break;
                        case 2:
                            tab.setText(R.string.plot_heat);
                            break;
                    }
                }
        );
        //绑定
        tabLayoutMediator.attach();
        //监听滑动栏
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                TabStyleUtil.setTabBold(tab,true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onWeatherLiveSearched(LocalWeatherLiveResult result, int code) {
        if (code == 1000) {
            if (result != null && result.getLiveResult() != null) {
                LocalWeatherLive weatherLive = result.getLiveResult();
                weather.setText(weatherLive.getTemperature() + "°C");
            } else {
                weather.setText("暂无天气数据");
            }
        } else {
            weather.setText("获取天气失败");
        }
    }

    @Override
    public void onWeatherForecastSearched(LocalWeatherForecastResult result, int code) {
        if (code == 1000) {
            if (result != null && result.getForecastResult() != null) {
                LocalWeatherForecast weatherForecast = result.getForecastResult();
                List<LocalDayWeatherForecast> weatherLists = weatherForecast.getWeatherForecast();
                if(weatherLists!=null&&!weatherLists.isEmpty()){
                    weather.setText(weatherLists.get(0).getNightTemp()+"°C/"+weatherLists.get(0).getDayTemp()+"°C"+" "+weatherLists.get(0).getDayWeather());
                }
            } else {
                weather.setText("暂无天气数据");
            }
        } else {
            weather.setText("获取天气失败");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次回到页面时检查权限
        checkTargetingPermissions(activity);
          // 每次页面恢复时检查未读状态并更新红点
        updateMessageDotStatus();
    }

}
