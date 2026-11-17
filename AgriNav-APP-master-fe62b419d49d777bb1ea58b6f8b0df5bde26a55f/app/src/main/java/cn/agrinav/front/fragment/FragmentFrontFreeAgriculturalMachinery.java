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
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;

import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.agrinav.R;
import cn.agrinav.front.adapter.GridMachineryAdapter;
import cn.agrinav.front.entity.AgriculturalMachinery;
import cn.agrinav.front.entity.AgriculturalMachineryType;
import cn.agrinav.front.entity.dto.AgriculturalMachineryDTO;
import cn.agrinav.front.util.CommonUtils;
import cn.agrinav.front.util.QueryUtils;
import cn.agrinav.front.util.ResultData;
import cn.agrinav.measure.utils.DefaultInfoUtils;
import cn.agrinav.measure.utils.ThreadPoolUtils;
import cn.agrinav.trajectory.entity.vo.CornEntity;

/**空闲农机的fragment页面
 * @author 18506
 */
public class FragmentFrontFreeAgriculturalMachinery extends MyFragment{
    private AppCompatActivity activity;
    private GridView gridMachinery;
    private SharedPreferences sharedPreferences;
    private Handler handler = new Handler(Looper.getMainLooper());
    //记录总数据条数
    private Integer total;
    //刷新的判断标准
    private boolean isRefresh = true;
    //适配器需要的数据
    private List<AgriculturalMachinery> list = new ArrayList<>();
    private AgriculturalMachineryDTO dto;
    private GridMachineryAdapter adapter;
    private TextView conditionModel;
    private TextView conditionCanArea;
    private TextView conditionCrops;
    private TextView conditionGrainTruck;
    private TextView conditionTime;
    private PopupMenu popupMenuConditionModel;
    private PopupMenu popupMenuConditionCanArea;
    private PopupMenu popupMenuConditionCrops;
    private List<AgriculturalMachineryType> models;
    private List<CornEntity> crops;
    private List<Long> cropId = new ArrayList<>();
    // 添加一个存储可作业区域的列表
    private List<String> workAreaList = new ArrayList<>();

    private SmartRefreshLayout refreshLayout;

    //点击配备运梁车的次数
    private int grainTruckCount = 0;
    //点击时间的次数
    private int timeCount = 0;

    private List<String> selectedModelNames = new ArrayList<>();
    private List<String> selectedModelIds = new ArrayList<>();
    private List<String> selectedCropNames = new ArrayList<>();
    private List<Long> selectedCropIds = new ArrayList<>();

