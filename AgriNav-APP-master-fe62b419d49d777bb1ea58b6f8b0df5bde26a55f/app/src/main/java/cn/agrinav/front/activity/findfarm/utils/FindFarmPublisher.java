package cn.agrinav.front.activity.findfarm.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import cn.agrinav.R;
import cn.agrinav.front.activity.findfarm.FindFarmActivity;
import cn.agrinav.front.activity.findfarm.FarmDetailActivity;
import cn.agrinav.front.entity.FindFarmDetail;
import cn.agrinav.front.entity.NewLandPlot;
import cn.agrinav.utils.OkHttpUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 农场任务发布工具类
 */
public class FindFarmPublisher {
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final FilterManager filterManager;

    /**
     * 构造函数
     *
     * @param context           上下文
     * @param sharedPreferences SharedPreferences实例，用于获取用户token
     * @param filterManager     过滤器管理器，用于处理选择器数据
     */
    public FindFarmPublisher(Context context, SharedPreferences sharedPreferences, FilterManager filterManager) {
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        this.filterManager = filterManager;
    }

    /**
     * 发布农场任务
     *
     * @param detail 农场任务详情对象
     */
    public void publishTask(FindFarmDetail detail) {
        try {
            JSONObject requestBody = createRequestBody(detail);
            sendRequest(requestBody);
        } catch (JSONException e) {
            UIUtils.showToast(context, context.getString(R.string.find_farm_error_data_format));
        }
    }

    /**
     * 创建请求体
     *
     * @param detail 农场任务详情对象
     * @return 组装好的JSON请求体
     * @throws JSONException JSON操作异常
     */
    private JSONObject createRequestBody(FindFarmDetail detail) throws JSONException {
        JSONObject requestBody = new JSONObject();
        // 基本信息
        requestBody.put(context.getString(R.string.find_farm_param_name), detail.getTaskName());
        requestBody.put(context.getString(R.string.find_farm_param_phone_number), detail.getContactMethod());

        // 结束时间
        LocalDateTime workEndTime = DateTimeUtils.parseDateTime(detail.getOperationDate());
        requestBody.put(context.getString(R.string.find_farm_param_end_time), workEndTime.toString());

        // 作物ID数组
        JSONArray cropIdArray = new JSONArray();
        List<Long> selectedCropIds = filterManager.getSelectedCropIds(detail.getCrops());
        for (Long cropId : selectedCropIds) {
            cropIdArray.put(cropId);
        }
        requestBody.put(context.getString(R.string.find_farm_param_crop_id), cropIdArray);

        // 作业类型ID数组
        JSONArray workTypeIdArray = new JSONArray();
        List<Long> selectedWorkTypeIds = filterManager.getSelectedWorkTypeIds(detail.getOperationTypes());
        for (Long workTypeId : selectedWorkTypeIds) {
            workTypeIdArray.put(workTypeId);
        }
        requestBody.put(context.getString(R.string.find_farm_param_work_type_id), workTypeIdArray);

        // 地块ID数组
        JSONArray landPlotIdArray = new JSONArray();
        List<NewLandPlot> selectedLandPlots = detail.getLandPlots();
        if (selectedLandPlots != null) {
            for (NewLandPlot landPlot : selectedLandPlots) {
                landPlotIdArray.put(landPlot.getId());
            }
        }
        requestBody.put(context.getString(R.string.find_farm_param_land_plot_id), landPlotIdArray);

        // 面积
        requestBody.put(context.getString(R.string.find_farm_param_area), detail.getLandPlotArea());

        return requestBody;
    }

    /**
     * 发送HTTP请求
     *
     * @param requestBody JSON格式的请求体
     */
    private void sendRequest(JSONObject requestBody) {
        RequestBody body = RequestBody.create(
                MediaType.parse(context.getString(R.string.find_farm_content_type)),
                requestBody.toString());

        Request request = new OkHttpUtils().createBuilder(
                        context.getString(R.string.server_url),
                        context.getString(R.string.find_farm_api_path))
                .addHeader(context.getString(R.string.request_header_key_token),
                        sharedPreferences.getString(
                                context.getString(R.string.user_pref_key_token),
                                context.getString(R.string.find_farm_empty_token)))
                .post(body)
                .build();

        // 异步发送请求
        OkHttpUtils.okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                ((FarmDetailActivity) context).runOnUiThread(() ->
                        UIUtils.showToast(context, context.getString(R.string.find_farm_error_network)));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                handleResponse(response);
            }
        });
    }

    /**
     * 处理服务器响应
     *
     * @param response HTTP响应对象
     * @throws IOException IO异常
     */
    private void handleResponse(Response response) throws IOException {
        if (response.isSuccessful() && response.body() != null) {
            String responseData = response.body().string();
            try {
                JSONObject jsonObject = new JSONObject(responseData);
                // 检查响应码
                if (jsonObject.getInt("code") == context.getResources().getInteger(R.integer.request_success)) {
                    handleSuccessResponse();
                } else {
                    handleErrorResponse(jsonObject.optString("msg"));
                }
            } catch (JSONException e) {
                ((FarmDetailActivity) context).runOnUiThread(() ->
                        UIUtils.showToast(context, context.getString(R.string.find_farm_error_data_parse)));
                // 重新启用发布按钮
                ((FarmDetailActivity) context).findViewById(R.id.btn_find_farm_publish).setEnabled(true);
            }
        } else {
            ((FarmDetailActivity) context).runOnUiThread(() -> {
                UIUtils.showToast(context, context.getString(R.string.find_farm_error_network));
                // 重新启用发布按钮
                ((FarmDetailActivity) context).findViewById(R.id.btn_find_farm_publish).setEnabled(true);
            });
        }
    }

    /**
     * 处理成功响应
     * 返回到农场列表页面并刷新
     */
    private void handleSuccessResponse() {
        ((FarmDetailActivity) context).runOnUiThread(() -> {
            Intent intent = new Intent(context, FindFarmActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(context.getString(R.string.find_farm_extra_refresh), true);
            context.startActivity(intent);
            UIUtils.showToast(context, context.getString(R.string.find_farm_success_publish));
            ((FarmDetailActivity) context).finish();
        });
    }

    /**
     * 处理错误响应
     *
     * @param errorMessage 错误信息
     */
    private void handleErrorResponse(String errorMessage) {
        ((FarmDetailActivity) context).runOnUiThread(() ->
                UIUtils.showToast(context,
                        context.getString(R.string.find_farm_error_publish_failed, errorMessage)));
        // 重新启用发布按钮
        ((FarmDetailActivity) context).findViewById(R.id.btn_find_farm_publish).setEnabled(true);
    }
}