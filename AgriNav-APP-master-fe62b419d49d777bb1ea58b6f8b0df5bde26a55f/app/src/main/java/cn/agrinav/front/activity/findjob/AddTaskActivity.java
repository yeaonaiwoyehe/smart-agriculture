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
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.io.File;

import cn.agrinav.R;
import cn.agrinav.front.activity.findjob.utils.JobFilterManager;
import cn.agrinav.front.adapter.MachineImagesAdapter;
import cn.agrinav.front.entity.AgriculturalMachineryType;
import cn.agrinav.front.entity.FindJobDetail;
import cn.agrinav.front.entity.TagSelector;
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

import cn.agrinav.front.activity.findjob.utils.TaskInputValidationUtils;
import cn.agrinav.front.activity.findjob.utils.JobUIUtils;
import cn.agrinav.front.activity.findjob.utils.JobDateTimeUtils;
import cn.agrinav.front.activity.findjob.utils.JobImageUtils;

public class AddTaskActivity extends AppCompatActivity {

    private EditText taskName;
    private EditText phone;
    private TextView validityPeriod;
    private Button publish;
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
    private TagSelector workableCropsSelector;
    private ActivityResultLauncher launcher;
    private EditText edtFindJobMachineType;
    private TextView tvTapMachineType;
    private LinearLayout llMachineTypeTags;
    private TagSelector machineTypeSelector;
    private EditText edtFindJobWorkArea;
    private TextView tvTapWorkArea;
    private LinearLayout llWorkAreaTags;
    private TagSelector workAreaSelector;
    private EditText edtGrainTransportVehicle;
    private TextView tvTapGrainTransportVehicle;
    private LinearLayout llGrainTransportVehicleTags;
    private TagSelector grainTransportVehicleSelector;

    private Snackbar snackbar;
    private int id;
    private JobFilterManager filterManager;

