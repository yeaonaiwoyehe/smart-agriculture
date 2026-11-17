package cn.agrinav.front.fragment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.widget.EditText;
import android.text.InputType;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import cn.agrinav.R;
import cn.agrinav.front.adapter.GridFarmWorkAdapter;
import cn.agrinav.front.entity.FarmWork;
import cn.agrinav.front.entity.dto.FarmWorkDTO;
import cn.agrinav.front.util.CommonUtils;
import cn.agrinav.front.util.QueryUtils;
import cn.agrinav.front.util.ResultData;
import cn.agrinav.measure.utils.DefaultInfoUtils;
import cn.agrinav.measure.utils.ThreadPoolUtils;
import cn.agrinav.trajectory.entity.CornTypeEntity;
import cn.agrinav.trajectory.entity.vo.CornEntity;

/**
 * @author 18506
 */
public class FragmentFrontDemandFarmWork extends MyFragment {
    private AppCompatActivity activity;
    private GridView gridWorK;
    private SharedPreferences sharedPreferences;
    private Handler handler = new Handler(Looper.getMainLooper());
    // 查询到的数据
    private List<FarmWork> list = new ArrayList<>();
    private TextView conditionTime,
            conditionWorkArea,
            conditionWorkType,
            conditionCrop,
            conditionDeadline,
            conditionPlotArea
    ;

    private boolean isRefresh = true;
    //记录总数据条数
    private Integer total;
    private GridFarmWorkAdapter adapter;
    //查询的dto
    private FarmWorkDTO dto;
    private List<CornEntity> crops;
    private List<String> selectedCropNames = new ArrayList<>();
    private List<Long> selectedCropIds = new ArrayList<>();
    private List<String> selectedWorkTypeNames = new ArrayList<>();
    private List<Long> selectedWorkTypeIds = new ArrayList<>();

    private int timeCount  = 0;
    private List<CornTypeEntity> types;

    private SmartRefreshLayout refreshLayout;

