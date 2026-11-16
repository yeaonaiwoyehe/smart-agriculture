package cn.agrinav.front.util;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import cn.agrinav.measure.adapter.GsonLocalDateTimeAdapter;
import cn.agrinav.measure.entity.LandPlot;
import cn.agrinav.measure.entity.Result;
import cn.agrinav.utils.OkHttpUtils;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author 11230
 */
public class PlotUtil {
    public List<LandPlot> getLandPlotListNoneCondition(String ip, String path, String token, int pageNum,int pageSize) {
        HashMap<String,Integer> pageMap = new HashMap<>();
        pageMap.put("current",pageNum);
        pageMap.put("size",pageSize);
        //访问后端
        OkHttpUtils okHttpUtils = new OkHttpUtils();
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
}
