package cn.agrinav.front.activity.findfarm;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.agrinav.R;
import cn.agrinav.front.activity.findfarm.utils.DateTimeUtils;
import cn.agrinav.front.activity.findfarm.utils.FilterManager;
import cn.agrinav.front.activity.findfarm.utils.UIUtils;
import cn.agrinav.front.adapter.FindFarmAdapter;
import cn.agrinav.front.entity.FindFarmDetail;
import cn.agrinav.front.entity.NewLandPlot;
import cn.agrinav.front.entity.ObjectTagSelector;
import cn.agrinav.front.entity.TagSelector;
import cn.agrinav.utils.OkHttpUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FindFarmActivity extends AppCompatActivity {
    private DrawerLayout findFarmDrawer;
    private ImageView ivFarmAdd, ivFarmSearch;
    private GridView gvFindFarm;
    private FindFarmAdapter adapter;
    private SharedPreferences sharedPreferences;
    private SmartRefreshLayout srlFindFarm;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 8;
    private RefreshLayout refreshLayout;
    private ClassicsFooter classicsFooter;
    private boolean isLoading = false;
    private boolean isRefresh = true;
    private ObjectTagSelector landPlotSelector;
    private TagSelector cropSelector;
    private TagSelector operationSelector;
    private FilterManager filterManager;
    private EditText edtLandPlot;
    private LinearLayout llLandPlotTags;
    private TextView tvLandPlotIndicator;
    private EditText edtCrop;
    private LinearLayout llCropTags;
    private TextView tvCropIndicator;
    private EditText edtOperation;
    private LinearLayout llOperationTags;
    private TextView tvOperationIndicator;
    private NavigationView navigationView;
    private View headerView;
    private EditText edtFindFarmSizeFrom;
    private EditText edtFindFarmSizeTo;
    private View findFarmSidebar;
    //    private EditText edtFindFarmStartDate;
//    private EditText edtFindFarmEndDate;
    private Button btnFindFarmNavigationReset;
    private Button btnFindFarmNavigationConfirm;
    private JSONObject currentFilterParams;
    private static final float MAX_AREA_SIZE = 9999999f;
    private Button btnFindFarmNavigationCancel;
    private ThreadPoolExecutor threadPool;
    // 记录上次应用的筛选条件
    private JSONObject lastAppliedFilters;
    // 标记筛选条件是否改变
    private boolean filtersChanged = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_farm);
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        // 初始化视图组件
        initViews();
        // 初始化线程池
        initThreadPool();
        // 初始化选择器
        initSelectors();
        // 初始化筛选管理器
        initFilterManager();
        //初始化FilterManager
        initFilterManager();
        // 初始化事件监听器
        initEvents();
        // 初始化适配器
        initAdapter();
        // 初始化下拉刷新
        initRefreshLayout();
        // 首次加载数据
        loadData(true);
    }

    /**
     * 初始化线程池
     */
    private void initThreadPool() {
        threadPool = new ThreadPoolExecutor(
                2,
                4,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    // 初始化选择器方法
    private void initSelectors() {
        landPlotSelector = new ObjectTagSelector<>(this, llLandPlotTags, edtLandPlot, tvLandPlotIndicator);
        cropSelector = new TagSelector(this, llCropTags, edtCrop, tvCropIndicator, new String[0]);
        operationSelector = new TagSelector(this, llOperationTags, edtOperation, tvOperationIndicator, new String[0]);
    }

    // 初始化筛选管理器
    private void initFilterManager() {
        filterManager = new FilterManager(this, sharedPreferences);
        filterManager.setSelectors(landPlotSelector, cropSelector, operationSelector);
        threadPool.execute(() -> {
            filterManager.showLandPlotsSelector();
        });
        threadPool.execute(() -> {
            filterManager.showCropSelector();
        });
        threadPool.execute(() -> {
            filterManager.showOperationSelector();
        });
    }

    private void initRefreshLayout() {
        refreshLayout.setOnRefreshListener(refreshlayout -> {
            isRefresh = true;
            currentPage = 1;
            loadData(true);
        });

        refreshLayout.setOnLoadMoreListener(refreshlayout -> {
            if (!isLoading && adapter.getCount() % PAGE_SIZE == 0 && adapter.getCount() > 0) {
                isRefresh = false;
                currentPage++;
                loadData(false);
            } else {
                refreshLayout.finishLoadMore(false);
            }
        });
    }

    private void loadData(boolean isRefresh) {
        if (isLoading) {
            return;
        }
        isLoading = true;

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("myFarmWork", true);
            requestBody.put("orderByCreateTimeDesc", true);
            // 如果有筛选条件，添加筛选参数
            if (currentFilterParams != null) {
                for (Iterator<String> it = currentFilterParams.keys(); it.hasNext(); ) {
                    String key = it.next();
                    requestBody.put(key, currentFilterParams.get(key));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), requestBody.toString());

        HashMap<String, Object> params = new HashMap<>();
        params.put("pageNum", currentPage);
        params.put("pageSize", PAGE_SIZE);
        OkHttpUtils.okHttpClient
                .newCall(new OkHttpUtils().createBuilder(
                                getString(R.string.server_url),
                                "/farmWork/list",
                                params)
                        .addHeader(getString(R.string.request_header_key_token),
                                sharedPreferences.getString(getString(R.string.user_pref_key_token), ""))
                        .post(body)
                        .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        handleError("网络请求失败，请检查网络连接", isRefresh);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        String responseData = response.body().string();
                        Log.d("FindFarmActivity", "Response: " + responseData);
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                JSONObject jsonObject = new JSONObject(responseData);
                                if (jsonObject.getInt("code") == 200) {
                                    JSONArray dataArray = jsonObject.getJSONArray("data");
                                    List<FindFarmDetail> farmDetails = new ArrayList<>();

                                    for (int i = 0; i < dataArray.length(); i++) {
                                        JSONObject farmObject = dataArray.getJSONObject(i);
                                        FindFarmDetail detail = parseFarmDetail(farmObject);
                                        if (detail != null) {
                                            farmDetails.add(detail);
                                        }
                                    }
                                    updateUIWithNewData(farmDetails, isRefresh);
                                } else {
                                    handleError("加载失败: " + jsonObject.optString("msg"), isRefresh);
                                }
                            } catch (JSONException e) {
                                handleError("加载失败", isRefresh);
                            }
                        } else {
                            handleError("加载失败", isRefresh);
                        }
                    }
                });
    }

    private FindFarmDetail parseFarmDetail(JSONObject farmObject) throws JSONException {
        FindFarmDetail detail = new FindFarmDetail();

        // 设置基本信息
        detail.setFindFarmId(farmObject.getString("id"));
        detail.setTaskName(farmObject.getString("name"));
        detail.setLandPlotArea(farmObject.getString("area"));
        detail.setOperationDate(DateTimeUtils.formatDate(farmObject.getString("endTime")));
        detail.setContactMethod(farmObject.getString("phoneNumber"));
        detail.setPublishDate(DateTimeUtils.formatDateTimeDetail(farmObject.getString("createTime")));

        // 解析作物列表
        JSONArray cropsArray = farmObject.getJSONArray("cropName");
        for (int j = 0; j < cropsArray.length(); j++) {
            detail.addCrop(cropsArray.getString(j));
        }

        // 解析作业类型列表
        JSONArray operationsArray = farmObject.getJSONArray("workTypeName");
        for (int j = 0; j < operationsArray.length(); j++) {
            detail.addOperationType(operationsArray.getString(j));
        }

        // 解析图片
        JSONArray imageArray = farmObject.getJSONArray("landPlotImage");
        if (imageArray != null) {
            for (int j = 0; j < imageArray.length(); j++) {
                detail.addImage(imageArray.getString(j));
            }
        }
        //获取状态
        detail.setPublishStatus(farmObject.getInt("status"));
        JSONArray landPlotIdsArray = farmObject.getJSONArray("landPlotId");

        List<NewLandPlot> landPlots = new ArrayList<>();
        final int[] completedLandPlots = {0};
        final int totalLandPlots = landPlotIdsArray.length();

        if (totalLandPlots == 0) {
            detail.setLandPlots(landPlots);
        } else {
            for (int j = 0; j < totalLandPlots; j++) {
                String landPlotId = landPlotIdsArray.getString(j);
                fetchLandPlotDetails(landPlotId, new LandPlotCallback() {
                    @Override
                    public void onSuccess(NewLandPlot landPlot) {
                        landPlots.add(landPlot);
                        checkLandPlotsCompletion();
                    }

                    @Override
                    public void onError(String error) {
                        checkLandPlotsCompletion();
                    }

                    private void checkLandPlotsCompletion() {
                        completedLandPlots[0]++;
                        if (completedLandPlots[0] == totalLandPlots) {
                            detail.setLandPlots(landPlots);
                        }
                    }
                });
            }
        }

        return detail;
    }

    private void updateUIWithNewData(List<FindFarmDetail> farmDetails, boolean isRefresh) {
        runOnUiThread(() -> {
            hideAllOperationLayers();
            if (isRefresh) {
                adapter.update(farmDetails);
            } else {
                adapter.loadMore(farmDetails);
            }

            isLoading = false;
            finishRefreshLoadMore(isRefresh);

            // 如果数据不足PAGE_SIZE，说明没有更多数据了
            if (farmDetails.size() < PAGE_SIZE) {
                refreshLayout.setNoMoreData(true);
            } else {
                refreshLayout.setNoMoreData(false);
            }
        });
    }

    private void handleError(String message, boolean isRefresh) {
        runOnUiThread(() -> {
            UIUtils.showToast(this, message);
            isLoading = false;
            finishRefreshLoadMore(isRefresh);
        });
    }

    /**
     * 根据ID获取地块详情
     *
     * @param landPlotId 地块ID
     * @param callback   回调接口
     */
    private void fetchLandPlotDetails(String landPlotId, LandPlotCallback callback) {
        Request request = new OkHttpUtils().createBuilder(getString(R.string.server_url), "/landPlot/" + landPlotId)
                .addHeader(getString(R.string.request_header_key_token), sharedPreferences.getString(getString(R.string.user_pref_key_token), " "))
                .build();
        OkHttpUtils.okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = response.body().string();
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        if (jsonObject.getInt("code") == 200) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            NewLandPlot landPlot = new NewLandPlot();
                            landPlot.setId(data.getString("id"));
                            landPlot.setLandName(data.getString("landName"));
                            landPlot.setAddress(data.getString("address"));
                            landPlot.setLandArea((float) data.getDouble("landArea"));
                            callback.onSuccess(landPlot);
                        } else {
                            String message = jsonObject.optString("message", "获取地块详情失败");
                            callback.onError(message);
                        }
                    } catch (JSONException e) {
                        callback.onError("解析地块详情失败");
                    }
                } else {
                    callback.onError("请求地块详情失败");
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("FindFarmActivity", "获取地块详情网络请求失败", e);
                callback.onError("网络请求失败");
            }
        });
    }

    /**
     * 地块详情回调接口
     */
    private interface LandPlotCallback {
        void onSuccess(NewLandPlot landPlot);

        void onError(String error);
    }

    private void finishRefreshLoadMore(boolean isRefresh) {
        if (isRefresh) {
            refreshLayout.finishRefresh();
        } else {
            refreshLayout.finishLoadMore();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initEvents() {
        Log.i("FindFarmActivity", "ivFarmAdd");
        ivFarmAdd.setOnClickListener(v -> {
            //跳转到FarmDetailActivity
            Intent intent = new Intent(FindFarmActivity.this, FarmDetailActivity.class);
            startActivity(intent);
        });

        // GridView的item点击事件
        gvFindFarm.setOnItemClickListener((parent, view, position, id) -> {
            hideAllOperationLayers();
            View operationLayer = view.findViewById(R.id.farm_choose_open);
            if (operationLayer != null) {
                operationLayer.setVisibility(View.VISIBLE);
            }
        });

        // 设置GridView的滚动监听
        gvFindFarm.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                hideAllOperationLayers();
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                // 滚动时不需要处理
            }
        });
        // 设置下拉刷新和上拉加载的监听
        refreshLayout.setOnRefreshListener(refreshLayout -> {
            hideAllOperationLayers();
            if (!isLoading) {
                currentPage = 1;
                loadData(true);
            }
        });
        // 上拉加载
        refreshLayout.setOnLoadMoreListener(refreshLayout -> {
            hideAllOperationLayers();
            if (!isLoading && adapter.getCount() % PAGE_SIZE == 0) {
                isRefresh = false;
                currentPage++;
                loadData(false);
            } else {
                refreshLayout.finishLoadMore(false);
            }
        });
        // 点击筛选按钮
        ivFarmSearch.setOnClickListener(v -> {
            findFarmDrawer.openDrawer(GravityCompat.END);
        });

        // 地块选择器点击事件
        tvLandPlotIndicator.setOnClickListener(v -> {
            cropSelector.collapse();
            operationSelector.collapse();
            landPlotSelector.toggle();
        });

        // 作物选择器点击事件
        tvCropIndicator.setOnClickListener(v -> {
            landPlotSelector.collapse();
            operationSelector.collapse();
            cropSelector.toggle();
        });

        // 作业类型选择器点击事件
        tvOperationIndicator.setOnClickListener(v -> {
            landPlotSelector.collapse();
            cropSelector.collapse();
            operationSelector.toggle();
        });
        // 侧边栏点击事件
        headerView.setOnClickListener(v -> {
            filterManager.hideAllSelectors();
        });
        findFarmDrawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                // 只在筛选条件改变时重新加载数据
                if (filtersChanged) {
                    currentPage = 1;
                    isRefresh = true;
                    loadData(true);
                    filtersChanged = false; // 重置标志位
                }
            }
        });
        // 添加焦点变化监听，在失去焦点时自动调整大小值
        View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
            if (!hasFocus) {
                adjustSizeValues();
            }
        };
        edtFindFarmSizeFrom.setOnFocusChangeListener(focusChangeListener);
        edtFindFarmSizeTo.setOnFocusChangeListener(focusChangeListener);

        // 最早日期选择
