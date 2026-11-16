package cn.agrinav.measure.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.navigation.NavigationView;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import cn.agrinav.R;
import cn.agrinav.measure.activity.MeasureInfoActivity;
import cn.agrinav.measure.adapter.MeasureFragmentAdapter;
import cn.agrinav.measure.entity.LandPlot;
import cn.agrinav.measure.utils.DefaultInfoUtils;
import cn.agrinav.measure.utils.QueryUtils;
import cn.agrinav.measure.utils.ThreadPoolUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

/**
 * @author 18506
 */
public class FragmentMeasure extends Fragment {
    private boolean isRefresh = true;
    // 页面
    private AppCompatActivity activity;
    //量一下按钮
    private ImageButton buttonMeasure;
    //查一下按钮
    private ImageButton buttonCheck;
    //搜索框
    private SearchView searchView;
    //网格视图
    private GridView gridView;
    //适配器需要的数据
    private List<LandPlot> list = new ArrayList<>();
    //handler
    private Handler handler;
    //侧边栏
    private DrawerLayout drawerLayout;
    //适配器
    private MeasureFragmentAdapter measureFragmentAdapter;
    //筛选条件
    private TextView spinner;
    private TextView spinnerMethod;
    //确认按钮
    private Button buttonConfirm;
    //取消按钮
    private Button buttonCancel;
    //重置按钮
    private Button buttonReset;
    //开始时间text
    private EditText editTimeBegin;
    //结束时间text
    private EditText editTimeEnd;
    //起始面积筛选编辑框
    private EditText editFirstArea;
    //结束面积筛选编辑框
    private EditText editEndArea;
    //搜索框输入的文本
    private String text;
    private RelativeLayout search;
    private RelativeLayout relativeChooseType;
    private RelativeLayout relativeChooseMethod;
    private ImageButton btnLandPlotMethod;
    private ImageButton btnLandPlotType;
    private PopupMenu popupTypeMenu;
    private PopupMenu popupMethodMenu;
    private SmartRefreshLayout slrLandPlot;
    //地址筛选
    private HashMap<String, Object> params;
    //多条件筛选
    private HashMap<String, Object> paramMap;
    private static String token;

