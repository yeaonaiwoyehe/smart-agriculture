package cn.agrinav.measure.utils;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;


import cn.agrinav.front.util.ResultData;
import cn.agrinav.measure.adapter.GsonLocalDateTimeAdapter;
import cn.agrinav.measure.entity.LandAddress;
import cn.agrinav.measure.entity.LandPlot;

import java.io.IOException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


import cn.agrinav.measure.entity.Result;
import cn.agrinav.utils.OkHttpUtils;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author 18506
 */
public class QueryUtils {
    //用于存放需要获取从多少条到多少条数据的值
    private static HashMap<String,Integer> pageMap =  new HashMap<>(4);
    private static OkHttpUtils okHttpUtils;
    //查询最近的地块，显示在GirdView中
    private final static int SIX = 6;
    public static List<LandPlot> getLandPlotList(String ip, String path, String token,int page) {
        pageMap.put("current",page);
        pageMap.put("size",SIX);
        //访问后端
        okHttpUtils = new OkHttpUtils();
            Request request= okHttpUtils.createBuilder(ip,path,pageMap).addHeader("Authorization",token).get().build();
            try (Response response = okHttpUtils.sendRequest(request)) {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    String json = response.body().string();
                    //添加适配器
                    GsonBuilder gsonBuilder = new GsonBuilder();
                    //适配localDateTime
                    gsonBuilder.registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTimeAdapter());
                    Gson gson = gsonBuilder.create();
                    Result<List<LandPlot>> result = gson.fromJson(json,new TypeToken<Result<List<LandPlot>>>() {}.getType());
                    Log.i("aaa", result.toString());
                    if (result.getCode() != 200) {
                        return null;
                    } else {
                        return result.getData();
                    }
                } else {
                    // 尝试从响应体中获取更详细的错误信息
                    String errorMessage = response.body()!= null? response.body().string() : "Unknown error";
                    System.out.println("Failed to upload image and landPlot. Response code: " + response.code() + ", Error message: " + errorMessage);
                }
            } catch (IOException e) {
                System.out.println("Error occurred during upload: " + e.getMessage());
            }
        return null;
        }
        //查询省份
        public static List<LandAddress> getLandProvince(String url,String path, String token) {
             okHttpUtils = new OkHttpUtils();
             Request request= okHttpUtils.createBuilder(url,path).addHeader("Authorization",token).get().build();
             try (Response response = okHttpUtils.sendRequest(request)){
                 if(response.isSuccessful()){
                     assert response.body() != null;
                     String json = response.body().string();
                     Gson gson =  new Gson();
                     Result<List<LandAddress>> result = gson.fromJson(json,new TypeToken<Result<List<LandAddress>>>() {}.getType());
                     return result.getData();
                 }else {
                     return null;
                 }
             }catch (IOException e){
                 return null;
             }
        }
        //查询城市
    public static List<LandAddress> getLandAddresses(String url,String path, String token,LandAddress landAddress) {
        okHttpUtils = new OkHttpUtils();
        Gson gson =  new Gson();
        String json1 = gson.toJson(landAddress);
        RequestBody requestBody = RequestBody.create(json1, MediaType.get("application/json; charset=utf-8"));
        Request request= okHttpUtils.createBuilder(url,path).post(requestBody).addHeader("Authorization",token).build();
        try (Response response = okHttpUtils.sendRequest(request)){
            if(response.isSuccessful()){
                assert response.body() != null;
                String json = response.body().string();
                ResultData<List<LandAddress>> result = gson.fromJson(json,new TypeToken<ResultData<List<LandAddress>>>() {}.getType());
                return result.getData();
            }else {
                return null;
            }
        }catch (IOException e){
            return null;
        }
    }

//    根据村名称查询地块
    public static List<LandPlot> queryLandPlot(String ip,String path,String token, HashMap<String,?> params) {
            okHttpUtils = new OkHttpUtils();
            Request request= okHttpUtils.createBuilder(ip,path,params).get().addHeader("Authorization",token).build();
            //添加适配器
            GsonBuilder gsonBuilder = new GsonBuilder();
            //适配localDateTime
            gsonBuilder.registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTimeAdapter());
            Gson gson = gsonBuilder.create();
                try (Response response = okHttpUtils.sendRequest(request)){
                if(response.isSuccessful()){
                    assert response.body() != null;
                    String json = response.body().string();
                    Result<List<LandPlot>> result = gson.fromJson(json,new TypeToken<Result<List<LandPlot>>>() {}.getType());
                    return result.getData();
                }else {
                    return null;
                }
            }catch (IOException e){
                return null;
            }
    }
    public static List<LandPlot> queryLandPlot(String ip, String path, String token, HashMap<String, Object> params, int page) {
        params.put("current",page);
        params.put("size",SIX);
        okHttpUtils = new OkHttpUtils();
        Request request= okHttpUtils.createBuilder(ip,path,params).get().addHeader("Authorization",token).build();
        //添加适配器
        GsonBuilder gsonBuilder = new GsonBuilder();
        //适配localDateTime
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTimeAdapter());
        Gson gson = gsonBuilder.create();
        try (Response response = okHttpUtils.sendRequest(request)){
            if(response.isSuccessful()){
                assert response.body() != null;
                String json = response.body().string();
                Result<List<LandPlot>> result = gson.fromJson(json,new TypeToken<Result<List<LandPlot>>>() {}.getType());
                return result.getData();
            }else {
                return null;
            }
        }catch (IOException e){
            return null;
        }
    }
}
