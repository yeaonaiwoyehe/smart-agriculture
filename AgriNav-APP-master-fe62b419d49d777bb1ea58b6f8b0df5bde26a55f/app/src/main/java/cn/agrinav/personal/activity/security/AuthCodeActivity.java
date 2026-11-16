package cn.agrinav.personal.activity.security;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

import cn.agrinav.R;
import cn.agrinav.personal.utils.OkHttpUtil;
import cn.agrinav.user.VerificationCodeView;
import cn.agrinav.user.login.LoginActivity;
import cn.agrinav.utils.mobile.PhoneNumberUtil;

public class AuthCodeActivity extends AppCompatActivity {

    private TextView tvPhoneNew;
    private VerificationCodeView edtCodeNew;
    private Button btnCodeNew;
    private String phoneNumber;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_code);
        initViews();
        initData();
        initEvents();
        click(btnCodeNew);
    }

    private void click(Button btnCodeNew) {
        btnCodeNew.performClick();
    }

    private void initData() {
        // 获取SharedPreferences的实例
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        // 拿到新手机号
        phoneNumber = getIntent().getStringExtra(getString(R.string.security_code_send_request));
    }

    private void initEvents() {
        btnCodeNew.setOnClickListener(v -> {
            sendCode();
            //在这段事时间内设定按钮状态不可使用
            btnCodeNew.setEnabled(false);
            // 设置手机号
            String phone = PhoneNumberUtil.changePhoneNumber(phoneNumber);
            tvPhoneNew.setText(phone);
            //这个时候显示出来手机号
            tvPhoneNew.setVisibility(View.VISIBLE);
            //这里进行按钮的计时并且定时更改文本内容
            new CountDownTimer(60000,1000){
                @SuppressLint("SetTextI18n")
                @Override
                public void onTick(long millisUntilFinished) {
                    //首先按钮内容更改
                    btnCodeNew.setText(getString(R.string.security_verification_get_code_again)+"("+(((int)millisUntilFinished/1000)+1)+"s)");
                }
                @Override
                public void onFinish() {
                    //更改按钮状态
                    btnCodeNew.setEnabled(true);
                    //更改按钮的内容
                    btnCodeNew.setText(getString(R.string.security_verification_get_code_again));
                }
            }.start();
        });
        edtCodeNew.setOnInputFinishListener(v->{
            if (edtCodeNew.getText().toString().isEmpty()) {
                Toast.makeText(this, getString(R.string.security_verification_code_input_please), Toast.LENGTH_SHORT).show();
            } else {
                // 校验
                String code = Objects.requireNonNull(edtCodeNew.getText()).toString();
                if (code.isEmpty()) {
                    Toast.makeText(this, getString(R.string.security_verification_code_input_please), Toast.LENGTH_SHORT).show();
                } else {
                    sendUpdatePhoneNumber(code);
                }
            }
        });
    }

    // 发起更新手机号请求
    private void sendUpdatePhoneNumber(String code) {
        String url = getString(R.string.server_url) + getString(R.string.security_phone_number_update) + phoneNumber + getString(R.string.security_and_request) +getString(R.string.verification_code_request_param_key_login_captcha) + getString(R.string.security_equal_request) + code;
        String auth = getString(R.string.request_header_key_token);
        String token = sharedPreferences.getString(getString(R.string.user_pref_key_token), "");
        OkHttpUtil.updatePhoneNumber(url, auth, token, "", new OkHttpUtil.OnResponseListener() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    logoutUser();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(AuthCodeActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // 退出登录
    private void logoutUser() {
        // 清除登录状态
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.clear();
        edit.putBoolean(getString(R.string.user_first), false);
        edit.apply();
        // 跳转到手机号登录界面
        Intent intent = new Intent(AuthCodeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void sendCode() {
        String url = getString(R.string.server_url) + getString(R.string.verification_code_router_captcha) + "?" + getString(R.string.security_code_send_request) + getString(R.string.security_equal_request) + phoneNumber;
        String auth = getString(R.string.request_header_key_token);
        String token = sharedPreferences.getString(getString(R.string.user_pref_key_token), "");
        // 获取验证码
        OkHttpUtil.getVerificationCode(url, auth, token, new OkHttpUtil.OnResponseListener() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    Toast.makeText(AuthCodeActivity.this, getString(R.string.security_verification_code_send), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(AuthCodeActivity.this, getString(R.string.security_verification_code_send_failure) + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });

    }

    private void initViews() {
        tvPhoneNew = findViewById(R.id.tv_phone_new);
        edtCodeNew = findViewById(R.id.edt_code_new);
        btnCodeNew = findViewById(R.id.btn_code_new_get);
    }
}