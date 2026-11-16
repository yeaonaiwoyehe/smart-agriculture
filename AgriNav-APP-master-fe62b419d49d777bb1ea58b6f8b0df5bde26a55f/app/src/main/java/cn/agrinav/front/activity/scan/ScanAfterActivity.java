package cn.agrinav.front.activity.scan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.utils.OkHttpUtils;
import cn.agrinav.utils.Result;
import cn.agrinav.utils.ToastUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

/**
 * @author 11230
 */
public class ScanAfterActivity extends AppCompatActivity {

    private Button btnConfirm;
    private Button btnCancel;
    private String result;
    private SharedPreferences preferences;
    private int successCode;
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scan_after);
        result = getIntent().getStringExtra("result");
        preferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        successCode = getResources().getInteger(R.integer.request_success);
        threadPoolExecutor = new ThreadPoolExecutor(
                10,
                20,
                10,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        initView();
        initEvents();
    }

    private void initEvents() {
        btnCancel.setOnClickListener(v -> {
            Intent intentBack = new Intent(this, BaseActivity.class);
            intentBack.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            intentBack.putExtra(getString(R.string.fragment_index), getResources().getInteger(R.integer.fragment_index_front));
            startActivity(intentBack);
            finish();
        });
        btnConfirm.setOnClickListener(v -> {
            OkHttpUtils okHttpUtils = new OkHttpUtils();
              HashMap<String, String> params = new HashMap<>();
            params.put("uuid",result);
            Request.Builder builder = okHttpUtils
                    .createBuilder(getString(R.string.server_url), "/loginQR", params)
                    .addHeader(getString(R.string.request_header_key_token), Objects.requireNonNull(preferences.getString(getString(R.string.user_pref_key_token), null)));
            builder.post(new RequestBody() {
                @Nullable
                @Override
                public MediaType contentType() {
                    return null;
                }

                @Override
                public void writeTo(@NonNull BufferedSink bufferedSink) throws IOException {

                }
            });
            Request request = builder.build();
            threadPoolExecutor.execute(() -> {
                Callback callback = new Callback() {
                    Handler handler = new Handler(Looper.getMainLooper());
                    ToastUtil toastUtil = new ToastUtil();
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        //告知用户网络异常或者服务器状态异常
                        showToast(getString(R.string.toast_err_net_or_server));
                        // 添加日志记录
                        Log.e("AgriNavScan", "网络请求失败", e);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
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
                                        handler.post(()->{
                                            Intent intentBack = new Intent(ScanAfterActivity.this, BaseActivity.class);
                                            intentBack.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                                            intentBack.putExtra(getString(R.string.fragment_index), getResources().getInteger(R.integer.fragment_index_front));
                                            startActivity(intentBack);
                                            finish();
                                        });
                                    }else {
                                        showToast(result.getMsg());
                                    }
                                }catch (JsonSyntaxException e){
                                    showToast(getString(R.string.toast_err_json));
                                    Log.e("getWorkCallBack", "JSON解析错误", e);
                                }
                            } else {
                                // 响应体为空就提示用户，一般是后端有了问题
                                showToast(getString(R.string.toast_err_server));
                            }
                        } else {
                            //这里说明状态码都有问题，那么我就需要修改请求
                            showToast(getString(R.string.toast_err_request));
                        }
                    }
                    private void showToast(String message) {
                        // 使用 Handler 在主线程中显示 Toast
                        handler.post(() -> {
                            toastUtil.getToast(ScanAfterActivity.this, message+"请重新授权");
                        });
                     }
                };
                okHttpUtils.sendRequest(request,callback);
            });
        });
    }
    private void initView() {
        btnConfirm = findViewById(R.id.btn_confirm_scan_after);
        btnCancel = findViewById(R.id.btn_cancel_scan_after);
    }
}