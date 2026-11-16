package cn.agrinav.trajectory.util;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Button;
import android.widget.GridView;
import android.widget.PopupMenu;
import androidx.appcompat.app.AppCompatActivity;
import com.amap.api.maps.AMap;
import com.amap.api.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cn.agrinav.R;
import cn.agrinav.front.database.entity.ExcludeLandPlot;
import cn.agrinav.front.database.entity.LandPlot;
import cn.agrinav.trajectory.adapter.ItemWorkAdapter;
import cn.agrinav.trajectory.adapter.ReloadAdapter;
import cn.agrinav.trajectory.entity.ItemWork;
import cn.agrinav.trajectory.entity.SortMessage;
import cn.agrinav.trajectory.entity.Trajectory;
import cn.agrinav.utils.OkHttpUtils;
import cn.agrinav.utils.Result;
import cn.agrinav.utils.ToastUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author 11230
 */
public class DataUtil {
    private final Resources resources;
    private final SharedPreferences preferences;
    private final WeakReference<AppCompatActivity> activity;
    private final int successCode;
    private static String url;
    private final ToastUtil toastUtil;
    private static final Pattern LAND_PLOT_PATTERN = Pattern.compile("\\(([^()]+)\\)");
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("(\\d+\\.\\d+)\\s+(\\d+\\.\\d+)");

    public DataUtil(AppCompatActivity activity) {
        this.activity = new WeakReference<>(activity);
        resources = activity.getResources();
        //获取成功码
        successCode = resources.getInteger(R.integer.request_success);
        //获取preferences
        preferences = activity.getSharedPreferences(activity.getString(R.string.user_pref_name),Context.MODE_PRIVATE);
        if (url==null){
            url = preferences.getString(activity.getString(R.string.user_pref_key_server),activity.getString(R.string.server_url));
        }
        toastUtil = new ToastUtil();
    }
    /**
     * 获取工作类型的方法
     * @param popupMenuCorn 弹出作物的菜单对象
     * @param cornEntities 存储作物类型的哈希表
     * @return 回调方法，要在activity结束的时候销毁掉
     * 查询所有作物并且覆盖掉存储作物类型的哈希表然后展示在菜单上
     */
    public Call getCornData(PopupMenu popupMenuCorn, ConcurrentHashMap<String,Long> cornEntities) {
        if (activity!=null) {
            OkHttpUtils okHttpUtils = new OkHttpUtils();
            Request request = okHttpUtils.createBuilder(url, resources.getString(R.string.trajectory_request_get_corn))
                    .addHeader(activity.get().getString(R.string.request_header_key_token), Objects.requireNonNull(preferences.getString(activity.get().getString(R.string.user_pref_key_token), null)))
                    .build();
            Callback callback = new TrajectoryCallBackUtils(successCode).getCornCallBack(cornEntities, popupMenuCorn, activity);
            return okHttpUtils.sendRequest(request, callback);
        }else{
            return null;
        }
    }

    /**
     * 获取工作类型的方法
     * @param popupMenuWorkType 弹出工作菜单的菜单对象
     * @param workTypeHashMap 存储工作类型的哈希表
     * @param id 某个作物的id
     * @return 回调方法，要在activity结束的时候销毁掉
     * 通过作物id覆盖原有工作类型哈希表并且展示在菜单上
     */
    public Call getCornTypeData(PopupMenu popupMenuWorkType,ConcurrentHashMap<String,Long> workTypeHashMap,Long id) {
        if (activity!=null) {
            //构造okhttpUtil
            OkHttpUtils okHttpUtils = new OkHttpUtils();
            //构造参数
            HashMap<String, Long> params = new HashMap<>();
            params.put("cropId", id);
            //构造请求
            Request request = okHttpUtils.createBuilder(url, resources.getString(R.string.trajectory_request_get_corn_type), params)
                    .addHeader(activity.get().getString(R.string.request_header_key_token), Objects.requireNonNull(preferences.getString(activity.get().getString(R.string.user_pref_key_token), null)))
                    .build();
            Callback callback =  new TrajectoryCallBackUtils(successCode).getWorkTypeBack(workTypeHashMap, popupMenuWorkType, activity);
            return okHttpUtils.sendRequest(request, callback);
        }else {
            return null;
        }
    }

