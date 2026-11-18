package cn.agrinav.user.login;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.user.entity.RegisterAndLoginBodyEntity;
import cn.agrinav.user.register.RegisterActivity;
import cn.agrinav.utils.OkHttpUtils;
import cn.agrinav.utils.ToastUtil;
import cn.agrinav.utils.mobile.PhoneNumberUtil;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 登录页：仅输入手机号即可直接登录（测试/演示模式，后端免验证码）
 */
public class LoginActivity extends AppCompatActivity {
    private Button btnLogin;
    private TextInputEditText edtPhone;
    private TextView tvRegister;

    private OkHttpUtils okHttpUtils;
    private ThreadPoolExecutor executor;
    private ToastUtil toastUtil;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private final int handlerLogin = 1;
    private final int handlerMsg = 2;
    private final int handlerError = 3;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initViews();
        initInfra();
        initEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initInfra() {
        preferences = getSharedPreferences(getString(R.string.user_pref_name), Context.MODE_PRIVATE);
        editor = preferences.edit();
        okHttpUtils = new OkHttpUtils();
        toastUtil = new ToastUtil();
        executor = new ThreadPoolExecutor(
                10, 20, 10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private void initEvents() {
        mainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == handlerLogin) {
                    String content = (String) msg.obj;
                    RegisterAndLoginBodyEntity body = new Gson().fromJson(content,
                            new TypeToken<RegisterAndLoginBodyEntity>() {}.getType());
                    if (body.getCode() != getResources().getInteger(R.integer.request_success)) {
                        toastUtil.getToast(LoginActivity.this, body.getMsg());
                    } else {
                        // 先存登录凭据，再拉取用户信息
                        editor.putString(getString(R.string.user_pref_key_phone),
                                edtPhone.getText().toString());
                        editor.putString(getString(R.string.user_pref_key_token), body.getToken());
                        editor.putString(getString(R.string.user_pref_key_rsa_public),
                                body.getRsa_public_key());
                        editor.commit();
                        getUserInfo(body.getToken());
                    }
                } else if (msg.what == handlerMsg) {
                    editor.putString(getString(R.string.user_pref_key_user_info), (String) msg.obj);
                    editor.putString(getString(R.string.user_pref_key_user_state),
                            getResources().getString(R.string.user_state_login));
                    editor.commit();
                    Intent intent = new Intent(LoginActivity.this, BaseActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else if (msg.what == handlerError) {
                    toastUtil.getToast(LoginActivity.this, (String) msg.obj);
                }
            }
        };

        btnLogin.setOnClickListener(v -> {
            String userPhone = edtPhone.getText().toString();
            if (!new PhoneNumberUtil().isPhoneNumber(userPhone)) {
                toastUtil.getToast(this, getString(R.string.toast_first_error_not_phone));
                return;
            }
            executor.execute(() -> {
                HashMap<String, String> params = new HashMap<>(1);
                params.put(getString(R.string.verification_code_request_param_key_login_phone), userPhone);
                Request.Builder builder = okHttpUtils.createBuilder(
                        getString(R.string.server_url),
                        getString(R.string.verification_code_router_login), params);
                builder.setMethod$okhttp("POST");
                try (Response response = okHttpUtils.sendRequest(builder.build())) {
                    if (response.body() != null) {
                        mainHandler.sendMessage(mainHandler.obtainMessage(handlerLogin, response.body().string()));
                    }
                } catch (IOException e) {
                    mainHandler.sendMessage(mainHandler.obtainMessage(handlerError,
                            getString(R.string.toast_first_error_verification_code_request_false_text)));
                }
            });
        });

        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void getUserInfo(String token) {
        executor.execute(() -> {
            Request request = okHttpUtils
                    .createBuilder(getString(R.string.server_url),
                            getString(R.string.verification_code_router_getInfo))
                    .addHeader(getString(R.string.request_header_key_token), token)
                    .build();
            try (Response response = okHttpUtils.sendRequest(request)) {
                if (response.body() != null) {
                    mainHandler.sendMessage(mainHandler.obtainMessage(handlerMsg, response.body().string()));
                }
            } catch (IOException e) {
                mainHandler.sendMessage(mainHandler.obtainMessage(handlerError,
                        getString(R.string.toast_first_error_verification_code_request_false_text)));
            }
        });
    }

    private void initViews() {
        tvRegister = findViewById(R.id.tv_login_to_register);
        edtPhone = findViewById(R.id.edt_login_phone);
        btnLogin = findViewById(R.id.btn_login_confirm);
    }
}
