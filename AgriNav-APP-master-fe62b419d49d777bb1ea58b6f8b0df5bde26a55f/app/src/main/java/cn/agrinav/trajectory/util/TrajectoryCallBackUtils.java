package cn.agrinav.trajectory.util;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.widget.Button;
import android.widget.GridView;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.BaseHoleOptions;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.PolygonHoleOptions;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.layout.api.RefreshLayout;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.front.database.entity.LandPlot;
import cn.agrinav.trajectory.adapter.ItemWorkAdapter;
import cn.agrinav.trajectory.adapter.ReloadAdapter;
import cn.agrinav.trajectory.dao.TrajectoryDao;
import cn.agrinav.trajectory.dao.WorkTrajectoryPositionDao;
import cn.agrinav.trajectory.database.AbstractTrajectoryDatabase;
import cn.agrinav.trajectory.entity.ItemWork;
import cn.agrinav.trajectory.entity.Point;
import cn.agrinav.trajectory.entity.ResultLandPlot;
import cn.agrinav.trajectory.entity.Trajectory;
import cn.agrinav.trajectory.entity.TrajectoryExcludePoint;
import cn.agrinav.trajectory.entity.vo.ResultLandPlotByOrigin;
import cn.agrinav.trajectory.entity.vo.TrajectoryPointExcludeOnPlot;
import cn.agrinav.trajectory.entity.vo.TrajectoryResult;
import cn.agrinav.trajectory.entity.vo.CornEntity;
import cn.agrinav.trajectory.entity.vo.WorkTrajectoryPosition;
import cn.agrinav.trajectory.entity.vo.WorkTypeEntity;
import cn.agrinav.utils.Result;
import cn.agrinav.utils.ToastUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * @author 11230
 */
