package cn.agrinav.front.util;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.agrinav.front.adapter.GsonLocalDateTimeIsoAdapter;
import cn.agrinav.front.entity.AgriculturalMachinery;
import cn.agrinav.front.entity.AgriculturalMachineryType;
import cn.agrinav.front.entity.FarmWork;
import cn.agrinav.front.entity.HeatmapData;
import cn.agrinav.front.entity.dto.AgriculturalMachineryDTO;
import cn.agrinav.front.entity.dto.FarmWorkDTO;
import cn.agrinav.measure.adapter.GsonLocalDateTimeAdapter;
import cn.agrinav.measure.entity.LandPlot;
import cn.agrinav.measure.entity.Result;
import cn.agrinav.trajectory.entity.CornTypeEntity;
import cn.agrinav.trajectory.entity.vo.CornEntity;
import cn.agrinav.utils.OkHttpUtils;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author 18506
 */
public class QueryUtils {
    private static HashMap<String,Object> pageMap =  new HashMap<>(2);
    private final static int SIX = 6;
    //查询空闲农机
    public static ResultData<List<AgriculturalMachinery>> getQueryFreeAgriculturalMachinery(String url, String path, String token, int pageNum, AgriculturalMachineryDTO agriculturalMachineryDTO) {
        pageMap.put("pageNum",pageNum);
        pageMap.put("pageSize",SIX);
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        //添加适配器
        GsonBuilder gsonBuilder = new GsonBuilder();
        //适配localDateTime
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTimeIsoAdapter());
        Gson gson = gsonBuilder.create();
        String str = gson.toJson(agriculturalMachineryDTO);
        RequestBody requestBody = RequestBody.create(str, MediaType.parse("application/json; charset=utf-8"));
        Request request= okHttpUtils.createBuilder(url,path,pageMap).post(requestBody).addHeader("Authorization",token).build();
        try (Response response = okHttpUtils.sendRequest(request)){
            if(response.isSuccessful()){
                assert response.body() != null;
                String json = response.body().string();
                //todo 修改这个
                ResultData<List<AgriculturalMachinery>> result = gson.fromJson(json,new TypeToken<ResultData<List<AgriculturalMachinery>>>() {}.getType());
                return result;
            }else {
                return null;
            }
        }catch (IOException e){
            return null;
        }
    }

    //查询农活需求
    public static ResultData<List<FarmWork>> getQueryFarmWork(String url, String path, String token, int pageNum, FarmWorkDTO farmWorkDTO) {
        pageMap.put("pageNum",pageNum);
        pageMap.put("pageSize",SIX);
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        //添加适配器
        GsonBuilder gsonBuilder = new GsonBuilder();
        //适配localDateTime
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTimeIsoAdapter());
        Gson gson = gsonBuilder.create();
        String str = gson.toJson(farmWorkDTO);
        RequestBody requestBody = RequestBody.create(str, MediaType.parse("application/json; charset=utf-8"));
        Request request= okHttpUtils.createBuilder(url,path,pageMap).post(requestBody).addHeader("Authorization",token).build();
        try (Response response = okHttpUtils.sendRequest(request)){
            if(response.isSuccessful()){
                assert response.body() != null;
                String json = response.body().string();
                ResultData<List<FarmWork>> result = gson.fromJson(json,new TypeToken<ResultData<List<FarmWork>>>() {}.getType());
                return result;
            }else {
                return null;
            }
        }catch (IOException e){
            return null;
        }
    }
    //添加农机型号
    public static List<AgriculturalMachineryType> getMachineryModel(String url, String path,String token){
        pageMap.put("pageNum",1);
        pageMap.put("pageSize",Integer.MAX_VALUE);
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        //添加适配器
        GsonBuilder gsonBuilder = new GsonBuilder();
        //适配localDateTime
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTimeIsoAdapter());
        Gson gson = gsonBuilder.create();
        Request request= okHttpUtils.createBuilder(url,path,pageMap).get().addHeader("Authorization",token).build();
        try (Response response = okHttpUtils.sendRequest(request)){
            if(response.isSuccessful()){
                assert response.body() != null;
                String json = response.body().string();
                ResultData<List<AgriculturalMachineryType>> result = gson.fromJson(json,new TypeToken<ResultData<List<AgriculturalMachineryType>>>() {}.getType());
                return result.getData();
            }else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<CornEntity> getCrops(String url, String path, String token) {
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        //添加适配器
        GsonBuilder gsonBuilder = new GsonBuilder();
        //适配localDateTime
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTimeIsoAdapter());
        Gson gson = gsonBuilder.create();
        Request request= okHttpUtils.createBuilder(url,path,pageMap).get().addHeader("Authorization",token).build();
        try (Response response = okHttpUtils.sendRequest(request)){
            if(response.isSuccessful()){
                assert response.body() != null;
                String json = response.body().string();
                ResultData<List<CornEntity>> result = gson.fromJson(json,new TypeToken<ResultData<List<CornEntity>>>() {}.getType());
                return result.getData();
            }else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 热力图
     */
    public static List<HeatmapData> getHeatMap(String url, String path, String token) {
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        Gson gson = new Gson();
        Request request= okHttpUtils.createBuilder(url,path).get().addHeader("Authorization",token).build();
        try(Response response = okHttpUtils.sendRequest(request)) {
            if(response.isSuccessful()){
                assert response.body() != null;
                String json = response.body().string();
                Result<List<HeatmapData>> result = gson.fromJson(json,new TypeToken<Result<List<HeatmapData>>>() {}.getType());
                return result.getData();
            }else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询当前所在县的农地数量
     * @param address
     * @return
     */

    public static Integer getLandPlotNum(String url, String path, String address,String token) {
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        pageMap.put("isCurrent",false);
        pageMap.put("address",address);
        GsonBuilder gsonBuilder = new GsonBuilder();
        //适配localDateTime
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTimeAdapter());
        Gson gson = gsonBuilder.create();
        Request request= okHttpUtils.createBuilder(url,path,pageMap).get().addHeader("Authorization", token).build();
        try(Response response = okHttpUtils.sendRequest(request)) {
            if (response.isSuccessful()){
                assert response.body() != null;
                String json = response.body().string();
                ResultData<List<LandPlot>> resultData = gson.fromJson(json,new TypeToken<ResultData<List<LandPlot>>>(){}.getType());
                return  resultData.getTotal();
            }else{
                return -1;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询当前县的地块总面积
     */

    public static Float getLandPlotAreaSum(String url, String path, String address,String token) {
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        pageMap.put("address",address);
        Gson gson = new Gson();
        Request request= okHttpUtils.createBuilder(url,path,pageMap).get().addHeader("Authorization", token).build();
        try(Response response = okHttpUtils.sendRequest(request)) {
            if (response.isSuccessful()){
                assert response.body() != null;
                String json = response.body().string();
                ResultData<Float> resultData = gson.fromJson(json,new TypeToken<ResultData<Float>>(){}.getType());
                return  resultData.getData();
            }else{
                return -0.1f;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取工作类型
     */
    public static List<CornTypeEntity> getWorkTypes(String url, String path, String token) {
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        Gson gson = new Gson();
        Request request= okHttpUtils.createBuilder(url,path).get().addHeader("Authorization",token).build();
        try(Response response = okHttpUtils.sendRequest(request)) {
            if(response.isSuccessful()){
                assert response.body() != null;
                String json = response.body().string();
                Result<List<CornTypeEntity>> result = gson.fromJson(json,new TypeToken<Result<List<CornTypeEntity>>>(){}.getType());
                return result.getData();
            }else {
                return new ArrayList<CornTypeEntity>();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
