package cn.agrinav.front.activity.findfarm.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.agrinav.R;
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
import okhttp3.Request;
import okhttp3.Response;

public class FilterManager {
    // 网络请求和数据持久化
    private final Context context;
    private final SharedPreferences sharedPreferences;
    // 选择器
    private ObjectTagSelector landPlotSelector;
    private TagSelector cropSelector;
    private TagSelector operationSelector;
    // 数据列表
    private List<CornEntity> cropList = new ArrayList<>();
    private List<WorkTypeEntity> workTypeList = new ArrayList<>();
    private List<NewLandPlot> landPlotList = new ArrayList<>();

    /**
     * 构造函数
     *
     * @param context           上下文
     * @param sharedPreferences SharedPreferences实例
     */
    public FilterManager(Context context, SharedPreferences sharedPreferences) {
        this.context = context;
        this.sharedPreferences = sharedPreferences;
    }

    /**
     * 设置选择器实例
     *
     * @param landPlotSelector  地块选择器
     * @param cropSelector      作物选择器
     * @param operationSelector 作业类型选择器
     */
    public void setSelectors(ObjectTagSelector landPlotSelector, TagSelector cropSelector, TagSelector operationSelector) {
        this.landPlotSelector = landPlotSelector;
        this.cropSelector = cropSelector;
        this.operationSelector = operationSelector;
    }

    /**
     * 隐藏所有选择器
     */
    public void hideAllSelectors() {
        if (landPlotSelector != null) {
            landPlotSelector.collapse();
        }
        if (cropSelector != null) {
            cropSelector.collapse();
        }
        if (operationSelector != null) {
            operationSelector.collapse();
        }
    }

