package cn.agrinav.front.activity.findjob;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.google.android.material.navigation.NavigationView;
import com.scwang.smart.refresh.layout.api.RefreshLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.agrinav.R;
import cn.agrinav.front.activity.findjob.utils.JobDateTimeUtils;
import cn.agrinav.front.activity.findjob.utils.JobFilterManager;
import cn.agrinav.front.activity.findjob.utils.JobUIUtils;
import cn.agrinav.front.adapter.FindJobAdapter;
import cn.agrinav.front.entity.AgriculturalMachineryType;
import cn.agrinav.front.entity.FindJobDetail;
import cn.agrinav.measure.entity.LandAddress;
import cn.agrinav.measure.utils.DefaultInfoUtils;
import cn.agrinav.measure.utils.QueryUtils;
import cn.agrinav.trajectory.entity.vo.CornEntity;
import cn.agrinav.trajectory.permission.PermissionTrajectory;
import cn.agrinav.utils.DialogUtils;
import cn.agrinav.utils.OkHttpUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FindJobActivity extends AppCompatActivity {

    private ImageView ivPhoto;
    private ImageView ivAdd;
    private ImageView ivSearch;
    private GridView gridView;
    private SharedPreferences sharedPreferences;
    private List<AgriculturalMachineryType> machineTypeList;
    private List<CornEntity> cropList;
    private Map<String, String> typeMap = new HashMap<>();
    private Map<Long, String> cropMap = new HashMap<>();
    private FindJobAdapter adapter;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 8;
    private boolean isRefresh = false;
    private boolean enough = false;
    private RefreshLayout refreshLayout;
    private List<FindJobDetail> jobDetails;

    private DrawerLayout findJobDrawer;
    private NavigationView navigationView;
    private View headerView;
    private View findJobSidebar;

    private EditText edtWorkableCrops;
    private LinearLayout llWorkableCropsTags;
    private TextView tvWorkableCropsTap;

    private EditText edtMachineType;
    private LinearLayout llMachineTypeTags;
    private TextView tvMachineTypeTap;

    private EditText edtWorkArea;
    private LinearLayout llWorkAreaTags;
    private TextView tvWorkAreaTap;

    private JobFilterManager filterManager;

    private EditText edtGrainTransport;
    private LinearLayout llGrainTransportTags;
    private TextView tvGrainTransportTap;

    private EditText edtJobName;

    private ActivityResultLauncher launcher;

    private final ThreadPoolExecutor threadPoolExecutor;

    private List<LandAddress> provinceList;
    private AMapLocationClient aMapLocationClient;
    private JSONObject filterConditions = new JSONObject();

    // 添加变量保存上次确认的筛选条件
    private JSONObject lastConfirmedFilters = null;

    public FindJobActivity() {
        threadPoolExecutor = new ThreadPoolExecutor(
                10,
                20,
                10,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_job);
        
        // 初始化基础组件
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        initViews();
        
        // 添加整个界面的触摸监听器
        View rootView = findViewById(R.id.find_job_refresh);  // 使用 refreshLayout 作为根视图
        rootView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideAllOperationLayers();
            }
            return false;
        });
        
        // 添加顶部区域的点击监听
        View topBar = findViewById(R.id.iv_job_add);  // 使用顶部的添加按钮所在的区域
        topBar.getRootView().setOnClickListener(v -> hideAllOperationLayers());
        
        // 初始化数据相关组件
        initLocationClient();
        initProvinceList();
        initSelectors();
        
        // 使用单独的方法处理数据初始化
        initializeData();
        
        // 注册 launcher
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            checkTargetingPermissions();
        });
        createImageStorageDir();
    }

    private void initializeData() {
        // 重置数据状态
        currentPage = 1;
        isRefresh = true;
        enough = false;

        threadPoolExecutor.execute(() -> {
            // 等待 filterManager 初始化完成
            while (filterManager == null || filterManager.getCropList().isEmpty() ||
                   filterManager.getMachineTypeList().isEmpty()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e(getString(R.string.find_job_toast),
                          getString(R.string.find_job_filter_manager_init_failure), e);
                    return; // 添加错误处理
                }
            }

            runOnUiThread(() -> {
                initData();      // 等 filterManager 准备好后再初始化数据
                initAdapter();   // 数据准备好后初始化适配器
                initEvents();    // 最后初始化事件
                showJobList();   // 加载初始数据
            });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 移除这里的重复刷新，因为 initializeData 已经会调用 showJobList
    }

    private void checkTargetingPermissions() {
        ArrayList<String> permissions = null;
        //在这里把每一个权限都进行一次检验
        for(int i = 0; i< PermissionTrajectory.PERMISSIONS_LOCATION.length; i++) {
            //判断是否拥有某项权限
            if (ContextCompat.checkSelfPermission(this, PermissionTrajectory.PERMISSIONS_LOCATION[i]) != PackageManager.PERMISSION_GRANTED) {
                //判断是否有未获得的权限已经了集合，如果不是，现在就开始新建集合
                if(permissions==null){
                    permissions = new ArrayList<>();
                }
                //把欠缺的权限加入到集合中
                permissions.add(PermissionTrajectory.PERMISSIONS_LOCATION[i]);
            }
        }//判断是否有欠缺权限,如果有就去申请，没有就开始正常操作
        //判断是否有缺少的限
        if(permissions!=null) {
            String[] strings = new String[permissions.size()];
            for (short j=0;j<strings.length;j++){
                strings[j]=permissions.get(j);
            }
            //在这里设置去请
            ActivityCompat.requestPermissions(this,strings, getResources().getInteger(R.integer.find_job_code_success));
        }else {
            // 开启定位
            setTargetingOption();
        }
    }

    // 这个方法用于让用户自己去授权
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //如果回调方法中的是trajectory的内容状态码就执行以下逻辑
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == getResources().getInteger(R.integer.find_job_code_success)) {
            if (PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                Toast.makeText(this, getString(R.string.find_job_location_permission_get), Toast.LENGTH_SHORT).show();
                // 开启定位
                setTargetingOption();
            } else {
                //如果没有给权限就提示他，并且让他去给予权限，重新授权，不给权限不让用
                DialogUtils dialogUtils = new DialogUtils(this);
                AlertDialog dialog = dialogUtils.getDialog();
                dialogUtils.setTitle(getString(R.string.find_job_location_permission_title))
                        .setContent(getString(R.string.find_job_location_permission_content))
                        .setPositiveText(getString(R.string.find_job_location_permission_positive))
                        .setPositiveListener(v -> {
                            //如果要重新授权就直接让他新给权限重新调用一遍直到给权限为止
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse(getString(R.string.find_job_location_permission_package) + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeText(getString(R.string.find_job_location_permission_negative))
                        .setNegativeListener(v -> dialog.cancel());
                View view = dialogUtils.getView();
                dialog.setContentView(view);
                dialog.show();
            }
        }
    }

    /**
     * 设置定位参数并且开始定位
     */
    private void setTargetingOption() {
        //首先获取定位客户端
        aMapLocationClient = null;
        try {
            aMapLocationClient = new AMapLocationClient(this.getApplicationContext());
        } catch (Exception e) {
            Log.e(getString(R.string.find_job_toast), getString(R.string.find_job_client_create_failure));
        }
        AMapLocationClientOption aMapLocationClientOption = new AMapLocationClientOption();
        //置定位模式为高精度模
        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置北斗定优先
        aMapLocationClientOption.setBeidouFirst(true);
        aMapLocationClientOption.getGeoLanguage();
        //设置wifi自动刷新
        aMapLocationClientOption.setWifiScan(true);
        //设置开启wifi终扫描
        AMapLocationClientOption.setOpenAlwaysScanWifi(true);
        aMapLocationClientOption.setInterval(2000);
        //配置项加载到客户端中
        aMapLocationClient.setLocationOption(aMapLocationClientOption);
        //使用客户端开始定位
        aMapLocationClient.startLocation();
    }

    private void createImageStorageDir() {
        File storageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), getString(R.string.find_job_image_path));
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
    }

    private void initData() {
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        cropList = filterManager.getCropList();
        for (CornEntity crop : cropList) {
            cropMap.put(crop.getId(), crop.getCropName());
        }
        machineTypeList = filterManager.getMachineTypeList();
        for (AgriculturalMachineryType type : machineTypeList) {
            typeMap.put(type.getId(), type.getName());
        }
    }

    private void showJobList() {
        if(isRefresh) {
            enough = false;
        }

        // 确保 adapter 已初始化
        if (adapter == null) {
            adapter = new FindJobAdapter(this, new ArrayList<>());
            adapter.setGridView(gridView);
            gridView.setAdapter(adapter);
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put(getString(R.string.find_job_my_agricultural_machinery), true);
            requestBody.put(getString(R.string.find_job_order_by_create_time_desc), true);

            // 添加筛选条件（如果有）
            if (filterConditions != null) {
                // 将筛选条件合并到请求体中
                for (Iterator<String> it = filterConditions.keys(); it.hasNext(); ) {
                    String key = it.next();
                    requestBody.put(key, filterConditions.get(key));
                }
            }

        } catch (JSONException e) {
            Log.e(getString(R.string.find_job_toast), getString(R.string.find_job_json_failure), e);
        }

        RequestBody body = RequestBody.create(MediaType.parse(getString(R.string.find_job_request_type)), requestBody.toString());
        OkHttpUtils.okHttpClient.newCall(new Request.Builder()
                .url(getString(R.string.server_url) + getString(R.string.find_job_request_list) + "?" + getString(R.string.find_job_page_num) + getString(R.string.find_job_equal) + currentPage + getString(R.string.find_job_and) + getString(R.string.find_job_page_size) + getString(R.string.find_job_equal) + PAGE_SIZE)
                .addHeader(getString(R.string.request_header_key_token), sharedPreferences.getString(getString(R.string.user_pref_key_token), ""))
                .post(body)
                .build()
        ).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(FindJobActivity.this, getString(R.string.find_job_internet_failure), Toast.LENGTH_SHORT).show();
                    // 确保在失败时也显示 GridView（如果有缓存数据的话）
                    if (adapter != null && adapter.getCount() > 0) {
                        gridView.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        if (jsonObject.getInt(getString(R.string.find_job_code)) == getResources().getInteger(R.integer.find_job_code_success)) {
                            JSONArray data = jsonObject.getJSONArray(getString(R.string.find_job_data));
                            jobDetails = new ArrayList<>();

                            for (int i = 0; i < data.length(); i++) {
                                JSONObject jobObject = data.getJSONObject(i);
                                FindJobDetail detail = new FindJobDetail();

                                detail.setId(jobObject.getString(getString(R.string.find_job_id)));
                                detail.setName(jobObject.getString(getString(R.string.find_job_name)));
                                detail.setPhone(jobObject.getString(getString(R.string.find_job_phone_number)));
                                detail.setTaskStatus(jobObject.getInt(getString(R.string.find_job_status)));

                                // 使用 JobDateTimeUtils 处理时间
                                String createTime = jobObject.getString(getString(R.string.find_job_create_time));
                                String validTime = jobObject.getString(getString(R.string.find_job_end_time));
                                detail.setTaskCreateTime(JobDateTimeUtils.parseServerDateTime(createTime));
                                detail.setTaskValidityPeriod(JobDateTimeUtils.parseServerDateTime(validTime));

                                detail.setHasGrainTransportVehicle(jobObject.getBoolean(getString(R.string.find_job_having_grain_carrier)));

                                // 获取农机类型名称
                                detail.setMachineType(jobObject.getString(getString(R.string.find_job_type_name)));

                                // 处理作物
                                JSONArray cropsArray = jobObject.getJSONArray(getString(R.string.find_job_crop_array));
                                String[] crops = new String[cropsArray.length()];
                                for (int j = 0; j < cropsArray.length(); j++) {
                                    crops[j] = cropsArray.getString(j);
                                }
                                detail.setWorkableCrops(crops);
                                // 位置前缀
                                detail.setAddressPrefix(jobObject.getString(getString(R.string.find_job_address_prefix)));
                                // 处理置
                                JSONArray locationArray = jobObject.getJSONArray(getString(R.string.find_job_location));
                                String[] locations = new String[locationArray.length()];
                                for(int j = 0; j < locationArray.length(); j++) {
                                    locations[j] = locationArray.getString(j);
                                }
                                detail.setWorkArea(locations);

                                // 处理图片
                                JSONArray imageArray = jobObject.getJSONArray(getString(R.string.find_job_image));
                                String[] images = new String[imageArray.length()];
                                for (int j = 0; j < imageArray.length(); j++) {
                                    images[j] = imageArray.getString(j);
                                }
                                detail.setImage(images);

                                jobDetails.add(detail);
                                if(data.length() < PAGE_SIZE && !isRefresh) {
                                    enough = true;
                                }
                            }

                            runOnUiThread(() -> {
                                updateAdapter(isRefresh);
                                // 完成刷新
                                if(isRefresh) {
                                    refreshLayout.finishRefresh();
                                } else {
                                    refreshLayout.finishLoadMore();
                                }
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(FindJobActivity.this,
                                    getString(R.string.find_job_wait_data_failure), Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(FindJobActivity.this,
                                getString(R.string.find_job_analysis_data_failure), Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    private void updateAdapter(boolean isRefresh) {
        // 添加空值检查
        if (adapter == null) {
            adapter = new FindJobAdapter(this, new ArrayList<>());
            adapter.setGridView(gridView);
            gridView.setAdapter(adapter);
        }

        if (jobDetails == null) {
            jobDetails = new ArrayList<>();
        }

        if(isRefresh) {
            adapter.updateData(jobDetails);
            refreshLayout.finishRefresh();
        } else {
            adapter.addData(jobDetails);
            refreshLayout.finishLoadMore();
        }

        // 确保数据加载完成后再显示
        if (gridView.getVisibility() != View.VISIBLE) {
            gridView.setVisibility(View.VISIBLE);
        }
    }

    private void initSelectors() {
        threadPoolExecutor.execute(() -> {
            while (aMapLocationClient == null || provinceList == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e(getString(R.string.find_job_toast), getString(R.string.find_job_client_init_failure), e);
                }
            }

            runOnUiThread(() -> {
                filterManager = new JobFilterManager(this, sharedPreferences, aMapLocationClient, provinceList);
                filterManager.setupSelectors(
                        edtMachineType, llMachineTypeTags, tvMachineTypeTap,
                        edtWorkableCrops, llWorkableCropsTags, tvWorkableCropsTap,
                        edtWorkArea, llWorkAreaTags, tvWorkAreaTap,
                        edtGrainTransport, llGrainTransportTags, tvGrainTransportTap
                );
                filterManager.initSelectorData();
            });
        });
    }

    private void initEvents() {
        ivAdd.setOnClickListener(v -> {
            hideAllOperationLayers();  // 添加隐藏操作
            Intent intent = new Intent(FindJobActivity.this, AddTaskActivity.class);
            startActivity(intent);
        });

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            // 获取当前点击的操作层
            View currentOperationLayer = view.findViewById(R.id.job_operation_layer);
            
            // 如果当前操作层已经显示，则隐藏它
            if (currentOperationLayer.getVisibility() == View.VISIBLE) {
                currentOperationLayer.setVisibility(View.GONE);
                return;
            }
            
            // 隐藏所有其他操作层
            hideAllOperationLayers();
            
            // 显示当前点击项的操作层
            currentOperationLayer.setVisibility(View.VISIBLE);
            
            // 阻止事件冒泡到根视图
            view.setOnTouchListener((v, event) -> true);
        });
        // 下拉刷新
        refreshLayout.setOnRefreshListener(refreshLayout -> {
            hideAllOperationLayers();  // 添加隐藏操作
            isRefresh = true;
            currentPage = 1;
            showJobList();
        });
        // 上拉加载
        refreshLayout.setOnLoadMoreListener(refreshLayout -> {
            hideAllOperationLayers();  // 添加隐藏操作
            isRefresh = false;
            if(enough) {
                refreshLayout.finishLoadMoreWithNoMoreData();
                return;
            }
            currentPage++;
            showJobList();
        });

        // 点击筛选按钮
        ivSearch.setOnClickListener(v -> {
            hideAllOperationLayers();  // 添加隐藏操作
            checkTargetingPermissions();
            // 在打开抽屉前恢复上次的筛选条件
            if (lastConfirmedFilters != null) {
                restoreLastConfirmedFilters();
            }
            findJobDrawer.openDrawer(GravityCompat.END);
        });

        // 修改农机类型选择器点击事件
        tvMachineTypeTap.setOnClickListener(v -> {
            JobUIUtils.handleSelectorToggle(
                llMachineTypeTags,
                filterManager,
                this::hideEditText
            );
            if (llMachineTypeTags.getVisibility() == View.VISIBLE) {
                filterManager.showMachineTypeSelector();
            }
        });

        // 修改作物选择器点击事件
        tvWorkableCropsTap.setOnClickListener(v -> {
            JobUIUtils.handleSelectorToggle(
                llWorkableCropsTags,
                filterManager,
                this::hideEditText
            );
            if (llWorkableCropsTags.getVisibility() == View.VISIBLE) {
                filterManager.showCropSelector();
            }
        });

        // 修改作业区域选择器点击事件
        tvWorkAreaTap.setOnClickListener(v -> {
            JobUIUtils.handleSelectorToggle(
                llWorkAreaTags,
                filterManager,
                this::hideEditText
            );
            if (llWorkAreaTags.getVisibility() == View.VISIBLE) {
                filterManager.showWorkAreaSelector();
            }
        });

        // 修改运粮车选择器点击事件
        tvGrainTransportTap.setOnClickListener(v -> {
            JobUIUtils.handleSelectorToggle(
                llGrainTransportTags,
                filterManager,
                this::hideEditText
            );
            if (llGrainTransportTags.getVisibility() == View.VISIBLE) {
                filterManager.showGrainTransportSelector();
            }
        });

        // 添加任务名入框点变化监听
        edtJobName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                filterManager.hideAllSelectors();
            }
        });

        // 移除输入框的点击事件
        edtMachineType.setOnClickListener(null);
        edtWorkableCrops.setOnClickListener(null);
        edtWorkArea.setOnClickListener(null);
        edtGrainTransport.setOnClickListener(null);

        // 修改重置按钮事件
        headerView.findViewById(R.id.btn_find_job_navigation_reset).setOnClickListener(v -> {
            // 重置所有选择器
            filterManager.resetAllSelectors();
            edtJobName.setText("");

            // 重置筛选条件
            filterConditions = new JSONObject();

            // 重置页码
            currentPage = 1;
            isRefresh = true;

            // 直接调用 showJobList，不需要额外的 autoRefresh
            showJobList();
        });

        // 修改确定按钮点击事件
        headerView.findViewById(R.id.btn_find_job_navigation_confirm).setOnClickListener(v -> {
            // 保存当前的筛选条件为已确认的条件
            try {
                lastConfirmedFilters = new JSONObject();
                if (filterConditions != null) {
                    for (Iterator<String> it = filterConditions.keys(); it.hasNext(); ) {
                        String key = it.next();
                        lastConfirmedFilters.put(key, filterConditions.get(key));
                    }
                }
            } catch (JSONException e) {
                Log.e(getString(R.string.find_job_toast), getString(R.string.find_job_select_conditions_save), e);
            }
            
            findJobDrawer.closeDrawer(GravityCompat.END);
            // 向后端发送请求
            getFilterConditions();
        });

        // 修改取消按钮点击事件
        headerView.findViewById(R.id.btn_find_job_navigation_cancel).setOnClickListener(v -> {
            // 直接重置所有选择器，不需要恢复上次的条件
            filterManager.resetAllSelectors();
            edtJobName.setText("");
            // 关闭抽屉
            findJobDrawer.closeDrawer(GravityCompat.END);
        });

        // 修改抽屉关闭监听器
        findJobDrawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                clearInputFocus();
            }
        });
    }

    // 添加恢复上次确认条件的方法
    private void restoreLastConfirmedFilters() {
        // 重置所有选择器
        filterManager.resetAllSelectors();
        edtJobName.setText("");

        // 如果有上次确认的条件，则恢复
        if (lastConfirmedFilters != null) {
            try {
                // 恢复任务名称
                if (lastConfirmedFilters.has(getString(R.string.find_job_name))) {
                    edtJobName.setText(lastConfirmedFilters.getString(getString(R.string.find_job_name)));
                }

                // 恢复农机类型选择
                if (lastConfirmedFilters.has(getString(R.string.find_job_type_id))) {
                    String machineTypeId = lastConfirmedFilters.getString(getString(R.string.find_job_type_id));
                    filterManager.restoreMachineTypeSelection(machineTypeId);
                }

                // 恢复作物选择
                if (lastConfirmedFilters.has(getString(R.string.find_job_crop_id))) {
                    JSONArray cropIds = lastConfirmedFilters.getJSONArray(getString(R.string.find_job_crop_id));
                    filterManager.restoreCropSelection(cropIds);
                }

                // 恢复作业区域选择
                if (lastConfirmedFilters.has(getString(R.string.find_job_location))) {
                    JSONArray locations = lastConfirmedFilters.getJSONArray(getString(R.string.find_job_location));
                    filterManager.restoreWorkAreaSelection(locations);
                }

                // 恢复运粮车选择
                if (lastConfirmedFilters.has(getString(R.string.find_job_having_grain_carrier))) {
                    boolean hasGrainTransport = lastConfirmedFilters.getBoolean(getString(R.string.find_job_having_grain_carrier));
                    filterManager.restoreGrainTransportSelection(hasGrainTransport);
                }

                // 恢复筛选条件
                filterConditions = new JSONObject(lastConfirmedFilters.toString());
            } catch (JSONException e) {
                Log.e(getString(R.string.find_job_toast), getString(R.string.find_job_select_conditions_save), e);
            }
        }
    }

    private void hideAllOperationLayers() {
        if (gridView != null) {
            for (int i = 0; i < gridView.getChildCount(); i++) {
                View child = gridView.getChildAt(i);
                View operationLayer = child.findViewById(R.id.job_operation_layer);
                if (operationLayer != null) {
                    operationLayer.setVisibility(View.GONE);
                }
            }
        }
    }

    private void initAdapter() {
        adapter = new FindJobAdapter(this, new ArrayList<>());
        adapter.setGridView(gridView);
        adapter.setOnItemOperationListener(new FindJobAdapter.OnItemOperationListener() {
            @Override
            public void onViewDetail(FindJobDetail jobDetail) {
                // 处理查看详情
                Intent intent = new Intent(FindJobActivity.this, JobDetailActivity.class);
                intent.putExtra(getString(R.string.find_job_update_job_detail), jobDetail);
                startActivity(intent);
            }

            @Override
            public void onViewUpdate(FindJobDetail jobDetail) {
                // 处理编辑操作
                Intent intent = new Intent(FindJobActivity.this, UpdateTaskActivity.class);
                intent.putExtra(getString(R.string.find_job_update_job_detail), jobDetail);
                startActivity(intent);
            }

            @Override
            public void onRemove(FindJobDetail jobDetail) {
                // 下架弹窗
                DialogUtils dialogUtils = new DialogUtils(FindJobActivity.this);
                androidx.appcompat.app.AlertDialog dialog = dialogUtils.getDialog();
                dialogUtils.setTitle(getString(R.string.find_job_item_remove))
                        .setContent(getString(R.string.find_job_item_remove_message))
                        .setPositiveText(getString(R.string.find_job_item_remove_confirm))
                        .setNegativeText(getString(R.string.find_job_item_remove_cancel))
                        .setPositiveListener(v -> {
                            // 用户点了“确认”
                            removeItem(jobDetail);
                            dialog.cancel();
                            hideAllOperationLayers();
                        })
                        .setNegativeListener(v -> {
                            // 用户点击了“取消”，不执行任何操作
                            dialog.cancel();
                            adapter.notifyDataSetChanged();
                            hideAllOperationLayers();
                        });
                View view = dialogUtils.getView();
                dialog.setContentView(view);
                dialog.show();
            }

            private void removeItem(FindJobDetail jobDetail) {
                // 下架请求
                OkHttpUtils.okHttpClient.newCall(new Request.Builder()
                        .url(getString(R.string.server_url) + getString(R.string.find_job_request_remove) + jobDetail.getId())
                        .addHeader(getString(R.string.request_header_key_token), sharedPreferences.getString(getString(R.string.user_pref_key_token), ""))
                        .delete()
                        .build()
                ).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> Toast.makeText(FindJobActivity.this, 
                                     getString(R.string.find_job_item_remove_failure), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        if(response.isSuccessful()) {
                            if(response.code() == 200) {
                                runOnUiThread(() -> {
                                    // 直接更新状态而不是重新加载
                                    jobDetail.setTaskStatus(1);  // 更新为下架状态
                                    Toast.makeText(FindJobActivity.this, 
                                         getString(R.string.find_job_item_remove_success), Toast.LENGTH_SHORT).show();
                                    // 通知适配器刷新
                                    adapter.notifyDataSetChanged();
                                });
                            }
                        } else {
                            runOnUiThread(() -> Toast.makeText(FindJobActivity.this, 
                                         getString(R.string.find_job_item_remove_failure), Toast.LENGTH_SHORT).show());
                        }
                    }
                });
            }
        });
        gridView.setAdapter(adapter);

    }

    private void initViews() {
        ivPhoto = findViewById(R.id.iv_photo);
        ivAdd = findViewById(R.id.iv_job_add);
        ivSearch = findViewById(R.id.iv_job_search);
        gridView = findViewById(R.id.gv_find_job);
        gridView.setVisibility(View.INVISIBLE);  // 初始设置为不可见
        refreshLayout = findViewById(R.id.find_job_refresh);

        // 初始化 DrawerLayout
        findJobDrawer = findViewById(R.id.drawer_find_job);

        // 获取 NavigationView 和它的 headerView
        navigationView = findViewById(R.id.nav_find_job_select);
        headerView = navigationView.getHeaderView(0);

        findJobSidebar = headerView.findViewById(R.id.find_job_sidebar);
        setupTouchListener(findJobSidebar);

        // 农机类型选择器
        edtMachineType = headerView.findViewById(R.id.edt_find_job_machine_type);
        llMachineTypeTags = headerView.findViewById(R.id.ll_machine_type_tags);
        tvMachineTypeTap = headerView.findViewById(R.id.tv_machine_type_tap);

        // 作物选器
        edtWorkableCrops = headerView.findViewById(R.id.edt_workable_crops);
        llWorkableCropsTags = headerView.findViewById(R.id.ll_workable_crops_tags);
        tvWorkableCropsTap = headerView.findViewById(R.id.tv_workable_crops_tap);

        // 作业区域选择器
        edtWorkArea = headerView.findViewById(R.id.edt_find_job_work_area);
        llWorkAreaTags = headerView.findViewById(R.id.ll_work_area_tags);
        tvWorkAreaTap = headerView.findViewById(R.id.tv_work_area_tap);

        // 运粮车选择器
        edtGrainTransport = headerView.findViewById(R.id.edt_grain_transport_vehicle);
        llGrainTransportTags = headerView.findViewById(R.id.ll_grain_transport_tags);
        tvGrainTransportTap = headerView.findViewById(R.id.tv_grain_transport_vehicle_tap);

        // 任务名输入框
        edtJobName = headerView.findViewById(R.id.edt_find_job_name);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchListener(View rootView) {
        rootView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                clearInputFocus();
            }
            return false;
        });
    }

    private void clearInputFocus() {
        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            getCurrentFocus().clearFocus();
        }
    }

    // 添加隐藏输入法和清除焦点的方法
    private void hideEditText() {
        edtJobName.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edtJobName.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void initLocationClient() {
        try {
            AMapLocationClient.updatePrivacyShow(this, true, true);
            AMapLocationClient.updatePrivacyAgree(this, true);
            aMapLocationClient = new AMapLocationClient(this);

            AMapLocationClientOption option = new AMapLocationClientOption();
            option.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            option.setOnceLocation(true);
            aMapLocationClient.setLocationOption(option);
            aMapLocationClient.startLocation();
        } catch (Exception e) {
            Log.e(getString(R.string.find_job_toast), getString(R.string.find_job_client_create_failure), e);
        }
    }

    private void initProvinceList() {
        threadPoolExecutor.execute(() -> {
            provinceList = QueryUtils.getLandProvince(
                    getString(R.string.server_url),
                    getString(R.string.address_url) + getString(R.string.province_url),
                    sharedPreferences.getString(getString(R.string.user_pref_key_token), "")
            );
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aMapLocationClient != null) {
            aMapLocationClient.onDestroy();
        }
        // 添加线程池关闭
        if (threadPoolExecutor != null && !threadPoolExecutor.isShutdown()) {
            threadPoolExecutor.shutdown();
        }
    }

    /**
     * 获取筛选条件
     */
    private void getFilterConditions() {
        try {
            // 重置筛选条件
            filterConditions = new JSONObject();
            
            // 检查任务名称
            String jobName = edtJobName.getText().toString().trim();
            if (!jobName.isEmpty()) {
                filterConditions.put(getString(R.string.find_job_name), jobName);
            }

            // 只有当filterManager已初始化时才检查筛选条件
            if (filterManager != null) {
                if(!edtMachineType.getText().toString().isEmpty()) {
                    // 检查农机类型
                    String selectedMachineTypes = filterManager.getSelectedMachineTypeIds();
                    if (!selectedMachineTypes.isEmpty()) {
                        filterConditions.put(getString(R.string.find_job_type_id), selectedMachineTypes);
                    }
                }

                if(!edtWorkableCrops.getText().toString().isEmpty()) {
                    // 检查作物类型
                    List<Long> selectedCrops = filterManager.getSelectedCropIds();
                    if (!selectedCrops.isEmpty()) {
                        filterConditions.put(getString(R.string.find_job_crop_id), new JSONArray(selectedCrops));
                    }
                }

                if(!edtWorkArea.getText().toString().isEmpty()) {
                    // 检查作业区域
                    List<String> selectedAreas = filterManager.getSelectedWorkAreas();
                    if (!selectedAreas.isEmpty()) {
                        filterConditions.put(getString(R.string.find_job_location), new JSONArray(selectedAreas));
                    }
                }

                if(!edtGrainTransport.getText().toString().isEmpty()) {
                    // 检查运粮车选择状态
                    Boolean hasGrainTransport = filterManager.getGrainTransportSelection();
                    if (hasGrainTransport != null) {
                        filterConditions.put(getString(R.string.find_job_having_grain_carrier), hasGrainTransport);
                    }
                }
            }

            // 保存当前确认的筛选条件
            lastConfirmedFilters = new JSONObject(filterConditions.toString());
            
            // 关闭抽屉
            findJobDrawer.closeDrawer(GravityCompat.END);
            // 重置页码
            currentPage = 1;
            isRefresh = true;
            // 直接调用 showJobList不需要额外的 autoRefresh
            showJobList();
        } catch (JSONException e) {
            Log.e(getString(R.string.find_job_toast), getString(R.string.find_job_filter_init_failure), e);
        }
    }
}