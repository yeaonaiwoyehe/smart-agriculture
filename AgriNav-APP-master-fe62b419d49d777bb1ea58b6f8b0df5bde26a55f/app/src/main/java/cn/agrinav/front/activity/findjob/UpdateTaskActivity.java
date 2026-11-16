package cn.agrinav.front.activity.findjob;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.io.File;

import cn.agrinav.R;
import cn.agrinav.front.activity.findjob.utils.JobDateTimeUtils;
import cn.agrinav.front.activity.findjob.utils.JobFilterManager;
import cn.agrinav.front.activity.findjob.utils.JobImageUtils;
import cn.agrinav.front.activity.findjob.utils.JobUIUtils;
import cn.agrinav.front.activity.findjob.utils.TaskInputValidationUtils;
import cn.agrinav.front.adapter.MachineImagesAdapter;
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
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UpdateTaskActivity extends AppCompatActivity {

    private EditText taskName;
    private EditText phone;
    private TextView validityPeriod;
    private Button save;
    private SharedPreferences sharedPreferences;
    private RecyclerView machineImagesRecyclerView;
    private List<String> machineImages = new ArrayList<>();
    private MachineImagesAdapter machineImagesAdapter;
    private final int MAX_IMAGES = 3;
    private ImageView addImageView;
    private boolean hasUserSelectedImage = false;
    private AMapLocationClient aMapLocationClient;
    private List<LandAddress> provinceList;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> albumLauncher;
    private ActivityResultLauncher<String> permissionLauncher;
    private final ThreadPoolExecutor threadPoolExecutor;
    private EditText edtWorkableCrops;
    private TextView tvTapWorkableCrops;
    private LinearLayout llWorkableCropsTags;
    private ActivityResultLauncher launcher;
    private EditText edtFindJobMachineType;
    private TextView tvTapMachineType;
    private LinearLayout llMachineTypeTags;
    private EditText edtFindJobWorkArea;
    private TextView tvTapWorkArea;
    private LinearLayout llWorkAreaTags;
    private StringBuilder location = new StringBuilder();
    // 后端所有农机型号
    private static List<AgriculturalMachineryType> machineTypeList = new ArrayList<>();
    // 后端所有作物
    private List<CornEntity> cropList = new ArrayList<>();
    private EditText edtGrainTransportVehicle;
    private TextView tvTapGrainTransportVehicle;
    private LinearLayout llGrainTransportVehicleTags;
    private FindJobDetail jobDetail;
    private int id;
    private Snackbar snackbar;
    private JobFilterManager filterManager;

    public UpdateTaskActivity() {
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
        setContentView(R.layout.activity_update_task);
        // 在这里初始化所有的 launcher
        initLauncher();
        // 初始化定位客户端和省份列表
        initLocationClient();
        initProvinceList();
        // 等待定位客户端初始化完成
        threadPoolExecutor.execute(() -> {
            while (aMapLocationClient == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e(getString(R.string.find_job_update_toast), getString(R.string.find_job_client_create_failure), e);
                }
            }
            runOnUiThread(() -> {
                initViews();
                setupImagePicker();
                initData();
                initSelectors();
                initEvents();
                checkTargetingPermissions();
            });
        });
    }

    private void initData() {
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        Intent intent = getIntent();
        jobDetail = (FindJobDetail) intent.getSerializableExtra(getString(R.string.find_job_update_job_detail));

        if (jobDetail != null) {
            // 回显基本信息
            taskName.setText(jobDetail.getName());
            phone.setText(jobDetail.getPhone());

            // 回显有效期
            if (jobDetail.getTaskValidityPeriod() != null) {
                String formattedDate = JobDateTimeUtils.formatDate(jobDetail.getTaskValidityPeriod());
                validityPeriod.setText(formattedDate);
            }

            // 修改图片回显逻辑
            if (jobDetail.getImage() != null && jobDetail.getImage().length > 0) {
                machineImages.clear();  // 清空现有图片列表
                CountDownLatch latch = new CountDownLatch(jobDetail.getImage().length);

                for (String imageUrl : jobDetail.getImage()) {
                    // 使用 JobImageUtils 生成文件名
                    String fileName = JobImageUtils.generateImageFileName();
                    // 使用 JobImageUtils 创建文件
                    File imageFile = JobImageUtils.createImageFile(this, fileName);
                    // 使用 JobImageUtils 下载图片
                    JobImageUtils.downloadImage(imageUrl, imageFile, machineImages, latch);
                }

                threadPoolExecutor.execute(() -> {
                    try {
                        latch.await(30, TimeUnit.SECONDS);
                        runOnUiThread(() -> {
                            hasUserSelectedImage = true;
                            if (machineImagesAdapter == null) {
                                initRecyclerView();
                            } else {
                                machineImagesAdapter.notifyDataSetChanged();
                            }
                            updateAddImageViewVisibility();
                        });
                    } catch (InterruptedException e) {
                        Log.e(getString(R.string.find_job_update_toast), getString(R.string.find_job_image_load_failure), e);
                    }
                });
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private void initEvents() {
        // 有效期选择器
        validityPeriod.setOnClickListener(v -> {
            hideAllSelectors();
            showCustomDateTimePicker();
        });

        // 保存按钮
        save.setOnClickListener(v -> {
            if (filterManager == null) {
                Toast.makeText(this, getString(R.string.find_job_wait_data), Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (validateInputs()) {
                FindJobDetail task = createFindJobDetail();
                saveToServer(task);
            }
        });

        // 等待 filterManager 初始化完成后再设置选择器的点击事件
        threadPoolExecutor.execute(() -> {
            while (filterManager == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e(getString(R.string.find_job_update_toast), getString(R.string.find_job_filter_manager_init_failure), e);
                }
            }

            runOnUiThread(() -> {
                // 作物选择器
                tvTapWorkableCrops.setOnClickListener(v -> {
                    hideEditText();
                    if (llWorkableCropsTags.getVisibility() == View.VISIBLE) {
                        filterManager.hideAllSelectors();
                        return;
                    }
                    filterManager.hideAllSelectors();
                    filterManager.showCropSelector();
                });

                // 农机类型选择器
                tvTapMachineType.setOnClickListener(v -> {
                    hideEditText();
                    if (llMachineTypeTags.getVisibility() == View.VISIBLE) {
                        filterManager.hideAllSelectors();
                        return;
                    }
                    filterManager.hideAllSelectors();
                    filterManager.showMachineTypeSelector();
                });

                // 作业区域选择器
                tvTapWorkArea.setOnClickListener(v -> {
                    hideEditText();
                    if (llWorkAreaTags.getVisibility() == View.VISIBLE) {
                        filterManager.hideAllSelectors();
                        return;
                    }
                    filterManager.hideAllSelectors();
                    filterManager.showWorkAreaSelector();
                });

                // 运粮车选择器
                tvTapGrainTransportVehicle.setOnClickListener(v -> {
                    hideEditText();
                    if (llGrainTransportVehicleTags.getVisibility() == View.VISIBLE) {
                        filterManager.hideAllSelectors();
                        return;
                    }
                    filterManager.hideAllSelectors();
                    filterManager.showGrainTransportSelector();
                });
            });
        });

        // 输入框焦点变化监听
        taskName.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus) {
                hideAllSelectors();
            }
        });

        phone.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus) {
                hideAllSelectors();
            }
        });
    }

    private void hideEditText() {
        JobUIUtils.hideKeyboardAndClearFocus(this, taskName, phone);
    }

    private void hideAllSelectors() {
        if (filterManager != null) {
            filterManager.hideAllSelectors();
        }
    }

    private void saveToServer(FindJobDetail task) {
        try {
            // 创建MultipartBody构建器
            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            // 添加农信息JSON
            JSONObject agriculturalMachinery = new JSONObject();
            // id
            agriculturalMachinery.put(getString(R.string.find_job_id), task.getId());
            // 农机型id
            String typeId = "";
            for (AgriculturalMachineryType machineType : machineTypeList) {
                if (machineType.getName().equals(task.getMachineType())) {
                    typeId = machineType.getId();
                    break;
                }
            }
            agriculturalMachinery.put(getString(R.string.find_job_type_id), typeId);
            agriculturalMachinery.put(getString(R.string.find_job_name), task.getName());

            // 作物id
            JSONArray cropIdArray = new JSONArray();
            for (String cropName : task.getWorkableCrops()) {
                for (CornEntity crop : cropList) {
                    if (crop.getCropName().equals(cropName)) {
                        cropIdArray.put(crop.getId());
                        break;
                    }
                }
            }
            agriculturalMachinery.put(getString(R.string.find_job_crop_id), cropIdArray);

            // 运粮车
            agriculturalMachinery.put(getString(R.string.find_job_having_grain_carrier), task.isHasGrainTransportVehicle());

            // 作业区域前缀
            agriculturalMachinery.put(getString(R.string.find_job_address_prefix), task.getAddressPrefix());

            // 修改作业区域的处理逻辑
            JSONArray workAreaArray = new JSONArray();
            for (String area : task.getWorkArea()) {
                // 直接用完整区域名称不再拼前缀
                workAreaArray.put(area);
            }
            agriculturalMachinery.put(getString(R.string.find_job_location), workAreaArray);

            // 联系方
            agriculturalMachinery.put(getString(R.string.find_job_phone_number), task.getPhone());

            // 有效期
            agriculturalMachinery.put(getString(R.string.find_job_end_time), JobDateTimeUtils.formatDateForServer(task.getTaskValidityPeriod()));

            // 添加JSON数据到form-data
            builder.addFormDataPart(getString(R.string.find_job_agricultural_machinery), agriculturalMachinery.toString());

            // 添加图片到form-data
            for (int i = 0; i < machineImages.size(); i++) {
                String imagePath = machineImages.get(i);
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    RequestBody imageBody = RequestBody.create(MediaType.parse(getString(R.string.find_job_image_type)), imageFile);
                    builder.addFormDataPart(getString(R.string.find_job_image), imageFile.getName(), imageBody);
                }
            }

            // 创建请求
            Request request = new Request.Builder()
                    .url(getString(R.string.server_url) + getString(R.string.find_job_request_update))
                    .addHeader(getString(R.string.request_header_key_token),
                            sharedPreferences.getString(getString(R.string.user_pref_key_token), ""))
                    .put(builder.build())
                    .build();

            // 发送请求
            OkHttpUtils.okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(UpdateTaskActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    if(response.isSuccessful()) {
                        try {
                            String responseData = response.body().string();
                            JSONObject jsonObject = new JSONObject(responseData);
                            if (jsonObject.getInt(getString(R.string.find_job_code)) == getResources().getInteger(R.integer.find_job_code_success)) {
                                runOnUiThread(() -> {
                                    Toast.makeText(UpdateTaskActivity.this, getString(R.string.find_job_update_success), Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(UpdateTaskActivity.this, FindJobActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                    // 使用 JobImageUtils 删除图片
                                    String path = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + getString(R.string.find_job_image_path);
                                    JobImageUtils.deleteImages(path);
                                    finish();
                                });
                            }else {
                                runOnUiThread(() -> Toast.makeText(UpdateTaskActivity.this, jsonObject.optString(getString(R.string.find_job_msg)), Toast.LENGTH_SHORT).show());
                            }
                        }catch (Exception e) {
                            Log.e(getString(R.string.find_job_update_toast), getString(R.string.find_job_response_failure), e);
                        }
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(getString(R.string.find_job_update_toast), getString(R.string.find_job_json_failure), e);
        }
    }

    private boolean validateInputs() {
        // 确保 filterManager 已经初始化
        if (filterManager == null) {
            Log.e(getString(R.string.find_job_update_toast), getString(R.string.find_job_filter_manager_init_failure));
            return false;
        }

        return TaskInputValidationUtils.validateTaskInputs(
                this,
                taskName,
                filterManager.getMachineTypeSelector(),
                edtFindJobMachineType,
                filterManager.getWorkableCropsSelector(),
                edtWorkableCrops,
                filterManager.getWorkAreaSelector(),
                edtFindJobWorkArea,
                filterManager.getGrainTransportSelector(),
                edtGrainTransportVehicle,
                phone,
                validityPeriod,
                hasUserSelectedImage
        );
    }

    private FindJobDetail createFindJobDetail() {
        FindJobDetail task = new FindJobDetail();
        task.setId(jobDetail.getId());

        // 设置任务名称
        if(taskName.getText().toString().trim().isEmpty()) {
            task.setName(getString(R.string.find_job_task_name_default) + System.currentTimeMillis());
        } else {
            task.setName(taskName.getText().toString());
        }

        // 设置农机类型
        List<String> machineTypes = filterManager.getMachineTypeSelector().getSelectedTags();
        if (!machineTypes.isEmpty()) {
            task.setMachineType(machineTypes.get(0));
        }

        // 设置可作业作物
        List<String> selectedCrops = filterManager.getWorkableCropsSelector().getSelectedTags();
        if (!selectedCrops.isEmpty()) {
            task.setWorkableCrops(selectedCrops.toArray(new String[0]));
        } else if (jobDetail != null && jobDetail.getWorkableCrops() != null) {
            // 如果没有新选择，使用原有的作物数据
            task.setWorkableCrops(jobDetail.getWorkableCrops());
        }

        // 设置是否有运粮车
        List<String> transportSelection = filterManager.getGrainTransportSelector().getSelectedTags();
        task.setHasGrainTransportVehicle(!transportSelection.isEmpty() && transportSelection.get(0).equals("是"));

        // 设置联系电话
        task.setPhone(phone.getText().toString());

        // 设置任务有效期
        String validityDateStr = validityPeriod.getText().toString();
        task.setTaskValidityPeriod(JobDateTimeUtils.parseDate(validityDateStr));

        // 设置任务创建时间
        task.setTaskCreateTime(new Date());
        task.setTaskStatus(0);

        // 修改作业区域的设置逻辑
        if (!edtFindJobWorkArea.getText().toString().isEmpty()) {
            // 如果没有修改，使用原有的数据
            if (jobDetail != null && jobDetail.getWorkArea() != null &&
                    edtFindJobWorkArea.getText().toString().equals(String.join(" ", Arrays.stream(jobDetail.getWorkArea())
                            .map(area -> area.substring(jobDetail.getAddressPrefix().length()))
                            .toArray(String[]::new)))) {
                task.setAddressPrefix(jobDetail.getAddressPrefix());
                task.setWorkArea(jobDetail.getWorkArea());
            } else {
                // 如果有修改，构建完整的区域名称
                // 检查 location 是否为空，如果为空则使用原有的前缀
                String prefix = location.length() > 0 ? location.toString() : 
                              (jobDetail != null ? jobDetail.getAddressPrefix() : "");
                task.setAddressPrefix(prefix);
                
                List<String> selectedAreas = filterManager.getWorkAreaSelector().getSelectedTags();
                String[] areas = new String[selectedAreas.size()];
                for (int i = 0; i < selectedAreas.size(); i++) {
                    areas[i] = prefix + selectedAreas.get(i);
                }
                task.setWorkArea(areas);
            }
        }

        return task;
    }

    private void checkTargetingPermissions() {
        ArrayList<String> permissions = null;
        //这里把每一个权限都进行一次检验
        for(int i = 0; i< PermissionTrajectory.PERMISSIONS_LOCATION.length; i++) {
            //判断是否拥有某项权限
            if (ContextCompat.checkSelfPermission(this, PermissionTrajectory.PERMISSIONS_LOCATION[i]) != PackageManager.PERMISSION_GRANTED) {
                //判断是否有未获得的权限已经创建了集合，如果不是，现在就开始新建集合
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
            //在这里设置去请用户权限
            ActivityCompat.requestPermissions(this,strings, getResources().getInteger(R.integer.permission_trajectory_request_code));
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
        if (requestCode == getResources().getInteger(R.integer.permission_trajectory_request_code)) {
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
        //首先获取定位客户
        aMapLocationClient = null;
        try {
            aMapLocationClient = new AMapLocationClient(this.getApplicationContext());
        } catch (Exception e) {
            Log.i(getString(R.string.find_job_update_toast), getString(R.string.find_job_client_create_failure));
        }
        AMapLocationClientOption aMapLocationClientOption = new AMapLocationClientOption();
        //置定模式为高精度模
        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置北斗定优先
        aMapLocationClientOption.setBeidouFirst(true);
        aMapLocationClientOption.getGeoLanguage();
        //设置wifi自动刷新
        aMapLocationClientOption.setWifiScan(true);
        //设置开启wifi终扫描
        AMapLocationClientOption.setOpenAlwaysScanWifi(true);
        aMapLocationClientOption.setInterval(2000);
        //配置项加载到客户中
        aMapLocationClient.setLocationOption(aMapLocationClientOption);
        //使用客户端开始定位
        aMapLocationClient.startLocation();
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

    private void initViews() {
        taskName = findViewById(R.id.edt_update_task_name);
        phone = findViewById(R.id.edt_update_phone);
        validityPeriod = findViewById(R.id.tv_update_task_validity_period);
        save = findViewById(R.id.btn_update_save);
        machineImagesRecyclerView = findViewById(R.id.rv_update_machine_images);
        addImageView = findViewById(R.id.iv_update_image);
        edtWorkableCrops = findViewById(R.id.edt_update_workable_crops);
        tvTapWorkableCrops = findViewById(R.id.tv_update_tap_workable_crops);
        llWorkableCropsTags = findViewById(R.id.ll_update_workable_crops_tags);
        edtFindJobMachineType = findViewById(R.id.edt_update_machine_type);
        tvTapMachineType = findViewById(R.id.tv_update_tap_machine_type);
        llMachineTypeTags = findViewById(R.id.ll_update_machine_type_tags);
        edtFindJobWorkArea = findViewById(R.id.edt_update_work_area);
        tvTapWorkArea = findViewById(R.id.tv_update_tap_work_area);
        llWorkAreaTags = findViewById(R.id.ll_update_work_area_tags);
        edtGrainTransportVehicle = findViewById(R.id.edt_update_grain_transport_vehicle);
        tvTapGrainTransportVehicle = findViewById(R.id.tv_update_tap_grain_transport_vehicle);
        llGrainTransportVehicleTags = findViewById(R.id.ll_update_grain_transport_vehicle_tags);
    }

    private void setupImagePicker() {
        addImageView.setOnClickListener(v -> {
            if (machineImages.size() < MAX_IMAGES || !hasUserSelectedImage) {
                showImagePickerDialog();
            } else {
                Toast.makeText(this, getString(R.string.find_job_machine_image_limit), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showImagePickerDialog() {
        PopupMenu menu = new PopupMenu(UpdateTaskActivity.this, addImageView);
        menu.inflate(R.menu.menu_image_picker);

        menu.setOnMenuItemClickListener(item -> {
            id = item.getItemId();
            if(id == R.id.menu_camera) {
                if (checkCameraPermission()) {
                    cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE_SECURE));
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA);
                    snackbar = Snackbar.make(findViewById(R.id.find_job_update_snack_bar_container), "需要权限来进行拍照", Snackbar.LENGTH_INDEFINITE).setAnchorView(R.id.find_job_update_snack_bar_container);
                    snackbar.show();
                }
                return true;
            }else if(id == R.id.menu_gallery) {
                if (checkStoragePermission()) {
                    albumLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
                } else {
                    String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                            Manifest.permission.READ_MEDIA_IMAGES :
                            Manifest.permission.READ_EXTERNAL_STORAGE;
                    permissionLauncher.launch(permission);
                    snackbar = Snackbar.make(findViewById(R.id.find_job_update_snack_bar_container), "需要权限来进行相册选择", Snackbar.LENGTH_INDEFINITE).setAnchorView(R.id.find_job_update_snack_bar_container);
                    snackbar.show();
                }
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void initLauncher() {
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            // 获取定位权限
            checkTargetingPermissions();
        });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get(getString(R.string.find_job_data));
                        Uri imageUri = getImageUri(this, imageBitmap);
                        addImageToList(imageUri);
                    }
                });

        albumLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        addImageToList(selectedImageUri);
                    }
                });

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if(isGranted) {
                        if(id == R.id.menu_camera) {
                            snackbar.dismiss();
                            cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
                        }
                        if(id == R.id.menu_gallery) {
                            snackbar.dismiss();
                            albumLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
                        }
                    } else {
                        snackbar.setText(getString(R.string.find_job_image_reject));
                        snackbar.setDuration(1000);
                        snackbar.show();
                        new Handler().postDelayed(() -> {
                            if (snackbar != null) {
                                snackbar.dismiss();
                            }
                        }, 1000);
                    }
                });
    }

    private void addImageToList(Uri imageUri) {
        if (imageUri != null && machineImages.size() < MAX_IMAGES) {
            if (!hasUserSelectedImage) {
                machineImages.clear(); // 清除默认图片（如果有的话）
                hasUserSelectedImage = true;
            }

            JobImageUtils.addImageToList(this, imageUri, machineImages, hasUserSelectedImage,
                    () -> {
                        machineImagesAdapter.notifyDataSetChanged();
                        updateAddImageViewVisibility();
                    });
        }
    }

    private Uri getImageUri(Context context, Bitmap bitmap) {
        return JobImageUtils.getImageUri(context, bitmap);
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void initRecyclerView() {
        machineImagesAdapter = new MachineImagesAdapter(this, machineImages, uri -> {
            machineImages.remove(uri.toString());
            machineImagesAdapter.notifyDataSetChanged();
            updateAddImageViewVisibility();
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        machineImagesRecyclerView.setLayoutManager(layoutManager);
        machineImagesRecyclerView.setAdapter(machineImagesAdapter);
    }

    private void showCustomDateTimePicker() {
        JobDateTimeUtils.showDatePicker(this, validityPeriod);
    }

    private void initSelectors() {
        threadPoolExecutor.execute(() -> {
            while (aMapLocationClient == null || provinceList == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e(getString(R.string.find_job_update_toast), getString(R.string.find_job_location_failure), e);
                }
            }

            runOnUiThread(() -> {
                // 初始化 filterManager
                filterManager = new JobFilterManager(this, sharedPreferences, aMapLocationClient, provinceList);
                filterManager.setupSelectors(
                    edtFindJobMachineType, llMachineTypeTags, tvTapMachineType,
                    edtWorkableCrops, llWorkableCropsTags, tvTapWorkableCrops,
                    edtFindJobWorkArea, llWorkAreaTags, tvTapWorkArea,
                    edtGrainTransportVehicle, llGrainTransportVehicleTags, tvTapGrainTransportVehicle
                );

                // 初始化选择器数据
                filterManager.initSelectorData();
                
                // 等待数据加载完成
                threadPoolExecutor.execute(() -> {
                    while (filterManager.getMachineTypeList().isEmpty() || 
                           filterManager.getCropList().isEmpty()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Log.e(getString(R.string.find_job_update_toast), getString(R.string.find_job_wait_data_failure), e);
                        }
                    }

                    // 从 filterManager 获取数据
                    machineTypeList = filterManager.getMachineTypeList();
                    cropList = filterManager.getCropList();

                    runOnUiThread(() -> {
                        // 如果有需要回显的数据，设置已选择值
                        if (jobDetail != null) {
                            if (jobDetail.getMachineType() != null) {
                                filterManager.getMachineTypeSelector().selectTag(jobDetail.getMachineType());
                            }
                            if (jobDetail.getWorkableCrops() != null) {
                                filterManager.getWorkableCropsSelector().setSelectedTags(Arrays.asList(jobDetail.getWorkableCrops()));
                            }
                            if (jobDetail.getWorkArea() != null) {
                                String[] areas = new String[jobDetail.getWorkArea().length];
                                String addressPrefix = jobDetail.getAddressPrefix();
                                if (addressPrefix != null) {
                                    for (int i = 0; i < jobDetail.getWorkArea().length; i++) {
                                        String[] split = jobDetail.getWorkArea()[i].split(addressPrefix, 2);
                                        areas[i] = split[1];
                                    }
                                    filterManager.getWorkAreaSelector().setSelectedTags(Arrays.asList(areas));
                                    // 设置 EditText 的文本
                                    edtFindJobWorkArea.setText(String.join(" ", areas));
                                }
                            }
                            String hasGrainTransport = jobDetail.isHasGrainTransportVehicle() ? "是" : "否";
                            filterManager.getGrainTransportSelector().selectTag(hasGrainTransport);
                        }
                    });
                });
            });
        });
    }

    private void updateAddImageViewVisibility() {
        addImageView.setVisibility((machineImages.size() < MAX_IMAGES || !hasUserSelectedImage) ? View.VISIBLE : View.GONE);
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
            Log.e(getString(R.string.find_job_update_toast), getString(R.string.find_job_client_create_failure), e);
        }
    }

    private void initProvinceList() {
        // 获取省份数据
        threadPoolExecutor.execute(()-> {
            provinceList = QueryUtils.getLandProvince(getResources().getString(R.string.server_url),
                    getResources().getString(R.string.address_url)+ getResources().getString(R.string.province_url),
                    DefaultInfoUtils.getToken(this));
            while(provinceList == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.i(getString(R.string.find_job_update_toast), getString(R.string.find_job_province_list_failure), e);
                }
            }
        });
    }
}
