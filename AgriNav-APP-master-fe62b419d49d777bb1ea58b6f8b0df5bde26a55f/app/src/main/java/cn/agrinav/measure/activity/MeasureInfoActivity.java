package cn.agrinav.measure.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.measure.entity.LandAddress;
import cn.agrinav.measure.entity.LandPlot;
import cn.agrinav.measure.utils.DefaultInfoUtils;
import cn.agrinav.measure.utils.PermissionMeasure;
import cn.agrinav.measure.utils.QueryUtils;
import cn.agrinav.measure.utils.ThreadPoolUtils;
import cn.agrinav.user.entity.SysUser;
import cn.agrinav.utils.mobile.PhoneNumberUtil;


/**
 * @author 18506
 */
public class MeasureInfoActivity extends AppCompatActivity{
    private AMapLocationClient aMapLocationClient;
    private Button buttonStartMeasure;
    //用于获取定位信息
    private EditText editPlotName ;
    private TextView editProvince;
    private TextView editCity;
    private TextView editDistrict;
    private TextView editHamlet;
    private TextView editVillage;
    private PopupMenu popProvincesMenu;
    private PopupMenu popCityMenu;
    private PopupMenu popDistrictMenu;
    private PopupMenu popHamletMenu;
    private PopupMenu popVillageMenu;
    private ImageButton imageButtonProvince;
    private ImageButton imageButtonCity;
    private ImageButton imageButtonDistrict;
    private ImageButton imageButtonHamlet;
    private ImageButton imageButtonVillage;
    private RelativeLayout relativeType;
    private PopupMenu popupTypeMenu;
    private TextView spinner;
    private ImageButton buttonBack;
    private Handler handler ;
    //勇于接受具体那个父级地址
    private LinkedHashMap<String, LandAddress> addressMap = new LinkedHashMap<>();
    //用于分别获取省、市、县、镇、村
    private List<LandAddress> provinceList =new ArrayList<>();
    private List<LandAddress> cityList = new ArrayList<>();
    private List<LandAddress> districtlist = new ArrayList<>();
    private List<LandAddress> hamletList = new ArrayList<>();
    private List<LandAddress> villageList =new ArrayList<>();
    //地块类型选择的按钮
    private ImageButton btnLandPlotType;
    private ImageButton imageButtonLocation;
    private RelativeLayout rlProvince;
    private RelativeLayout rlCity;
    private RelativeLayout rlDistrict;
    private RelativeLayout rlHamlet;
    private RelativeLayout rlVillage;
    private RelativeLayout rlLandPlotMeasureMethod;
    private TextView spinnerMethod;
    private PopupMenu popupMeasureMethodMenu;
    private ImageButton btnLandPlotMethod;
    private EditText editOwnerName;
    private EditText editOwnerPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_measure_info);
        server();
        //初始化页面组件
        initViews();
        //设置按钮的大小
        initSidebar();
        initHandler();
        //设置事件
        initEvents();
    }

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

    private void ifHavePermission() {
        try {
            setLocationOption();
        } catch (Exception e) {
            Log.e(getResources().getString(R.string.trajectory_tag), e.toString());
        }
    }

    private void initHandler() {
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                //处理图像与控件绑定
                if (msg.what == 1 && msg.obj != null) {
                    // 假设 msg 是从某个消息或数据源获取的对象
                    if (msg.obj instanceof List) {
                        List<?> tempList = (List<?>) msg.obj;
                        if (!tempList.isEmpty() && tempList.get(0) instanceof LandAddress) {
                            // 安全地进行类型转换
                            provinceList = tempList.stream()
                                    .map(item -> (LandAddress) item)
                                    .collect(Collectors.toList());
                        }
                    }
                    //将拿到的数据显示在popupWindow中
                    popProvincesMenu.getMenu().clear();
                    for(LandAddress province : provinceList){
                        popProvincesMenu.getMenu().add(province.getName());
                    }
                    popProvincesMenu.show();
                }
                if (msg.what == 2 && msg.obj != null){
                    // 假设 msg 是从某个消息或数据源获取的对象
                    if (msg.obj instanceof List) {
                        List<?> tempList = (List<?>) msg.obj;
                        if (!tempList.isEmpty() && tempList.get(0) instanceof LandAddress) {
                            // 清除上一次的数据
                            cityList.clear();
                            // 安全地进行类型转换
                            cityList = tempList.stream()
                                    .map(item -> (LandAddress) item)
                                    .collect(Collectors.toList());
                        }
                    }
                    //将拿到的数据显示在popupWindow中
                    popCityMenu.getMenu().clear();
                    popDistrictMenu.getMenu().clear();
                    popHamletMenu.getMenu().clear();
                    popVillageMenu.getMenu().clear();
                    for(LandAddress city : cityList){
                        popCityMenu.getMenu().add(city.getName());
                    }
                    popCityMenu.show();
                }
                if (msg.what == 3 && msg.obj != null){
                    // 假设 msg 是从某个消息或数据源获取的对象
                    if (msg.obj instanceof List) {
                        List<?> tempList = (List<?>) msg.obj;
                        if (!tempList.isEmpty() && tempList.get(0) instanceof LandAddress) {
                            // 清除上一次的数据
                            districtlist.clear();
                            // 安全地进行类型转换
                            districtlist = tempList.stream()
                                    .map(item -> (LandAddress) item)
                                    .collect(Collectors.toList());
                        }
                    }
                    //将拿到的数据显示在popupWindow中
                    popDistrictMenu.getMenu().clear();
                    popHamletMenu.getMenu().clear();
                    popVillageMenu.getMenu().clear();
                    for(LandAddress district : districtlist ){
                        popDistrictMenu.getMenu().add(district.getName());
                    }
                    popDistrictMenu.show();
                }
                if (msg.what == 4 && msg.obj != null){
                    // 假设 msg 是从某个消息或数据源获取的对象
                    if (msg.obj instanceof List) {
                        List<?> tempList = (List<?>) msg.obj;
                        if (!tempList.isEmpty() && tempList.get(0) instanceof LandAddress) {
                            // 清除上一次的数据
                            hamletList.clear();
                            // 安全地进行类型转换
                            hamletList = tempList.stream()
                                    .map(item -> (LandAddress) item)
                                    .collect(Collectors.toList());
                        }
                    }
                    //将拿到的数据显示在popupWindow中
                    popHamletMenu.getMenu().clear();
                    popVillageMenu.getMenu().clear();
                    for(LandAddress hamlet : hamletList ){
                        popHamletMenu.getMenu().add(hamlet.getName());
                    }
                    popHamletMenu.show();
                }
                if (msg.what == 5 && msg.obj != null){
                    // 假设 msg 是从某个消息或数据源获取的对象
                    if (msg.obj instanceof List) {
                        List<?> tempList = (List<?>) msg.obj;
                        if (!tempList.isEmpty() && tempList.get(0) instanceof LandAddress) {
                            // 清除上一次的数据
                            villageList.clear();
                            // 安全地进行类型转换
                            villageList = tempList.stream()
                                    .map(item -> (LandAddress) item)
                                    .collect(Collectors.toList());
                        }
                    }
                    //将拿到的数据显示在popupWindow中
                    popVillageMenu.getMenu().clear();
                    for(LandAddress village : villageList ){
                        popVillageMenu.getMenu().add(village.getName());
                    }
                    popVillageMenu.show();
                }
            }
        };
    }





    private void initSidebar() {
        ViewGroup.LayoutParams params = buttonStartMeasure.getLayoutParams();
        // 获取屏幕宽度
        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        params.width = (int) (screenWidth * 0.62);
        //按钮大小动态设置
        buttonStartMeasure.setLayoutParams(params);
    }

    private void initEvents() {
        imageButtonLocation.setOnClickListener(v -> {
            if (aMapLocationClient == null){
                try {
                    aMapLocationClient = new AMapLocationClient(getApplicationContext());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            //重新获取定位
            if (aMapLocationClient.getLastKnownLocation()==null){
                server();
            }
            if(aMapLocationClient.getLastKnownLocation()!=null) {
                //获取到地址，根据地址名称去找对应的
                for (LandAddress landAddress : provinceList) {
                    if (landAddress.getName().equals(aMapLocationClient.getLastKnownLocation().getProvince())){
                        Handler handler = new Handler(Looper.getMainLooper());
                        editProvince.setText(landAddress.getName());
                        //存入选中的省的信息
                        popCityMenu.getMenu().clear();
                        popDistrictMenu.getMenu().clear();
                        popHamletMenu.getMenu().clear();
                        popVillageMenu.getMenu().clear();
                        //把子类的地址信息置空
                        editCity.setText("");
                        editDistrict.setText("");
                        editHamlet.setText("");
                        editVillage.setText("");
                        //获取市的信息
                        ThreadPoolUtils.threadPoolExecutor.execute(()->{
                            cityList = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                                    getResources().getString(R.string.address_url)+ getResources().getString(R.string.city_url),
                                    DefaultInfoUtils.getToken(this),landAddress);
                            Menu menu = popCityMenu.getMenu();
                            menu.clear();
                            if(cityList != null&& !cityList.isEmpty()) {
                                cityList.forEach(landAddressCity -> {
                                    menu.add(Menu.NONE, menu.size(), menu.size(), landAddressCity.getName());
                                    if (landAddressCity.getName().equals(aMapLocationClient.getLastKnownLocation().getCity())){
                                        handler.post(()->{
                                            editCity.setText(landAddressCity.getName());
                                            editDistrict.setText("");
                                            editHamlet.setText("");
                                            editVillage.setText("");
                                        });
                                        //把子类的地址信息置空
                                        popDistrictMenu.getMenu().clear();
                                        popHamletMenu.getMenu().clear();
                                        popVillageMenu.getMenu().clear();
                                        //获取市的信息
                                        districtlist = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                                                getResources().getString(R.string.address_url)+ getResources().getString(R.string.district_url),
                                                DefaultInfoUtils.getToken(this),landAddressCity);
                                        Menu menuCountry =popDistrictMenu.getMenu();
                                        menuCountry.clear();
                                        if(districtlist != null&& !districtlist.isEmpty()) {
                                            districtlist.forEach(landAddressCountry ->{
                                                menuCountry.add(Menu.NONE, menuCountry.size(), menuCountry.size(), landAddressCountry.getName());
                                                if (landAddressCountry.getName().equals(aMapLocationClient.getLastKnownLocation().getDistrict())) {
                                                    //把子类的地址信息置空
                                                    handler.post(() -> {
                                                        editDistrict.setText(landAddressCountry.getName());
                                                        editHamlet.setText("");
                                                        editVillage.setText("");
                                                    });
                                                    popHamletMenu.getMenu().clear();
                                                    popVillageMenu.getMenu().clear();
                                                    //获取市的信息
                                                    hamletList = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                                                            getResources().getString(R.string.address_url) + getResources().getString(R.string.village_url),
                                                            DefaultInfoUtils.getToken(this), landAddressCountry);
                                                    Menu menuTown = popHamletMenu.getMenu();
                                                    menuTown.clear();
                                                    if (hamletList != null && !hamletList.isEmpty()) {
                                                        hamletList.forEach(landAddressTown -> menuTown.add(Menu.NONE, menuTown.size(), menuTown.size(), landAddressTown.getName()));
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
                Toast.makeText(this, "正在定位，稍后再试", Toast.LENGTH_SHORT).show();
            }
        });
        popProvincesMenu.setOnMenuItemClickListener(m -> {
            editProvince.setText(m.getTitle());
            for(LandAddress province : provinceList){
                if(province.getName().contentEquals(m.getTitle())){
                    if(addressMap.get("province")==null || !addressMap.get("province").equals(province)){
                        addressMap.put("province", province);
                        addressMap.remove("city");
                        editCity.setText("");
                        editDistrict.setText("");
                        editHamlet.setText("");
                        editVillage.setText("");
                        // 更新城市列表，但不显示
                        updateCityList();
                    }
                    break;
                }
            }
            return true;
        });
        popCityMenu.setOnMenuItemClickListener(m -> {
            editCity.setText(m.getTitle());
            for(LandAddress city : cityList ){
                if(city.getName().contentEquals(m.getTitle())){
                    if(addressMap.get("city")==null || !addressMap.get("city").equals(city)){
                        addressMap.put("city", city);
                        addressMap.remove("district");
                        editDistrict.setText("");
                        editHamlet.setText("");
                        editVillage.setText("");
                        // 更新区县列表，但不显示
                        updateDistrictList();
                    }
                    break;
                }
            }
            return true;
        });
        popDistrictMenu.setOnMenuItemClickListener(m -> {
            editDistrict.setText(m.getTitle());
            for(LandAddress district : districtlist){
                if(district.getName().contentEquals(m.getTitle())){
                    if(addressMap.get("district")==null || !addressMap.get("district").equals(district)){
                        addressMap.put("district", district);
                        addressMap.remove("hamlet");
                        editHamlet.setText("");
                        editVillage.setText("");
                        // 更新乡镇列表，但不显示
                        updateHamletList();
                    }
                    break;
                }
            }
            return true;
        });
        popHamletMenu.setOnMenuItemClickListener(m -> {
            editHamlet.setText(m.getTitle());
            for(LandAddress hamlet : hamletList){
                if(hamlet.getName().contentEquals(m.getTitle())){
                    if(addressMap.get("hamlet")==null || !addressMap.get("hamlet").equals(hamlet)){
                        addressMap.put("hamlet", hamlet);
                        addressMap.remove("village");
                        editVillage.setText("");
                        // 更新村列表，但不显示
                        updateVillageList();
                    }
                    break;
                }
            }
            return true;
        });
        popVillageMenu.setOnMenuItemClickListener(m -> {
            editVillage.setText(m.getTitle());
            for(LandAddress village : villageList){
                if(village.getName().contentEquals(m.getTitle())){
                    addressMap.put("village", village);
                    break;
                }
            }
            return true;
        });
        //返回按钮
        buttonBack.setOnClickListener(v -> finish());
        // 使用View.OnClickListener接口
        View.OnClickListener provinceClickListener = v -> fetchProvince();
        imageButtonProvince.setOnClickListener(provinceClickListener);
        rlProvince.setOnClickListener(provinceClickListener);
        editProvince.setOnClickListener(provinceClickListener);
        
        View.OnClickListener cityClickListener = v -> fetchCity();
        imageButtonCity.setOnClickListener(cityClickListener);
        rlCity.setOnClickListener(cityClickListener);
        editCity.setOnClickListener(cityClickListener);
        
        View.OnClickListener districtClickListener = v -> fetchDistrict();
        imageButtonDistrict.setOnClickListener(districtClickListener);
        rlDistrict.setOnClickListener(districtClickListener);
        editDistrict.setOnClickListener(districtClickListener);
        
        View.OnClickListener hamletClickListener = v -> fetchVillage();
        imageButtonHamlet.setOnClickListener(hamletClickListener);
        rlHamlet.setOnClickListener(hamletClickListener);
        editHamlet.setOnClickListener(hamletClickListener);
        
        View.OnClickListener villageClickListener = v -> fetchHamlet();
        imageButtonVillage.setOnClickListener(villageClickListener);
        rlVillage.setOnClickListener(villageClickListener);
        editVillage.setOnClickListener(villageClickListener);
        // 确保TextView不会拦截触摸事件
        View.OnTouchListener passThroughListener = (v, event) -> false;
        editProvince.setOnTouchListener(passThroughListener);
        editCity.setOnTouchListener(passThroughListener);
        editDistrict.setOnTouchListener(passThroughListener);
        editHamlet.setOnTouchListener(passThroughListener);
        editVillage.setOnTouchListener(passThroughListener);
        buttonStartMeasure.setOnClickListener(v ->{
            //添加数据 保证该填写的信息必须填写
            if(editProvince.getText().toString().isEmpty()){
                Toast.makeText(this, getResources().getString(R.string.choose_location), Toast.LENGTH_SHORT).show();
            }else if(editPlotName.getText().toString().isEmpty()){
                Toast.makeText(this, getResources().getString(R.string.input_plot_name), Toast.LENGTH_SHORT).show();
            } else if (spinner.getText().toString().isEmpty()) {
                Toast.makeText(this, getResources().getString(R.string.choose_plot_type), Toast.LENGTH_SHORT).show();
            }
            else if (spinnerMethod.getText().toString().isEmpty()){
                Toast.makeText(this, getResources().getString(R.string.choose_measurement_method), Toast.LENGTH_SHORT).show();
            }else if(editOwnerName.getText().toString().isEmpty()){
                Toast.makeText(this, getResources().getString(R.string.input_owner_name), Toast.LENGTH_SHORT).show();
            }else if (editOwnerPhone.getText().toString().isEmpty()||editOwnerPhone.getText().toString().length()!=11||!(new PhoneNumberUtil().isPhoneNumber(editOwnerPhone.getText().toString()))){
                Toast.makeText(this, getResources().getString(R.string.input_correct_owner_phone), Toast.LENGTH_SHORT).show();
            }else {
                //创建地块对象
                LandPlot landPlot = new LandPlot();
                //设置地块部分信息
                landPlot.setAddress(editProvince.getText().toString()+editCity.getText().toString()+editDistrict.getText().toString()+editHamlet.getText().toString()+editVillage.getText().toString());
                //设置地块名称
                landPlot.setLandName(editPlotName.getText().toString());
                //设置地块类型
                landPlot.setLandType(spinner.getText().toString());
                //设置测量方法
                landPlot.setMeasurementMethod(spinnerMethod.getText().toString());
                //所属人姓名
                landPlot.setOwnerName(editOwnerName.getText().toString());
                //所属人电话
                landPlot.setOwnerPhone(editOwnerPhone.getText().toString());
                //默认为手动打点
                //跳转
                Intent intent = new Intent(
                        this,
                        MeasureActivity.class
                );
                intent.putExtra("LandPlot",landPlot);
                startActivity(intent);
            }
        });
        //点击选择类型框的事件
        relativeType.setOnClickListener(v ->{
            //点击每一个子项的事件
            popupTypeMenu.setOnMenuItemClickListener(m->{
                spinner.setText(m.getTitle());
                return true;
            });
            //显示菜单
            popupTypeMenu.show();
        });
        //点击类型选择按钮的事件
        btnLandPlotType.setOnClickListener(v -> {
            //点击每一个子项的事件
            popupTypeMenu.setOnMenuItemClickListener(m->{
                spinner.setText(m.getTitle());
                return true;
            });
            //显示菜单
            popupTypeMenu.show();
        });
        //点击量地类型的事件
        rlLandPlotMeasureMethod.setOnClickListener(v -> {
            popupMeasureMethodMenu.setOnMenuItemClickListener(m->{
                spinnerMethod.setText(m.getTitle());
                return true;
            });
            popupMeasureMethodMenu.show();
        });
        btnLandPlotMethod.setOnClickListener(v -> {
            //点击每一个子项的事件
            popupMeasureMethodMenu.setOnMenuItemClickListener(m->{
                spinnerMethod.setText(m.getTitle());
                return true;
            });
            //显示菜单
            popupMeasureMethodMenu.show();
        });
    }

    private void fetchHamlet() {
        if (villageList.isEmpty()){
            if (addressMap.get("hamlet")!=null){
                ThreadPoolUtils.threadPoolExecutor.execute(()->{
                    List<LandAddress> list = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                            getResources().getString(R.string.address_url)+ getResources().getString(R.string.hamlet_url),
                            DefaultInfoUtils.getToken(this),addressMap.get("hamlet"));
                    handler.sendMessage(handler.obtainMessage(5, list));
                });
            }else {
                Toast.makeText(this, getResources().getString(R.string.choose_village), Toast.LENGTH_SHORT).show();
            }
        }else {
            popVillageMenu.show();
        }
    }

    private void fetchVillage() {
        if (hamletList.isEmpty()){
            if (addressMap.get("district")!=null){
                ThreadPoolUtils.threadPoolExecutor.execute(()->{
                    List<LandAddress> list = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                            getResources().getString(R.string.address_url)+ getResources().getString(R.string.village_url),
                            DefaultInfoUtils.getToken(this),addressMap.get("district"));
                    handler.sendMessage(handler.obtainMessage(4, list));
                });
            }else {
                Toast.makeText(this, getResources().getString(R.string.choose_district), Toast.LENGTH_SHORT).show();
            }
        }else {
            popHamletMenu.show();
        }
    }

    private void fetchDistrict() {
        if (districtlist.isEmpty()){
            if(addressMap.get("city")!=null){
                ThreadPoolUtils.threadPoolExecutor.execute(()->{
                    List<LandAddress> list = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                            getResources().getString(R.string.address_url)+ getResources().getString(R.string.district_url),
                            DefaultInfoUtils.getToken(this),addressMap.get("city"));
                    handler.sendMessage(handler.obtainMessage(3, list));
                });
            }else {
                Toast.makeText(this, getResources().getString(R.string.choose_city), Toast.LENGTH_SHORT).show();
            }
        }else {
            popDistrictMenu.show();
        }
    }
    //第一次加载的方法
    private void fetchCity() {
        if(cityList.isEmpty()){
            if(addressMap.get("province")!=null) {
                ThreadPoolUtils.threadPoolExecutor.execute(() -> {
                    List<LandAddress> list = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                            getResources().getString(R.string.address_url) + getResources().getString(R.string.city_url),
                            DefaultInfoUtils.getToken(this), addressMap.get("province"));
                    handler.sendMessage(handler.obtainMessage(2, list));
                });
            }else{
                Toast.makeText(this, getResources().getString(R.string.choose_province), Toast.LENGTH_SHORT).show();
            }
        }else{
            popCityMenu.show();
        }
    }

    private void fetchProvince() {
        if(provinceList.isEmpty()){
            ThreadPoolUtils.threadPoolExecutor.execute(()->{
                List<LandAddress> list = QueryUtils.getLandProvince(getResources().getString(R.string.server_url),
                        getResources().getString(R.string.address_url)+ getResources().getString(R.string.province_url),
                        DefaultInfoUtils.getToken(this));
                handler.sendMessage(handler.obtainMessage(1, list));
            });
        }else{
            popProvincesMenu.show();
        }
    }

    private void initViews()  {
        imageButtonLocation = findViewById(R.id.ImageButtonLocation);
        //地址选择
        imageButtonProvince = findViewById(R.id.imageButtonProvince);
        imageButtonCity = findViewById(R.id.imageButtonCity);
        imageButtonDistrict = findViewById(R.id.imageButtonDistrict);
        imageButtonHamlet = findViewById(R.id.imageButtonHamlet);
        imageButtonVillage = findViewById(R.id.imageButtonVillage);
        rlProvince = findViewById(R.id.rl_province);
        rlCity = findViewById(R.id.rl_city);
        rlDistrict = findViewById(R.id.rl_district);
        rlHamlet = findViewById(R.id.rl_hamlet);
        rlVillage = findViewById(R.id.rl_village);
        editProvince = findViewById(R.id.editProvince);
        editCity = findViewById(R.id.editCity);
        editDistrict = findViewById(R.id.editDistrict);
        editHamlet = findViewById(R.id.editHamlet);
        editVillage = findViewById(R.id.editVillage);
        popProvincesMenu = new PopupMenu(this, editProvince);
        popCityMenu = new PopupMenu(this, editCity);
        popDistrictMenu = new PopupMenu(this, editDistrict);
        popHamletMenu = new PopupMenu(this, editHamlet);
        popVillageMenu = new PopupMenu(this, editVillage);
        //类型选择
        relativeType = findViewById(R.id.rl_landPlot_type);
        popupTypeMenu = new PopupMenu(this, relativeType);
        //获取资源
        popupTypeMenu.getMenuInflater().inflate(R.menu.menu_land_plot_type, popupTypeMenu.getMenu());
        //返回按钮
        buttonBack = findViewById(R.id.buttonBack);
        //开始测量按钮
        buttonStartMeasure = findViewById(R.id.buttonStartMeasure);
        //地块号编辑框
        editPlotName = findViewById(R.id.editPlotName);
        //地理位置编辑框
        editProvince = findViewById(R.id.editProvince);
        //地块类型选择
        //TODO：自定义适配器
        spinner = findViewById(R.id.spinner);
        //量地人姓名
        TextView editMeasureName = findViewById(R.id.edit_measure_name);
        editMeasureName.setText(DefaultInfoUtils.getUserDataName(this));
        //量地人电话
        TextView editMeasurePhone = findViewById(R.id.edit_measure_phone);
        editMeasurePhone.setText(DefaultInfoUtils.getUserDataPhone(this));
        //所属人姓名
        editOwnerName = findViewById(R.id.edit_owner_name);
        //所属人电话
        editOwnerPhone = findViewById(R.id.edit_owner_phone);
        btnLandPlotType = findViewById(R.id.btn_landPlot_type);
        //量地方式
        rlLandPlotMeasureMethod = findViewById(R.id.rl_landPlot_measure_method);
        popupMeasureMethodMenu = new PopupMenu(this, rlLandPlotMeasureMethod);
        //获取资源
        popupMeasureMethodMenu.getMenuInflater().inflate(R.menu.menu_land_plot_method, popupMeasureMethodMenu.getMenu());
        spinnerMethod = findViewById(R.id.spinner_measure_method);
        btnLandPlotMethod = findViewById(R.id.btn_landPlot_measure_method);
        // 确保所有相关视图都是可点击的
        rlProvince.setClickable(true);
        rlCity.setClickable(true);
        rlDistrict.setClickable(true);
        rlHamlet.setClickable(true);
        rlVillage.setClickable(true);
        
        // 确保TextView不会拦截触摸事件
        View.OnTouchListener passThroughListener = (v, event) -> false;
        editProvince.setOnTouchListener(passThroughListener);
        editCity.setOnTouchListener(passThroughListener);
        editDistrict.setOnTouchListener(passThroughListener);
        editHamlet.setOnTouchListener(passThroughListener);
        editVillage.setOnTouchListener(passThroughListener);
    }
    private void setLocationOption() {
        //首先获取定位客户端
        aMapLocationClient = null;
        try {
            aMapLocationClient = new AMapLocationClient(getApplicationContext());
        } catch (Exception e) {
            Toast.makeText(this, getResources().getString(R.string.error_locate), Toast.LENGTH_SHORT).show();
        }
        AMapLocationClientOption aMapLocationClientOption = new AMapLocationClientOption();
        //设置定位模式为高精度模式
        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置北斗定位优先
        aMapLocationClientOption.setBeidouFirst(true);
        aMapLocationClientOption.getGeoLanguage();
        //设置wifi自动刷新
        aMapLocationClientOption.setWifiScan(true);
        //设置定位是否等待wifi列表刷新
//        aMapLocationClientOption.setOnceLocationLatest(true);
        //设置开启wifi始终扫描
        AMapLocationClientOption.setOpenAlwaysScanWifi(true);
        aMapLocationClientOption.setInterval(2000);
        //配置项加载到客户端中
        aMapLocationClient.setLocationOption(aMapLocationClientOption);
        //使用客户端开始定位
        aMapLocationClient.startLocation();
    }


    //拿到行政区号数据库
    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        String token = (String) intent.getSerializableExtra("token");
        ThreadPoolUtils.initThreadPool();
        //获取省份数据
        ThreadPoolUtils.threadPoolExecutor.execute(()-> {
            provinceList = QueryUtils.getLandProvince(getResources().getString(R.string.server_url),
                    getResources().getString(R.string.address_url)+ getResources().getString(R.string.province_url),
                    token);
            Menu menu = popProvincesMenu.getMenu();
            //如果返回的省份信息为空，说明没有权限
            if(provinceList == null && provinceList.isEmpty()){
                Toast.makeText(this, "没有权限,请联系管理员", Toast.LENGTH_SHORT).show();
            }else{
                provinceList.forEach(
                        landAddress -> menu.add(Menu.NONE, menu.size(), menu.size(), landAddress.getName())
                );
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        ThreadPoolUtils.shutdownThreadPool();
        if (aMapLocationClient!=null) {
            aMapLocationClient.stopLocation();
            aMapLocationClient.onDestroy();
        }
    }

    private void updateCityList() {
        ThreadPoolUtils.threadPoolExecutor.execute(() -> {
            List<LandAddress> list = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                    getResources().getString(R.string.address_url) + getResources().getString(R.string.city_url),
                    DefaultInfoUtils.getToken(this), addressMap.get("province"));
            handler.post(() -> {
                cityList.clear();
                cityList.addAll(list);
                popCityMenu.getMenu().clear();
                for(LandAddress city : cityList){
                    popCityMenu.getMenu().add(city.getName());
                }
            });
        });
    }

    private void updateDistrictList() {
        ThreadPoolUtils.threadPoolExecutor.execute(() -> {
            List<LandAddress> list = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                    getResources().getString(R.string.address_url) + getResources().getString(R.string.district_url),
                    DefaultInfoUtils.getToken(this), addressMap.get("city"));
            handler.post(() -> {
                districtlist.clear();
                districtlist.addAll(list);
                popDistrictMenu.getMenu().clear();
                for(LandAddress district : districtlist){
                    popDistrictMenu.getMenu().add(district.getName());
                }
            });
        });
    }

    private void updateHamletList() {
        ThreadPoolUtils.threadPoolExecutor.execute(() -> {
            List<LandAddress> list = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                    getResources().getString(R.string.address_url) + getResources().getString(R.string.village_url),
                    DefaultInfoUtils.getToken(this), addressMap.get("district"));
            handler.post(() -> {
                hamletList.clear();
                hamletList.addAll(list);
                popHamletMenu.getMenu().clear();
                for(LandAddress hamlet : hamletList){
                    popHamletMenu.getMenu().add(hamlet.getName());
                }
            });
        });
    }

    private void updateVillageList() {
        ThreadPoolUtils.threadPoolExecutor.execute(() -> {
            List<LandAddress> list = QueryUtils.getLandAddresses(getResources().getString(R.string.server_url),
                    getResources().getString(R.string.address_url) + getResources().getString(R.string.hamlet_url),
                    DefaultInfoUtils.getToken(this), addressMap.get("hamlet"));
            handler.post(() -> {
                villageList.clear();
                villageList.addAll(list);
                popVillageMenu.getMenu().clear();
                for(LandAddress village : villageList){
                    popVillageMenu.getMenu().add(village.getName());
                }
            });
        });
    }

}
