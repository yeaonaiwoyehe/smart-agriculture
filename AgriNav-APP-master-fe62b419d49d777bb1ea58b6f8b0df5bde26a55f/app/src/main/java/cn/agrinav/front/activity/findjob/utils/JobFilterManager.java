package cn.agrinav.front.activity.findjob.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.amap.api.location.AMapLocationClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.agrinav.R;
import cn.agrinav.front.entity.AgriculturalMachineryType;
import cn.agrinav.front.entity.TagSelector;
import cn.agrinav.measure.entity.LandAddress;
import cn.agrinav.measure.utils.QueryUtils;
import cn.agrinav.trajectory.entity.vo.CornEntity;
import cn.agrinav.utils.OkHttpUtils;
import cn.agrinav.utils.Result;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class JobFilterManager {
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final AMapLocationClient aMapLocationClient;
    private final List<LandAddress> provinceList;

    // 选择器相关视图
    private EditText edtMachineType;
    private LinearLayout llMachineTypeTags;
    private TextView tvMachineTypeTap;

    private EditText edtWorkableCrops;
    private LinearLayout llWorkableCropsTags;
    private TextView tvWorkableCropsTap;

    private EditText edtWorkArea;
    private LinearLayout llWorkAreaTags;
    private TextView tvWorkAreaTap;

    private EditText edtGrainTransport;
    private LinearLayout llGrainTransportTags;
    private TextView tvGrainTransportTap;

    // 选择器
    private TagSelector machineTypeSelector;
    private TagSelector cropSelector;
    private TagSelector workAreaSelector;
    private TagSelector grainTransportSelector;

    // 数据列表
    private List<AgriculturalMachineryType> machineTypeList = new ArrayList<>();
    private List<CornEntity> cropList = new ArrayList<>();
    private List<String> workAreaOptions = new ArrayList<>();
    private List<LandAddress> cityList;
    private List<LandAddress> districtList;
    private StringBuilder location = new StringBuilder();
    private final ThreadPoolExecutor threadPoolExecutor;

    public JobFilterManager(Context context, SharedPreferences sharedPreferences, 
            AMapLocationClient aMapLocationClient, List<LandAddress> provinceList) {
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        this.aMapLocationClient = aMapLocationClient;
        this.provinceList = provinceList;
        this.threadPoolExecutor = new ThreadPoolExecutor(
                10, 20, 10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public void setupSelectors(
            EditText edtMachineType, LinearLayout llMachineTypeTags, TextView tvMachineTypeTap,
            EditText edtWorkableCrops, LinearLayout llWorkableCropsTags, TextView tvWorkableCropsTap,
            EditText edtWorkArea, LinearLayout llWorkAreaTags, TextView tvWorkAreaTap,
            EditText edtGrainTransport, LinearLayout llGrainTransportTags, TextView tvGrainTransportTap) {

        // 保存视图引用
        this.edtMachineType = edtMachineType;
        this.llMachineTypeTags = llMachineTypeTags;
        this.tvMachineTypeTap = tvMachineTypeTap;

        this.edtWorkableCrops = edtWorkableCrops;
        this.llWorkableCropsTags = llWorkableCropsTags;
        this.tvWorkableCropsTap = tvWorkableCropsTap;

        this.edtWorkArea = edtWorkArea;
        this.llWorkAreaTags = llWorkAreaTags;
        this.tvWorkAreaTap = tvWorkAreaTap;

        this.edtGrainTransport = edtGrainTransport;
        this.llGrainTransportTags = llGrainTransportTags;
        this.tvGrainTransportTap = tvGrainTransportTap;

        // 初始化选择器
        initSelectors();
    }

    public void initSelectors() {
        // 农机类型选择器
        machineTypeSelector = new TagSelector(context, llMachineTypeTags,
            edtMachineType, tvMachineTypeTap, new String[0]);
        machineTypeSelector.setSingleSelection(true);
        machineTypeSelector.setOnTagSelectedListener(tags -> {
            if (!tags.isEmpty()) {
                edtMachineType.setError(null);
            }
        });

        // 作物选择器
        cropSelector = new TagSelector(context, llWorkableCropsTags,
            edtWorkableCrops, tvWorkableCropsTap, new String[0]);
        cropSelector.setOnTagSelectedListener(tags -> {
            if (!tags.isEmpty()) {
                edtWorkableCrops.setError(null);
            }
        });

        // 作业区域选择器
        workAreaSelector = new TagSelector(context, llWorkAreaTags,
            edtWorkArea, tvWorkAreaTap, new String[0]);
        workAreaSelector.setOnTagSelectedListener(tags -> {
            if (!tags.isEmpty()) {
                edtWorkArea.setError(null);
            }
        });

        // 运粮车选择器
        String[] transportOptions = new String[]{"是", "否"};
        grainTransportSelector = new TagSelector(context, llGrainTransportTags,
            edtGrainTransport, tvGrainTransportTap, transportOptions);
        grainTransportSelector.setSingleSelection(true);
        grainTransportSelector.setOnTagSelectedListener(tags -> {
            if (!tags.isEmpty()) {
                edtGrainTransport.setError(null);
            }
        });
    }

    public void initSelectorData() {
        loadMachineTypes();
        loadCropTypes();
    }

    // 添加作业区域选择器显示方法
    public void loadWorkArea() {
        // 清空之前的选项
        workAreaOptions.clear();
        // 从后端获取作业区域
        getLocationAndWorkArea();

        // 使用Handler在主线程更新UI
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!workAreaOptions.isEmpty()) {
                String[] workAreas = workAreaOptions.toArray(new String[0]);
                workAreaSelector.setOptions(workAreas);
                llWorkAreaTags.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(context, context.getString(R.string.find_job_location_null), Toast.LENGTH_SHORT).show();
            }
        }, 500);
    }

    public void getLocationAndWorkArea() {
        if (aMapLocationClient != null && aMapLocationClient.getLastKnownLocation() != null) {
            // 清空之前的location内容，避免重复拼接
            location.setLength(0);

            for (LandAddress landAddress : provinceList) {
                if (landAddress.getName().equals(aMapLocationClient.getLastKnownLocation().getProvince())) {
                    location.append(landAddress.getName());
                    threadPoolExecutor.execute(() -> {
                        cityList = QueryUtils.getLandAddresses(
                                context.getString(R.string.server_url),
                                context.getString(R.string.address_url) + context.getString(R.string.city_url),
                                sharedPreferences.getString(context.getString(R.string.user_pref_key_token), ""),
                                landAddress);
                        if(cityList != null && !cityList.isEmpty()) {
                            cityList.forEach(landAddressCity -> {
                                if (landAddressCity.getName().equals(aMapLocationClient.getLastKnownLocation().getCity())) {
                                    location.append(landAddressCity.getName());
                                    //获取区县的信息
                                    districtList = QueryUtils.getLandAddresses(
                                            context.getString(R.string.server_url),
                                            context.getString(R.string.address_url) + context.getString(R.string.district_url),
                                            sharedPreferences.getString(context.getString(R.string.user_pref_key_token), ""),
                                            landAddressCity);
                                    districtList.forEach(landAddressDistrict -> workAreaOptions.add(landAddressDistrict.getName()));
                                }
                            });
                        }
                    });
                    break;
                }
            }
        }
    }

    public void loadMachineTypes() {
        OkHttpUtils.okHttpClient
                .newCall(new Request.Builder()
                        .url(context.getString(R.string.server_url) + context.getString(R.string.find_job_request_machine_type))
                        .addHeader(context.getString(R.string.request_header_key_token),
                            sharedPreferences.getString(context.getString(R.string.user_pref_key_token), ""))
                        .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        showToast(context.getString(R.string.find_job_machine_type_failure));
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseData = response.body().string();
                            try {
                                Gson gson = new Gson();
                                Result<List<AgriculturalMachineryType>> result = gson.fromJson(responseData,
                                    new TypeToken<Result<List<AgriculturalMachineryType>>>() {}.getType());
                                if(result.getCode() == 200){
                                    machineTypeList = result.getData();
                                    List<String> typeNames = new ArrayList<>();
                                    for (AgriculturalMachineryType type : machineTypeList) {
                                        typeNames.add(type.getName());
                                    }
                                    ((Activity)context).runOnUiThread(() -> {
                                        machineTypeSelector.setOptions(typeNames.toArray(new String[0]));
                                    });
                                }
                            } catch (Exception e) {
                                showToast(context.getString(R.string.find_job_machine_type_parse_failure));
                            }
                        }
                    }
                });
    }

    public void loadCropTypes() {
        OkHttpUtils.okHttpClient
                .newCall(new Request.Builder()
                        .url(context.getString(R.string.server_url) + context.getString(R.string.find_job_request_crop))
                        .addHeader(context.getString(R.string.request_header_key_token),
                            sharedPreferences.getString(context.getString(R.string.user_pref_key_token), ""))
                        .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        showToast(context.getString(R.string.find_job_crop_failure));
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseData = response.body().string();
                            try {
                                Gson gson = new Gson();
                                Result<List<CornEntity>> result = gson.fromJson(responseData,
                                    new TypeToken<Result<List<CornEntity>>>() {}.getType());
                                if(result.getCode() == 200){
                                    cropList = result.getData();
                                    List<String> cropNames = new ArrayList<>();
                                    for (CornEntity crop : cropList) {
                                        cropNames.add(crop.getCropName());
                                    }
                                    ((Activity)context).runOnUiThread(() -> {
                                        cropSelector.setOptions(cropNames.toArray(new String[0]));
                                    });
                                }
                            } catch (Exception e) {
                                showToast(context.getString(R.string.find_job_crop_parse_failure));
                            }
                        }
                    }
                });
    }

    public void hideAllSelectors() {
        if (machineTypeSelector != null) machineTypeSelector.collapse();
        if (cropSelector != null) cropSelector.collapse();
        if (workAreaSelector != null) workAreaSelector.collapse();
        if (grainTransportSelector != null) grainTransportSelector.collapse();
    }

    public void showMachineTypeSelector() {
        machineTypeSelector.toggle();
    }

    public void showCropSelector() {
        cropSelector.toggle();
    }

    public void showWorkAreaSelector() {
        loadWorkArea();
        workAreaSelector.toggle();
    }

    public void showGrainTransportSelector() {
        grainTransportSelector.toggle();
    }

    public void resetAllSelectors() {
        machineTypeSelector.clearSelection();
        cropSelector.clearSelection();
        workAreaSelector.clearSelection();
        grainTransportSelector.clearSelection();
    }

    private void showToast(String message) {
        ((Activity)context).runOnUiThread(() -> 
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    // 获取选中的数据方法
    public String getSelectedMachineTypeIds() {
        List<String> ids = new ArrayList<>();
        if (machineTypeSelector != null && machineTypeList != null) {
            for (String typeName : machineTypeSelector.getSelectedTags()) {
                for (AgriculturalMachineryType type : machineTypeList) {
                    if (type.getName().equals(typeName)) {
                        ids.add(type.getId());
                        break;
                    }
                }
            }
        }
        return ids.get(0);
    }

    public List<Long> getSelectedCropIds() {
        List<Long> ids = new ArrayList<>();
        if (cropSelector != null && cropList != null) {
            for (String cropName : cropSelector.getSelectedTags()) {
                for (CornEntity crop : cropList) {
                    if (crop.getCropName().equals(cropName)) {
                        ids.add(crop.getId());
                        break;
                    }
                }
            }
        }
        return ids;
    }

    public List<String> getSelectedWorkAreas() {
        return workAreaSelector != null ? workAreaSelector.getSelectedTags() : new ArrayList<>();
    }

    /**
     * 获取选中的农机类型名称列表
     */
    public String getSelectedMachineTypes() {
        return machineTypeSelector != null ? machineTypeSelector.getSelectedTags().get(0) : "";
    }

    /**
     * 获取选中的作物名称列表
     */
    public List<String> getSelectedCrops() {
        return cropSelector != null ? cropSelector.getSelectedTags() : new ArrayList<>();
    }

    /**
     * 获取运粮车选择状态
     */
    public Boolean getGrainTransportSelection() {
        if (grainTransportSelector != null) {
            List<String> selected = grainTransportSelector.getSelectedTags();
            if (!selected.isEmpty()) {
                return selected.get(0).equals("是");
            }
        }
        return null;
    }

    public String getLocation() {
        return location.toString();
    }

    // 添加getter方法获取各个选择器
    public TagSelector getMachineTypeSelector() {
        return machineTypeSelector;
    }
    
    public TagSelector getWorkableCropsSelector() {
        return cropSelector;
    }
    
    public TagSelector getWorkAreaSelector() {
        return workAreaSelector;
    }
    
    public TagSelector getGrainTransportSelector() {
        return grainTransportSelector;
    }

    // 添加获取农机类型列表的getter方法
    public List<AgriculturalMachineryType> getMachineTypeList() {
        return machineTypeList;
    }

    // 添加获取作物列表的getter方法
    public List<CornEntity> getCropList() {
        return cropList;
    }

    /**
     * 恢复农机类型选择
     */
    public void restoreMachineTypeSelection(String machineTypeId) {
        if (machineTypeId == null || machineTypeId.isEmpty() || machineTypeSelector == null) {
            edtMachineType.setText("");
            llMachineTypeTags.removeAllViews();
            return;
        }

        for (AgriculturalMachineryType type : machineTypeList) {
            if (type.getId().equals(machineTypeId)) {
                machineTypeSelector.clearSelection();
                machineTypeSelector.selectTag(type.getName());
                break;
            }
        }
    }

    /**
     * 恢复作物选择
     */
    public void restoreCropSelection(JSONArray cropIds) {
        if (cropIds == null || cropIds.length() == 0 || cropSelector == null) {
            edtWorkableCrops.setText("");
            llWorkableCropsTags.removeAllViews();
            return;
        }

        cropSelector.clearSelection();
        try {
            for (int i = 0; i < cropIds.length(); i++) {
                Long cropId = cropIds.getLong(i);
                for (CornEntity crop : cropList) {
                    if (crop.getId().equals(cropId)) {
                        cropSelector.selectTag(crop.getCropName());
                        break;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 恢复作业区域选择
     */
    public void restoreWorkAreaSelection(JSONArray locations) {
        if (locations == null || locations.length() == 0 || workAreaSelector == null) {
            edtWorkArea.setText("");
            llWorkAreaTags.removeAllViews();
            return;
        }

        workAreaSelector.clearSelection();
        try {
            for (int i = 0; i < locations.length(); i++) {
                String location = locations.getString(i);
                workAreaSelector.selectTag(location);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 恢复运粮车选择
     */
    public void restoreGrainTransportSelection(boolean hasGrainTransport) {
        if (grainTransportSelector == null) {
            edtGrainTransport.setText("");
            llGrainTransportTags.removeAllViews();
            return;
        }

        grainTransportSelector.clearSelection();
        grainTransportSelector.selectTag(hasGrainTransport ? "是" : "否");
    }
} 