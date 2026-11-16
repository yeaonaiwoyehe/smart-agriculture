package cn.agrinav.front.activity.sysmessage.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.front.entity.SysMessage;
import cn.agrinav.measure.utils.ThreadPoolUtils;
import cn.agrinav.utils.OkHttpUtils;
import cn.agrinav.utils.Result;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 数据工具类，处理系统消息相关的所有操作
 */
public class SysMessageUtils {
    private static final String TAG = "DataUtil";
    private static final String KEY_HAS_UNREAD_MESSAGE = "has_unread_message";
    /**
     * 线程池配置
     */
    private static final int CORE_POOL_SIZE = 5;
    private static final int MAXIMUM_POOL_SIZE = 6;
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    /**
     * 创建线程池
     */
    public static final ExecutorService threadPoolExecutor = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAXIMUM_POOL_SIZE,
            KEEP_ALIVE_TIME,
            KEEP_ALIVE_TIME_UNIT,
            new LinkedBlockingQueue<>()
    );
    /**
     * 获取所有系统通知
     */
    public static void getAllMessages(Context context, Consumer<List<SysMessage>> callback) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.user_pref_name), Context.MODE_PRIVATE);

        threadPoolExecutor.execute(() -> {
            try {
                OkHttpUtils.okHttpClient
                        .newCall(new OkHttpUtils().createBuilder(context.getString(R.string.server_url),
                                        "/system/notification")
                                .addHeader(context.getString(R.string.request_header_key_token),
                                        sharedPreferences.getString(context.getString(R.string.user_pref_key_token), " "))
                                .get()
                                .build())
                        .enqueue(new Callback() {
                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                handleMessageResponse(response, callback);
                            }

                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                Log.e(TAG, "获取所有消息失败: " + e.getMessage());
                                callback.accept(null);
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "执行获取所有消息失败: " + e.getMessage());
                callback.accept(null);
            }
        });
    }

    /**
     * 处理消息响应
     */
    private static void handleMessageResponse(Response response, Consumer<List<SysMessage>> callback) {
        try {
            if (response.isSuccessful() && response.body() != null) {
                String responseData = response.body().string();
                Gson gson = new Gson();
                Result<List<SysMessage>> result = gson.fromJson(responseData,
                        new TypeToken<Result<List<SysMessage>>>() {}.getType());

                if (result.getCode() == 200) {
                    List<SysMessage> data = result.getData();
                    if (callback != null) {
                        callback.accept(data);
                    }
                } else {
                    Log.e(TAG, "获取消息列表失败: " + result.getMsg());
                    if (callback != null) {
                        callback.accept(null);
                    }
                }
            } else {
                Log.e(TAG, "获取消息列表失败: HTTP " + response.code());
                if (callback != null) {
                    callback.accept(null);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "解析消息响应失败: " + e.getMessage());
            if (callback != null) {
                callback.accept(null);
            }
        }
    }
    /**
     * 标记所有消息为已读
     */
    public static void markAllMessagesAsRead(Context context, Runnable onSuccess) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.user_pref_name), Context.MODE_PRIVATE);

        threadPoolExecutor.execute(() -> {
            try {
                OkHttpUtils.okHttpClient
                        .newCall(new OkHttpUtils().createBuilder(context.getString(R.string.server_url),
                                        "/system/notification/all")
                                .addHeader(context.getString(R.string.request_header_key_token),
                                        sharedPreferences.getString(context.getString(R.string.user_pref_key_token), " "))
                                .put(RequestBody.create("", null))
                                .build())
                        .enqueue(new Callback() {
                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                if (response.isSuccessful() && onSuccess != null) {
                                    // 清除本地未读状态
                                    clearUnreadMessageStatus(context);
                                    // 通知 BaseActivity 更新状态
                                    if (onSuccess != null) {
                                        onSuccess.run();
                                    }
                                }
                            }
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                Log.e(TAG, "一键已读失败: " + e.getMessage());
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "执行一键已读失败: " + e.getMessage());
            }
        });
    }

    /**
     * 检查是否有未读消息并更新状态
     * @param context 上下文
     * @return 是否成功检查
     */
    public static boolean checkUnreadMessages(Context context, SharedPreferences sharedPreferences) {
        threadPoolExecutor.execute(() -> {
            try {
                OkHttpUtils okHttpUtils = new OkHttpUtils(); // 创建实例
                Request request = okHttpUtils.createBuilder(context.getString(R.string.server_url),
                                "/system/notification/unread")
                        .addHeader(context.getString(R.string.request_header_key_token),
                                sharedPreferences.getString(context.getString(R.string.user_pref_key_token), " "))
                        .get()
                        .build();

                okHttpUtils.sendRequest(request, new Callback() {
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseData = response.body().string();
                            Log.i("boom", "收到的响应数据: " + responseData);
                            Gson gson = new Gson();
                            try {
                                Result<List<SysMessage>> result = gson.fromJson(responseData,
                                        new TypeToken<Result<List<SysMessage>>>() {}.getType());
                                if (result.getCode() == 200) {
                                    boolean hasUnread = result.getData() != null && !result.getData().isEmpty();
                                    // 保存未读消息状态
                                    saveUnreadMessageStatus(context, hasUnread);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "解析响应失败: " + e.getMessage());
                                Log.e(TAG, "异常堆栈: ", e);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e(TAG, "检查未读消息失败: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "执行网络请求失败: " + e.getMessage());
            }
        });
        return true;
    }




    /**
     * 获取未读系统通知
     */
    public static void getUnreadMessages(Context context, Consumer<List<SysMessage>> onSuccess) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.user_pref_name), Context.MODE_PRIVATE);

        threadPoolExecutor.execute(() -> {
            try {
                OkHttpUtils.okHttpClient
                        .newCall(new OkHttpUtils().createBuilder(context.getString(R.string.server_url),
                                        "/system/notification/unread")
                                .addHeader(context.getString(R.string.request_header_key_token),
                                        sharedPreferences.getString(context.getString(R.string.user_pref_key_token), " "))
                                .get()
                                .build())
                        .enqueue(new Callback() {
                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                handleMessageResponse(response, onSuccess);
                            }

                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                Log.e(TAG, "获取未读消息失败: " + e.getMessage());
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "执行获取未读消息失败: " + e.getMessage());
            }
        });
    }

    /**
     * 标记指定消息为已读
     */
    public static void markMessageAsRead(Context context, String messageId, Runnable onSuccess) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.user_pref_name), Context.MODE_PRIVATE);

        threadPoolExecutor.execute(() -> {
            try {
                OkHttpUtils.okHttpClient
                        .newCall(new OkHttpUtils().createBuilder(context.getString(R.string.server_url),
                                        "/system/notification/" + messageId)
                                .addHeader(context.getString(R.string.request_header_key_token),
                                        sharedPreferences.getString(context.getString(R.string.user_pref_key_token), " "))
                                .put(RequestBody.create("", null))
                                .build())
                        .enqueue(new Callback() {
                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                if (response.isSuccessful() && onSuccess != null) {
                                    // 更新本地未读状态
                                    checkUnreadMessages(context,sharedPreferences);
                                    onSuccess.run();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                Log.e(TAG, "标记已读失败: " + e.getMessage());
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "执行标记已读失败: " + e.getMessage());
            }
        });
    }



    /**
     * 删除所有系统通知
     */
    public static void deleteAllMessages(Context context, Runnable onSuccess) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.user_pref_name), Context.MODE_PRIVATE);

        threadPoolExecutor.execute(() -> {
            try {
                OkHttpUtils.okHttpClient
                        .newCall(new OkHttpUtils().createBuilder(context.getString(R.string.server_url),
                                        "/system/notification")
                                .addHeader(context.getString(R.string.request_header_key_token),
                                        sharedPreferences.getString(context.getString(R.string.user_pref_key_token), " "))
                                .delete()
                                .build())
                        .enqueue(new Callback() {
                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                if (response.isSuccessful() && onSuccess != null) {
                                    // 清除本地未读状态
                                    clearUnreadMessageStatus(context);
                                    onSuccess.run();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                Log.e(TAG, "删除所有消息失败: " + e.getMessage());
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "执行删除所有消息失败: " + e.getMessage());
            }
        });
    }


    /**
     * 保存未读消息状态
     */
    private static void saveUnreadMessageStatus(Context context, boolean hasUnread) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.user_pref_name), Context.MODE_PRIVATE);
        sharedPreferences.edit()
                .putBoolean(KEY_HAS_UNREAD_MESSAGE, hasUnread)
                .apply();
    }

    /**
     * 获取未读消息状态
     */
    public static boolean hasUnreadMessages(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.user_pref_name), Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(KEY_HAS_UNREAD_MESSAGE, false);
    }

    /**
     * 清除未读消息状态
     */
    public static void clearUnreadMessageStatus(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.user_pref_name), Context.MODE_PRIVATE);
        sharedPreferences.edit()
                .putBoolean(KEY_HAS_UNREAD_MESSAGE, false)
                .apply();
    }
}