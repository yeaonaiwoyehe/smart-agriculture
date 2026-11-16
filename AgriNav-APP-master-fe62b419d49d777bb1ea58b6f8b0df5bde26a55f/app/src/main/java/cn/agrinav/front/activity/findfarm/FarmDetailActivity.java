package cn.agrinav.front.activity.findfarm;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cn.agrinav.R;
import cn.agrinav.front.activity.findfarm.utils.DateTimeUtils;
import cn.agrinav.front.activity.findfarm.utils.FilterManager;
import cn.agrinav.front.activity.findfarm.utils.FindFarmPublisher;
import cn.agrinav.front.activity.findfarm.utils.InputValidationUtils;
import cn.agrinav.front.activity.findfarm.utils.UIUtils;
import cn.agrinav.front.entity.FindFarmDetail;
import cn.agrinav.front.entity.NewLandPlot;
import cn.agrinav.front.entity.ObjectTagSelector;
import cn.agrinav.front.entity.TagSelector;

/**
 * 发布界面
 */
public class FarmDetailActivity extends AppCompatActivity {
    private EditText edtFarmCrop, edtEarthType, edtWorkName, edtEarthNum, edtFarmPhone, edtEarthSelect, edtFarmTime;
    private TextView tvTapOne;
    private TextView tvTapTwo;
    private LinearLayout llLandPlotTags;
    private LinearLayout llCropTags;
    private LinearLayout llOperationTags;
    private ObjectTagSelector landPlotSelector;
    private TagSelector cropSelector;
    private TagSelector operationSelector;
    private Button btn_find_farm_publish;
    private SharedPreferences sharedPreferences;
    private TextView tvTap;
    private FilterManager filterManager;
    private FindFarmPublisher taskPublisher;
    private ThreadPoolExecutor threadPool;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farm_detail);
        // 初始化视图组件
        initViews();
        // 初始化线程池
        initThreadPool();
        // 加载数据
        initData();
        // 初始化选择器
        initSelectors();
        // 初始化选择器数据
        initFilterManager();
        // 初始化事件监听器
        initEvents();
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

    /**
     * 自动填充手机号
     */
    private void initData() {
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        // 自动填充手机号
        String phoneNumber = sharedPreferences.getString(getString(R.string.user_pref_key_phone), "");
        edtFarmPhone.setText(phoneNumber);
    }

    /**
     * 选择器数据
     */
    private void initFilterManager() {
        filterManager = new FilterManager(this, sharedPreferences);
        filterManager.setSelectors(landPlotSelector, cropSelector, operationSelector);
        taskPublisher = new FindFarmPublisher(this, sharedPreferences, filterManager);
        //TODO: 选择器加载实现异步
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

    /**
     * 事件监听器
     */
    private void initEvents() {
        tvTap.setOnClickListener(v -> {
            cropSelector.collapse();
            operationSelector.collapse();
            hideEditText();
            landPlotSelector.toggle();
        });

        tvTapOne.setOnClickListener(v -> {
            landPlotSelector.collapse();
            operationSelector.collapse();
            hideEditText();
            cropSelector.toggle();
        });

        tvTapTwo.setOnClickListener(v -> {
            landPlotSelector.collapse();
            cropSelector.collapse();
            hideEditText();
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
           if (validateInputs()) {
                // 禁用发布按钮防止重复提交
                btn_find_farm_publish.setEnabled(false);
                try {
                    threadPool.execute(() -> {
                        try {
                            FindFarmDetail farmDetail = createFindFarmDetail();
                            // 发布任务
                            taskPublisher.publishTask(farmDetail);

                            // 在主线程更新UI
                            runOnUiThread(() -> {
                                btn_find_farm_publish.setEnabled(true);
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                UIUtils.showToast(FarmDetailActivity.this, "系统繁忙，请稍后重试");
                                btn_find_farm_publish.setEnabled(true);
                            });
                        }
                    });
                } catch (Exception e) {
                    UIUtils.showToast(FarmDetailActivity.this, "系统繁忙，请稍后重试");
                    btn_find_farm_publish.setEnabled(true);
                }
            }
        });

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

    /**
     * 初始化选择器
     */
    private void initSelectors() {
        landPlotSelector = new ObjectTagSelector(
                this,
                llLandPlotTags,
                edtEarthSelect,
                tvTap);
        // 监听地块选择器的选择变化
        landPlotSelector.setOnSelectionChangedListener(new ObjectTagSelector.OnSelectionChangedListener<NewLandPlot>() {
            @Override
            public void onSelectionChanged(List<NewLandPlot> selectedItems) {
                updateTotalArea(selectedItems);
            }
        });

        String[] crops = new String[0];
        cropSelector = new TagSelector(this, llCropTags, edtFarmCrop, tvTapOne, crops);

        String[] operations = new String[0];
        operationSelector = new TagSelector(this, llOperationTags, edtEarthType, tvTapTwo, operations);
    }

    /**
     * 更新总地块面积
     */
    private void updateTotalArea(List<NewLandPlot> selectedLandPlots) {
        UIUtils.updateTotalArea(selectedLandPlots, edtEarthNum);
    }

    /**
     * 日期时间选择器
     */
    private void showDateTimePicker() {
        DateTimeUtils.showDatePicker(this, edtFarmTime);
        hideEditText();
        hideAllSelectors();
    }

    /**
     * 隐藏键盘
     */
    private void hideEditText() {
        UIUtils.hideKeyboardAndClearFocus(this, edtWorkName, edtFarmPhone, edtEarthNum);
    }

    /**
     * 隐藏选择器
     */
    private void hideAllSelectors() {
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

    /**
     * 初始化视图组件
     */
    private void initViews() {
        edtWorkName = findViewById(R.id.edt_work_name);
        edtEarthSelect = findViewById(R.id.edt_earth_select);
        edtEarthNum = findViewById(R.id.edt_earth_num);
        edtFarmCrop = findViewById(R.id.edt_farm_crop);
        edtEarthType = findViewById(R.id.edt_earth_type);
        edtFarmPhone = findViewById(R.id.edt_farm_phone);
        edtFarmTime = findViewById(R.id.edt_farm_time);
        tvTap = findViewById(R.id.tv_tap);
        llLandPlotTags = findViewById(R.id.ll_land_plot_tags);
        tvTapOne = findViewById(R.id.tv_tap_one);
        llCropTags = findViewById(R.id.ll_crop_tags);
        tvTapTwo = findViewById(R.id.tv_tap_two);
        llOperationTags = findViewById(R.id.ll_operation_tags);
        btn_find_farm_publish = findViewById(R.id.btn_find_farm_publish);
    }

     @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭线程池
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                // 等待所有任务完成
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
            }
        }
    }
}