    public FragmentFrontFreeAgriculturalMachinery(){

    }
    public FragmentFrontFreeAgriculturalMachinery(SoftReference<AppCompatActivity> activityWeakReference, SharedPreferences sharedPreferences){
        this.sharedPreferences=sharedPreferences;
        this.activity=activityWeakReference.get();
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.fragment_free_agricultural_machinery, null);
        dto = new AgriculturalMachineryDTO();
        dto.setStatus(0);
        dto.setOrderByCreateTimeDesc(true);
        initViews(view);
        initEvents();
        initData();
        return view;
    }

    private void initData() {
        ThreadPoolUtils.threadPoolExecutor.execute(()->{
            ResultData<List<AgriculturalMachinery>> resultData= QueryUtils.getQueryFreeAgriculturalMachinery(getString(R.string.server_url), getString(R.string.getAgriculturalMachinery)+getString(R.string.list), DefaultInfoUtils.getToken(activity), 1, dto);
            while (resultData==null){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            handler.post(()->{
                total = resultData.getTotal();
                if(!list.isEmpty()){
                    list.clear();
                }
                // 接口失败或无数据时 getData() 可能为 null，直接 addAll(null) 会触发空指针崩溃，这里判空保护
                if (resultData.getData() != null){
                    list.addAll(resultData.getData());
                }
                bingEvents(gridMachinery);
            });
        });
    }

    private void initEvents() {
        //条件筛选
        conditionModel.setOnClickListener(v -> {
            ThreadPoolUtils.threadPoolExecutor.execute(() -> {
                models = QueryUtils.getMachineryModel(
                    getString(R.string.server_url),
                    getString(R.string.getAgriculturalMachineryType)+getString(R.string.list),
                    DefaultInfoUtils.getToken(activity));
                CommonUtils.waitDataGetting(models, activity);
                
                handler.post(() -> {
                    if (models == null || models.isEmpty()) {
                        Toast.makeText(activity, "获取数据失败，请检查当前网络情况", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] modelNames = new String[models.size()];
                    boolean[] checkedItems = new boolean[models.size()];
                    
                    for (int i = 0; i < models.size(); i++) {
                        modelNames[i] = models.get(i).getName();
                        checkedItems[i] = selectedModelNames.contains(modelNames[i]);
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                        .setTitle("选择农机型号")
                        .setMultiChoiceItems(modelNames, checkedItems, (dialog, which, isChecked) -> {
                            AgriculturalMachineryType selectedModel = models.get(which);
                            if (isChecked) {
                                selectedModelNames.add(selectedModel.getName());
                                selectedModelIds.add(selectedModel.getId());
                            } else {
                                int index = selectedModelNames.indexOf(selectedModel.getName());
                                if (index != -1) {
                                    selectedModelNames.remove(index);
                                    selectedModelIds.remove(index);
                                }
                            }
                        })
                        .setPositiveButton("确定", (dialog, which) -> {
                            if (selectedModelNames.isEmpty()) {
                                conditionModel.setText("农机型号");
                            } else {
                                String displayText = String.join("、", selectedModelNames);
                                if (displayText.length() > 10) {
                                    displayText = displayText.substring(0, 10) + "...";
                                }
                                conditionModel.setText(displayText);
                            }
                            
                            dto.setTypeId(String.valueOf(selectedModelIds.isEmpty() ? null : selectedModelIds.get(0))); // 暂时只取第一个，如果需要多选需要修改DTO
                            initData();
                            adapter.update(list);
                        })
                        .setNeutralButton("清除", (dialog, which) -> {
                            selectedModelNames.clear();
                            selectedModelIds.clear();
                            conditionModel.setText("农机型号");
                            
                            dto.setTypeId(null);
                            initData();
                            adapter.update(list);
                        })
                        .setNegativeButton("取消", null);

                    builder.show();
                });
            });
        });
        popupMenuConditionModel.setOnMenuItemClickListener(item -> {
            for (AgriculturalMachineryType model: models){
                if(Objects.equals(item.getTitle(), model.getName())){
                    dto.setTypeId(model.getId());
                    break;
                }
            }
            initData();
            adapter.update(list);
            return true;
        });
        conditionCanArea.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("输入可作业地址");
            
            // 创建输入框
            final EditText input = new EditText(activity);
            input.setInputType(InputType.TYPE_CLASS_TEXT);  // 改为文本输入
            builder.setView(input);

            builder.setPositiveButton("确定", (dialog, which) -> {
                String value = input.getText().toString();
                if (!value.isEmpty()) {
                    workAreaList.add(value);  // 将输入的地址添加到列表中
                    dto.setLocation(workAreaList);  // 设置到 DTO 中
                    
                    // 更新显示的文本
                    String displayText = String.format("已选择 %d 个地址", workAreaList.size());
                    conditionCanArea.setText(displayText);
                    
                    initData();
                    adapter.update(list);
                }
            });
            
            builder.setNeutralButton("清空选择", (dialog, which) -> {
                workAreaList.clear();
                dto.setLocation(null);
                conditionCanArea.setText("可作业地址");
                initData();
                adapter.update(list);
            });
            
            builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
            builder.show();
        });
        conditionCrops.setOnClickListener(v -> {
            ThreadPoolUtils.threadPoolExecutor.execute(() -> {
                crops = QueryUtils.getCrops(
                    getString(R.string.server_url), 
                    getString(R.string.getCrops), 
                    DefaultInfoUtils.getToken(activity));
                CommonUtils.waitDataGetting(crops, activity);
                
                handler.post(() -> {
                    if (crops == null || crops.isEmpty()) {
                        Toast.makeText(activity, "获取数据失败，请检查当前网络情况", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] cropNames = new String[crops.size()];
                    boolean[] checkedItems = new boolean[crops.size()];
                    
                    for (int i = 0; i < crops.size(); i++) {
                        cropNames[i] = crops.get(i).getCropName();
                        checkedItems[i] = selectedCropNames.contains(cropNames[i]);
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                        .setTitle("选择可作业作物")
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
                                conditionCrops.setText("可作业作物");
                            } else {
                                String displayText = String.join("、", selectedCropNames);
                                if (displayText.length() > 10) {
                                    displayText = displayText.substring(0, 10) + "...";
                                }
                                conditionCrops.setText(displayText);
                            }
                            
                            dto.setCropId(selectedCropIds.isEmpty() ? null : selectedCropIds); // 暂时只取第一个，如果需要多选需要修改DTO
                            initData();
                            adapter.update(list);
                        })
                        .setNeutralButton("清除", (dialog, which) -> {
                            selectedCropNames.clear();
                            selectedCropIds.clear();
                            conditionCrops.setText("可作业作物");
                            
                            dto.setCropId(null);
                            initData();
                            adapter.update(list);
                        })
                        .setNegativeButton("取消", null);

                    builder.show();
                });
            });
        });
        popupMenuConditionCrops.setOnMenuItemClickListener(item -> {
            for (CornEntity corn: crops){
                if(Objects.equals(item.getTitle(), corn.getCropName())){
                    cropId.add(corn.getId());
                    dto.setCropId(cropId);
                    break;
                }
            }
            initData();
            adapter.update(list);
            return true;
        });
        conditionGrainTruck.setOnClickListener(v -> {
            grainTruckCount ++;
            dto.setHavingGrainCarrier(grainTruckCount % 2 != 0);
            conditionGrainTruck.setTypeface(null, grainTruckCount % 2 != 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            initData();
            adapter.update(list);
        });
        conditionTime.setOnClickListener(v -> {
            timeCount++;
            dto.setOrderByCreateTimeDesc(timeCount % 2 == 0);
            initData();
            adapter.update(list);
        });
    }

    private void initViews(View view) {
        //条件筛选
        conditionModel = view.findViewById(R.id.tv_condition_model);
        conditionCanArea = view.findViewById(R.id.tv_condition_can_area);
        conditionCrops = view.findViewById(R.id.tv_condition_crops);
        conditionGrainTruck = view.findViewById(R.id.tv_condition_grain_truck);
        conditionTime = view.findViewById(R.id.tv_condition_time1);
        //获取菜单
        popupMenuConditionModel = new PopupMenu(activity, conditionModel);
        popupMenuConditionCanArea = new PopupMenu(activity, conditionCanArea);
        popupMenuConditionCrops = new PopupMenu(activity, conditionCrops);
        //滑动页面
        gridMachinery = view.findViewById(R.id.grid_machinery);
    }

    private void bingEvents(GridView gridMachinery) {
        adapter = new GridMachineryAdapter(activity, R.layout.grid_machinery_item, list);
        gridMachinery.setAdapter(adapter);
    }
    @Override
    public void onStart() {
        super.onStart();
        ThreadPoolUtils.initThreadPool();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (refreshLayout!=null){
            refreshLayout.setEnableLoadMore(true);
        }
        ThreadPoolUtils.initThreadPool();
        initData();
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ThreadPoolUtils.shutdownThreadPool();
    }

    //加载更多的方法
    @Override
    public void onLoadMore(SmartRefreshLayout layout) {
        refreshLayout = layout;
//            先要确定前一次是否记载了全部数据 如果是全部，继续加载数据 如果没有，就不加载了
        isRefresh = false;
        if (list.size()<total){
            loadMoreData(refreshLayout,(list.size()/6)+1);
        }else {
            refreshLayout.finishLoadMore();
        }
    };

    private void loadMoreData(SmartRefreshLayout layout,int pageNum) {
        ThreadPoolUtils.threadPoolExecutor.execute(()->{
                    ResultData<List<AgriculturalMachinery>> resultData= QueryUtils.getQueryFreeAgriculturalMachinery(getString(R.string.server_url),
                            getString(R.string.getAgriculturalMachinery)+getString(R.string.list),
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
                                adapter = new GridMachineryAdapter(activity, R.layout.grid_machinery_item, list);
                                gridMachinery.setAdapter(adapter);
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