    /**
     * 显示地块选择器并加载数据
     * 从服务器获取地块数据并更新选择器
     */
    public void showLandPlotsSelector() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("current", 1);
        params.put("size", 100);
        OkHttpUtils.okHttpClient
                .newCall(new OkHttpUtils().createBuilder(
                                context.getString(R.string.server_url),
                                "/landPlot",
                                params)
                        .addHeader(context.getString(R.string.request_header_key_token),
                                sharedPreferences.getString(context.getString(R.string.user_pref_key_token), ""))
                        .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        showToast("获取地块数据失败");
                        Log.e("FilterManager", "获取地块数据失败", e);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        handleLandPlotsResponse(response);
                    }
                });
    }

    /**
     * 显示作物选择器并加载数据
     * 从服务器获取作物数据并更新选择器
     */
    public void showCropSelector() {
        OkHttpUtils.okHttpClient
                .newCall(new OkHttpUtils().createBuilder(
                                context.getString(R.string.server_url),
                                "/crop/getCrop")
                        .addHeader(context.getString(R.string.request_header_key_token),
                                sharedPreferences.getString(context.getString(R.string.user_pref_key_token), ""))
                        .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        showToast("获取作物数据失败");
                        Log.e("FilterManager", "获取作物数据失败", e);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        handleCropsResponse(response);
                    }
                });
    }

    /**
     * 显示作业类型选择器并加载数据
     * 从服务器获取作业类型数据并更新选择器
     */
    public void showOperationSelector() {

        OkHttpUtils.okHttpClient
                .newCall(new OkHttpUtils().createBuilder(
                                context.getString(R.string.server_url),
                                "/fieldWork/listUnique")
                        .addHeader(context.getString(R.string.request_header_key_token),
                                sharedPreferences.getString(context.getString(R.string.user_pref_key_token), ""))
                        .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        showToast("获取作业类型失败");
                        Log.e("FilterManager", "获取作业类型失败", e);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        handleOperationsResponse(response);
                    }
                });
    }

    /**
     * 处理地块数据响应
     *
     * @param response 服务器响应
     */
    private void handleLandPlotsResponse(Response response) throws IOException {
        if (response.isSuccessful() && response.body() != null) {
            String responseData = response.body().string();
            try {
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTimeAdapter());
                Gson gson = gsonBuilder.create();
                Result<List<NewLandPlot>> result = gson.fromJson(responseData,
                        new TypeToken<Result<List<NewLandPlot>>>() {
                        }.getType());

                if (result.getCode() == 200) {
                    landPlotList = result.getData();
                    updateLandPlotSelector();
                } else {
                    showToast("获取地块数据失败");
                }
            } catch (Exception e) {
                showToast("解析地块数据失败");
                Log.e("FilterManager", "解析地块数据失败", e);
            }
        }
    }

    /**
     * 处理作物数据响应
     *
     * @param response 服务器响应
     */
    private void handleCropsResponse(Response response) throws IOException {
        if (response.isSuccessful() && response.body() != null) {
            String responseData = response.body().string();
            try {
                Gson gson = new Gson();
                Result<List<CornEntity>> result = gson.fromJson(responseData,
                        new TypeToken<Result<List<CornEntity>>>() {
                        }.getType());

                if (result.getCode() == 200) {
                    cropList = result.getData();
                    updateCropSelector();
                } else {
                    showToast("获取作物数据失败");
                }
            } catch (Exception e) {
                showToast("解析作物数据失败");
                Log.e("FilterManager", "解析作物数据失败", e);
            }
        }
    }

    /**
     * 处理作业类型数据响应
     *
     * @param response 服务器响应
     */
    private void handleOperationsResponse(Response response) throws IOException {
        if (response.isSuccessful() && response.body() != null) {
            String responseData = response.body().string();
            try {
                Gson gson = new Gson();
                Result<List<WorkTypeEntity>> result = gson.fromJson(responseData,
                        new TypeToken<Result<List<WorkTypeEntity>>>() {
                        }.getType());

                if (result.getCode() == 200) {
                    workTypeList = result.getData();
                    updateOperationSelector();
                } else {
                    showToast("获取作业类型失败");
                }
            } catch (Exception e) {
                showToast("解析作业类型数据失败");
                Log.e("FilterManager", "解析作业类型数据失败", e);
            }
        }
    }

    /**
     * 更新地块选择器的选项
     * 在UI线程中更新选择器显示
     */
    private void updateLandPlotSelector() {
        if (landPlotSelector != null) {
            ((android.app.Activity) context).runOnUiThread(() ->
                    landPlotSelector.setOptions(landPlotList,
                            new ObjectTagSelector.DisplayTextProvider<NewLandPlot>() {
                                // 返回地块名称
                                @Override
                                public String getDisplayText(NewLandPlot landPlot) {
                                    return landPlot.getLandName();
                                }
                            }));
        }
    }

    /**
     * 更新作物选择器的选项
     * 在UI线程中更新选择器显示
     */
    private void updateCropSelector() {
        if (cropSelector != null) {
            List<String> cropNames = new ArrayList<>();
            for (CornEntity crop : cropList) {
                cropNames.add(crop.getCropName());
            }
            ((android.app.Activity) context).runOnUiThread(() ->
                    cropSelector.setOptions(cropNames.toArray(new String[0])));
        }
    }

    /**
     * 更新作业类型选择器的选项
     * 在UI线程中更新选择器显示
     */
    private void updateOperationSelector() {
        if (operationSelector != null) {
            List<String> workTypeNames = new ArrayList<>();
            for (WorkTypeEntity workType : workTypeList) {
                workTypeNames.add(workType.getWorkName());
            }
            ((android.app.Activity) context).runOnUiThread(() ->
                    operationSelector.setOptions(workTypeNames.toArray(new String[0])));
        }
    }

    /**
     * 根据选中的作物名称获取对应的ID列表
     *
     * @param selectedCropNames 选中的作物名称列表
     * @return 作物ID列表
     */
    public List<Long> getSelectedCropIds(List<String> selectedCropNames) {
        List<Long> cropIds = new ArrayList<>();
        for (String cropName : selectedCropNames) {
            for (CornEntity crop : cropList) {
                if (crop.getCropName().equals(cropName)) {
                    cropIds.add(crop.getId());
                    break;
                }
            }
        }
        return cropIds;
    }

    /**
     * 根据选中的作业类型名称获取对应的ID列表
     *
     * @param selectedWorkTypeNames 选中的作业类型名称列表
     * @return 作业类型ID列表
     */
    public List<Long> getSelectedWorkTypeIds(List<String> selectedWorkTypeNames) {
        List<Long> workTypeIds = new ArrayList<>();
        for (String workTypeName : selectedWorkTypeNames) {
            for (WorkTypeEntity workType : workTypeList) {
                if (workType.getWorkName().equals(workTypeName)) {
                    workTypeIds.add(workType.getId());
                    break;
                }
            }
        }
        return workTypeIds;
    }

    /**
     * 在UI线程中显示Toast消息
     *
     * @param message 要显示的消息
     */
    private void showToast(String message) {
        ((android.app.Activity) context).runOnUiThread(() ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    /**
     * 获取地块列表
     */
    public List<NewLandPlot> getLandPlotList() {
        return landPlotList;
    }
    /**
     * 根据作物ID获取作物名称
     */
    public String getCropNameById(Long cropId) {
        for (CornEntity crop : cropList) {
            if (crop.getId().equals(cropId)) {
                return crop.getCropName();
            }
        }
        return null;
    }
    /**
     * 根据作业类型ID获取作业类型名称
     */
    public String getWorkTypeNameById(Long workTypeId) {
        for (WorkTypeEntity workType : workTypeList) {
            if (workType.getId().equals(workTypeId)) {
                return workType.getWorkName();
            }
        }
        return null;
    }
}