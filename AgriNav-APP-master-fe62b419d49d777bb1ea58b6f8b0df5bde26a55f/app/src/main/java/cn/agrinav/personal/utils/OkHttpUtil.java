package cn.agrinav.personal.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import cn.agrinav.utils.OkHttpUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHttpUtil {

    private static final OkHttpClient client;

    static {
        new OkHttpUtils();
        client = OkHttpUtils.okHttpClient;
    }
//    private static final OkHttpClient client = new OkHttpClient();

    public static void putNickName(String url, String auth, String token, String body, OnResponseListener listener) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body);
        // 创建请求
        Request request = new Request.Builder()
                .url(url)
                .addHeader(auth, token)
                .put(requestBody)
                .build();
        // 发送请求
        client.newCall(request).enqueue(new Callback() {
            // 请求失败
            @Override
            public void onFailure(Call call, IOException e) {
                listener.onFailure(e.getMessage());
            }

            // 请求成功
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObject = parser.parse(responseBody).getAsJsonObject();
                    int code = jsonObject.get("code").getAsInt();
                    if(code == 200) {
                        listener.onSuccess(responseBody);
                    }else {
                        listener.onFailure(jsonObject.get("msg").getAsString());
                    }
                }else {
                    listener.onFailure("请求失败");
                }
            }
        });
    }

    public static void upLoadAvatar(String url, String auth, String token, File avatarFile, OnResponseListener listener) {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("avatarfile", avatarFile.getName(), RequestBody.create(avatarFile, MediaType.parse("application/octet-stream")))
                .build();
        // 创建请求
        Request request = new Request.Builder()
                .url(url)
                .addHeader(auth, token)
                .post(requestBody)
                .build();
        // 发送请求
        client.newCall(request).enqueue(new Callback() {
            // 请求失败
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                listener.onFailure(e.getMessage());
            }

            // 请求成功
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObject = parser.parse(responseBody).getAsJsonObject();
                    int code = jsonObject.get("code").getAsInt();
                    if(code == 200) {
                        listener.onSuccess(responseBody);
                    }else {
                        listener.onFailure(jsonObject.get("msg").getAsString());
                    }
                }else {
                    listener.onFailure("请求失败");
                }
            }
        });
    }

    public static void getVerificationCode(String url, String auth, String token, OnResponseListener listener) {
        // 创建请求
        Request request = new Request.Builder()
                .url(url)
                .addHeader(auth, token)
                .get()
                .build();
        // 发送请求并获取响应
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObject = parser.parse(responseBody).getAsJsonObject();
                    int code = jsonObject.get("code").getAsInt();
                    if(code == 200) {
                        listener.onSuccess(responseBody);
                    }else {
                        listener.onFailure(jsonObject.get("msg").getAsString());
                    }
                }else {
                    listener.onFailure("请求失败");
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                listener.onFailure(e.getMessage());
            }
        });
    }

    public static void verifyCode(String url, String auth, String token, OnResponseListener listener) {
        // 创建请求
        Request request = new Request.Builder()
                .url(url)
                .addHeader(auth, token)
                .get()
                .build();
        // 发送请求
        client.newCall(request).enqueue(new Callback() {
            // 请求失败
            @Override
            public void onFailure(Call call, IOException e) {
                listener.onFailure(e.getMessage());
            }

            // 请求成功
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObject = parser.parse(responseBody).getAsJsonObject();
                    int code = jsonObject.get("code").getAsInt();
                    if(code == 200) {
                        listener.onSuccess(responseBody);
                    }else {
                        listener.onFailure(jsonObject.get("msg").getAsString());
                    }
                }else {
                    listener.onFailure("请求失败");
                }
            }
        });
    }

    public static void updatePhoneNumber(String url, String auth, String token, String body, OnResponseListener listener) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body);
        Request request = new Request.Builder()
                .url(url)
                .addHeader(auth, token)
                .put(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            // 请求失败
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                listener.onFailure(e.getMessage());
            }

            // 请求成功
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObject = parser.parse(responseBody).getAsJsonObject();
                    int code = jsonObject.get("code").getAsInt();
                    if(code == 200) {
                        listener.onSuccess(responseBody);
                    }else {
                        listener.onFailure(jsonObject.get("msg").getAsString());
                    }
                }else {
                    listener.onFailure("请求失败");
                }
            }
        });
    }

    public static void getTags(String url, String auth, String token, OnResponseListener listener) {
        // 创建请求
        Request request = new Request.Builder()
                .url(url)
                .addHeader(auth, token)
                .get()
                .build();
        // 发送请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObject = parser.parse(responseBody).getAsJsonObject();
                    int code = jsonObject.get("code").getAsInt();
                    if(code == 200) {
                        listener.onSuccess(responseBody);
                    }else {
                        listener.onFailure(jsonObject.get("msg").getAsString());
                    }
                }else {
                    listener.onFailure("请求失败");
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                listener.onFailure(e.getMessage());
            }
        });
    }

    public static void sendFeedBack(String url, String auth, String token, String feedback, List<Integer> selectedCodes, OnResponseListener listener) {
        try {
            // 创建JSON请求体
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("content", feedback);
            
            // 创建标签数组
            JSONArray labelsArray = new JSONArray();
            for (Integer code : selectedCodes) {
                JSONObject labelObj = new JSONObject();
                labelObj.put("dictCode", code);  // 使用dictCode作为标签ID
                labelsArray.put(labelObj);
            }
            jsonBody.put("labels", labelsArray);  // 使用labels字段名与后端对应
            
            // 创建RequestBody
            RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonBody.toString()
            );

            // 创建请求
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader(auth, token)
                    .post(requestBody)
                    .build();

            // 发送请求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    listener.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if(response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JsonParser parser = new JsonParser();
                        JsonObject jsonObject = parser.parse(responseBody).getAsJsonObject();
                        int code = jsonObject.get("code").getAsInt();
                        if(code == 200) {
                            listener.onSuccess(responseBody);
                        }else {
                            listener.onFailure(jsonObject.get("msg").getAsString());
                        }
                    }else {
                        listener.onFailure("请求失败");
                    }
                }
            });
        } catch (JSONException e) {
            listener.onFailure("创建请求数据失败");
        }
    }

    // 注销账户
    public static void logout(String url, String auth, String token, OnResponseListener listener) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader(auth, token)
                .delete()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObject = parser.parse(responseBody).getAsJsonObject();
                    int code = jsonObject.get("code").getAsInt();
                    if(code == 200) {
                        listener.onSuccess(responseBody);
                    }else {
                        listener.onFailure(jsonObject.get("msg").getAsString());
                    }
                }else {
                    listener.onFailure("请求失败");
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                listener.onFailure("请求失败");
            }
        });
    }

    public static void getFeedbackHistory(String url, String auth, String token, OnResponseListener listener) {
        // 创建请求
        Request request = new Request.Builder()
                .url(url)
                .addHeader(auth, token)
                .get()
                .build();
        // 发送请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObject = parser.parse(responseBody).getAsJsonObject();
                    int code = jsonObject.get("code").getAsInt();
                    if(code == 200) {
                        listener.onSuccess(responseBody);
                    }else {
                        listener.onFailure(jsonObject.get("msg").getAsString());
                    }
                }else {
                    listener.onFailure("请求失败");
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                listener.onFailure("请求失败");
            }
        });
    }

    public interface OnResponseListener {
        void onSuccess(String response);
        void onFailure(String errorMessage);
    }
}