    public AddTaskActivity() {
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
        setContentView(R.layout.activity_add_task);
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->{
            // 获取定位权限
            checkTargetingPermissions();
        });
        initViews();
        initData();
        initSelectors();
        initEvents();
        setupImagePicker();
        initLauncher();
        initRecyclerView();
        checkTargetingPermissions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //获取省份数据
        threadPoolExecutor.execute(()-> {
            provinceList = QueryUtils.getLandProvince(getResources().getString(R.string.server_url),
                    getResources().getString(R.string.address_url)+ getResources().getString(R.string.province_url),
                    DefaultInfoUtils.getToken(this));
            while(provinceList==null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.i(getString(R.string.find_job_add_toast), getString(R.string.find_job_province_list_failure), e);
                }
            }
        });
    }

    private void initData() {
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        // 自动填充手机号
        String phoneNumber = sharedPreferences.getString(getString(R.string.user_pref_key_phone), "");
        this.phone.setText(phoneNumber);
    }

    @SuppressLint("SimpleDateFormat")
    private void initEvents() {
        validityPeriod.setOnClickListener(v -> {
            hideAllSelectors();
            JobDateTimeUtils.showDatePicker(this, validityPeriod);
        });
        publish.setOnClickListener(v -> {
            if (validateInputs()) {
                FindJobDetail task = createFindJobDetail();
                saveToServer(task);
            }
        });
        tvTapWorkableCrops.setOnClickListener(v -> {
            hideEditText();
            if (llWorkableCropsTags.getVisibility() == View.VISIBLE) {
                filterManager.hideAllSelectors();
                return;
            }
            filterManager.hideAllSelectors();
            filterManager.showCropSelector();
        });
        tvTapMachineType.setOnClickListener(v -> {
            hideEditText();
            if (llMachineTypeTags.getVisibility() == View.VISIBLE) {
                filterManager.hideAllSelectors();
                return;
            }
            filterManager.hideAllSelectors();
            filterManager.showMachineTypeSelector();
        });
        tvTapWorkArea.setOnClickListener(v -> {
            hideEditText();
            if (llWorkAreaTags.getVisibility() == View.VISIBLE) {
                filterManager.hideAllSelectors();
                return;
            }
            filterManager.hideAllSelectors();
            filterManager.showWorkAreaSelector();
        });
        tvTapGrainTransportVehicle.setOnClickListener(v -> {
            hideEditText();
            if (llGrainTransportVehicleTags.getVisibility() == View.VISIBLE) {
                filterManager.hideAllSelectors();
                return;
            }
            filterManager.hideAllSelectors();
            filterManager.showGrainTransportSelector();
        });
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

        // 修改手机号输入监听
        phone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().trim().isEmpty()) {
                    phone.setError(null);
                }
            }
        });
    }

    private void hideEditText() {
        JobUIUtils.hideKeyboardAndClearFocus(this, taskName, phone);
    }

    private void hideAllSelectors() {
        if (filterManager != null) {
            filterManager.hideAllSelectors();
        } else {
            // 如果filterManager还没初始化，手动检查每个selector
            if (machineTypeSelector != null) {
                machineTypeSelector.collapse();
            }
            if (workableCropsSelector != null) {
                workableCropsSelector.collapse();
            }
            if (workAreaSelector != null) {
                workAreaSelector.collapse();
            }
            if (grainTransportVehicleSelector != null) {
                grainTransportVehicleSelector.collapse();
            }
        }
    }

    private void saveToServer(FindJobDetail task) {
        try {
            // 从filterManager获取最新的列表数据
            List<AgriculturalMachineryType> currentMachineTypes = filterManager.getMachineTypeList();
            List<CornEntity> currentCrops = filterManager.getCropList();

            if (currentMachineTypes == null || currentMachineTypes.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.find_job_wait_data), Toast.LENGTH_SHORT).show());
                return;
            }

            if (currentCrops == null || currentCrops.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.find_job_wait_data), Toast.LENGTH_SHORT).show());
                return;
            }

            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            JSONObject agriculturalMachinery = new JSONObject();

            // 使用从filterManager获取的列表查找ID
            String typeId = "";
            for (AgriculturalMachineryType machineType : currentMachineTypes) {
                if (machineType.getName().equals(task.getMachineType())) {
                    typeId = machineType.getId();
                    break;
                }
            }
            agriculturalMachinery.put(getString(R.string.find_job_type_id), typeId);
            agriculturalMachinery.put(getString(R.string.find_job_name), task.getName());

            // 使用从filterManager获取的作物列表查找ID
            JSONArray cropIdArray = new JSONArray();
            for (String cropName : task.getWorkableCrops()) {
                for (CornEntity crop : currentCrops) {
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

            // 作业区域
            JSONArray workAreaArray = new JSONArray();
            for (String area : task.getWorkArea()) {
                StringBuilder l = new StringBuilder();
                l.append(task.getAddressPrefix());
                l.append(area);
                workAreaArray.put(l.toString());
            }
            agriculturalMachinery.put(getString(R.string.find_job_location), workAreaArray);

            // 联系方式
            agriculturalMachinery.put(getString(R.string.find_job_phone_number), task.getPhone());

            // 有效期
            agriculturalMachinery.put(getString(R.string.find_job_end_time),
                JobDateTimeUtils.formatDateForServer(task.getTaskValidityPeriod()));

            // 添加JSON数据到form-data
            builder.addFormDataPart(getString(R.string.find_job_agricultural_machinery), agriculturalMachinery.toString());

            // 添加片到form-data
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
                    .url(getString(R.string.server_url) + getString(R.string.find_job_request_add))
                    .addHeader(getString(R.string.request_header_key_token),
                            sharedPreferences.getString(getString(R.string.user_pref_key_token), ""))
                    .post(builder.build())
                    .build();

            // 发送请求
            OkHttpUtils.okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddTaskActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(AddTaskActivity.this, getString(R.string.find_job_add_success), Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(AddTaskActivity.this, FindJobActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                    // 使用 JobImageUtils 删除图片
                                    String path = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + getString(R.string.find_job_image_path);
                                    JobImageUtils.deleteImages(path);
                                    finish();
                                });
                            }else {
                                runOnUiThread(() -> Toast.makeText(AddTaskActivity.this, jsonObject.optString(getString(R.string.find_job_msg)), Toast.LENGTH_SHORT).show());
                            }
                        }catch (Exception e) {
                            Log.e(getString(R.string.find_job_add_toast), getString(R.string.find_job_response_failure), e);
                        }
                    }
                }

            });

        } catch (JSONException e) {
            Log.e(getString(R.string.find_job_add_toast), getString(R.string.find_job_json_failure), e);
        }
    }

    private boolean validateInputs() {
        // 确保filterManager已初始化
        if (filterManager == null) {
            Toast.makeText(this, getString(R.string.find_job_wait_data), Toast.LENGTH_SHORT).show();
            return false;
        }

        // 使用filterManager获取选择器
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

        // 设置任务名称
        String taskNameText = taskName.getText().toString().trim();
        task.setName(taskNameText.isEmpty() ?
            JobUIUtils.generateDefaultTaskName(this) : taskNameText);

        // 使用JobFilterManager获取选中的值
        task.setMachineType(filterManager.getSelectedMachineTypes());
        task.setWorkableCrops(filterManager.getSelectedCrops().toArray(new String[0]));
        task.setHasGrainTransportVehicle(filterManager.getGrainTransportSelection());
        task.setPhone(phone.getText().toString());

        // 使用新的日期工具类处理日期
        String validityDateStr = validityPeriod.getText().toString();
        Date validityDate = JobDateTimeUtils.parseDate(validityDateStr);
        if (validityDate != null) {
            task.setTaskValidityPeriod(validityDate);
        }

        task.setTaskCreateTime(new Date());
        task.setTaskStatus(0);
        task.setWorkArea(filterManager.getSelectedWorkAreas().toArray(new String[0]));
        task.setImage(machineImages.toArray(new String[0]));
        task.setAddressPrefix(filterManager.getLocation());

        return task;
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
        //如果回调方法中的内容状态码就执行以下逻辑
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
            Log.e(getString(R.string.find_job_add_toast), getString(R.string.find_job_client_create_failure));
        }
        AMapLocationClientOption aMapLocationClientOption = new AMapLocationClientOption();
        //置定位模式为高精度模
        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置北斗定优先
        aMapLocationClientOption.setBeidouFirst(true);
        aMapLocationClientOption.getGeoLanguage();
        //设置wifi自动刷
        aMapLocationClientOption.setWifiScan(true);
        //设置开启wifi终扫描
        AMapLocationClientOption.setOpenAlwaysScanWifi(true);
        aMapLocationClientOption.setInterval(2000);
        //配置项加载到客户端中
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
        taskName = findViewById(R.id.edt_find_job_task_name);
        phone = findViewById(R.id.edt_find_job_phone);
        validityPeriod = findViewById(R.id.tv_find_job_task_validity_period);
        publish = findViewById(R.id.btn_find_job_publish);
        machineImagesRecyclerView = findViewById(R.id.rv_machine_images);
        addImageView = findViewById(R.id.iv_add_image);
        edtWorkableCrops = findViewById(R.id.edt_workable_crops);
        tvTapWorkableCrops = findViewById(R.id.tv_tap_workable_crops);
        llWorkableCropsTags = findViewById(R.id.ll_workable_crops_tags);
        edtFindJobMachineType = findViewById(R.id.edt_find_job_machine_type);
        tvTapMachineType = findViewById(R.id.tv_tap_machine_type);
        llMachineTypeTags = findViewById(R.id.ll_machine_type_tags);
        edtFindJobWorkArea = findViewById(R.id.edt_find_job_work_area);
        tvTapWorkArea = findViewById(R.id.tv_tap_work_area);
        llWorkAreaTags = findViewById(R.id.ll_work_area_tags);
        edtGrainTransportVehicle = findViewById(R.id.edt_grain_transport_vehicle);
        tvTapGrainTransportVehicle = findViewById(R.id.tv_tap_grain_transport_vehicle);
        llGrainTransportVehicleTags = findViewById(R.id.ll_grain_transport_vehicle_tags);
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
        PopupMenu menu = new PopupMenu(AddTaskActivity.this, addImageView);
        menu.inflate(R.menu.menu_image_picker);

        menu.setOnMenuItemClickListener(item -> {
            id = item.getItemId();
            if(id == R.id.menu_camera) {
                if (checkCameraPermission()) {
                    cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE_SECURE));
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA);
                    snackbar = Snackbar.make(findViewById(R.id.find_job_snack_bar_container), getString(R.string.find_job_image_permission_camera), Snackbar.LENGTH_INDEFINITE).setAnchorView(R.id.find_job_snack_bar_container);
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
                    snackbar = Snackbar.make(findViewById(R.id.find_job_snack_bar_container), getString(R.string.find_job_image_permission_gallery), Snackbar.LENGTH_INDEFINITE).setAnchorView(R.id.find_job_snack_bar_container);
                    snackbar.show();
                }
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void initLauncher() {
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
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri selectedImageUri = data.getData();
                            addImageToList(selectedImageUri);
                        }
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

            JobImageUtils.addImageToList(this, imageUri, machineImages, hasUserSelectedImage, () -> {
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
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES :
                Manifest.permission.READ_EXTERNAL_STORAGE;
        return ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void initRecyclerView() {
        machineImagesAdapter = new MachineImagesAdapter(this, machineImages, uri -> {
            // 从列表中移除
            machineImages.remove(uri.toString());
            // 如果没有图片，重置状态
            if (machineImages.isEmpty()) {
                hasUserSelectedImage = false;
            }
            machineImagesAdapter.notifyDataSetChanged();
            updateAddImageViewVisibility();
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        machineImagesRecyclerView.setLayoutManager(layoutManager);
        machineImagesRecyclerView.setAdapter(machineImagesAdapter);
    }

    private void initSelectors() {
        threadPoolExecutor.execute(() -> {
            while (aMapLocationClient == null || provinceList == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e(getString(R.string.find_job_add_toast), getString(R.string.find_job_location_failure), e);
                }
            }

            runOnUiThread(() -> {
                filterManager = new JobFilterManager(this, sharedPreferences, aMapLocationClient, provinceList);
                filterManager.setupSelectors(
                    edtFindJobMachineType, llMachineTypeTags, tvTapMachineType,
                    edtWorkableCrops, llWorkableCropsTags, tvTapWorkableCrops,
                    edtFindJobWorkArea, llWorkAreaTags, tvTapWorkArea,
                    edtGrainTransportVehicle, llGrainTransportVehicleTags, tvTapGrainTransportVehicle
                );

                // 添加初始化完成的日志
                filterManager.initSelectorData();
            });
        });
    }

    private void updateAddImageViewVisibility() {
        JobUIUtils.setViewVisibility(
            (machineImages.size() < MAX_IMAGES || !hasUserSelectedImage) ?
            View.VISIBLE : View.GONE,
            addImageView
        );
    }
}