    public FragmentFrontDemandFarmWork() {

    }
    public FragmentFrontDemandFarmWork(SoftReference<AppCompatActivity> activityWeakReference, SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
        this.activity = activityWeakReference.get();
        ThreadPoolUtils.initThreadPool();
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.fragment_demand_farm_work, null);
        dto = new FarmWorkDTO();
        dto.setStatus(0);
        dto.setOrderByCreateTimeDesc(true);
        initViews(view);
        initEvents(view);
        initData();
        return view;
    }

    private void initData() {
        ThreadPoolUtils.threadPoolExecutor.execute(()->{
            ResultData<List<FarmWork>> resultData = QueryUtils.getQueryFarmWork(getString(R.string.server_url), getString(R.string.getFarmWork) + getString(R.string.list), DefaultInfoUtils.getToken(activity), 1, dto);
            List<FarmWork> farmWorkList = resultData.getData();
            CommonUtils.waitDataGetting(farmWorkList, activity);
            handler.post(()->{
                total = resultData.getTotal();
                if (!list.isEmpty()){
                    list.clear();
                }
                // 接口失败或无数据时 getData() 可能为 null，直接 addAll(null) 会触发空指针崩溃，这里判空保护
                if (farmWorkList != null){
                    list.addAll(farmWorkList);
                }
                bingEvents(gridWorK);
            });
        });
    }


    private void initEvents(View view) {
        // 保存上次选择的日期
        final LocalDateTime[] selectedDate = {LocalDateTime.now()};
        
        conditionDeadline.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                activity,
                (view1, year, month, dayOfMonth) -> {
                    // 更新选中的日期，设置时间为当天的 23:59:59
                    selectedDate[0] = LocalDateTime.of(year, month + 1, dayOfMonth, 23, 59, 59);
                    
                    // 使用 DateTimeFormatter 格式化日期显示
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                    String dateStr = selectedDate[0].format(formatter);
                    conditionDeadline.setText(dateStr);
                    
                    // 直接设置 LocalDateTime 到 DTO
                    dto.setEndTime(selectedDate[0]);
                    
                    // 刷新数据并更新列表显示
                    initData();
                    adapter.update(list);
                },
                selectedDate[0].getYear(),
                selectedDate[0].getMonthValue() - 1,
                selectedDate[0].getDayOfMonth()
            );

            // 添加清除按钮
            datePickerDialog.setButton(
                DatePickerDialog.BUTTON_NEUTRAL,
                "清除",
                (dialog, which) -> {
                    // 重置显示文本
                    conditionDeadline.setText("作业期限");
                    // 清除查询条件
                    dto.setEndTime(null);
                    // 刷新数据
                    initData();
                    adapter.update(list);
                }
            );

            datePickerDialog.show();
        });
        
        // 保存上次输入的亩数范围
        final double[] lastAreaRange = {0.0, 0.0};  // [start, end]
        
        conditionPlotArea.setOnClickListener(v -> {
            LinearLayout layout = new LinearLayout(activity);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(50, 30, 50, 30);
            
            // 创建起始亩数输入框
            EditText startInput = new EditText(activity);
            startInput.setHint("最小亩数");
            startInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            startInput.setLayoutParams(startParams);
            
            // 创建分隔符
            TextView separator = new TextView(activity);
            separator.setText("到");
            separator.setPadding(20, 0, 20, 0);
            
            // 创建结束亩数输入框
            EditText endInput = new EditText(activity);
            endInput.setHint("最大亩数");
            endInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            LinearLayout.LayoutParams endParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            endInput.setLayoutParams(endParams);
            
            // 设置上次输入的值
            if (lastAreaRange[0] > 0 || lastAreaRange[1] > 0) {
                startInput.setText(String.valueOf(lastAreaRange[0]));
                endInput.setText(String.valueOf(lastAreaRange[1]));
            }
            
            // 添加视图到布局
            layout.addView(startInput);
            layout.addView(separator);
            layout.addView(endInput);
            
            new AlertDialog.Builder(activity)
                .setTitle("选择地块亩数范围")
                .setView(layout)
                .setPositiveButton("确定", (dialog, which) -> {
                    try {
                        double startArea = Double.parseDouble(startInput.getText().toString());
                        double endArea = Double.parseDouble(endInput.getText().toString());
                        
                        if (startArea > endArea) {
                            Toast.makeText(activity, "起始亩数不能大于结束亩数", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // 保存输入的值
                        lastAreaRange[0] = startArea;
                        lastAreaRange[1] = endArea;
                        
                        // 更新显示文本
                        String rangeText = String.format(Locale.CHINA, "%.1f到%.1f亩", startArea, endArea);
                        conditionPlotArea.setText(rangeText);
                        
                        dto.setMinArea(lastAreaRange[0]);
                        dto.setMaxArea(lastAreaRange[1]);
                        initData();
                        adapter.update(list);
                        
                    } catch (NumberFormatException e) {
                        Toast.makeText(activity, "请输入有效的数字", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("清除", (dialog, which) -> {
                    // 清除保存的值
                    lastAreaRange[0] = 0.0;
                    lastAreaRange[1] = 0.0;
                    // 重置显示文本
                    conditionPlotArea.setText("地块亩数");
                    // 清除查询条件并刷新数据
                    dto.setMinArea(null);
                    dto.setMaxArea(null);
                    initData();
                    adapter.update(list);
                })
                .setNegativeButton("取消", null)
                .show();
        });

        // 保存上次输入的地址
        final List<String> lastWorkArea = new ArrayList<>();
        lastWorkArea.add(""); // Initialize with empty string
        
        conditionWorkArea.setOnClickListener(v -> {
            final EditText input = new EditText(activity);
            // 设置上次输入的值
            if (!lastWorkArea.isEmpty() && lastWorkArea.get(0) != null) {
                input.setText(lastWorkArea.get(0));
            }
            
            // 设置输入框的布局参数
            input.setPadding(50, 30, 50, 30);
            input.setHint("请输入作业区域地址");
            
            new AlertDialog.Builder(activity)
                .setTitle("输入作业区域")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String workArea = input.getText().toString().trim();
                    
                    if (workArea.isEmpty()) {
                        Toast.makeText(activity, "作业区域不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // 保存输入的值
                    lastWorkArea.clear();
                    lastWorkArea.add(workArea);
                    // 更新显示文本
                    conditionWorkArea.setText(workArea);
                    
                    // 更新查询条件并刷新数据
                    dto.setLocation(lastWorkArea);
                    initData();
                    adapter.update(list);
                })
                .setNeutralButton("清除", (dialog, which) -> {
                    // 清除保存的值
                    lastWorkArea.clear();
                    lastWorkArea.add("");
                    // 重置显示文本
                    conditionWorkArea.setText("作业区域");
                    // 清除查询条件并刷新数据
                    dto.setLocation(null);
                    initData();
                    adapter.update(list);
                })
                .setNegativeButton("取消", null)
                .show();
        });


        conditionTime.setOnClickListener(v -> {
            timeCount++;
            dto.setOrderByCreateTimeDesc(timeCount % 2 == 0);
            initData();
            adapter.update(list);
        });

        conditionWorkType.setOnClickListener(v -> {
            ThreadPoolUtils.threadPoolExecutor.execute(() -> {
                types = QueryUtils.getWorkTypes(getString(R.string.server_url),
                    "/fieldWork/listUnique",
                    DefaultInfoUtils.getToken(activity));
                CommonUtils.waitDataGetting(types, activity);
                
                handler.post(() -> {
                    if (types == null || types.isEmpty()) {
                        Toast.makeText(activity, "获取数据失败，请检查当前网络情况", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 准备作业类型名称列表和选中状态数组
                    String[] typeNames = new String[types.size()];
                    boolean[] checkedItems = new boolean[types.size()];
                    
                    // 填充数据
                    for (int i = 0; i < types.size(); i++) {
                        typeNames[i] = types.get(i).getWorkName();
                        checkedItems[i] = selectedWorkTypeNames.contains(typeNames[i]);
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                        .setTitle("选择作业类型")
                        .setMultiChoiceItems(typeNames, checkedItems, (dialog, which, isChecked) -> {
                            CornTypeEntity selectedType = types.get(which);
                            if (isChecked) {
                                selectedWorkTypeNames.add(selectedType.getWorkName());
                                selectedWorkTypeIds.add(selectedType.getId());
                            } else {
                                int index = selectedWorkTypeNames.indexOf(selectedType.getWorkName());
                                if (index != -1) {
                                    selectedWorkTypeNames.remove(index);
                                    selectedWorkTypeIds.remove(index);
                                }
                            }
                        })
                        .setPositiveButton("确定", (dialog, which) -> {
                            if (selectedWorkTypeNames.isEmpty()) {
                                conditionWorkType.setText("作业类型");
                            } else {
                                String displayText = String.join("、", selectedWorkTypeNames);
                                if (displayText.length() > 10) {
                                    displayText = displayText.substring(0, 10) + "...";
                                }
                                conditionWorkType.setText(displayText);
                            }
                            
                            // 更新查询条件并刷新数据
                            dto.setWorkTypeId(selectedWorkTypeIds);
                            initData();
                            adapter.update(list);
                        })
                        .setNeutralButton("清除", (dialog, which) -> {
                            selectedWorkTypeNames.clear();
                            selectedWorkTypeIds.clear();
                            conditionWorkType.setText("作业类型");
                            
                            dto.setWorkTypeId(null);
                            initData();
                            adapter.update(list);
                        })
                        .setNegativeButton("取消", null);

                    builder.show();
                });
            });
        });



        conditionCrop.setOnClickListener(v -> {
            ThreadPoolUtils.threadPoolExecutor.execute(() -> {
                crops = QueryUtils.getCrops(getString(R.string.server_url), 
                    getString(R.string.getCrops), 
                    DefaultInfoUtils.getToken(activity));
                CommonUtils.waitDataGetting(crops, activity);
                
                handler.post(() -> {
                    if (crops == null || crops.isEmpty()) {
                        Toast.makeText(activity, "获取数据失败，请检查当前网络情况", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 准备作物名称列表和选中状态数组
                    String[] cropNames = new String[crops.size()];
                    boolean[] checkedItems = new boolean[crops.size()];
                    
                    // 填充数据
                    for (int i = 0; i < crops.size(); i++) {
                        cropNames[i] = crops.get(i).getCropName();
                        // 如果该作物在已选列表中，则标记为选中
                        checkedItems[i] = selectedCropNames.contains(cropNames[i]);
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                        .setTitle("选择作物")
                        .setMultiChoiceItems(cropNames, checkedItems, (dialog, which, isChecked) -> {
                            CornEntity selectedCrop = crops.get(which);
                            if (isChecked) {
                                selectedCropNames.add(selectedCrop.getCropName());
                                selectedCropIds.add(selectedCrop.getId());
                            } else {
                                int index = selectedCropNames.indexOf(selectedCrop.getCropName());
                                if (index != -1) {
                                    selectedCropNames.remove(index);
                                    selectedCropIds.remove(index);
                                }
                            }
                        })
                        .setPositiveButton("确定", (dialog, which) -> {
                            if (selectedCropNames.isEmpty()) {
                                conditionCrop.setText("作物");
                            } else {
                                // 将选中的作物名称用顿号连接显示
                                String displayText = String.join("、", selectedCropNames);
                                if (displayText.length() > 10) {
                                    displayText = displayText.substring(0, 10) + "...";
                                }
                                conditionCrop.setText(displayText);
                            }
                            
                            // 更新查询条件并刷新数据
                            dto.setCropId(selectedCropIds);  // 假设 dto 接受 List<Integer> 作为作物 ID
                            initData();
                            adapter.update(list);
                        })
                        .setNeutralButton("清除", (dialog, which) -> {
                            // 清除所有选择
                            selectedCropNames.clear();
                            selectedCropIds.clear();
                            conditionCrop.setText("作物");
                            
                            // 清除查询条件并刷新数据
                            dto.setCropId(null);
                            initData();
                            adapter.update(list);
                        })
                        .setNegativeButton("取消", null);

                    builder.show();
                });
            });
        });
    }

    private void initViews(View view) {
        gridWorK= view.findViewById(R.id.grid_demand_farm_work);
        //筛选条件
        //时间
        conditionTime = view.findViewById(R.id.tv_condition_time2);
        // 添加其他条件控件
        conditionDeadline = view.findViewById(R.id.tv_condition_deadline);
        conditionPlotArea = view.findViewById(R.id.tv_condition_plot_area);
        conditionWorkArea = view.findViewById(R.id.tv_condition_work_area);
        conditionCrop = view.findViewById(R.id.tv_condition_crop);
        conditionWorkType = view.findViewById(R.id.tv_condition_work_type);
    }

    private void bingEvents(GridView gridWorK) {
        adapter = new GridFarmWorkAdapter(activity, R.layout.grid_farm_work_item, list);
        gridWorK.setAdapter(adapter);
    }

    @Override
    public void onLoadMore(SmartRefreshLayout layout) {
        //            先要确定前一次是否记载了全部数据 如果是全部，继续加载数据 如果没有，就不加载了
        refreshLayout = layout;
        isRefresh = false;
        if (list.size()<total){
            loadMoreData(refreshLayout,(list.size()/6)+1);
        }else {
            refreshLayout.finishLoadMore();
        }
    }

    @Override
    public void onResume() {
        if (refreshLayout!=null){
            refreshLayout.setEnableLoadMore(true);
        }
        super.onResume();
    }

    private void loadMoreData(SmartRefreshLayout layout, int pageNum) {
        ThreadPoolUtils.threadPoolExecutor.execute(()->{
                    ResultData<List<FarmWork>> resultData= QueryUtils.getQueryFarmWork(getString(R.string.server_url),
                            getString(R.string.getFarmWork)+getString(R.string.list),
                            DefaultInfoUtils.getToken(activity),
                            pageNum,
                            dto);
                    while (resultData==null){
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {}
                    }
                    handler.post(()->{
                        // 判断数据是否为空
                        if(!resultData.getData().isEmpty()){
                            //不为空，判断适配器是否为空
                            if(adapter==null){
                                // 如果为空，创建新的适配器
                                adapter = new GridFarmWorkAdapter(activity, R.layout.grid_machinery_item, list);
                                gridWorK.setAdapter(adapter);
                            }else{
                                //通知适配器数据集改变
                                if(isRefresh){
                                    adapter.update(resultData.getData());
                                    layout.finishRefresh();
                                }else {
                                    adapter.loadMore(resultData.getData());
                                    layout.finishLoadMore();
                                }
                            }
                        }else {
                            // 处理没有数据的情况
                            if (isRefresh) {
                                layout.finishRefresh();
                            } else {
                                layout.finishLoadMore();
                            }
                        }
                    });
                }
        );
    }
}