    /**这是构造方法*/
    public FragmentMeasure() {
    }
    /**这是含参构造方法*/
    public FragmentMeasure(AppCompatActivity activity) {
        this.activity = activity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.fragment_measure, null);
        //初始化页面组件
        //初始化handler
        initHandler();
        //初始化页面控件
        initView(view);
        //初始化事件
        initEvents();
        return view;
    }

    private void executeTask(Runnable task) {
        if (ThreadPoolUtils.threadPoolExecutor != null && !ThreadPoolUtils.threadPoolExecutor.isShutdown()) {
            try {
                ThreadPoolUtils.threadPoolExecutor.execute(task);
            } catch (RejectedExecutionException e) {
                // 可以在这里添加一些用户反馈，比如显示一个Toast
                activity.runOnUiThread(() -> Toast.makeText(activity, "操作失败，请稍后重试", Toast.LENGTH_SHORT).show());
            }
        } else {
            // 可以在这里添加一些用户反馈
            activity.runOnUiThread(() -> Toast.makeText(activity, "系统忙，请稍后重试", Toast.LENGTH_SHORT).show());
        }
    }

    //初始化handler
    private void initHandler() {
        //拿需要的数据
        handler = new Handler(Looper.getMainLooper()) {
            //获取新数据
            List<LandPlot> landPlotList = new ArrayList<>();
            @Override
            public void handleMessage(@NonNull Message msg) {
                //处理图像与控件绑定
                if (msg.what == 1 && msg.obj != null) {
                    // 假设 msg 是从某个消息或数据源获取的对象
                    if (msg.obj instanceof List) {
                        List<?> tempList = (List<?>) msg.obj;
                        if (!tempList.isEmpty() && tempList.get(0) instanceof LandPlot) {
                            // 安全地进行类型转换
                            landPlotList = tempList.stream()
                                    .map(item -> (LandPlot) item)
                                    .collect(Collectors.toList());
                            // 添加到列表
                            list.addAll(landPlotList);
                        }
                    }
                    // 绑定GridView
                    bindingEvent(gridView);
                }
                if (msg.what == 2 && msg.obj != null) {
                    // 假设 msg 是从某个消息或数据源获取的对象
                    if (msg.obj instanceof List) {
                        List<?> tempList = (List<?>) msg.obj;
                        if (!tempList.isEmpty() && tempList.get(0) instanceof LandPlot) {
                            list.clear();
                            // 安全地进行类型转换
                            landPlotList = tempList.stream()
                                    .map(item -> (LandPlot) item)
                                    .collect(Collectors.toList());
                            // 添加到列表
                            list.addAll(landPlotList);
                        }
                    }
                    // 绑定GridView
                    bindingEvent(gridView);
                }
            }
        };
    }

    //滚动到最后加载数据
    private void loadMoreData(int page) {
        if (ThreadPoolUtils.threadPoolExecutor == null || ThreadPoolUtils.threadPoolExecutor.isShutdown()) {
            ThreadPoolUtils.initThreadPool();
        }
        if (params!=null&&!params.isEmpty()){
            // 加载更多数据的逻辑
            executeTask(() -> {
                // 在后台执行任务
                List<LandPlot> landPlotList = QueryUtils.queryLandPlot(getResources().getString(R.string.server_url),
                        getResources().getString(R.string.measure_url) + getResources().getString(R.string.query_url), token, params,page);
                while (landPlotList == null) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                    }
                }
                handler.post(() -> {
                    //在主线程中更新UI
                    if (!landPlotList.isEmpty()) {
                        if (measureFragmentAdapter == null) {
                            // 如果适配器为空，创建新的适配器
                            measureFragmentAdapter = new MeasureFragmentAdapter(getActivity(), R.layout.layout_item, list);
                            gridView.setAdapter(measureFragmentAdapter);
                        } else {
                            //通知适配器数据集改变
                            if (isRefresh){
                                measureFragmentAdapter = new MeasureFragmentAdapter(getActivity(), R.layout.layout_item, list);
                                gridView.setAdapter(measureFragmentAdapter);
                                measureFragmentAdapter.update(landPlotList);
                                slrLandPlot.finishRefresh();
                            }else{
                                measureFragmentAdapter.loadMore(landPlotList);
                                slrLandPlot.finishLoadMore();
                            }
                        }
                    } else {
                        // 处理没有数据的情况
                        if (isRefresh) {
                            slrLandPlot.finishRefresh();
                        } else {
                            slrLandPlot.finishLoadMore();
                        }
                    }
                });
            });
        } else if (paramMap!=null&&!paramMap.isEmpty()) {
            // 加载更多数据的逻辑
            executeTask(() -> {
                // 在后台执行任务
                List<LandPlot> landPlotList = QueryUtils.queryLandPlot(getResources().getString(R.string.server_url), getResources().getString(R.string.measure_url)+getResources().getString(R.string.query_url), token, paramMap,page);
                while (landPlotList == null) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                    }
                }
                handler.post(() -> {
                    //在主线程中更新UI
                    if (!landPlotList.isEmpty()) {
                        if (measureFragmentAdapter == null) {
                            // 如果适配器为空，创建新的适配器
                            measureFragmentAdapter = new MeasureFragmentAdapter(getActivity(), R.layout.layout_item, list);
                            gridView.setAdapter(measureFragmentAdapter);
                        } else {
                            //通知适配器数据集改变
                            if (isRefresh){
                                measureFragmentAdapter = new MeasureFragmentAdapter(getActivity(), R.layout.layout_item, list);
                                gridView.setAdapter(measureFragmentAdapter);
                                measureFragmentAdapter.update(landPlotList);
                                slrLandPlot.finishRefresh();
                            }else{
                                measureFragmentAdapter.loadMore(landPlotList);
                                slrLandPlot.finishLoadMore();
                            }
                        }
                    } else {
                        // 处理没有数据的情况
                        if (isRefresh) {
                            slrLandPlot.finishRefresh();
                        } else {
                            slrLandPlot.finishLoadMore();
                        }
                    }
                });
            });
        } else{
            // 加载更多数据的逻辑
            executeTask(() -> {
                // 在后台执行任务
                List<LandPlot> landPlotList = QueryUtils.getLandPlotList(getResources().getString(R.string.server_url), getResources().getString(R.string.measure_url), token, page);
                while (landPlotList == null) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        Log.i("sleep", "sleep error");
                    }
                }
                handler.post(() -> {
                    //在主线程中更新UI
                    if (!landPlotList.isEmpty()) {
                        if (measureFragmentAdapter == null) {
                            // 如果适配器为空，创建新的适配器
                            measureFragmentAdapter = new MeasureFragmentAdapter(getActivity(), R.layout.layout_item, list);
                            gridView.setAdapter(measureFragmentAdapter);
                        } else {
                            //通知适配器数据集改变
                            if (isRefresh){
                                measureFragmentAdapter = new MeasureFragmentAdapter(getActivity(), R.layout.layout_item, list);
                                gridView.setAdapter(measureFragmentAdapter);
                                measureFragmentAdapter.update(landPlotList);
                                slrLandPlot.finishRefresh();
                            }else{
                                measureFragmentAdapter.loadMore(landPlotList);
                                slrLandPlot.finishLoadMore();
                            }
                        }
                    } else {
                        // 处理没有数据的情况
                        if (isRefresh) {
                            slrLandPlot.finishRefresh();
                        } else {
                            slrLandPlot.finishLoadMore();
                        }
                    }
                });

            });
        }

    }


    //绑定适配器
    private void bindingEvent(GridView gridView) {
        measureFragmentAdapter = new MeasureFragmentAdapter(getActivity(), R.layout.layout_item, list);
        gridView.setAdapter(measureFragmentAdapter);
    }

    private void initEvents() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            //点击默认自带的搜索按钮
            @Override
            public boolean onQueryTextSubmit(String query) {
                text = query;
                return false;
            }
            // 输入框内容改变时，自动查询
            @Override
            public boolean onQueryTextChange(String newText) {
                text = newText;
                return false;
            }
        });
        search.setOnClickListener(v -> {
            params = new HashMap<>(4);
            params.put("address", text);
            params.put("isCurrent",true);
            ThreadPoolUtils.initThreadPool();
            //写搜索的事件
            executeTask(() -> {
                List<LandPlot> landPlotList = QueryUtils.queryLandPlot(getResources().getString(R.string.server_url),
                        getResources().getString(R.string.measure_url) + getResources().getString(R.string.query_url),
                        DefaultInfoUtils.getToken(activity), params);
                if (landPlotList==null){
                    landPlotList = new ArrayList<>();
                }
                List<LandPlot> finalList = landPlotList;
                handler.post(() -> {
                    //在主线程中更新UIF
                    //通知适配器数据集改变
                    if(measureFragmentAdapter==null){
                        measureFragmentAdapter = new MeasureFragmentAdapter(getActivity(), R.layout.layout_item, list);
                        gridView.setAdapter(measureFragmentAdapter);
                    }
                    measureFragmentAdapter.update(finalList);
                    slrLandPlot.finishRefresh();
                });
            });
            Toast.makeText(activity, getResources().getString(R.string.query_end), Toast.LENGTH_SHORT).show();
        });

        buttonMeasure.setOnClickListener(v -> {
            Intent intent = new Intent(activity, MeasureInfoActivity.class);
            intent.putExtra("token", token);
            startActivity(intent);
        });
        slrLandPlot.setOnRefreshListener(refreshlayout -> {
            //刷新数据
            isRefresh = true;
            loadMoreData(1);
            slrLandPlot.finishRefresh(true);
        });
        slrLandPlot.setOnLoadMoreListener(refreshLayout -> {
//            先要确定前一次是否记载了全部数据 如果是全部，继续加载数据 如果没有，就不加载了
            isRefresh = false;
            if (list.size()%6==0&& !list.isEmpty()){
                //当鼠标滚动停止并且视图控件处于最后一个位置的时候加载新的数据
                loadMoreData((list.size() / 6)+1);
            }else{
                slrLandPlot.finishLoadMore(false);
            }
        });
        //筛选按钮
        buttonCheck.setOnClickListener(v -> {
             //点击筛选按钮之后首先展开navigation
            drawerLayout.openDrawer(GravityCompat.END);
        });
        //选择地块类型
        relativeChooseType.setOnClickListener(v -> {
            //点击每一个子项的事件
            popupTypeMenu.setOnMenuItemClickListener(m->{
                spinner.setText(m.getTitle());
                return true;
            });
            //显示菜单
            popupTypeMenu.show();
        });
        //按钮选择地块类型
        btnLandPlotType.setOnClickListener(v -> {
            //点击每一个子项的事件
            popupTypeMenu.setOnMenuItemClickListener(m->{
                spinner.setText(m.getTitle());
                return true;
            });
            //显示菜单
            popupTypeMenu.show();
        });
        //选择地块方法
        relativeChooseMethod.setOnClickListener(v -> {
            //点击每一个子项的事件
            popupMethodMenu.setOnMenuItemClickListener(m->{
                spinnerMethod.setText(m.getTitle());
                return true;
            });
            //显示菜单
            popupMethodMenu.show();
        });
        //按钮选择地块方法
        btnLandPlotMethod.setOnClickListener(v -> {
            //点击每一个子项的事件
            popupMethodMenu.setOnMenuItemClickListener(m->{
                spinnerMethod.setText(m.getTitle());
                return true;
            });
            //显示菜单
            popupMethodMenu.show();
        });
        //开始时间选择
        editTimeBegin.setOnClickListener(v -> initDatePickerView(editTimeBegin));
        //结束时间
        editTimeEnd.setOnClickListener(v -> initDatePickerView(editTimeEnd));

        // 面积输入实时校验
        editFirstArea.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString().trim();
                if (!input.isEmpty()) {
                    try {
                        double value = Double.parseDouble(input);
                        if (value < 0) {
                            editFirstArea.setError("面积不能为负数");
                            return;
                        }
                        
                        // 检查结束面积
                        String endAreaStr = editEndArea.getText().toString().trim();
                        if (!endAreaStr.isEmpty()) {
                            double endValue = Double.parseDouble(endAreaStr);
                            if (value > endValue) {
                                editFirstArea.setError("起始面积不能大于结束面积");
                            }
                        }
                    } catch (NumberFormatException e) {
                        editFirstArea.setError("请输入有效的数字");
                    }
                }
            }
        });

        editEndArea.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString().trim();
                if (!input.isEmpty()) {
                    try {
                        double value = Double.parseDouble(input);
                        if (value < 0) {
                            editEndArea.setError("面积不能为负数");
                            return;
                        }
                        
                        // 检查起始面积
                        String beginAreaStr = editFirstArea.getText().toString().trim();
                        if (!beginAreaStr.isEmpty()) {
                            double beginValue = Double.parseDouble(beginAreaStr);
                            if (value < beginValue) {
                                editEndArea.setError("结束面积不能小于起始面积");
                            }
                        }
                    } catch (NumberFormatException e) {
                        editEndArea.setError("请输入有效的数字");
                    }
                }
            }
        });
        // 开始时间实时校验
        editTimeBegin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String startTime = s.toString();
                String endTime = editTimeEnd.getText().toString();
                
                if (!startTime.isEmpty() && !endTime.isEmpty()) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                        Date start = sdf.parse(startTime);
                        Date end = sdf.parse(endTime);
                        
                        if (start != null && end != null) {
                            if (start.after(end)) {
                                editTimeBegin.setError("开始时间不能晚于结束时间");
                            } else {
                                editTimeBegin.setError(null);
                                editTimeEnd.setError(null);
                            }
                        }
                    } catch (Exception e) {
                        editTimeBegin.setError("日期格式应为 yyyy/MM/dd");
                    }
                }
            }
        });

        // 结束时间实时校验 (更新原有的代码)
        editTimeEnd.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String endTime = s.toString();
                String startTime = editTimeBegin.getText().toString();
                
                if (!endTime.isEmpty() && !startTime.isEmpty()) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                        Date start = sdf.parse(startTime);
                        Date end = sdf.parse(endTime);
                        
                        if (start != null && end != null) {
                            if (end.before(start)) {
                                editTimeEnd.setError("结束时间不能早于开始时间");
                            } else {
                                editTimeBegin.setError(null);
                                editTimeEnd.setError(null);
                            }
                        }
                    } catch (Exception e) {
                        editTimeEnd.setError("日期格式应为 yyyy/MM/dd");
                    }
                }
            }
        });

        // 地块类型和测量方法的选择监听
        popupTypeMenu.setOnMenuItemClickListener(m -> {
            spinner.setText(m.getTitle());
            return true;
        });

        popupMethodMenu.setOnMenuItemClickListener(m -> {
            spinnerMethod.setText(m.getTitle());
            return true;
        });

        // 确认按钮点击事件修改
        buttonConfirm.setOnClickListener(v -> {
            // 最终校验，确保所有输入都是有效的
            if (hasValidationErrors()) {
                Toast.makeText(activity, "请修正输入错误后再提交", Toast.LENGTH_SHORT).show();
                return;
            }
            // 执行原有的查询逻辑
            paramMap = new HashMap<>(12);
            paramMap.put("landType", spinner.getText().toString());
            paramMap.put("startTime", editTimeBegin.getText().toString());
            paramMap.put("endTime", editTimeEnd.getText().toString());
            paramMap.put("beginArea", editFirstArea.getText().toString());
            paramMap.put("endArea", editEndArea.getText().toString());
            paramMap.put("measurementMethod", spinnerMethod.getText().toString());
            paramMap.put("current",1);
            paramMap.put("size",6);
            executeTask(() -> {
                List<LandPlot> list = QueryUtils.queryLandPlot(getResources().getString(R.string.server_url),
                        getResources().getString(R.string.measure_url) + getResources().getString(R.string.query_url),
                        DefaultInfoUtils.getToken(activity),
                        paramMap);
                handler.post(() -> {
                    //在主线程中更新UIF
                    //通知适配器数据集改变
                    if (measureFragmentAdapter == null){
                        measureFragmentAdapter = new MeasureFragmentAdapter(getActivity(), R.layout.layout_item, list);
                    }
                    measureFragmentAdapter.update(list);
                    slrLandPlot.finishRefresh();
                });
            });
            Toast.makeText(activity, getResources().getString(R.string.query_end), Toast.LENGTH_SHORT).show();
            // 处理取消操作
            drawerLayout.closeDrawer(GravityCompat.END);
        });

        //// 处理取消操作
        buttonCancel.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.END));
        buttonReset.setOnClickListener(v -> clear());
    }

    // 辅助方法：检查是否存在验证错误
    private boolean hasValidationErrors() {
        boolean hasError = editFirstArea.getError() != null || editEndArea.getError() != null;

        if (editTimeBegin.getError() != null || editTimeEnd.getError() != null) {
            hasError = true;
        }

        return hasError;
    }

    //初始化时间选择器，并点击确定按钮后，获取时间
    private void initDatePickerView(TextView textView) {
        //获取时间选择器，设置选择器的标题，设置选择器的选中时间，设置选择器的输入模式
        MaterialDatePicker<Long> timePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getResources().getString(R.string.date_begin_time))
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
                .build();
        //设置确认按钮的点击事件
        timePicker.addOnPositiveButtonClickListener(selection -> {
            long selectedDate = selection;
            // 使用 Calendar 来格式化日期
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selectedDate);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            String formattedDate = sdf.format(calendar.getTime());
            textView.setText(formattedDate);
        });
        timePicker.show(activity.getSupportFragmentManager(), "TIME_BEGIN_PICKER");
    }

    private void initView(View view) {
        //侧边栏
        drawerLayout = view.findViewById(R.id.drawerLayout);
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {

            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {

            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                clear();
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
        //搜索框
        search = view.findViewById(R.id.Search);
        //量一下按钮
        buttonMeasure = view.findViewById(R.id.buttonMeasure);
        //查一下按钮
        buttonCheck = view.findViewById(R.id.buttonCheck);
        //搜索框
        searchView = view.findViewById(R.id.searchView);
        //列表
        slrLandPlot = view.findViewById(R.id.srl_landPlot);
        gridView = view.findViewById(R.id.gridView);
        // 使用新的导航视图
        NavigationView navigationView = view.findViewById(R.id.nav_measure_select);
        initNavigation(navigationView.getHeaderView(0));
    }

    private void initNavigation(View view) {
         //选择类型
        relativeChooseType = view.findViewById(R.id.rl_choose_type);
        //绑定菜单
        popupTypeMenu = new PopupMenu(view.getContext(), relativeChooseType);
        //获取资源
        popupTypeMenu.getMenuInflater().inflate(R.menu.menu_land_plot_type, popupTypeMenu.getMenu());
        //选择方法
        relativeChooseMethod = view.findViewById(R.id.rl_choose_method);
        //绑定菜单
        popupMethodMenu = new PopupMenu(view.getContext(), relativeChooseMethod);
        //获取资源
        popupMethodMenu.getMenuInflater().inflate(R.menu.menu_land_plot_method, popupMethodMenu.getMenu());
        //textView
        spinner = view.findViewById(R.id.spinner);
        spinnerMethod = view.findViewById(R.id.spinner_method);
        //选择按钮
        btnLandPlotType = view.findViewById(R.id.btn_Type);
        btnLandPlotMethod = view.findViewById(R.id.btn_method);
        //确定按钮
        buttonConfirm = view.findViewById(R.id.buttonConfirm);
        //取消按钮
        buttonCancel = view.findViewById(R.id.buttonCancel);
        //重置按钮
        buttonReset = view.findViewById(R.id.buttonReset);
        buttonReset = view.findViewById(R.id.buttonReset);
        //开始时间
        editTimeBegin = view.findViewById(R.id.textTimeBegin);
        editTimeBegin.setFocusable(false);
        editTimeBegin.setKeyListener(null);
        editTimeBegin.setClickable(true);
        editTimeBegin.setFocusableInTouchMode(false);
        //结束时间
        editTimeEnd = view.findViewById(R.id.textTimeEnd);
        editTimeEnd.setFocusable(false);
        editTimeEnd.setKeyListener(null);
        editTimeEnd.setClickable(true);
        editTimeEnd.setFocusableInTouchMode(false);
        //面积
        editFirstArea = view.findViewById(R.id.editFirstArea);
        editEndArea = view.findViewById(R.id.editEndArea);
    }

    @Override
    public void onStart() {
        super.onStart();
        token = DefaultInfoUtils.getToken(activity);
        ThreadPoolUtils.initThreadPool();
    }

    @Override
    public void onPause() {
        super.onPause();
        clear();
        drawerLayout.closeDrawer(GravityCompat.END);
    }

    @Override
    public void onResume() {
        super.onResume();
        isRefresh = true;
        ThreadPoolUtils.initThreadPool();
        refreshData();
    }

    private void clear(){
        // 清空搜索框内容
        searchView.setQuery("", false);
        // 清除搜索框焦点
        searchView.clearFocus();
        // 清空搜索文本
        text = "";
        editFirstArea.setText("");
        editEndArea.setText("");
        editTimeBegin.setText("");
        editTimeEnd.setText("");
        spinner.setText("");
        spinnerMethod.setText("");
        //清除错误信息
        editFirstArea.setError(null);
        editEndArea.setError(null);
        editTimeBegin.setError(null);
        editTimeEnd.setError(null);
        spinner.setError(null);
        spinnerMethod.setError(null);
    }

    private void refreshData() {
        executeTask(() -> {
            List<LandPlot> landPlotList = QueryUtils.getLandPlotList(getString(R.string.server_url), getString(R.string.measure_url)+getString(R.string.query_url), token, 1);
            while (landPlotList == null) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Log.i("sleep", "sleep error");
                }
            }
            handler.post(() -> {
                //在主线程中更新UI
                if (!landPlotList.isEmpty()) {
                    list.clear();
                    list.addAll(landPlotList);
                    if (measureFragmentAdapter == null) {
                        // 如果适配器为空，创建新的适配器
                        measureFragmentAdapter = new MeasureFragmentAdapter(getActivity(), R.layout.layout_item, list);
                        gridView.setAdapter(measureFragmentAdapter);
                    } else {
                        //通知适配器数据集改变
                        measureFragmentAdapter = new MeasureFragmentAdapter(getActivity(), R.layout.layout_item, new ArrayList<>());
                        gridView.setAdapter(measureFragmentAdapter);
                        measureFragmentAdapter.update(landPlotList);
                        slrLandPlot.finishRefresh();
                    }
                } else {
                    // 处理没有数据的情况
                    if (isRefresh) {
                        slrLandPlot.finishRefresh();
                    } else {
                        slrLandPlot.finishLoadMore();
                    }
                }
            });
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ThreadPoolUtils.shutdownThreadPool();
    }
}
