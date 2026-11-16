package cn.agrinav.front.activity.findfarm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cn.agrinav.R;
import cn.agrinav.front.activity.findfarm.utils.DateTimeUtils;
import cn.agrinav.front.activity.findfarm.utils.InputValidationUtils;
import cn.agrinav.front.adapter.FindFarmAdapter;
import cn.agrinav.front.entity.FindFarmDetail;
import cn.agrinav.front.entity.NewLandPlot;
import cn.agrinav.front.entity.ObjectTagSelector;
import cn.agrinav.front.entity.TagSelector;
import cn.agrinav.measure.adapter.GsonLocalDateTimeAdapter;
import cn.agrinav.trajectory.entity.vo.CornEntity;
import cn.agrinav.trajectory.entity.vo.WorkTypeEntity;
import cn.agrinav.utils.OkHttpUtils;
import cn.agrinav.utils.Result;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 农场详情活动类，用于展示和选择作物。
 */
public class UpdateFarmActivity extends AppCompatActivity {
    private EditText edtFarmCrop, edtEarthType, edtWorkName, edtEarthNum, edtFarmPhone, edtEarthSelect, edtFarmTime;
    private TextView tvTapOne;
    private TextView tvTapTwo;
    private LinearLayout llLandPlotTags;
    private LinearLayout llCropTags;
    private LinearLayout llOperationTags;
    private TagSelector cropSelector;
    private TagSelector operationSelector;
    private Button btn_find_farm_publish;
    SharedPreferences sharedPreferences;
    private ThreadPoolExecutor threadPool;
    //获取后端所有作物
    private List<CornEntity> cropList = new ArrayList<>();
    //获取作业类型
    private List<WorkTypeEntity> workTypeList = new ArrayList<>();
    //获取所有地块
    private List<NewLandPlot> landPlotList = new ArrayList<>();
    private TextView tvTap;
    private FindFarmDetail editingFarmDetail;
    private ObjectTagSelector<NewLandPlot> landPlotSelector;
    private List<NewLandPlot> selectedLandPlots = new ArrayList<>();
    //标记是否已经选择了地块
    private boolean hasEverSelectedLandPlots = false;
    private InputMethodManager imm;
    // 添加数据加载状态标记
    private volatile boolean isLandPlotsLoaded = false;
    private volatile boolean isCropsLoaded = false;
    private volatile boolean isOperationsLoaded = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_farm);
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        editingFarmDetail = (FindFarmDetail) getIntent().getSerializableExtra("editFarmDetail");
        selectedLandPlots = (List<NewLandPlot>) getIntent().getSerializableExtra("selectedLandPlots");
        if (editingFarmDetail != null && editingFarmDetail.getLandPlots() != null) {
            landPlotList = editingFarmDetail.getLandPlots();
        }

        // 初始化视图组件
        initViews();
        
        // 初始化线程池
        initThreadPool();
        // 初始化选择器
        initSelectors();
        populateTextFields();
        // 加载数据
        initData();
        // 初始化事件监听器
        initEvents();

    }

    private void initThreadPool() {
        threadPool = new ThreadPoolExecutor(
                3,
                5,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    private void populateTextFields() {
        if (editingFarmDetail == null) return;

        // 填充任务名称
        if (editingFarmDetail.getTaskName() != null) {
            edtWorkName.setText(editingFarmDetail.getTaskName());
        }

        // 填充联系方式
        if (editingFarmDetail.getContactMethod() != null) {
            edtFarmPhone.setText(editingFarmDetail.getContactMethod());
        }

        // 填充操作日期
        if (editingFarmDetail.getOperationDate() != null) {
            edtFarmTime.setText(editingFarmDetail.getOperationDate());
        }


        // 填充地块名称
        if (editingFarmDetail.getLandPlots() != null && !editingFarmDetail.getLandPlots().isEmpty()) {
            String displayText = editingFarmDetail.getLandPlots().stream()
                    .map(NewLandPlot::getLandName)
                    .collect(Collectors.joining(" "));
            edtEarthSelect.setText(displayText);
        }
        //填充地块面积
        edtEarthNum.setText(editingFarmDetail.getLandPlotArea());
        if(editingFarmDetail.getLandPlotArea()!=null){
            edtEarthNum.setEnabled(false);
            edtEarthNum.setFocusable(false);
            edtEarthNum.setFocusableInTouchMode(false);
        }

        // 填充作物名称
        if (editingFarmDetail.getCrops() != null && !editingFarmDetail.getCrops().isEmpty()) {
            edtFarmCrop.setText(String.join(" ", editingFarmDetail.getCrops()));
        }

        // 填充作业类型
        if (editingFarmDetail.getOperationTypes() != null && !editingFarmDetail.getOperationTypes().isEmpty()) {
            edtEarthType.setText(String.join(" ", editingFarmDetail.getOperationTypes()));
        }
    }

    /**
     * 发布任务到服务器
     */

    private void saveToServer(FindFarmDetail detail) {
        // 构建请求体JSON
        JSONObject requestBody = new JSONObject();
        try {
            // ID (更新时需要)
            requestBody.put("id", editingFarmDetail.getFindFarmId());

            // 基本信息
            requestBody.put("name", detail.getTaskName());
            requestBody.put("phoneNumber", detail.getContactMethod());
            requestBody.put("area", detail.getLandPlotArea());
            requestBody.put("status", 0);

            // 结束日期
            String dateTimeStr = detail.getOperationDate();
            String workEndTimeStr = dateTimeStr + " 00:00:00";
            Log.i("dateTimeStr", dateTimeStr);

            // 将日期时间字符串转换为LocalDateTime格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime workEndTime = LocalDateTime.parse(workEndTimeStr, formatter);

            requestBody.put("endTime", workEndTime);

            // 地块ID数组
            JSONArray landPlotIdArray = new JSONArray();
            List<NewLandPlot> selectedLandPlots = detail.getLandPlots();
            if (selectedLandPlots != null) {
                for (NewLandPlot landPlot : selectedLandPlots) {
                    landPlotIdArray.put(landPlot.getId());
                }
            }
            requestBody.put("landPlotId", landPlotIdArray);

            // 作物ID数组
            JSONArray cropIdArray = new JSONArray();
            for (String cropName : detail.getCrops()) {
                for (CornEntity crop : cropList) {
                    if (crop.getCropName().equals(cropName)) {
                        cropIdArray.put(crop.getId());
                        break;
                    }
                }
            }
            requestBody.put("cropId", cropIdArray);
            // 作业类型ID数组
            JSONArray workTypeIdArray = new JSONArray();
            for (String operationType : detail.getOperationTypes()) {
                for (WorkTypeEntity workType : workTypeList) {
                    if (workType.getWorkName().equals(operationType)) {
                        workTypeIdArray.put(workType.getId());
                        break;
                    }
                }
            }
            requestBody.put("workTypeId", workTypeIdArray);

            // 其他必要字段
            requestBody.put("orderByCreateTimeDesc", true);
            requestBody.put("myFarmWork", true);

            // 发送PUT请求到后端
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"), requestBody.toString());
            Request request = new OkHttpUtils().createBuilder(getString(R.string.server_url), "/farmWork")
                    .addHeader(getString(R.string.request_header_key_token), sharedPreferences.getString(getString(R.string.user_pref_key_token), " "))
                    .put(body)
                    .build();
            // 处理请求响应
            OkHttpUtils.okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(UpdateFarmActivity.this,
                            "更新失败,请检查网络", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    handlePublishResponse(response);
                }
            });
        } catch (JSONException e) {
            Toast.makeText(this, "数据格式错误", Toast.LENGTH_SHORT).show();
        }
    }


    private void handlePublishResponse(Response response) throws IOException {
        if (response.isSuccessful() && response.body() != null) {
            String responseData = response.body().string();
            try {
                JSONObject jsonObject = new JSONObject(responseData);
                if (jsonObject.getInt("code") == 200) {
                    runOnUiThread(() -> {
                        //发布成功后跳转到FindFarmActivity
                        Intent intent = new Intent(UpdateFarmActivity.this, FindFarmActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("refresh", true); // 添加刷新标志
                        startActivity(intent);
                        Toast.makeText(UpdateFarmActivity.this, "更改成功", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(UpdateFarmActivity.this,
                            "更改失败: " + jsonObject.optString("msg"), Toast.LENGTH_SHORT).show());
                    Log.e("FarmDetailActivity", "更改失败: " + jsonObject.optString("msg"));
                }
            } catch (JSONException e) {
                runOnUiThread(() -> Toast.makeText(UpdateFarmActivity.this,
                        "数据解析错误", Toast.LENGTH_SHORT).show());
            }
        }
    }

    /**
     * 生成发布任务对象
     */

    private FindFarmDetail createFindFarmDetail() {
        FindFarmDetail detail = new FindFarmDetail();

        // 设置任务名
        detail.setTaskName(edtWorkName.getText().toString().trim());

        // 设置地块选择
        List<NewLandPlot> selectedLandPlots = landPlotSelector.getSelectedObjects();
        detail.setLandPlots(selectedLandPlots);

        // 设置地块亩数
        detail.setLandPlotArea(edtEarthNum.getText().toString());

        // 设置作物选择
        List<String> selectedCrops = cropSelector.getSelectedTags();
        for (String crop : selectedCrops) {
            detail.addCrop(crop);
        }

        // 设置作业类型
        List<String> selectedOperations = operationSelector.getSelectedTags();
        for (String operation : selectedOperations) {
            detail.addOperationType(operation);
        }

        // 设置联系方式
        detail.setContactMethod(edtFarmPhone.getText().toString().trim());

        // 设置任务有效期
        detail.setOperationDate(edtFarmTime.getText().toString().trim());

        return detail;
    }


    private void initData() {
        // 使用线程池异步加载数据
        threadPool.execute(() -> loadLandPlots(selectedLandPlots));
        threadPool.execute(this::showCropSelector);
        threadPool.execute(this::showOperationSelector);
    }
    private synchronized void checkAllDataLoaded() {
        if (isLandPlotsLoaded && isCropsLoaded && isOperationsLoaded) {
            runOnUiThread(this::setSelectedData);
        }
    }
    private void setSelectedData() {
        // 设置地块选中状态
        if (selectedLandPlots != null && !selectedLandPlots.isEmpty()) {
            landPlotSelector.setSelected(selectedLandPlots);
            String displayText = selectedLandPlots.stream()
                    .map(NewLandPlot::getLandName)
                    .collect(Collectors.joining(" "));
            edtEarthSelect.setText(displayText);
        }

        // 设置作物选中状态
        if (editingFarmDetail != null && editingFarmDetail.getCrops() != null) {
            cropSelector.setSelectedTags(editingFarmDetail.getCrops());
            edtFarmCrop.setText(String.join(" ", editingFarmDetail.getCrops()));
        }

        // 设置作业类型选中状态
        if (editingFarmDetail != null && editingFarmDetail.getOperationTypes() != null) {
            operationSelector.setSelectedTags(editingFarmDetail.getOperationTypes());
            edtEarthType.setText(String.join(" ", editingFarmDetail.getOperationTypes()));
        }

        // 填充其他数据
        if (editingFarmDetail != null) {
            edtWorkName.setText(editingFarmDetail.getTaskName());
            edtFarmPhone.setText(editingFarmDetail.getContactMethod());
            edtFarmTime.setText(editingFarmDetail.getOperationDate());
            if (editingFarmDetail.getLandPlotArea() != null) {
                edtEarthNum.setText(editingFarmDetail.getLandPlotArea());
            }
        }
    }

    /**
     * 初始化事件监听器
     */
    private void initEvents() {

        tvTap.setOnClickListener(v -> {
            // 收起其他展开的选择器
            operationSelector.collapse();
            cropSelector.collapse();

            hideEditText();
            // 切换当前选择器
            landPlotSelector.toggle();
        });
        tvTapOne.setOnClickListener(v -> {
            // 收起其他展开的选择器
            operationSelector.collapse();
            landPlotSelector.collapse();

            hideEditText();

            // 切换当前选择器
            cropSelector.toggle();

        });

        tvTapTwo.setOnClickListener(v -> {
            // 收起其他展开的选择器
            cropSelector.collapse();
            landPlotSelector.collapse();

            hideEditText();

            // 切换当前选择器
            operationSelector.toggle();

        });
        edtWorkName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                hideAllSelectors();
            }
        });

        edtEarthNum.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                hideAllSelectors();
            }
        });

        edtFarmPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                hideAllSelectors();
            }
        });

        //点击请选择有效期
        edtFarmTime.setOnClickListener(v -> showDateTimePicker());
        //点击发布
        btn_find_farm_publish.setOnClickListener(v -> {
            //先判断是否有效输入
            if (validateInputs()) {
                FindFarmDetail farmDetail = createFindFarmDetail();
                saveToServer(farmDetail);
            }
        });
        // 设置地块选择器的选择变化监听器
        landPlotSelector.setOnSelectionChangedListener(selectedLandPlots -> {
            updateTotalArea(selectedLandPlots,
                    editingFarmDetail != null ? editingFarmDetail.getLandPlotArea() : null);

            // 更新显示文本
            if (selectedLandPlots != null && !selectedLandPlots.isEmpty()) {
                String displayText = selectedLandPlots.stream()
                        .map(NewLandPlot::getLandName)
                        .collect(Collectors.joining(" "));
                edtEarthSelect.setText(displayText);
            } else {
                edtEarthSelect.setText("");
            }
        });

    }

    private void hideEditText() {
        edtWorkName.clearFocus();
        edtFarmPhone.clearFocus();
        edtEarthNum.clearFocus();
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edtWorkName.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        imm.hideSoftInputFromWindow(edtFarmPhone.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void hideAllSelectors() {
        landPlotSelector.collapse();
        cropSelector.collapse();
        operationSelector.collapse();
    }


    /**
     * 初始化选择器
     */
    private void initSelectors() {
        landPlotSelector = new ObjectTagSelector<NewLandPlot>(this,
                llLandPlotTags,
                edtEarthSelect,
                tvTap
        );


        String[] crops = new String[0];
        cropSelector = new TagSelector(this, llCropTags, edtFarmCrop, tvTapOne, crops);

        String[] operations = new String[0];
        operationSelector = new TagSelector(this, llOperationTags, edtEarthType, tvTapTwo, operations);
    }

    /**
     * 更新总地块面积
     */

    private void updateTotalArea(List<NewLandPlot> selectedLandPlots, String currentArea) {
        // 如果选择了地块，标记为已经选择过
        if (selectedLandPlots != null && !selectedLandPlots.isEmpty()) {
            hasEverSelectedLandPlots = true;
        }

        // 如果没有选择地块
        if (selectedLandPlots == null || selectedLandPlots.isEmpty()) {
            edtEarthNum.setEnabled(true);
            edtEarthNum.setFocusable(true);
            edtEarthNum.setFocusableInTouchMode(true);

            if (!hasEverSelectedLandPlots && editingFarmDetail != null
                    && editingFarmDetail.getLandPlotArea() != null
                    && !editingFarmDetail.getLandPlotArea().isEmpty()) {
                // 第一种情况：从未选择过地块，保持原有值
                edtEarthNum.setText(editingFarmDetail.getLandPlotArea());
            } else if (hasEverSelectedLandPlots) {
                // 第二种情况：选择过地块后又取消，显示提示文字
                edtEarthNum.setHint("请输入地块亩数");
                edtEarthNum.setText("");
            }
            return;
        }

        // 计算选中地块的总面积
        float totalArea = 0;
        for (NewLandPlot landPlot : selectedLandPlots) {
            Float area = landPlot.getLandArea();
            if (area != null) {
                totalArea += area;
            }
        }

        // 设置总面积三位小数
        edtEarthNum.setText(String.format("%.3f", totalArea));
        edtEarthNum.setEnabled(false);
        edtEarthNum.setFocusable(false);
        edtEarthNum.setFocusableInTouchMode(false);
    }
    /**
     * 加载地块选择器内容
     */
    private void loadLandPlots(List<NewLandPlot> preSelectedLandPlots) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("current", 1);
        params.put("size", 100);

        OkHttpUtils.okHttpClient
                .newCall(new OkHttpUtils().createBuilder(getString(R.string.server_url), "/landPlot", params)
                        .addHeader(getString(R.string.request_header_key_token),
                                sharedPreferences.getString(getString(R.string.user_pref_key_token), ""))
                        .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(UpdateFarmActivity.this, "获取地块数据失败", Toast.LENGTH_SHORT).show();
                            Log.e("UpdateFarmActivity", "获取地块数据失败", e);
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseData = response.body().string();
                            try {
                                GsonBuilder gsonBuilder = new GsonBuilder();
                                gsonBuilder.registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTimeAdapter());
                                Gson gson = gsonBuilder.create();
                                Result<List<NewLandPlot>> result = gson.fromJson(responseData, new TypeToken<Result<List<NewLandPlot>>>() {
                                }.getType());

                                if (result.getCode() == 200) {
                                    // 保存所有地块数据
                                    landPlotList = result.getData();
                                    // 设置所有地块并标记选中项
                                    runOnUiThread(() -> {
                                        // 先设置所有可选项
                                        landPlotSelector.setOptions(
                                                landPlotList,
                                                landPlot -> landPlot.getLandName()
                                        );
                                    });
                                    isLandPlotsLoaded = true;
                                    checkAllDataLoaded();
                                }
                            } catch (Exception e) {
                                Log.e("UpdateFarmActivity", "解析地块数据失败", e);
                            }
                        }
                    }
                });
    }


    /**
     * 加载作物选择器内容
     */
    private void showCropSelector() {
        OkHttpUtils.okHttpClient
                .newCall(new Request.Builder()
                        .url(getString(R.string.server_url) + "/crop/getCrop")
                        .addHeader(getString(R.string.request_header_key_token), sharedPreferences.getString(getString(R.string.user_pref_key_token), ""))
                        .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(UpdateFarmActivity.this, "获取作物数据失败", Toast.LENGTH_SHORT).show();
                            Log.e("FarmDetailActivity", "获取作物数据失败", e);
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseData = response.body().string();
                            try {
                                Gson gson = new Gson();
                                Result<List<CornEntity>> result = gson.fromJson(responseData, new TypeToken<Result<List<CornEntity>>>() {
                                }.getType());
                                if (result.getCode() == 200) {
                                    cropList = result.getData();
                                    List<String> cropNames = new ArrayList<>();
                                    for (CornEntity crop : cropList) {
                                        cropNames.add(crop.getCropName());
                                    }
                                    runOnUiThread(() -> {
                                        cropSelector.setOptions(cropNames.toArray(new String[0]));
                                    });
                                    isCropsLoaded = true;
                                    checkAllDataLoaded();
                                } else {
                                    runOnUiThread(() -> Toast.makeText(UpdateFarmActivity.this,
                                            "获取作物数据失败", Toast.LENGTH_SHORT).show());
                                }
                            } catch (Exception e) {
                                runOnUiThread(() -> Toast.makeText(UpdateFarmActivity.this,
                                        "解析作物数据失败", Toast.LENGTH_SHORT).show());
                                Log.e("FarmDetailActivity", "解析作物数据失败", e);
                            }
                        }
                    }
                });
    }


    /**
     * 加载作业类型选择器
     */
    private void showOperationSelector() {
        Request request = new OkHttpUtils().createBuilder(getString(R.string.server_url), "/fieldWork/listUnique")
                .addHeader(getString(R.string.request_header_key_token),
                        sharedPreferences.getString(getString(R.string.user_pref_key_token), ""))
                .build();
        OkHttpUtils.okHttpClient
                .newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(UpdateFarmActivity.this, "获取作业类型失败", Toast.LENGTH_SHORT).show();
                            Log.e("FarmDetailActivity", "获取作业类型失败", e);
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseData = response.body().string();
                            try {
                                Gson gson = new Gson();
                                Result<List<WorkTypeEntity>> result = gson.fromJson(responseData, new TypeToken<Result<List<WorkTypeEntity>>>() {
                                }.getType());
                                if (result.getCode() == 200) {
                                    workTypeList = result.getData();
                                    List<String> workTypeNames = new ArrayList<>();
                                    for (WorkTypeEntity workType : workTypeList) {
                                        workTypeNames.add(workType.getWorkName());
                                    }
                                    runOnUiThread(() -> {
                                        operationSelector.setOptions(workTypeNames.toArray(new String[0]));
                                    });
                                     isOperationsLoaded = true;
                                     checkAllDataLoaded();
                                } else {
                                    runOnUiThread(() -> Toast.makeText(UpdateFarmActivity.this,
                                            "获取作业类型失败", Toast.LENGTH_SHORT).show());
                                }
                            } catch (Exception e) {
                                runOnUiThread(() -> Toast.makeText(UpdateFarmActivity.this,
                                        "解析作业类型数据失败", Toast.LENGTH_SHORT).show());
                                Log.e("FarmDetailActivity", "解析作业类型数据失败", e);
                            }
                        }
                    }
                });
    }


    // 添加日期时间选择器方法
    private void showDateTimePicker() {
        DateTimeUtils.showDatePicker(this, edtFarmTime);
        hideEditText();
        hideAllSelectors();
    }

    /**
     * 数据校验
     */
    private boolean validateInputs() {
        String taskName = edtWorkName.getText().toString().trim();
        String area = edtEarthNum.getText().toString().trim();
        String phone = edtFarmPhone.getText().toString().trim();
        String operationDate = edtFarmTime.getText().toString().trim();
        List<String> selectedOperations = operationSelector.getSelectedTags();

        boolean isValid = InputValidationUtils.validateFarmTaskInputs(
                this, taskName, area, phone, operationDate, selectedOperations);

        if (!isValid && taskName.isEmpty()) {
            edtWorkName.requestFocus();
        } else if (!isValid && area.isEmpty()) {
            edtEarthNum.requestFocus();
        } else if (!isValid && phone.isEmpty()) {
            edtFarmPhone.requestFocus();
        }

        return isValid;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
            }
        }
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        edtWorkName = findViewById(R.id.edt_work_name_update);
        edtEarthSelect = findViewById(R.id.edt_earth_select_update);
        edtEarthNum = findViewById(R.id.edt_earth_num_update);
        edtFarmCrop = findViewById(R.id.edt_farm_crop_update);
        edtEarthType = findViewById(R.id.edt_earth_type_update);
        edtFarmPhone = findViewById(R.id.edt_farm_phone_update);
        edtFarmTime = findViewById(R.id.edt_farm_time_update);
        tvTap = findViewById(R.id.tv_tap_update);
        llLandPlotTags = findViewById(R.id.ll_land_plot_tags_update);
        tvTapOne = findViewById(R.id.tv_tap_one_update);
        llCropTags = findViewById(R.id.ll_crop_tags_update);
        tvTapTwo = findViewById(R.id.tv_tap_two_update);
        llOperationTags = findViewById(R.id.ll_operation_tags_update);
        btn_find_farm_publish = findViewById(R.id.btn_find_farm_publish_update);
    }
}