    /**
     * 获取用户工作轨迹集合的方法
     * @param results 存储轨迹的集合
     * @param pageNum 指定页号
     * @param pageSize 指定页大小
     * @return 返回回调方法对象
     * 要在activity结束的时候销毁掉
     */
    public Call getWorkTrajectoryData(ArrayList<ItemWork> results, int pageNum, int pageSize, GridView gridView, ItemWorkAdapter itemWorkAdapter) {
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        HashMap<String, Integer> params = new HashMap<>();
        params.put("pageNum",pageNum);
        params.put("sizeSize",pageSize);
        Request request = okHttpUtils.createBuilder(url,resources.getString(R.string.trajectory_request_get_work_trajectory),params)
                .addHeader(activity.get().getString(R.string.request_header_key_token), Objects.requireNonNull(preferences.getString(activity.get().getString(R.string.user_pref_key_token), null)))
                .post(RequestBody.create("",MediaType.parse("application/json")))
                .build();
        Callback callback =  new TrajectoryCallBackUtils(successCode).getWorkCallBack(activity,results,gridView,itemWorkAdapter);
        return okHttpUtils.sendRequest(request, callback);
    }
    /**
     * 获取用户工作轨迹集合的方法
     * @param results 存储轨迹的集合
     * @param sortMessage 存储查询条件
     * @param pageNum 指定页号
     * @param pageSize 指定页大小
     * @return 返回回调方法对象
     * 要在activity结束的时候销毁掉
     */
    public Call getWorkTrajectoryConditionData(ArrayList<ItemWork> results,SortMessage sortMessage,int pageNum,int pageSize,GridView gridView, ItemWorkAdapter itemWorkAdapter) {
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        HashMap<String, Integer> params = new HashMap<>();
        params.put("pageNum",pageNum);
        params.put("pageSize",pageSize);
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        String json = gson.toJson(sortMessage);
        RequestBody requestBody = RequestBody.create(json,MediaType.parse("application/json"));
        Request request1 = okHttpUtils.createBuilder(url,resources.getString(R.string.trajectory_request_get_work_trajectory_condition),params)
                .addHeader(activity.get().getString(R.string.request_header_key_token), Objects.requireNonNull(preferences.getString(activity.get().getString(R.string.user_pref_key_token), null)))
                .post(requestBody)
                .build();
        Callback callback = new TrajectoryCallBackUtils(successCode).getWorkCallBack(activity,results,gridView,itemWorkAdapter);
        return okHttpUtils.sendRequest(request1, callback);
    }
    /**
     * 获取用户原始轨迹集合的方法
     * @param sortMessage 筛选信息（请求体）
     * @param latLngList 存储轨迹点的集合
     * @return 返回回调方法对象
     * 要在activity结束的时候销毁掉
     */
    public Call getOriginTrajectoryData(ArrayList<LatLng> latLngList, SortMessage sortMessage, AMap map) {
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        //首先构造请求体
        String json =gson.toJson(sortMessage);
        RequestBody requestBody = RequestBody.create(json,MediaType.parse("application/json"));
        Request request = okHttpUtils.createBuilder(url,resources.getString(R.string.trajectory_request_get_origin))
                .addHeader(activity.get().getString(R.string.request_header_key_token), Objects.requireNonNull(preferences.getString(activity.get().getString(R.string.user_pref_key_token), null)))
                .post(requestBody)
                .build();
        Callback callback = new TrajectoryCallBackUtils(successCode).getOriginTrajectoryCallBack(activity,latLngList,map);
        return okHttpUtils.sendRequest(request, callback);
    }
    /**
     * 解析获取周围地块得到的请求体的json字符串
     * @param boundary 地块坐标字符串((1,2 1,2 1,2))
     * @return 返回坐标集合
     */
    public ArrayList<LatLng> decodeLandPlotBoundary(String boundary){
        //能执行到这里说明肯定boundary里面是有数据的
        ArrayList<LatLng> list = new ArrayList<>();
        // 使用正则表达式匹配坐标
        // 提取内部坐标字符串
        int startIndex = boundary.indexOf('(') + 2;
        int endIndex = boundary.lastIndexOf(')');
        String innerStr = boundary.substring(startIndex, endIndex-1);

        // 分割坐标
        String[] coordList = innerStr.split(",");

        for (String coord : coordList) {
            String[] xy = coord.trim().split("\\s+");
            String x = xy[0];
            String y = xy[1];
            LatLng latLng = new LatLng(Double.parseDouble(x), Double.parseDouble(y));
            list.add(latLng);
        }
        return list;
    }
    public void asyncLandPlot(LandPlot landplot , LandPlotInterface tool) {
        //第一步分析landPlot的类型
        ArrayList<ArrayList<LatLng>> landmarks = new ArrayList<>();
        landmarks.add(decodeLandPlotBoundary(landplot.getBoundary()));
        if (activity.get().getString(R.string.plot_type_insert).equals(landplot.getLandType())) {
            //如果分析出来的结果是插花地就要去发一个网络请求了
            getExcludeBoundary(landplot.getId(),tool,landmarks);
        }else{
            tool.onGetLandPlotBoundaryList(landmarks);
        }
    }
    public ArrayList<ArrayList<LatLng>> extractLandPlots(String boundary) {
        ArrayList<ArrayList<LatLng>> landPlots = new ArrayList<>();
        Matcher matcher = LAND_PLOT_PATTERN.matcher(boundary); // 使用常量
        while (matcher.find()) {
            String plotCoordinates = matcher.group(1);
            ArrayList<LatLng> plotPoints = decodeLandPlotsBoundary(plotCoordinates);
            landPlots.add(plotPoints);
        }
        return landPlots;
    }