public class TrajectoryCallBackUtils {
    /**
     * 状态码
     */
    private final int successCode;
    /**
     * 一个工具类，用于提示
     */
    private final ToastUtil toastUtil;
    /**
     * 获取实例以及必要的参数
     * @param successCode 请求的正确码
     */
    public TrajectoryCallBackUtils(int successCode){
        this.successCode = successCode;
        toastUtil = new ToastUtil();
    }
    //下面的todo是为了防止内存泄漏
    //TODO:这个方法在使用的时候要在OnDestroy方法中销毁：调用callback.cancel();
    /**
     * 这个方法用于获取作物的种类，给让hashMap中的数据发生变化
     * @param cornHashMap 这个是作物映射关系要保留下来
     * @return 返回获取作物种类的回调方法用于发送请求的时候添加回调方法
     */
    public Callback getCornCallBack(ConcurrentHashMap<String, Long> cornHashMap, PopupMenu popupMenuCorn, WeakReference<AppCompatActivity> activity) {
        // 创建一个 Handler，用于在主线程中处理 UI 更新
        Handler handler = new Handler(Looper.getMainLooper());
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                //获取到activity
                AppCompatActivity activityRef = activity.get();
                // 检测是否回收了 activity
                if (isActivityValid(activityRef)) {
                    //activity被回收了就直接终止
                    return;
                }
                // 获取响应状态码
                if (response.code() == successCode) {
                    //获取请求体
                    if (response.body() != null) {
                        //首先提取请求体字符串
                        String responseString = response.body().string();
                        Gson gson = new Gson();
                        try {
                            // 返回的结果体是作物集合
                            Result<ArrayList<CornEntity>> resultObj = gson.fromJson(responseString, new TypeToken<Result<ArrayList<CornEntity>>>() {}.getType());
                            //判断服务器是否可以正确的返回数据
                            if (resultObj.getCode() == successCode) {
                                // 在主线程中更新 UI和变量
                                handler.post(() -> {// 清空菜单和hashMap,菜单呈现给用户查看，hashMap为了联动作业类型
                                    Menu menuCorn = popupMenuCorn.getMenu();
                                    menuCorn.clear();
                                    cornHashMap.clear();
                                    // 更新菜单和哈希图
                                    if (resultObj.getData()!=null&&!resultObj.getData().isEmpty()) {
                                        resultObj.getData().forEach(cornEntity -> {
                                            menuCorn.add(Menu.NONE, Menu.NONE, menuCorn.size(), cornEntity.getCropName());
                                            cornHashMap.put(cornEntity.getCropName(), cornEntity.getId());
                                        });
                                    }else{
                                        //如果解析出来的状态数据就是空就提示童虎
                                         showToast(activityRef, activityRef.getString(R.string.toast_err_data));
                                    }
                                });
                            } else {
                                //如果没有正确返回数据就告诉用户为什么
                                showToast(activityRef, resultObj.getMsg());
                            }
                        } catch (JsonSyntaxException e) {
                            //捕捉json解析异常，一般是因为接口更改了字段
                            showToast(activityRef, activityRef.getString(R.string.toast_err_json));
                            Log.e("getCornCallBack", "JSON解析错误", e); // 添加日志记录
                        }
                    } else {
                        // 响应体为空就提示用户，一般是后端有了问题
                        showToast(activityRef, activityRef.getString(R.string.toast_err_server));
                    }
                } else {
                    //这里说明状态码都有问题，那么我就需要修改请求
                    showToast(activityRef, activityRef.getString(R.string.toast_err_request));
                }
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppCompatActivity activityRef = activity.get();
                // 检测是否回收了 activity
                if (isActivityValid(activityRef)) {
                    //如果已经回收了就直接终止，不再进行操作
                    return;
                }
                //告知用户网络异常或者服务器状态异常
                showToast(activityRef, activityRef.getString(R.string.toast_err_net_or_server));
                // 添加日志记录
                Log.e("getCornCallBack", "网络请求失败", e);
            }
            private boolean isActivityValid(AppCompatActivity activity) {
                return activity == null; // 检查 Activity 是否有效
            }
            private void showToast(AppCompatActivity activity, String message) {
                // 使用 Handler 在主线程中显示 Toast
                handler.post(() -> toastUtil.getToast(activity, message));
            }
        };
    }
    //TODO:这个方法在使用的时候要在OnDestroy方法中销毁：调用callback.cancel();
    /**
     * 获取工作类型的回调方法
     * @param workTypeHashMap 空不空都行反正必须不是null
     * @param popupMenuWorkType 要展示内容的菜单
     * @param activity 调用这个的activity
     * @return 返回回调方法
     */
    public Callback getWorkTypeBack(ConcurrentHashMap<String,Long> workTypeHashMap, PopupMenu popupMenuWorkType, WeakReference<AppCompatActivity> activity){
        // 创建一个 Handler，用于在主线程中处理 UI 更新
        Handler handler = new Handler(Looper.getMainLooper());
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                //获取到activity
                AppCompatActivity activityRef = activity.get();
                // 检测是否回收了 activity
                if (isActivityValid(activityRef)) {
                    //activity被回收了就直接终止
                    return;
                }
                // 获取响应状态码
                if (response.code() == successCode) {
                    //获取请求体
                    if (response.body() != null) {
                        //首先提取请求体字符串
                        String responseString = response.body().string();
                        Gson gson = new Gson();
                        try {
                            // 返回的结果体是作物集合
                            Result<ArrayList<WorkTypeEntity>> resultObj = gson.fromJson(responseString, new TypeToken<Result<ArrayList<WorkTypeEntity>>>() {}.getType());
                            //判断服务器是否可以正确的返回数据
                            if (resultObj.getCode() == successCode) {
                                // 在主线程中更新 UI和变量
                                handler.post(() -> {// 清空菜单和hashMap,菜单呈现给用户查看，hashMap为了联动作业类型
                                    Menu menuWorkType = popupMenuWorkType.getMenu();
                                    menuWorkType.clear();
                                    workTypeHashMap.clear();
                                    // 更新菜单和哈希图
                                    if (resultObj.getData()!=null&&!resultObj.getData().isEmpty()) {
                                        resultObj.getData().forEach(workTypeEntity -> {
                                            menuWorkType.add(Menu.NONE, Menu.NONE, menuWorkType.size(), workTypeEntity.getWorkName());
                                            workTypeHashMap.put(workTypeEntity.getWorkName(), workTypeEntity.getId());
                                        });
                                    }else{
                                        //如果解析出来的状态数据就是空就提示童虎
                                        showToast(activityRef, activityRef.getString(R.string.toast_err_data));
                                    }
                                });
                            } else {
                                //如果没有正确返回数据就告诉用户为什么
                                showToast(activityRef, resultObj.getMsg());
                            }
                        } catch (JsonSyntaxException e) {
                            //捕捉json解析异常，一般是因为接口更改了字段
                            showToast(activityRef, activityRef.getString(R.string.toast_err_json));
                            Log.e("getCornCallBack", "JSON解析错误", e); // 添加日志记录
                        }
                    } else {
                        // 响应体为空就提示用户，一般是后端有了问题
                        showToast(activityRef, activityRef.getString(R.string.toast_err_server));
                    }
                } else {
                    //这里说明状态码都有问题，那么我就需要修改请求
                    showToast(activityRef, activityRef.getString(R.string.toast_err_request));
                }
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppCompatActivity activityRef = activity.get();
                // 检测是否回收了 activity
                if (isActivityValid(activityRef)) {
                    //如果已经回收了就直接终止，不再进行操作
                    return;
                }
                //告知用户网络异常或者服务器状态异常
                showToast(activityRef, activityRef.getString(R.string.toast_err_net_or_server));
                // 添加日志记录
                Log.e("getCornCallBack", "网络请求失败", e);
            }
            private boolean isActivityValid(AppCompatActivity activity) {
                // 检查 Activity 是否有效
                return activity == null;
            }
            private void showToast(AppCompatActivity activity, String message) {
                // 使用 Handler 在主线程中显示 Toast
                handler.post(() -> toastUtil.getToast(activity, message));
            }
        };
    };
    //TODO:这个方法在使用的时候要在OnDestroy方法中销毁：调用callback.cancel();
    /**
     * 获取工作轨迹的回调
     * @param activity 调用这个的activity
     * @param itemWorks 存放工作轨迹信息的Result信息
     * @return 返回值是个回调对象
     */
    public Callback getWorkCallBack(WeakReference<AppCompatActivity> activity, ArrayList<ItemWork> itemWorks, GridView gridView, ItemWorkAdapter itemWorkAdapter){
        // 创建一个 Handler，用于在主线程中处理 UI 更新
        Handler handler = new Handler(Looper.getMainLooper());
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                //获取到activity
                AppCompatActivity activityRef = activity.get();
                // 检测是否回收了 activity
                if (isActivityValid(activityRef)) {
                    //activity被回收了就直接终止
                    return;
                }
                //这里获取response状态码
                if (response.code()==successCode) {
                    //这里说明后端可以收到请求并且进行处理
                    if(response.body()!=null){
                        String responseString = response.body().string();
                        Gson gson = new Gson();
                        try {
                            Result<ArrayList<TrajectoryResult>> result = gson.fromJson(responseString, new TypeToken<Result<ArrayList<TrajectoryResult>>>() {}.getType());
                            if (result.getCode()==successCode) {
                                handler.post(() -> {
                                    //清楚原有数据，覆盖掉
                                    itemWorks.clear();
                                   //在主线程操作ui和数据
                                    if (result.getData()!=null&&!result.getData().isEmpty()) {
                                        //从后端拿到数据后可以在这里把数据填充到list当中
                                        WorkTrajectoryPositionDao workTrajectoryPositionDao = AbstractTrajectoryDatabase.getInstance(activityRef).getWorkTrajectoryPositionDao();
                                        result.getData().forEach( trajectoryResult -> {
                                            //现在要开始获取到这个轨迹范围内的点了
                                            ArrayList<TrajectoryExcludePoint> trajectoryExcludePoints = handlePointInExcludePlot(trajectoryResult);
                                            //这里拿到一个轨迹信息，可以做点什么，比如添加到list当中
                                            ItemWork itemWork = new ItemWork();
                                            itemWork.setAddress(trajectoryResult.getLocation());
                                            itemWork.setTimeBegin(trajectoryResult.getStartTime());
                                            itemWork.setTimeEnd(trajectoryResult.getEndTime());
                                            itemWork.setWidth(String.valueOf(trajectoryResult.getWidth()));
                                            itemWork.setWorkType(trajectoryResult.getWorkType());
                                            itemWork.setImage(trajectoryResult.getTrajectoryImg());
                                            itemWork.setTrajectoryId(trajectoryResult.getTrajectoryInfoId());
                                            itemWork.setPhone(trajectoryResult.getPhoneNumber());
                                            itemWork.setArea(trajectoryResult.getArea());
                                            //拿到轨迹信息后，把轨迹信息中的轨迹点信息也添加到数据库中
                                            ArrayList<WorkTrajectoryPosition> workTrajectoryPositions = new ArrayList<>();
                                            //遍历每一个点建立一条
                                            if(trajectoryResult.getPoints()!=null) {
                                                for(Point point:trajectoryResult.getPoints()){
                                                    WorkTrajectoryPosition workTrajectoryPosition = new WorkTrajectoryPosition();
                                                    workTrajectoryPosition.setTrajectoryId(trajectoryResult.getTrajectoryInfoId());
                                                    workTrajectoryPosition.setLatitude(point.getLatitude());
                                                    workTrajectoryPosition.setLongitude(point.getLongitude());
                                                    workTrajectoryPosition.setTime(point.getTime());
                                                    workTrajectoryPosition.setPlotId(trajectoryResult.getPlotId());
                                                    if (trajectoryExcludePoints.contains(point)){
                                                        String id = trajectoryExcludePoints.get(trajectoryExcludePoints.indexOf(point)).getId();
                                                        workTrajectoryPosition.setExcludePlotId(id);
                                                        workTrajectoryPosition.setInExcludePlot(true);
                                                    }
                                                    workTrajectoryPositions.add(workTrajectoryPosition);
                                                };
                                            }
                                            //一次插入一整条轨迹
                                            new Thread(()->{
                                                workTrajectoryPositionDao.insert(workTrajectoryPositions);
                                            }).start();
                                            //TODO:glide添加图片
                                            itemWorks.add(itemWork);
                                        });
                                    }else{
                                        showToast(activityRef, activityRef.getString(R.string.toast_err_data));
                                    }
                                    gridView.setAdapter(itemWorkAdapter);
                                    itemWorkAdapter.notifyDataSetChanged(itemWorks);
                                });
                            }else {
                                showToast(activityRef, result.getMsg());
                            }
                        }catch (JsonSyntaxException e){
                            showToast(activityRef, activityRef.getString(R.string.toast_err_json));
                            Log.e("getWorkCallBack", "JSON解析错误", e);
                        }
                    } else {
                        // 响应体为空就提示用户，一般是后端有了问题
                        showToast(activityRef, activityRef.getString(R.string.toast_err_server));
                    }
                } else {
                    //这里说明状态码都有问题，那么我就需要修改请求
                    showToast(activityRef, activityRef.getString(R.string.toast_err_request));
                }
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppCompatActivity activityRef = activity.get();
                // 检测是否回收了 activity
                if (isActivityValid(activityRef)) {
                    //如果已经回收了就直接终止，不再进行操作
                    return;
                }
                //告知用户网络异常或者服务器状态异常
                showToast(activityRef, activityRef.getString(R.string.toast_err_net_or_server));
                // 添加日志记录
                Log.e("getCornCallBack", "网络请求失败", e);
            }
            private boolean isActivityValid(AppCompatActivity activity) {
                // 检查 Activity 是否有效
                return activity == null;
            }
            private void showToast(AppCompatActivity activity, String message) {
                // 使用 Handler 在主线程中显示 Toast
                handler.post(() -> toastUtil.getToast(activity, message));
            }
        };
    };
    //TODO:这个方法在使用的时候要在OnDestroy方法中销毁：调用callback.cancel();
    /**
     * 获取原始工作点集合的回调
     * @param activity 调用方法的activity
     * @param latLngList 轨迹点集合
     * @return 返回回调对象
     */
    public Callback getOriginTrajectoryCallBack(WeakReference<AppCompatActivity> activity, ArrayList<LatLng> latLngList, AMap map) {
        // 创建一个 Handler，用于在主线程中处理 UI 更新
        Handler handler = new Handler(Looper.getMainLooper());
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                //获取到activity
                AppCompatActivity activityRef = activity.get();
                // 检测是否回收了 activity
                if (isActivityValid(activityRef)) {
                    //activity被回收了就直接终止
                    return;
                }
                //这里获取response状态码
                if (response.code() == successCode) {
                    //这里说明后端可以收到请求并且进行处理
                    if (response.body() != null) {
                        String responseString = response.body().string();
                        Gson gson = new Gson();
                        try {
                            Result<ArrayList<Point>> result = gson.fromJson(responseString, new TypeToken<Result<ArrayList<Point>>>() {}.getType());
                            if (result.getCode() == successCode) {
                                handler.post(() -> {
                                    //在主线程操作ui和数据
                                    if (result.getData() != null &&!result.getData().isEmpty()) {
                                        //清楚原有数据，覆盖掉
                                        result.getData().forEach(point -> {
                                            LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
                                            latLngList.add(latLng);
                                        });
                                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(result.getData().get(0).getLatitude(), result.getData().get(0).getLongitude()),17));
                                    } else {
//                                        showToast(activityRef, activityRef.getString(R.string.toast_err_data));
                                    }
                                    map.addPolyline(new PolylineOptions()
                                                    .zIndex(3)
                                            .color(activityRef.getColor(R.color.trajectory_no))
                                            .addAll(latLngList)
                                            .width(activityRef.getResources().getInteger(R.integer.trajectory_line_size))
                                            .setCustomTexture(BitmapDescriptorFactory.fromResource(R.drawable.trajectory_no)));
                                });
                            } else {
                                showToast(activityRef, result.getMsg());
                            }
                        } catch (JsonSyntaxException e) {
                            showToast(activityRef, activityRef.getString(R.string.toast_err_json));
                            Log.e("getWorkCallBack", "JSON解析错误", e);
                        }
                    } else {
                        // 响应体为空就提示用户，一般是后端有了问题
                        showToast(activityRef, activityRef.getString(R.string.toast_err_server));
                    }
                } else {
                    //这里说明状态码都有问题，那么我就需要修改请求
                    showToast(activityRef, activityRef.getString(R.string.toast_err_request));
                }
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppCompatActivity activityRef = activity.get();
                // 检测是否回收了 activity
                if (isActivityValid(activityRef)) {
                    //如果已经回收了就直接终止，不再进行操作
                    return;
                }
                //告知用户网络异常或者服务器状态异常
                showToast(activityRef, activityRef.getString(R.string.toast_err_net_or_server));
                // 添加日志记录
                Log.e("getCornCallBack", "网络请求失败", e);
            }
            private boolean isActivityValid(AppCompatActivity activity) {
                // 检查 Activity 是否有效
                return activity == null;
            }
            private void showToast(AppCompatActivity activity, String message) {
                // 使用 Handler 在主线程中显示 Toast
                handler.post(() -> toastUtil.getToast(activity, message));
            }
        };
    }

    public Callback addWorkCallBack(WeakReference<AppCompatActivity> activity, ArrayList<ItemWork> itemWorks, RefreshLayout refreshLayout, ItemWorkAdapter itemWorkAdapter, ClassicsFooter classicsFooter) {
        // 创建一个 Handler，用于在主线程中处理 UI 更新
        Handler handler = new Handler(Looper.getMainLooper());
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                //获取到activity
                AppCompatActivity activityRef = activity.get();
                // 检测是否回收了 activity
                if (isActivityValid(activityRef)) {
                    //activity被回收了就直接终止
                    return;
                }
                //这里获取response状态码
                if (response.code()==successCode) {
                    //这里说明后端可以收到请求并且进行处理
                    if(response.body()!=null){
                        String responseString = response.body().string();
                        Gson gson = new Gson();
                        try {
                            Result<ArrayList<TrajectoryResult>> result = gson.fromJson(responseString, new TypeToken<Result<ArrayList<TrajectoryResult>>>() {}.getType());
                            if (result.getCode()==successCode) {
                                handler.post(() -> {
                                    //在主线程操作ui和数据
                                    if (result.getData()!=null&&!result.getData().isEmpty()) {
                                        //从后端拿到数据后可以在这里把数据填充到list当中
                                        WorkTrajectoryPositionDao workTrajectoryPositionDao = AbstractTrajectoryDatabase.getInstance(activityRef).getWorkTrajectoryPositionDao();
                                        result.getData().forEach( trajectoryResult -> {
                                            //现在要开始获取到这个轨迹范围内的点了
                                            ArrayList<TrajectoryExcludePoint> trajectoryExcludePoints = handlePointInExcludePlot(trajectoryResult);
                                            //这里拿到一个轨迹信息，可以做点什么，比如添加到list当中
                                            ItemWork itemWork = new ItemWork();
                                            itemWork.setAddress(trajectoryResult.getLocation());
                                            itemWork.setTimeBegin(trajectoryResult.getStartTime());
                                            itemWork.setTimeEnd(trajectoryResult.getEndTime());
                                            itemWork.setWidth(String.valueOf(trajectoryResult.getWidth()));
                                            itemWork.setWorkType(trajectoryResult.getWorkType());
                                            itemWork.setImage(trajectoryResult.getTrajectoryImg());
                                            itemWork.setTrajectoryId(trajectoryResult.getTrajectoryInfoId());
                                            itemWork.setPhone(trajectoryResult.getPhoneNumber());
                                            itemWork.setArea(trajectoryResult.getArea());
                                            //拿到轨迹信息后，把轨迹信息中的轨迹点信息也添加到数据库中
                                            ArrayList<WorkTrajectoryPosition> workTrajectoryPositions = new ArrayList<>();
                                            //遍历每一个点建立一条
                                            if(trajectoryResult.getPoints()!=null) {
                                                for(Point point:trajectoryResult.getPoints()){
                                                    WorkTrajectoryPosition workTrajectoryPosition = new WorkTrajectoryPosition();
                                                    workTrajectoryPosition.setTrajectoryId(trajectoryResult.getTrajectoryInfoId());
                                                    workTrajectoryPosition.setLatitude(point.getLatitude());
                                                    workTrajectoryPosition.setLongitude(point.getLongitude());
                                                    workTrajectoryPosition.setTime(point.getTime());
                                                    workTrajectoryPosition.setPlotId(trajectoryResult.getPlotId());
                                                    if (trajectoryExcludePoints.contains(point)){
                                                        String id = trajectoryExcludePoints.get(trajectoryExcludePoints.indexOf(point)).getId();
                                                        workTrajectoryPosition.setExcludePlotId(id);
                                                        workTrajectoryPosition.setInExcludePlot(true);
                                                    }
                                                    workTrajectoryPositions.add(workTrajectoryPosition);
                                                };
                                            }
                                            //一次插入一整条轨迹
                                            new Thread(()->{
                                                workTrajectoryPositionDao.insert(workTrajectoryPositions);
                                            }).start();
                                            //TODO:glide添加图片
                                            itemWorks.add(itemWork);
                                            refreshLayout.finishLoadMore();
                                        });
                                    }else{
                                        if (classicsFooter!=null) {
                                           classicsFooter.setNoMoreData(true);
                                           refreshLayout.finishLoadMore();
                                        }
                                    }
                                    itemWorkAdapter.notifyDataSetChanged(itemWorks);
                                });
                            }else {
                                showToast(activityRef, result.getMsg());
                            }
                        }catch (JsonSyntaxException e){
                            showToast(activityRef, activityRef.getString(R.string.toast_err_json));
                            Log.e("getWorkCallBack", "JSON解析错误", e);
                        }
                    } else {
                        // 响应体为空就提示用户，一般是后端有了问题
                        showToast(activityRef, activityRef.getString(R.string.toast_err_server));
                    }
                } else {
                    //这里说明状态码都有问题，那么我就需要修改请求
                    showToast(activityRef, activityRef.getString(R.string.toast_err_request));
                }
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppCompatActivity activityRef = activity.get();
                // 检测是否回收了 activity
                if (isActivityValid(activityRef)) {
                    //如果已经回收了就直接终止，不再进行操作
                    return;
                }
                //告知用户网络异常或者服务器状态异常
                showToast(activityRef, activityRef.getString(R.string.toast_err_net_or_server));
                // 添加日志记录
                Log.e("getCornCallBack", "网络请求失败", e);
            }
            private boolean isActivityValid(AppCompatActivity activity) {
                // 检查 Activity 是否有效
                return activity == null;
            }
            private void showToast(AppCompatActivity activity, String message) {
                // 使用 Handler 在主线程中显示 Toast
                handler.post(() -> toastUtil.getToast(activity, message));
                refreshLayout.finishLoadMore();
            }
        };

    }


    public Callback endTrajectory(WeakReference<AppCompatActivity> activity,String trajectoryInfoId) {
        Handler handler = new Handler(Looper.getMainLooper());
        return new Callback() {
            private TrajectoryDao dao = AbstractTrajectoryDatabase.getInstance(activity.get()).getTrajectoryDao();
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                //获取到activity
                AppCompatActivity activityRef = activity.get();
                // 检测是否回收了 activity
                if (isActivityValid(activityRef)) {
                    //activity被回收了就直接终止
                    return;
                }
                //这里获取response状态码
                if (response.code()==successCode) {
                    //这里说明后端可以收到请求并且进行处理
                    if(response.body()!=null){
                        String responseString = response.body().string();
                        Gson gson = new Gson();
                        try {
                            Result result = gson.fromJson(responseString, new TypeToken<Result>() {}.getType());
                            if (result.getCode()==successCode) {
                                //这里表示上传成功了
                                showToast(activityRef, "上传成功",ToastUtil.TOAST_TRUE);
                            }else {
                                showToast(activityRef, result.getMsg());
                            }
                        }catch (JsonSyntaxException e){
                            showToast(activityRef, activityRef.getString(R.string.toast_err_json));
                            Log.e("getWorkCallBack", "JSON解析错误", e);
                        }
                    } else {
                        // 响应体为空就提示用户，一般是后端有了问题
                        showToast(activityRef, activityRef.getString(R.string.toast_err_server));
                    }
                } else {
                    //这里说明状态码都有问题，那么我就需要修改请求
                    showToast(activityRef, activityRef.getString(R.string.toast_err_request));
                }
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppCompatActivity activityRef = activity.get();
                // 检测是否回收了 activity
                if (isActivityValid(activityRef)) {
                    //如果已经回收了就直接终止，不再进行操作
                    return;
                }
                //告知用户网络异常或者服务器状态异常
                showToast(activityRef, activityRef.getString(R.string.toast_err_net_or_server));
                // 添加日志记录
                Log.e("getCornCallBack", "网络请求失败", e);
            }
            private boolean isActivityValid(AppCompatActivity activity) {
                // 检查 Activity 是否有效
                return activity == null;
            }
            private void showToast(AppCompatActivity activity, String message) {
                if ("上传成功".equals(message)){
                    dao.updateState(trajectoryInfoId,true);
                }
                // 使用 Handler 在主线程中显示 Toast
                handler.post(() -> {
                    toastUtil.getToast(activity, message);
                    //跳回BaseActivity
                    Intent intentBack = new Intent(activity, BaseActivity.class);
                    intentBack.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                    intentBack.putExtra(activity.getString(R.string.fragment_index), activity.getResources().getInteger(R.integer.fragment_index_trajectory));
                    activity.startActivity(intentBack);
                    activity.finish();
                });
            }
            private void showToast(AppCompatActivity activity, String message,int type) {
                if ("上传成功".equals(message)){
                    dao.updateState(trajectoryInfoId,true);
                }
                // 使用 Handler 在主线程中显示 Toast
                handler.post(() -> {
                    toastUtil.getToast(activity, message,type);
                    //跳回BaseActivity
                    Intent intentBack = new Intent(activity, BaseActivity.class);
                    intentBack.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                    intentBack.putExtra(activity.getString(R.string.fragment_index), activity.getResources().getInteger(R.integer.fragment_index_trajectory));
                    activity.startActivity(intentBack);
                    activity.finish();
                });
            }
        };
    }
    public Callback reloadTrajectory(WeakReference<AppCompatActivity> activity, String trajectoryInfoId, Button btn, ReloadAdapter reloadAdapter, Trajectory trajectory) {
        Handler handler = new Handler(Looper.getMainLooper());
        return new Callback() {
            private TrajectoryDao dao = AbstractTrajectoryDatabase.getInstance(activity.get()).getTrajectoryDao();
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                //获取到activity
                AppCompatActivity activityRef = activity.get();
                // 检测是否回收了 activity
                if (isActivityValid(activityRef)) {
                    //activity被回收了就直接终止
                    return;
                }
                //这里获取response状态码
                if (response.code()==successCode) {
                    //这里说明后端可以收到请求并且进行处理
                    if(response.body()!=null){
                        String responseString = response.body().string();
                        Gson gson = new Gson();
                        try {
                            Result result = gson.fromJson(responseString, new TypeToken<Result>() {}.getType());
                            if (result.getCode()==successCode) {
                                //这里表示上传成功了
                                showToast(activityRef, "上传成功");
                            }else {
                                showToast(activityRef, result.getMsg());
                            }
                        }catch (JsonSyntaxException e){
                            showToast(activityRef, activityRef.getString(R.string.toast_err_json));
                            Log.e("getWorkCallBack", "JSON解析错误", e);
                        }
                    } else {
                        // 响应体为空就提示用户，一般是后端有了问题
                        showToast(activityRef, activityRef.getString(R.string.toast_err_server));
                    }
                } else {
                    //这里说明状态码都有问题，那么我就需要修改请求
                    showToast(activityRef, activityRef.getString(R.string.toast_err_request));
                }
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppCompatActivity activityRef = activity.get();
                // 检测是否回收了 activity
                if (isActivityValid(activityRef)) {
                    //如果已经回收了就直接终止，不再进行操作
                    return;
                }
                //告知用户网络异常或者服务器状态异常
                showToast(activityRef, activityRef.getString(R.string.toast_err_net_or_server));
                // 添加日志记录
                Log.e("getCornCallBack", "网络请求失败", e);
            }
            private boolean isActivityValid(AppCompatActivity activity) {
                // 检查 Activity 是否有效
                return activity == null;
            }
            private void showToast(AppCompatActivity activity, String message) {
                if ("上传成功".equals(message)){
                    dao.updateState(trajectoryInfoId,true);
                    handler.post(() -> {
                        trajectory.setIsUpload(true);
                        reloadAdapter.notified();
                        btn.setText("已上传");
                        btn.setEnabled(false);
                    });
                }
                // 使用 Handler 在主线程中显示 Toast
                handler.post(() -> {
                    toastUtil.getToast(activity, message);
                });
            }
        };
    }

    public Callback getPlotCallBack(WeakReference<AppCompatActivity> activity,AMap aMap) {
        // 创建一个 Handler，用于在主线程中处理 UI 更新
        Handler handler = new Handler(Looper.getMainLooper());
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                //获取到activity
                AppCompatActivity activityRef = activity.get();
                // 检测是否回收了 activity
                if (isActivityValid(activityRef)) {
                    //activity被回收了就直接终止
                    return;
                }
                //这里获取response状态码
                if (response.code()==successCode) {
                    //这里说明后端可以收到请求并且进行处理
                    if(response.body()!=null){
                        String responseString = response.body().string();
                        Gson gson = new Gson();
                        try {
                            Result<ArrayList<ResultLandPlot>> result = gson.fromJson(responseString, new TypeToken<Result<ArrayList<ResultLandPlot >>>() {}.getType());
                            if (result.getCode()==successCode) {
                                handler.post(() -> {
                                    //在主线程操作ui和数据
                                    if (result.getData()!=null&&!result.getData().isEmpty()) {
                                        ArrayList<ResultLandPlot> landPlots = result.getData();
                                        //再解析还要考虑周围有没有地块,有才绘制，没有就不绘制
                                        if (!landPlots.isEmpty()) {
                                            for (ResultLandPlot aResultLandPlot : landPlots) {
                                                LandPlot landPlot = new LandPlot();
                                                landPlot.setId(aResultLandPlot.getId());
                                                landPlot.setBoundary(aResultLandPlot.getBoundary());
                                                landPlot.setLandType(aResultLandPlot.getLandType());
                                                new Thread(()->{
                                                    new DataUtil(activityRef).asyncLandPlot(landPlot, landPlotBoundaryList -> {
                                                        if (!landPlotBoundaryList.isEmpty()) {
                                                            //然后判断有几块内容
                                                            List<BaseHoleOptions> list = new ArrayList<>();
                                                            if (landPlotBoundaryList.size()>1){
                                                                for (int i = 1; i < landPlotBoundaryList.size(); i++){
                                                                    PolygonHoleOptions polygonHoleOptions = new PolygonHoleOptions();
                                                                    polygonHoleOptions.addAll(landPlotBoundaryList.get(i));
                                                                    list.add(polygonHoleOptions);
                                                                }
                                                                //把地块的数据直接绘制出来
                                                                PolygonOptions polygonOptions = new PolygonOptions();
                                                                // 添加 多边形的每个顶点（顺序添加）
                                                                polygonOptions.addAll(landPlotBoundaryList.get(0));
                                                                // 多边形的边框
                                                                // 边框颜色
                                                                // 多边形的填充色
                                                                polygonOptions.strokeWidth(activityRef.getResources().getInteger(R.integer.trajectory_line_size))
                                                                        .zIndex(1)
                                                                        .strokeColor(activityRef.getColor(R.color.plot))
                                                                        .fillColor(activityRef.getColor(R.color.plot_fill))
                                                                        .setHoleOptions(list);
                                                                aMap.addPolygon(polygonOptions);
                                                            }else {
                                                                PolygonOptions polygonOptions = new PolygonOptions();
                                                                // 添加 多边形的每个顶点（顺序添加）
                                                                polygonOptions.addAll(landPlotBoundaryList.get(0));
                                                                // 多边形的边框
                                                                // 边框颜色
                                                                // 多边形的填充色
                                                                polygonOptions.strokeWidth(activityRef.getResources().getInteger(R.integer.trajectory_line_size))
                                                                        .zIndex(1)
                                                                        .strokeColor(activityRef.getColor(R.color.plot))
                                                                        .fillColor(activityRef.getColor(R.color.plot_fill))
                                                                        .setHoleOptions(list);
                                                                aMap.addPolygon(polygonOptions);
                                                            }
                                                        }else{
                                                            showToast(activityRef,  "该地块信息为空");
                                                        }
                                                    });
                                                }).start();
                                            }
                                        }
                                    }else{
                                        showToast(activityRef, activityRef.getString(R.string.toast_err_data));
                                    }
                                });
                            }else {
                                showToast(activityRef, result.getMsg());
                            }
                        }catch (JsonSyntaxException e){
                            showToast(activityRef, activityRef.getString(R.string.toast_err_json));
                        }
                    } else {
                        // 响应体为空就提示用户，一般是后端有了问题
                        showToast(activityRef, activityRef.getString(R.string.toast_err_server));
                    }
                } else {
                    //这里说明状态码都有问题，那么我就需要修改请求
                    showToast(activityRef, activityRef.getString(R.string.toast_err_request));
                }
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppCompatActivity activityRef = activity.get();
                // 检测是否回收了 activity
                if (isActivityValid(activityRef)) {
                    //如果已经回收了就直接终止，不再进行操作
                    return;
                }
                //告知用户网络异常或者服务器状态异常
                showToast(activityRef, activityRef.getString(R.string.toast_err_net_or_server));
            }
            private boolean isActivityValid(AppCompatActivity activity) {
                // 检查 Activity 是否有效
                return activity == null;
            }
            private void showToast(AppCompatActivity activity, String message) {
                // 使用 Handler 在主线程中显示 Toast
                handler.post(() -> toastUtil.getToast(activity, message));
            }
        };
    }

    private static ArrayList<TrajectoryExcludePoint> handlePointInExcludePlot(TrajectoryResult trajectoryResult) {
        ArrayList<TrajectoryExcludePoint> trajectoryExcludePoints = new ArrayList<>();
        //首先判断有没有在空洞里的轨迹
        ArrayList<TrajectoryPointExcludeOnPlot> trajectoryExclude = trajectoryResult.getTrajectoryExclude();
        if (trajectoryExclude != null){
            //当存在空洞的时候，遍历每一个空洞并且提取出来每一个点
            for (TrajectoryPointExcludeOnPlot trajectoryPointExcludeOnPlot : trajectoryExclude) {
                ArrayList<Point> pointsExcludePoint = trajectoryPointExcludeOnPlot.getPointsExclude();
                if (pointsExcludePoint!=null&&!pointsExcludePoint.isEmpty()){
                    for (Point point : pointsExcludePoint) {
                        TrajectoryExcludePoint trajectoryExcludePoint = new TrajectoryExcludePoint(trajectoryPointExcludeOnPlot.getId());
                        trajectoryExcludePoint.setLatitude(point.getLatitude());
                        trajectoryExcludePoint.setLongitude(point.getLongitude());
                        trajectoryExcludePoint.setTime(point.getTime());
                        trajectoryExcludePoints.add(trajectoryExcludePoint);
                    }
                }
            }
        }
        return trajectoryExcludePoints;
    }

}