//        edtFindFarmStartDate.setOnClickListener(v -> {
//            DateTimeUtils.showDatePickerWithoutLimit(FindFarmActivity.this, edtFindFarmStartDate, date -> {
//                // 清空最晚日期（如果最晚日期早于新选择的最早日期）
//                String endDateStr = edtFindFarmEndDate.getText().toString();
//                if (!endDateStr.isEmpty() && DateTimeUtils.compareDate(endDateStr, date) < 0) {
//                    edtFindFarmEndDate.setText("");
//                }
//            });
//        });
//
//        // 最晚日期选择
//        edtFindFarmEndDate.setOnClickListener(v -> {
//            String startDateStr = edtFindFarmStartDate.getText().toString();
//            if (startDateStr.isEmpty()) {
//                UIUtils.showToast(this, "请先选择最早日期");
//                return;
//            }
//            DateTimeUtils.showDatePickerWithLimit(FindFarmActivity.this, edtFindFarmEndDate, startDateStr);
//        });

        // 重置
        btnFindFarmNavigationReset.setOnClickListener(v -> {
            resetFilters();
            if (lastAppliedFilters != null) {
                lastAppliedFilters = null;
                filtersChanged = true;
            }
            loadData(true);
        });
        // 确定
        btnFindFarmNavigationConfirm.setOnClickListener(v -> {
            JSONObject newFilters = createFilterParams();
            if (newFilters != null && !newFilters.toString().equals(lastAppliedFilters != null ? lastAppliedFilters.toString() : "")) {
                currentFilterParams = newFilters;
                lastAppliedFilters = newFilters;
                filtersChanged = true;
            }
            findFarmDrawer.closeDrawer(GravityCompat.END);
        });
        // 取消
        btnFindFarmNavigationCancel.setOnClickListener(v -> {
            if(lastAppliedFilters != null){
                currentFilterParams = lastAppliedFilters;
                restoreLastAppliedFilters();
            }else{
                resetFilters();
            }

            findFarmDrawer.closeDrawer(GravityCompat.END);
        });
        findFarmSidebar.setOnTouchListener((v, event) -> {
            // 点击空白处时清除焦点
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                clearInputFocus();
            }
            // 返回false以允许点击事件继续传递
            return false;
        });
    }

    private void restoreLastAppliedFilters() {
        // 恢复筛选器UI状态
        try {
            // 恢复亩数范围
            if (lastAppliedFilters.has("minArea")) {
                edtFindFarmSizeFrom.setText(String.valueOf(lastAppliedFilters.getDouble("minArea")));
            } else {
                edtFindFarmSizeFrom.setText("");
            }

            if (lastAppliedFilters.has("maxArea")) {
                double maxArea = lastAppliedFilters.getDouble("maxArea");
                if (maxArea != MAX_AREA_SIZE) {
                    edtFindFarmSizeTo.setText(String.valueOf(maxArea));
                } else {
                    edtFindFarmSizeTo.setText("");
                }
            } else {
                edtFindFarmSizeTo.setText("");
            }
            // 恢复地块选择
            if (lastAppliedFilters.has("landPlotId")) {
                JSONArray landPlotIds = lastAppliedFilters.getJSONArray("landPlotId");
                List<NewLandPlot> selectedLandPlots = new ArrayList<>();
                for (int i = 0; i < landPlotIds.length(); i++) {
                    String landPlotId = landPlotIds.getString(i);
                    for (NewLandPlot landPlot : filterManager.getLandPlotList()) {
                        if (landPlot.getId().equals(landPlotId)) {
                            selectedLandPlots.add(landPlot);
                            break;
                        }
                    }
                }
                landPlotSelector.setSelected(selectedLandPlots);
            } else {
                landPlotSelector.clearSelection();
            }
            // 恢复作物选择
            if (lastAppliedFilters.has("cropId")) {
                JSONArray cropIds = lastAppliedFilters.getJSONArray("cropId");
                List<String> selectedCropNames = new ArrayList<>();
                for (int i = 0; i < cropIds.length(); i++) {
                    Long cropId = cropIds.getLong(i);
                    String cropName = filterManager.getCropNameById(cropId);
                    if (cropName != null) {
                        selectedCropNames.add(cropName);
                    }
                }
                cropSelector.setSelectedTags(selectedCropNames);
            } else {
                cropSelector.clearSelection();
            }
            // 恢复作业类型选择
            if (lastAppliedFilters.has("workTypeId")) {
                JSONArray workTypeIds = lastAppliedFilters.getJSONArray("workTypeId");
                List<String> selectedWorkTypeNames = new ArrayList<>();
                for (int i = 0; i < workTypeIds.length(); i++) {
                    Long workTypeId = workTypeIds.getLong(i);
                    String workTypeName = filterManager.getWorkTypeNameById(workTypeId);
                    if (workTypeName != null) {
                        selectedWorkTypeNames.add(workTypeName);
                    }
                }
                operationSelector.setSelectedTags(selectedWorkTypeNames);
            } else {
                operationSelector.clearSelection();
            }
        } catch (JSONException e) {
            Log.e("FindFarmActivity", "恢复筛选条件失败", e);
        }
    }

    private JSONObject createFilterParams() {
        // 筛选条件
        JSONObject filterParams = new JSONObject();
        try {
            // 亩数范围
            adjustSizeValues();
            String sizeFrom = edtFindFarmSizeFrom.getText().toString().trim();
            String sizeTo = edtFindFarmSizeTo.getText().toString().trim();
            // 验证亩数范围
            if (!sizeFrom.isEmpty() || !sizeTo.isEmpty()) {
                float fromValue = sizeFrom.isEmpty() ? 0 : Float.parseFloat(sizeFrom);
                float toValue = sizeTo.isEmpty() ? MAX_AREA_SIZE : Float.parseFloat(sizeTo);
                // 验证输入值不超过1000万亩
                if (fromValue > MAX_AREA_SIZE || toValue > MAX_AREA_SIZE) {
                    UIUtils.showToast(this, "筛选亩数不能超过1000万亩");
                    return null;
                }

                filterParams.put("minArea", fromValue);  // 如果最小值为空，传0
                filterParams.put("maxArea", toValue);    // 如果最大值为空，传MAX_VALUE
            }

            // 地块选择
            JSONArray landPlotIdArray = new JSONArray();
            List<NewLandPlot> selectedLandPlots = landPlotSelector.getSelectedObjects();
            if (selectedLandPlots != null && !selectedLandPlots.isEmpty()) {

                for (NewLandPlot landPlot : selectedLandPlots) {
                    landPlotIdArray.put(landPlot.getId());
                }
            }
            filterParams.put("landPlotId", landPlotIdArray);

            // 作物选择
            List<String> selectedCropNames = cropSelector.getSelectedTags();
            if (selectedCropNames != null && !selectedCropNames.isEmpty()) {

                List<Long> cropIds = filterManager.getSelectedCropIds(selectedCropNames);
                JSONArray cropIdArray = new JSONArray();
                for (Long cropId : cropIds) {
                    cropIdArray.put(cropId);
                }
                filterParams.put("cropId", cropIdArray);
            }

            // 作业类型选择
            List<String> selectedWorkTypeNames = operationSelector.getSelectedTags();
            if (selectedWorkTypeNames != null && !selectedWorkTypeNames.isEmpty()) {

                List<Long> workTypeIds = filterManager.getSelectedWorkTypeIds(selectedWorkTypeNames);
                JSONArray workTypeIdArray = new JSONArray();
                for (Long workTypeId : workTypeIds) {
                    workTypeIdArray.put(workTypeId);
                }
                filterParams.put("workTypeId", workTypeIdArray);
            }

//            // 日期范围
//            String startDate = edtFindFarmStartDate.getText().toString().trim();
//            String endDate = edtFindFarmEndDate.getText().toString().trim();
//            if (!startDate.isEmpty()) {
//                hasFilter = true;
//                filterParams.put("startTime", startDate);
//            }
//            if (!endDate.isEmpty()) {
//                hasFilter = true;
//                filterParams.put("endTime", endDate);
//            }
//
//            if (!hasFilter) {
//                UIUtils.showToast(this, "请先选择筛选条件");
//                return;
//            }

//            // 保存当前的筛选参数
//            currentFilterParams = filterParams;
//
//            // 重置页码并加载数据
//            currentPage = 1;
//            isRefresh = true;
//            loadData(true);
            return filterParams;

        } catch (JSONException e) {
            Log.e("FindFarmActivity", "筛选失败", e);
            UIUtils.showToast(this, "筛选失败");
            return null;
        }
    }

    // 隐藏所有操作层
    private void hideAllOperationLayers() {
        if (gvFindFarm == null) {
            return;
        }
        // 遍历每个item，找到并隐藏操作层
        for (int i = 0; i < gvFindFarm.getChildCount(); i++) {
            View child = gvFindFarm.getChildAt(i);
            View operationLayer = child.findViewById(R.id.farm_choose_open);
            if (operationLayer != null && operationLayer.getVisibility() == View.VISIBLE) {
                operationLayer.setVisibility(View.GONE);
            }
        }
        // 同时通知适配器刷新视图状态
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }


    private void initAdapter() {
        adapter = new FindFarmAdapter(this);
        adapter.setGridView(gvFindFarm);
        adapter.setOnItemOperationListener(new FindFarmAdapter.OnItemOperationListener() {
            @Override
            public void onViewDetail(FindFarmDetail farmDetail) {
                // 处理查看详情
                Intent intent = new Intent(FindFarmActivity.this, FarmItemDetailActivity.class);
                intent.putExtra("FarmDetail", farmDetail);
                startActivity(intent);
            }

            @Override
            public void onViewUpdate(FindFarmDetail farmDetail) {
                // 处理编辑操作
                Intent intent = new Intent(FindFarmActivity.this, UpdateFarmActivity.class);
                //将要编辑的寻找农机详情传入
                intent.putExtra("editFarmDetail", farmDetail);
                intent.putExtra("selectedLandPlots", new ArrayList<>(farmDetail.getLandPlots()));
                startActivity(intent);
            }

            @Override
            public void onRemove(FindFarmDetail farmDetail) {
                Request request = new OkHttpUtils().createBuilder(getString(R.string.server_url), "/farmWork/" + farmDetail.getFindFarmId())
                        .addHeader(getString(R.string.request_header_key_token), sharedPreferences.getString(getString(R.string.user_pref_key_token), ""))
                        .delete()
                        .build();
                OkHttpUtils.okHttpClient.newCall(request)
                        .enqueue(new Callback() {
                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                if (response.isSuccessful()) {
                                    runOnUiThread(() -> {
                                        farmDetail.setPublishStatus(1);
                                        Toast.makeText(FindFarmActivity.this, "下架成功", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                runOnUiThread(() -> UIUtils.showToast(FindFarmActivity.this, "下架失败"));
                            }
                        });
            }
        });
        gvFindFarm.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //当从编辑界面返回时刷新数据
        if (getIntent().getBooleanExtra("isRefresh", false)) {
            loadData(true);
            getIntent().removeExtra("refresh");
        }
    }

    // 自动调整按亩数筛选大小值的位置
    private void adjustSizeValues() {
        String fromText = edtFindFarmSizeFrom.getText().toString().trim();
        String toText = edtFindFarmSizeTo.getText().toString().trim();

        if (!fromText.isEmpty() && !toText.isEmpty()) {
            try {
                int fromValue = Integer.parseInt(fromText);
                int toValue = Integer.parseInt(toText);

                if (fromValue > toValue) {
                    // 交换值
                    edtFindFarmSizeFrom.setText(String.valueOf(toValue));
                    edtFindFarmSizeTo.setText(String.valueOf(fromValue));
                }
            } catch (NumberFormatException e) {
                Log.e("FindFarmActivity", "解析亩数失败", e);
            }
        }
    }


    /**
     * 清除输入框焦点并隐藏软键盘
     */
    private void clearInputFocus() {
        UIUtils.hideKeyboardAndClearFocus(this, edtFindFarmSizeFrom, edtFindFarmSizeTo);
    }

    /**
     * 重置筛选条件
     */
    private void resetFilters() {
        // 重置所有筛选条件
        edtFindFarmSizeFrom.setText("");
        edtFindFarmSizeTo.setText("");
//        edtFindFarmStartDate.setText("");
//        edtFindFarmEndDate.setText("");

        landPlotSelector.clearSelection();
        cropSelector.clearSelection();
        operationSelector.clearSelection();
        // 清除保存的筛选参数
        currentFilterParams = null;
        // 重新加载数据
        currentPage = 1;
        isRefresh = true;
    }

    private void initViews() {
        findFarmDrawer = findViewById(R.id.find_farm_drawer);
        ivFarmAdd = findViewById(R.id.iv_farm_add);
        ivFarmSearch = findViewById(R.id.iv_farm_search);
        gvFindFarm = findViewById(R.id.gv_find_farm);
        refreshLayout = findViewById(R.id.find_farm_refresh);
        classicsFooter = findViewById(R.id.find_farm_grid_footer);
        // 获取 NavigationView 和它的 headerView
        navigationView = findViewById(R.id.nav_find_farm_select);
        headerView = navigationView.getHeaderView(0);
        findFarmSidebar = headerView.findViewById(R.id.find_farm_sidebar);

        // 从 headerView 中查找筛选相关的视图
        edtLandPlot = headerView.findViewById(R.id.tv_find_farm_item_land);
        llLandPlotTags = headerView.findViewById(R.id.ll_land_select_tags);
        tvLandPlotIndicator = headerView.findViewById(R.id.tv_select_land_tap);
        edtCrop = headerView.findViewById(R.id.tv_find_farm_item_corn);
        llCropTags = headerView.findViewById(R.id.ll_crop_select_tags);
        tvCropIndicator = headerView.findViewById(R.id.tv_select_crop_tap);
        edtOperation = headerView.findViewById(R.id.tv_find_farm_item_operation);
        llOperationTags = headerView.findViewById(R.id.ll_operation_selection_tags);
        tvOperationIndicator = headerView.findViewById(R.id.tv_select_operation_tap);

        // 亩数筛选
        edtFindFarmSizeFrom = headerView.findViewById(R.id.edt_find_farm_size_from);
        edtFindFarmSizeTo = headerView.findViewById(R.id.edt_find_farm_size_to);
        // 日期筛选
//        edtFindFarmStartDate = headerView.findViewById(R.id.edt_find_farm_start_date);
//        edtFindFarmEndDate = headerView.findViewById(R.id.edt_find_farm_end_date);
        // 重置
        btnFindFarmNavigationReset = headerView.findViewById(R.id.btn_find_farm_navigation_reset);
        // 确定
        btnFindFarmNavigationConfirm = headerView.findViewById(R.id.btn_find_farm_navigation_confirm);
        // 取消
        btnFindFarmNavigationCancel = headerView.findViewById(R.id.btn_find_farm_navigation_cancel);
    }
}