    public ArrayList<LatLng> decodeLandPlotsBoundary(String boundary){
        ArrayList<LatLng> list = new ArrayList<>();
        // 使用正则表达式匹配坐标点
        Pattern pattern = COORDINATE_PATTERN;
        // 匹配经纬度值
        Matcher matcher = pattern.matcher(boundary);
        while (matcher.find()) {
            try {
                double latitude = Double.parseDouble(matcher.group(1));
                double longitude = Double.parseDouble(matcher.group(2));
                LatLng latLng = new LatLng(latitude, longitude);
                list.add(latLng);
            } catch (NumberFormatException e) {
                // 处理解析错误
                Log.e("agrinav " , e.getMessage());
            }
        }
        return list;
    }
    public Call addWorkTrajectoryConditionData(ArrayList<ItemWork> results, SortMessage sortMessage, int pageNum, int pageSize, RefreshLayout refreshLayout, ItemWorkAdapter itemWorkAdapter,ClassicsFooter classicsFooter){
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        HashMap<String, Integer> params = new HashMap<>();
        params.put("pageNum",pageNum);
        params.put("pageSize",pageSize);
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        RequestBody requestBody = RequestBody.create(gson.toJson(sortMessage),MediaType.parse("application/json"));
        Request request1 = okHttpUtils.createBuilder(url,resources.getString(R.string.trajectory_request_get_work_trajectory_condition),params)
                .addHeader(activity.get().getString(R.string.request_header_key_token), Objects.requireNonNull(preferences.getString(activity.get().getString(R.string.user_pref_key_token), null)))
                .post(requestBody)
                .build();
        Callback callback = new TrajectoryCallBackUtils(successCode).addWorkCallBack(activity,results,refreshLayout,itemWorkAdapter,classicsFooter);
        return okHttpUtils.sendRequest(request1, callback);
    }
    public Call addWorkTrajectoryData(ArrayList<ItemWork> results, int pageNum, int pageSize, RefreshLayout refreshLayout, ItemWorkAdapter itemWorkAdapter, ClassicsFooter classicsFooter){
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        HashMap<String, Integer> params = new HashMap<>();
        params.put("pageNum",pageNum);
        params.put("pageSize",pageSize);
        Request request = okHttpUtils.createBuilder(url,resources.getString(R.string.trajectory_request_get_work_trajectory),params)
                .addHeader(activity.get().getString(R.string.request_header_key_token), Objects.requireNonNull(preferences.getString(activity.get().getString(R.string.user_pref_key_token), null)))
                .post(RequestBody.create("",MediaType.parse("application/json")))
                .build();
        Callback callback =  new TrajectoryCallBackUtils(successCode).addWorkCallBack(activity,results,refreshLayout,itemWorkAdapter,classicsFooter);
        return okHttpUtils.sendRequest(request, callback);
    }
    public Call endTrajectory(WeakReference<AppCompatActivity> activity,String startTime,String endTime,String trajectoryId){
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        HashMap<String, String> params = new HashMap<>();
        params.put("startTime",startTime);
        params.put("endTime",endTime);
        Request request = okHttpUtils.createBuilder(url,resources.getString(R.string.trajectory_request_end_trajectory),params)
                .addHeader(activity.get().getString(R.string.request_header_key_token), Objects.requireNonNull(preferences.getString(activity.get().getString(R.string.user_pref_key_token), null)))
                .build();
        Callback callback =  new TrajectoryCallBackUtils(successCode).endTrajectory(activity,trajectoryId);
        return okHttpUtils.sendRequest(request, callback);
    }
    public void reloadTrajectory(WeakReference<AppCompatActivity> activity, Button btn, ReloadAdapter adapter, Trajectory trajectory){
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        HashMap<String, String> params = new HashMap<>();
        params.put("startTime",trajectory.getStartTime());
        params.put("endTime",trajectory.getEndTime());
        Request request = okHttpUtils.createBuilder(url,resources.getString(R.string.trajectory_request_end_trajectory),params)
                .addHeader(activity.get().getString(R.string.request_header_key_token), Objects.requireNonNull(preferences.getString(activity.get().getString(R.string.user_pref_key_token), null)))
                .build();
        Callback callback = new TrajectoryCallBackUtils(successCode).reloadTrajectory(activity, trajectory.getTrajectoryInfoId(), btn,adapter,trajectory);
        okHttpUtils.sendRequest(request, callback);
    }

