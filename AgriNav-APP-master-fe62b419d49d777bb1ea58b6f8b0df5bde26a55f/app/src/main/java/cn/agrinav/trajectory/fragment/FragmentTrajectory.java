package cn.agrinav.trajectory.fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.timepicker.MaterialTimePicker;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.agrinav.MainActivity;
import cn.agrinav.R;
import cn.agrinav.measure.entity.LandAddress;
import cn.agrinav.measure.utils.DefaultInfoUtils;
import cn.agrinav.measure.utils.QueryUtils;
import cn.agrinav.trajectory.activity.TrajectoryActivity;
import cn.agrinav.trajectory.entity.SortMessage;
import cn.agrinav.trajectory.util.DataUtil;
import cn.agrinav.utils.ToastUtil;

/**
 * @author 11230
 * 这个fragment一onstart就去拿作物数据
 * 筛选条件：作业类型、作业地址、作业时间、作业人
 * 可以跳入工作页
 */
public class FragmentTrajectory extends Fragment {
    private String phone;
    private final AppCompatActivity activity;
    private final SharedPreferences sharedPreferences;
    /**
     * 定位客户端
     */
    private AMapLocationClient aMapLocationClient;
    private boolean isRight;
    private DrawerLayout drawerLayout;
    private Button btnLeft;
    private Button btnRight;
    private ImageView btnWork;
    private LinearLayout btnSort;
    private TextView sortTvCorn;
    private TextView sortTvWorkType;
    private TextView tvDate;
    private TextView tvStartTime;
    private TextView tvEndTime;
    private ImageButton imageButton;
    private TextView province;
    private TextView city;
    private TextView country;
    private TextView townShip;
    private TextView village;
    private ImageButton btnProvince;
    private ImageButton btnCity;
    private ImageButton btnCountry;
    private ImageButton btnTownShip;
    private ImageButton btnVillage;
    private EditText tvPhone;
    private Button btnCancel;
    private Button btnReset;
    private Button btnConfirm;
    private FragmentTrajectoryWork fragmentTrajectoryWork;
    private FragmentTrajectoryOrigin fragmentTrajectoryOrigin;
    private SortMessage workSortMessage;
    private SortMessage originSortMessage;
    private final ConcurrentHashMap<String, Long> cornEntities;
    private final ConcurrentHashMap<String, Long> workTypeEntities;
    private final ThreadPoolExecutor threadPoolExecutor;
    private MaterialDatePicker<Long> datePickerView;
    private MaterialTimePicker timeBeginPickerView;
    private MaterialTimePicker timeEndPickerView;
    private PopupMenu popupMenuWork;
    private PopupMenu popupMenuCorn;
    private PopupMenu provinceMenu;
    private PopupMenu cityMenu;
    private PopupMenu countryMenu;
    private PopupMenu townShipMenu;
    private PopupMenu villageMenu;
    private List<LandAddress> provinceList;
    private List<LandAddress> cityList;
    private List<LandAddress> countryList;
    private List<LandAddress> townShipList;
    private List<LandAddress> villageList;
    public FragmentTrajectory(){
        cornEntities = null;
        workTypeEntities = null;
        threadPoolExecutor = null;
        activity = null;
        sharedPreferences = null;
    }
    public FragmentTrajectory(AppCompatActivity activity, SharedPreferences sharedPreferences) {
        this.activity = activity;
        this.sharedPreferences = sharedPreferences;
        //维护两个hashMap用来寻找作物或作业类型对应的id
        cornEntities = new ConcurrentHashMap<>();
        workTypeEntities = new ConcurrentHashMap<>();
        threadPoolExecutor = new ThreadPoolExecutor(
                10,
                20,
                10,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        Log.i("aa","Trajectory构造");
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(sharedPreferences!=null) {
            phone = sharedPreferences.getString(getString(R.string.user_pref_key_phone), "");
        }
        //维护两个筛选条件，到这里开始设置一下gridView的初始状态
        workSortMessage = new SortMessage(phone);
        originSortMessage = new SortMessage(phone);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i(getString(R.string.trajectory_tag),"fragmentTrajectory_onCreate");
        super.onCreate(savedInstanceState);
        if (activity==null&&sharedPreferences==null&&cornEntities==null&&workTypeEntities==null&&threadPoolExecutor==null){
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        setTargetingOption();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(getString(R.string.trajectory_tag),"fragmentTrajectory_onCreateView");
        View view = inflater.inflate(R.layout.fragment_trajectory,null);
        //初始化页面组件
        initViews(view);
        initData();
        initEvents();
        setViewPager2();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(getString(R.string.trajectory_tag),"fragmentTrajectory_onStart");
        //获取菜单数据
        if (cornEntities!=null) {
            threadPoolExecutor.execute(() -> new DataUtil(activity).getCornData(popupMenuCorn, cornEntities));
        }
        //获取省份数据
        threadPoolExecutor.execute(()-> {
            provinceList = QueryUtils.getLandProvince(getResources().getString(R.string.server_url),
                    getResources().getString(R.string.address_url)+ getResources().getString(R.string.province_url),
                    DefaultInfoUtils.getToken(activity));
            while(provinceList==null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e("aaa",e.getMessage());
                }
            }
            Menu menu = provinceMenu.getMenu();
            provinceList.forEach(
                    landAddress -> menu.add(Menu.NONE, menu.size(), menu.size(), landAddress.getName())
            );
        });
        //加载的时候要根据是否曾经打开过以及切换过来进行切换
        if (!isRight) {
            btnLeft.performClick();
        }else {
            btnRight.performClick();
        }

    }
    @Override
    public void onResume() {
        Log.i(getString(R.string.trajectory_tag),"fragmentTrajectory_onResume");
        super.onResume();
    }
    @Override
    public void onPause() {
        Log.i(getString(R.string.trajectory_tag),"fragmentTrajectory_onPause");
        super.onPause();
    }
    @Override
    public void onStop() {
        Log.i(getString(R.string.trajectory_tag),"fragmentTrajectory_onStop");
        super.onStop();
    }
    @Override
    public void onDestroyView() {
        Log.i(getString(R.string.trajectory_tag),"fragmentTrajectory_onDestroyView");
        if (aMapLocationClient!=null) {
            aMapLocationClient.stopLocation();
            aMapLocationClient.onDestroy();
        }
        super.onDestroyView();
    }
    @Override
    public void onDestroy() {
        Log.i(getString(R.string.trajectory_tag),"fragmentTrajectory_onDestroy");
        super.onDestroy();
    }
    private void initViews(View view){
        drawerLayout = view.findViewById(R.id.drawerLayout_trajectory);
        //左按钮
        btnLeft = view.findViewById(R.id.buttonLeft);
        //右按钮
        btnRight = view.findViewById(R.id.buttonRight);
        //进入农事作业按钮
        btnWork = view.findViewById(R.id.tv_begin_work);
        //筛选layout
        btnSort = view.findViewById(R.id.ll_trajectory_sort);
        //Navigation
        NavigationView navigationView = view.findViewById(R.id.nav_trajectory_select);
        //初始化子控件
        initNavigation(navigationView.getHeaderView(0));
    }
    private void initNavigation(View navigationView){
        //作物按钮
        ImageView sortBtnCorn = navigationView.findViewById(R.id.img_trajectory_sort_corn);
        //作物文本输入框
        sortTvCorn = navigationView.findViewById(R.id.tv_trajectory_navigation_item_corn);
        //作业类型按钮
        ImageView sortBtnWorkType = navigationView.findViewById(R.id.img_trajectory_sort_corn_type);
        //作业类型文本输入框\
        sortTvWorkType = navigationView.findViewById(R.id.tv_trajectory_navigation_item_corn_type);
        //作业日期框
        tvDate = navigationView.findViewById(R.id.tv_trajectory_navigation_item_date);
        //开始时间框
        tvStartTime = navigationView.findViewById(R.id.tv_trajectory_navigation_item_time_begin);
        //结束时间框
        tvEndTime = navigationView.findViewById(R.id.tv_trajectory_navigation_item_time_end);
        //定位框
        imageButton = navigationView.findViewById(R.id.image_location);
        //地理位置信息和对应的按钮
        province = navigationView.findViewById(R.id.tv_trajectory_address_province);
        city = navigationView.findViewById(R.id.tv_trajectory_address_city);
        country = navigationView.findViewById(R.id.tv_trajectory_address_country);
        townShip = navigationView.findViewById(R.id.tv_trajectory_address_township);
        village = navigationView.findViewById(R.id.tv_trajectory_address_village);
        btnProvince = navigationView.findViewById(R.id.image_trajectory_address_province);
        btnCity = navigationView.findViewById(R.id.image_trajectory_address_city);
        btnCountry = navigationView.findViewById(R.id.image_trajectory_address_country);
        btnTownShip = navigationView.findViewById(R.id.image_trajectory_address_township);
        btnVillage = navigationView.findViewById(R.id.image_trajectory_address_village);
        //手机号输入栏
        tvPhone = navigationView.findViewById(R.id.edt_trajectory_navigation_item_phone);
        btnCancel = navigationView.findViewById(R.id.btn_trajectory_navigation_item_cancel);
        //重置按钮
        btnReset = navigationView.findViewById(R.id.btn_trajectory_navigation_item_return);
        //确定按钮
        btnConfirm = navigationView.findViewById(R.id.btn_trajectory_navigation_item_ok);
    }
    /**
     * 这里设置两个子视图的一些相关信息
     */
    private void setViewPager2() {
        fragmentTrajectoryWork = new FragmentTrajectoryWork(new WeakReference<>(activity),new WeakReference<>(workSortMessage));
        fragmentTrajectoryOrigin = new FragmentTrajectoryOrigin(new WeakReference<>(activity),new WeakReference<>(originSortMessage));
    }
    /**
     * 切换视图的时候调用的方法
     */
    private void switchFragment(Fragment fragment) {
        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.vp_trajectory, fragment);
        transaction.commitAllowingStateLoss();
    }
    private void initData(){
        //日期选择器
        datePickerView = MaterialDatePicker.Builder.datePicker().build();
        //开始时间选择器
        timeBeginPickerView = new MaterialTimePicker.Builder().setTitleText("选择开始时间").build();
        //结束时间选择器
        timeEndPickerView = new MaterialTimePicker.Builder().setTitleText("选择开始时间").build();
        //设置作物菜单选项的下拉框,先有了，onStart的时候才拿数据
        popupMenuWork = new PopupMenu(activity, sortTvWorkType);
        //设置作业菜单选项的下拉框先有了，onStart的时候才拿数据
        popupMenuCorn = new PopupMenu(activity, sortTvCorn);
        provinceMenu = new PopupMenu(getActivity(), btnProvince);
        cityMenu = new PopupMenu(getActivity(), btnCity);
        countryMenu = new PopupMenu(getActivity(), btnCountry);
        townShipMenu = new PopupMenu(getActivity(), btnTownShip);
        villageMenu = new PopupMenu(getActivity(), btnVillage);
    }
    private void initEvents(){
        btnLeft.setOnClickListener(v->{
            //首先不允许点击本按钮只允许点击另一个按钮
            btnLeft.setEnabled(false);
            btnRight.setEnabled(true);
            //当前是Origin页面，返回到原页面
            isRight = false;
            switchFragment(fragmentTrajectoryWork);
            buttonBackgroundTurnToLeft();
        });
        btnRight.setOnClickListener(v->{
            btnRight.setEnabled(false);
            btnLeft.setEnabled(true);
            isRight = true;
            //当前是Work页面，返回到原页面
            switchFragment(fragmentTrajectoryOrigin);
            //除此之外还要进行颜色的转化
            buttonBackgroundTurnToRight();
        });
        btnConfirm.setOnClickListener(v-> confirm());
        btnCancel.setOnClickListener(v-> closeNavigation());
        btnReset.setOnClickListener(v-> reset());
        btnWork.setOnClickListener(v -> {
            Intent intent = new Intent(activity, TrajectoryActivity.class);
            //直接清空，他想退出来就直接给他重新加载一遍
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
        btnSort.setOnClickListener(v->{
            //点击筛选按钮之后首先展开navigation
            openNavigation();
            Log.i(getString(R.string.trajectory_tag),"开启了navigation");
        });
        btnProvince.setOnClickListener(v->{
            if (provinceMenu.getMenu()!=null&& provinceMenu.getMenu().size()>0){
                provinceMenu.show();
            }else {
                showTips(getString(R.string.toast_trajectory_data_address_error));
            }
        });
        btnCity.setOnClickListener(v->{
            if (cityMenu.getMenu()!=null&& cityMenu.getMenu().hasVisibleItems()&&!province.getText().toString().isEmpty()){
                cityMenu.show();
            }else {
                showTips(getString(R.string.toast_trajectory_location_province));
            }
        });
        btnCountry.setOnClickListener(v->{
            if (countryMenu.getMenu()!=null&& countryMenu.getMenu().hasVisibleItems()&&!city.getText().toString().isEmpty()){
                countryMenu.show();
            }else {
                showTips(getString(R.string.toast_trajectory_location_city));
            }
        });
        btnTownShip.setOnClickListener(v->{
            if (townShipMenu.getMenu()!=null&& townShipMenu.getMenu().hasVisibleItems()&&!country.getText().toString().isEmpty()){
                townShipMenu.show();
            }else {
                showTips(getString(R.string.toast_trajectory_location_country));
            }
        });
        btnVillage.setOnClickListener(v->{
            if (villageMenu.getMenu()!=null&& villageMenu.getMenu().hasVisibleItems()&&!townShip.getText().toString().isEmpty()){
                villageMenu.show();
            }else {
               showTips(getString(R.string.toast_trajectory_location_town));
            }
        });
        provinceMenu.setOnMenuItemClickListener(m ->{
            province.setText(m.getTitle());
            //存入选中的省的信息
            cityMenu.getMenu().clear();
            countryMenu.getMenu().clear();
            townShipMenu.getMenu().clear();
            villageMenu.getMenu().clear();
            //把子类的地址信息置空
            city.setText("");
            country.setText("");
            townShip.setText("");
            village.setText("");
            //获取市的信息
            threadPoolExecutor.execute(()->{
                cityList = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                        getResources().getString(R.string.address_url)+ getResources().getString(R.string.city_url),
                        DefaultInfoUtils.getToken(activity),provinceList.get(m.getItemId()));
                Menu menu = cityMenu.getMenu();
                menu.clear();
                if(cityList != null&& !cityList.isEmpty()) {
                    cityList.forEach(landAddress -> menu.add(Menu.NONE, menu.size(), menu.size(), landAddress.getName()));
                }
            });
            return false;
        });
        cityMenu.setOnMenuItemClickListener(m ->{
            city.setText(m.getTitle());
            //把子类的地址信息置空
            countryMenu.getMenu().clear();
            townShipMenu.getMenu().clear();
            villageMenu.getMenu().clear();
            country.setText("");
            townShip.setText("");
            village.setText("");
            //获取市的信息
            threadPoolExecutor.execute(()->{
                countryList = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                        getResources().getString(R.string.address_url)+ getResources().getString(R.string.district_url),
                        DefaultInfoUtils.getToken(activity),cityList.get(m.getItemId()));
                Menu menu =countryMenu.getMenu();
                menu.clear();
                if(countryList != null&& !countryList.isEmpty()) {
                    countryList.forEach(landAddress -> menu.add(Menu.NONE, menu.size(), menu.size(), landAddress.getName()));
                }
            });
            return false;
        });
        countryMenu.setOnMenuItemClickListener(m ->{
            country.setText(m.getTitle());
            townShipMenu.getMenu().clear();
            villageMenu.getMenu().clear();
            //把子类的地址信息置空
            townShip.setText("");
            village.setText("");
            //获取市的信息
            threadPoolExecutor.execute(()->{
                townShipList = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                        getResources().getString(R.string.address_url)+ getResources().getString(R.string.village_url),
                        DefaultInfoUtils.getToken(activity),countryList.get(m.getItemId()));
                Menu menu =townShipMenu.getMenu();
                menu.clear();
                if(townShipList != null&& !townShipList.isEmpty()) {
                    townShipList.forEach(landAddress -> menu.add(Menu.NONE, menu.size(), menu.size(), landAddress.getName()));
                }
            });
            return false;
        });
        villageMenu.setOnMenuItemClickListener(m ->{
            village.setText(m.getTitle());
            return false;
        });
        townShipMenu.setOnMenuItemClickListener(m ->{
            townShip.setText(m.getTitle());
            villageMenu.getMenu().clear();
            //把子类的地址信息置空
            village.setText("");
            //获取村的信息
            threadPoolExecutor.execute(()->{
                villageList = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                        getResources().getString(R.string.address_url)+  getResources().getString(R.string.hamlet_url),
                        DefaultInfoUtils.getToken(activity),townShipList.get(m.getItemId()));
                Menu menu =villageMenu.getMenu();
                menu.clear();
                if(villageList != null&& !villageList.isEmpty()) {
                    villageList.forEach(landAddress -> menu.add(Menu.NONE, menu.size(), menu.size(), landAddress.getName()));
                }
            });
            return false;
        });
        sortTvCorn.setOnClickListener(v->{
            if (popupMenuCorn.getMenu().size()!=0) {
                popupMenuCorn.show();
            }
        });
        //作业类型选择框的点击事件
        sortTvWorkType.setOnClickListener(v->{
            if (popupMenuWork.getMenu().size()!=0&& !sortTvCorn.getText().toString().isEmpty()) {
                popupMenuWork.show();
            }else {
                showTips("请先选择作物");
            }
        });
        //设置日期选择框的点击事件：弹出选择框
        tvDate.setOnClickListener(v->{
            FragmentManager manager = activity.getSupportFragmentManager();
            String tag = "DATE_PICKER";
            if (manager.findFragmentByTag(tag)==null) {
                datePickerView.show(activity.getSupportFragmentManager(),tag);
            }
        });
        //开始时间选择框的事件：弹出选择框
        tvStartTime.setOnClickListener(v->{
            FragmentManager manager = activity.getSupportFragmentManager();
            String tag = "TIME_BEGIN_PICKER";
            if (manager.findFragmentByTag(tag)==null) {
                timeBeginPickerView.show(activity.getSupportFragmentManager(), tag);
            }
        });
        //结束时间的额选择事件：弹出选择框
        tvEndTime.setOnClickListener(v->{
            FragmentManager manager = activity.getSupportFragmentManager();
            String tag = "TIME_END_PICKER";
            if (manager.findFragmentByTag(tag)==null) {
                timeEndPickerView.show(activity.getSupportFragmentManager(),tag);
            }
        });
        //TODO:先给个假的地址数据，有了组件之后再加数据
        imageButton.setOnClickListener(v->{
            if(aMapLocationClient.getLastKnownLocation()!=null) {
                //获取到地址，根据地址名称去找对应的
                for (LandAddress landAddress : provinceList) {
                    if (landAddress.getName().equals(aMapLocationClient.getLastKnownLocation().getProvince())){
                        Handler handler = new Handler(Looper.getMainLooper());
                        province.setText(landAddress.getName());
                        //存入选中的省的信息
                        cityMenu.getMenu().clear();
                        countryMenu.getMenu().clear();
                        townShipMenu.getMenu().clear();
                        villageMenu.getMenu().clear();
                        //把子类的地址信息置空
                        city.setText("");
                        country.setText("");
                        townShip.setText("");
                        village.setText("");
                        //获取市的信息
                        threadPoolExecutor.execute(()->{
                            cityList = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                                    getResources().getString(R.string.address_url)+ getResources().getString(R.string.city_url),
                                    DefaultInfoUtils.getToken(activity),landAddress);
                            Menu menu = cityMenu.getMenu();
                            menu.clear();
                            if(cityList != null&& !cityList.isEmpty()) {
                                cityList.forEach(landAddressCity -> {
                                    menu.add(Menu.NONE, menu.size(), menu.size(), landAddressCity.getName());
                                    if (landAddressCity.getName().equals(aMapLocationClient.getLastKnownLocation().getCity())){
                                        handler.post(()->{
                                            city.setText(landAddressCity.getName());
                                            country.setText("");
                                            townShip.setText("");
                                            village.setText("");
                                        });
                                        //把子类的地址信息置空
                                        countryMenu.getMenu().clear();
                                        townShipMenu.getMenu().clear();
                                        villageMenu.getMenu().clear();
                                        //获取市的信息
                                        countryList = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                                                getResources().getString(R.string.address_url)+ getResources().getString(R.string.district_url),
                                                DefaultInfoUtils.getToken(activity),landAddressCity);
                                        Menu menuCountry =countryMenu.getMenu();
                                        menuCountry.clear();
                                        if(countryList != null&& !countryList.isEmpty()) {
                                            countryList.forEach(landAddressCountry ->{
                                                menuCountry.add(Menu.NONE, menuCountry.size(), menuCountry.size(), landAddressCountry.getName());
                                                if (landAddressCountry.getName().equals(aMapLocationClient.getLastKnownLocation().getDistrict())) {
                                                    //把子类的地址信息置空
                                                    handler.post(() -> {
                                                        country.setText(landAddressCountry.getName());
                                                        townShip.setText("");
                                                        village.setText("");
                                                    });
                                                    townShipMenu.getMenu().clear();
                                                    villageMenu.getMenu().clear();
                                                    //获取市的信息
                                                    townShipList = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                                                            getResources().getString(R.string.address_url) + getResources().getString(R.string.village_url),
                                                            DefaultInfoUtils.getToken(activity), landAddressCountry);
                                                    Menu menuTown = townShipMenu.getMenu();
                                                    menuTown.clear();
                                                    if (townShipList != null && !townShipList.isEmpty()) {
                                                        townShipList.forEach(landAddressTown -> menuTown.add(Menu.NONE, menuTown.size(), menuTown.size(), landAddressTown.getName()));
                                                    }
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        });
                        break;
                    }
                }
            }else{
                 showTips(getString(R.string.toast_trajectory_location_ing));
            }

        });
        //设置作物菜单中的某个选项的选中的事件：设置文本，获取作业类型数据
        popupMenuCorn.setOnMenuItemClickListener(item -> {
            sortTvCorn.setText(item.getTitle());
            //获取工作类型数据
            threadPoolExecutor.execute(() -> new DataUtil(activity).getCornTypeData(popupMenuWork, workTypeEntities, cornEntities.get(item.getTitle())));
            return false;
        });
        //设置作业类型菜单选项的点击事件，设置文本
        popupMenuWork.setOnMenuItemClickListener(item -> {
            sortTvWorkType.setText(item.getTitle());
            return false;
        });
        //选中日期的事件：更改文本框的内容
        datePickerView.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            //点击之后实例化一个
            tvDate.setText(simpleDateFormat.format(new Date(selection)));
        });
        //选中开始时间的事件：更改文本框的内容
        timeBeginPickerView.addOnPositiveButtonClickListener(m->{
            //添加的时候就是筛选条件里的真实信息
            tvStartTime.setText(LocalTime.of(timeBeginPickerView.getHour(),timeBeginPickerView.getMinute()).toString());
        });
        //结束时间选中事件：更改文本框的内容
        timeEndPickerView.addOnPositiveButtonClickListener(m-> tvEndTime.setText(LocalTime.of(timeEndPickerView.getHour(),timeEndPickerView.getMinute()).toString()));
    }
    private void buttonBackgroundTurnToLeft() {
        btnLeft.setBackgroundColor(activity.getColor(R.color.green_final));
        btnLeft.setTextColor(activity.getColor(R.color.white_dark));
        btnRight.setTextColor(activity.getColor(R.color.green_final));
        btnRight.setBackgroundColor(activity.getColor(R.color.white_dark));
    }
    private void buttonBackgroundTurnToRight() {
        btnRight.setBackgroundColor(activity.getColor(R.color.green_final));
        btnRight.setTextColor(activity.getColor(R.color.white_dark));
        btnLeft.setTextColor(activity.getColor(R.color.green_final));
        btnLeft.setBackgroundColor(activity.getColor(R.color.white_dark));
    }
    /**
     * 打开侧边栏
     */
    private void openNavigation(){
        //打开navigation
        setTargetingOption();
        drawerLayout.openDrawer(GravityCompat.END);
        //根据页面选择要加载的筛选条件
        loadSortMessage();
    }
    /**
     * 由于一关就置空所以筛选就重新填充
     * 筛选信息不存在就不填充
     * 筛选信息不存在就不填充
     * 不确认按钮也不给他更改筛选对象的内容
     */
    private void loadSortMessage(){
        //需要填充内容到筛选框内
        if(isRight){
            //加载sort筛选条件
            sortTvCorn.setText(originSortMessage.getCorn());
            sortTvWorkType.setText(originSortMessage.getWorkTypeContent());
            tvDate.setText(originSortMessage.getLocalDate());
            tvStartTime.setText(originSortMessage.getBeginTime());
            tvEndTime.setText(originSortMessage.getEndTime());
            province.setText(originSortMessage.getProvince());
            city.setText(originSortMessage.getCity());
            country.setText(originSortMessage.getCountry());
            townShip.setText(originSortMessage.getTownShip());
            village.setText(originSortMessage.getVillage());
            tvPhone.setText(originSortMessage.getPhone());
        }else{
            //加载work筛选条件:有筛选条件就填充到内部，空的就只填充手机号
            sortTvCorn.setText(workSortMessage.getCorn());
            sortTvWorkType.setText(workSortMessage.getWorkTypeContent());
            tvDate.setText(workSortMessage.getLocalDate());
            tvStartTime.setText(workSortMessage.getBeginTime());
            tvEndTime.setText(workSortMessage.getEndTime());
            province.setText(workSortMessage.getProvince());
            city.setText(workSortMessage.getCity());
            country.setText(workSortMessage.getCountry());
            townShip.setText(workSortMessage.getTownShip());
            village.setText(workSortMessage.getVillage());
            tvPhone.setText(workSortMessage.getPhone());
        }
    }
    /**
     * 确定按钮的点击事件
     */
    private void confirm(){
        //点击确认按钮之后才更改筛选条件,首先判断是否完全填充
        if(!sortTvCorn.getText().toString().isEmpty()&&!sortTvWorkType.getText().toString().isEmpty()&&!tvDate.getText().toString().isEmpty()&&!tvStartTime.getText().toString().isEmpty()&&!tvEndTime.getText().toString().isEmpty()&&!tvPhone.getText().toString().isEmpty()){
            //检测是哪边
            if (isRight) {
                originSortMessage.setCorn(sortTvCorn.getText().toString());
                originSortMessage.setWorkType(String.valueOf(workTypeEntities.get(sortTvWorkType.getText().toString())));
                originSortMessage.setLocalDate(tvDate.getText().toString());
                originSortMessage.setBeginTime(tvStartTime.getText().toString());
                originSortMessage.setEndTime(tvEndTime.getText().toString());
                originSortMessage.setPhone(tvPhone.getText().toString());
                originSortMessage.setAddress(province.getText().toString()+city.getText().toString()+country.getText().toString()+townShip.getText().toString()+village.getText().toString());
                originSortMessage.setProvince(province.getText().toString());
                originSortMessage.setCity(city.getText().toString());
                originSortMessage.setCountry(country.getText().toString());
                originSortMessage.setTownShip(townShip.getText().toString());
                originSortMessage.setVillage(village.getText().toString());
                originSortMessage.setWorkTypeContent(sortTvWorkType.getText().toString());
                closeNavigation();
                fragmentTrajectoryOrigin.loadData();
            }else{
                workSortMessage.setCorn(sortTvCorn.getText().toString());
                workSortMessage.setWorkType(String.valueOf(workTypeEntities.get(sortTvWorkType.getText().toString())));
                workSortMessage.setLocalDate(tvDate.getText().toString());
                workSortMessage.setBeginTime(tvStartTime.getText().toString());
                workSortMessage.setEndTime(tvEndTime.getText().toString());
                workSortMessage.setPhone(tvPhone.getText().toString());
                workSortMessage.setAddress(province.getText().toString()+city.getText().toString()+country.getText().toString()+townShip.getText().toString()+village.getText().toString());
                workSortMessage.setProvince(province.getText().toString());
                workSortMessage.setCity(city.getText().toString());
                workSortMessage.setCountry(country.getText().toString());
                workSortMessage.setTownShip(townShip.getText().toString());
                workSortMessage.setVillage(village.getText().toString());
                workSortMessage.setWorkTypeContent(sortTvWorkType.getText().toString());
                closeNavigation();
                fragmentTrajectoryWork.refresh();
            }
        }else{
          showTips(getString(R.string.trajectory_toast_sort_err));
        }
    }
    /**
     * 重置按钮的点击事件
     */
    private void reset(){
        workTypeEntities.clear();
        closeNavigation();
        if (isRight){
            originSortMessage.clear();
            //还要清除界面上的内容
            fragmentTrajectoryOrigin.clearPlot();
            closeNavigation();

        }else{
            workSortMessage.clear();
            closeNavigation();
            fragmentTrajectoryWork.refresh();
        }
    }
    /**
     * 只要关了就更新筛选条件ui,只控制筛选条件
     * 取消和确认都可以关闭侧边栏
     * 在填充之后不更改sort
     * 如果确认就要更新的sort内容
     * 如果要取消->视图不变&&sort不变
     * 重置sort
     *      原来有内容,打开就有内容，tv置空，sort置空更新ui
     *      原来没有内容，打开就是空，tv置空，sort置空
     */
    private void closeNavigation(){
        drawerLayout.closeDrawer(GravityCompat.END);
        sortTvCorn.setText("");
        sortTvWorkType.setText("");
        tvDate.setText("");
        tvStartTime.setText("");
        tvEndTime.setText("");
        province.setText("");
        city.setText("");
        country.setText("");
        townShip.setText("");
        village.setText("");
        tvPhone.setText(phone);
//        //清空作业类型菜单
//        popupMenuWork.getMenu().clear();
//        //清空工作类型映射关系
//        workTypeEntities.clear();
    }
    /**
     * 设置定位参数并且开始定位
     */
    private void setTargetingOption() {
        //首先获取定位客户端
        aMapLocationClient = null;
        try {
            aMapLocationClient = new AMapLocationClient(activity.getApplicationContext());
        } catch (Exception e) {
            //TODO:更改位资源文件
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
    }
    /**
     * 显示提示信息
     * @param content 提示信息的内容
     */
    private void showTips(String content){
        new ToastUtil().getToast(activity,content);
    }
}
