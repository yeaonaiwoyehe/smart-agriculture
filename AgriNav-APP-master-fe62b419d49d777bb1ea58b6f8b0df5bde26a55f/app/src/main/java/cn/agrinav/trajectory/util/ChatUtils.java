package cn.agrinav.trajectory.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;

import cn.agrinav.R;
import cn.agrinav.trajectory.activity.TrajectoryActivity;
import cn.agrinav.trajectory.entity.Trajectory;
import cn.agrinav.utils.OkHttpUtils;
import cn.agrinav.utils.Result;
import okhttp3.MediaType;
import okhttp3.OkHttp;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

/**
 * @author 11230
 */
public class ChatUtils {
    private final Resources resources;
    private final SharedPreferences preferences;
    private AppCompatActivity activity;
    private int successCode;
    private static String url;

    public ChatUtils(AppCompatActivity activity) {
        this.activity = activity;
        resources = activity.getResources();
        //获取成功码
        successCode = resources.getInteger(R.integer.request_success);
        //获取preferences
        preferences = activity.getSharedPreferences(activity.getString(R.string.user_pref_name), Context.MODE_PRIVATE);
        if (url==null){
            url = preferences.getString(activity.getString(R.string.user_pref_key_server),activity.getString(R.string.server_url));
        }
    }
    public boolean sendTrajectoryMessage(String trajectoryJson){
        OkHttpUtils okHttpUtils =new OkHttpUtils();
        Request.Builder builder = okHttpUtils.createBuilder(url, "/trajectoryInfo");
        RequestBody requestBody = new RequestBody() {
            @Nullable
            @Override
            public MediaType contentType() {
                return MediaType.parse("application/json");
            }

            @Override
            public void writeTo(@NonNull BufferedSink bufferedSink) throws IOException {
                bufferedSink.write(trajectoryJson.getBytes());
                bufferedSink.flush();
            }
        };
        builder.header(activity.getString(R.string.request_header_key_token),preferences.getString(activity.getString(R.string.user_pref_key_token), ""));
        builder.method("POST",requestBody);
        Request build = builder.build();
        try(Response response = okHttpUtils.sendRequest(build)){
            String resultJson;
            //检测是否可以得到正确的状态码
            if (response.code()==successCode){
                //正确链接
                if (response.body()!=null){
                    //获取响应体
                    resultJson = response.body().string();
                    Gson gson = new Gson();
                    int result = gson.fromJson(resultJson,Result.class).getCode();
                    return result == successCode;
                }else {
                    //未获取到正确结果，默认返回false
                    return false;
                }
            }else {
                //错误链接返回false
                return false;
            }
        }catch (IOException e){
            //错误链接返回false
            return false;
        }
    }


}
