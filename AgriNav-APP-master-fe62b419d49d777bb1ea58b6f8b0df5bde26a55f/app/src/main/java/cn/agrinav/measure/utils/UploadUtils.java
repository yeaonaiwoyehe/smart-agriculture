package cn.agrinav.measure.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;


import com.amap.api.maps.model.LatLng;
import com.google.gson.Gson;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import cn.agrinav.front.entity.LandPlotExclude;
import cn.agrinav.measure.entity.LandPlot;
import cn.agrinav.measure.entity.MyPoint;

import cn.agrinav.measure.entity.Result;
import cn.agrinav.utils.OkHttpUtils;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author 18506
 */
public class UploadUtils {
    public static void upload(Activity activity, String url, LandPlot landPlot, File file, Handler handler) {
        // 获取本地存储的 token
        SharedPreferences preferences = activity.getSharedPreferences("verification", Context.MODE_PRIVATE);
        String token = preferences.getString("token", "");
        // 将 landPlot 转成 JSON 字符串
        Gson gson = new Gson();
        String str = gson.toJson(landPlot);
        OkHttpUtils okHttp = new OkHttpUtils();
        // 创建 MultipartBody.Builder
        MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        // 将 landPlot 添加到请求体中
        multipartBodyBuilder.addFormDataPart("landPlot",str);
        // 根据文件类型设置合适的媒体类型
        multipartBodyBuilder.addFormDataPart("file", file.getPath(), RequestBody.create(file, MediaType.parse("application/octet-stream")));
        RequestBody requestBody = multipartBodyBuilder.build();
        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("Authorization",token)
                .post(requestBody);
        try (Response response = okHttp.sendRequest(builder.build())) {
            String string = response.body().string();
            Result result = gson.fromJson(string, Result.class);
            if (response.isSuccessful()) {
                handler.sendMessage(handler.obtainMessage(result.getCode(), result.getData()));
            } else {
                // 尝试从响应体中获取更详细的错误信息
                handler.sendMessage(handler.obtainMessage((result.getCode())));
            }
        } catch (IOException e) {
            Log.i("uploadError", e.getMessage());
        }
    }

    public static void upload(Activity activity, String url, LandPlotExclude landPlot, File file, Handler handler) {
        // 获取本地存储的 token
        SharedPreferences preferences = activity.getSharedPreferences("verification", Context.MODE_PRIVATE);
        String token = preferences.getString("token", "");
        // 将 landPlot 转成 JSON 字符串
        Gson gson = new Gson();
        String str = gson.toJson(landPlot);
        OkHttpUtils okHttp = new OkHttpUtils();
        // 创建 MultipartBody.Builder
        MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        // 将 landPlot 添加到请求体中
        multipartBodyBuilder.addFormDataPart("landPlot",str);
        // 根据文件类型设置合适的媒体类型
        multipartBodyBuilder.addFormDataPart("file", file.getPath(), RequestBody.create(file, MediaType.parse("application/octet-stream")));
        RequestBody requestBody = multipartBodyBuilder.build();
        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("Authorization",token)
                .post(requestBody);
        try (Response response = okHttp.sendRequest(builder.build())) {
            String string = response.body().string();
            Result result = gson.fromJson(string, Result.class);
            if (response.isSuccessful()) {
                handler.sendMessage(handler.obtainMessage(result.getCode(), result.getData()));
            } else {
                // 尝试从响应体中获取更详细的错误信息
                handler.sendMessage(handler.obtainMessage((result.getCode())));
            }
        } catch (IOException e) {
        }
    }
    //对上传的点集进行拼接
    public static String transformation(List<MyPoint> myPoints){
        StringBuilder sb = new StringBuilder();
        sb.append("POLYGON((");
        for (MyPoint myPoint : myPoints){
            sb.append(myPoint.getX()).append(" ").append(myPoint.getY()).append(",");
        }
        //删除最后一个逗号
        sb.append(myPoints.get(0).getX()).append(" ").append(myPoints.get(0).getY());
        sb.append("))");
        return sb.toString();
    }
    //将返回的字符串转变为点
    public static List<LatLng> reverseTransformation(String polygonString) {
        List<LatLng> latLngList = new ArrayList<>();

        // 移除 "POLYGON((" 和 "))"
        String coordinates = polygonString.substring(9, polygonString.length() - 2);

        // 分割坐标对
        String[] pairs = coordinates.split(",");

        for (String pair : pairs) {
            String[] xy = pair.trim().split("\\s+");
            double x = Double.parseDouble(xy[0]);
            double y = Double.parseDouble(xy[1]);
            latLngList.add(new LatLng(x, y));
        }

        // 移除最后一个点(因为它与第一个点重复)
        latLngList.remove(latLngList.size() - 1);

        return latLngList;
    }
    //对中心点进行修改toString
    public static String transformationCenter(MyPoint myPoint){
        return "(" +
                myPoint.getX() + " " + myPoint.getY() +
                ")";
    }


}