    public void showPlotByOriginTrajectory(SortMessage sortMessage,AMap aMap) {
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        HashMap<String, Object> params = new HashMap<>();
        params.put(activity.get().getString(R.string.trajectory_request_param_plot_address),sortMessage.getAddress());
        params.put("isCurrent",false);
        Request request = okHttpUtils.createBuilder(url,resources.getString(R.string.font_plot_detail_list_request_get_plot_by_condition),params)
                .addHeader(activity.get().getString(R.string.request_header_key_token), Objects.requireNonNull(preferences.getString(activity.get().getString(R.string.user_pref_key_token), null)))
                .build();
        Callback callback = new TrajectoryCallBackUtils(successCode).getPlotCallBack(activity,aMap);
        okHttpUtils.sendRequest(request, callback);
    }
    public void getExcludeBoundary(Long landPlotId,LandPlotInterface tool,ArrayList<ArrayList<LatLng>> landPlotBoundaryList){
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        HashMap<String, Long> params = new HashMap<>();
        params.put(activity.get().getString(R.string.request_plot_exclude_boundary_param_id), landPlotId);
        Request request = okHttpUtils.createBuilder(url,resources.getString(R.string.request_plot_exclude_boundary),params)
                .addHeader(activity.get().getString(R.string.request_header_key_token), Objects.requireNonNull(preferences.getString(activity.get().getString(R.string.user_pref_key_token), null)))
                .build();
        try(Response response = okHttpUtils.sendRequest(request)){
            //这里是成功的响应
            if(response.isSuccessful()) {
                Gson gson = new Gson();
                Result<ArrayList<ExcludeLandPlot>> result = gson.fromJson(response.body().string(), new TypeToken<Result<ArrayList<ExcludeLandPlot>>>() {
                }.getType());
                if (result.getCode() == successCode) {
                    if (result.getData() != null) {
                        for (ExcludeLandPlot datum : result.getData()) {
                            landPlotBoundaryList.add(decodeLandPlotsBoundary(datum.getBoundary()));
                        }
                    }else{
                        new ToastUtil().getToast(activity.get(),activity.get().getString(R.string.request_plot_exclude_boundary_error_no_data));
                    }
                }else{
                    new ToastUtil().getToast(activity.get(),result.getMsg());
                }
            }else{
                new ToastUtil().getToast(activity.get(),activity.get().getString(R.string.request_plot_exclude_boundary_error_request));
            }
            tool.onGetLandPlotBoundaryList(landPlotBoundaryList);
        }catch (IOException e){
            tool.onGetLandPlotBoundaryList(landPlotBoundaryList);
        };
    }
    public interface LandPlotInterface{
        /**
         * 用于处理拿到了地块边界集合之后的处理逻辑
         * @param landPlotBoundaryList 这个是获取到的地块的边界集合，第一个元素是地块的边界，以后的都是被掏空的地块边界的集合
         */
        void onGetLandPlotBoundaryList(ArrayList<ArrayList<LatLng>> landPlotBoundaryList);
    }
}

