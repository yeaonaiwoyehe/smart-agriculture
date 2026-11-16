package cn.agrinav.personal.activity.security;

import android.annotation.SuppressLint;
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
import cn.agrinav.utils.mobile.PhoneNumberUtil;

public class PhoneVerifyActivity extends AppCompatActivity {

    private TextView tvPhoneVerify;
    private VerificationCodeView edtCode;
    private Button btnGetCode;
    private SharedPreferences sharedPreferences;
    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_verify);
        initViews();
        initData();
        initEvents();
        click(btnGetCode);
    }

    private void click(Button btnGetCode) {
        btnGetCode.performClick();
    }

    private void initData() {
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        phoneNumber = sharedPreferences.getString(getString(R.string.user_pref_key_phone), "");
    }

    private void initEvents() {
        btnGetCode.setOnClickListener(v -> {
            // 获取验证码
            sendCode();
            //在这段事时间内设定按钮状态不可使用
            btnGetCode.setEnabled(false);
            // 设置手机号
            String phone = PhoneNumberUtil.changePhoneNumber(phoneNumber);
            tvPhoneVerify.setText(phone);
            //这个时候显示出来手机号
            tvPhoneVerify.setVisibility(View.VISIBLE);
            //这里进行按钮的计时并且定时更改文本内容
            new CountDownTimer(60000,1000){
                @SuppressLint("SetTextI18n")
                @Override
                public void onTick(long millisUntilFinished) {
                    //首先按钮内容更改
                    btnGetCode.setText(getString(R.string.security_verification_get_code_again)+"("+(((int)millisUntilFinished/1000)+1)+"s)");
                }
                @Override
                public void onFinish() {
                    //更改按钮状态
                    btnGetCode.setEnabled(true);
                    //更改按钮的内容
                    btnGetCode.setText(getString(R.string.security_verification_get_code_again));
                }
            }.start();
        });
        // 输入六位验证码之后
        edtCode.setOnInputFinishListener(v->{
            // 校验
            String input = Objects.requireNonNull(edtCode.getText()).toString();
            if (input.isEmpty()) {
                Toast.makeText(this, getString(R.string.security_verification_code_input_please), Toast.LENGTH_SHORT).show();
            } else {
                // 发送校验请求
                String url = getString(R.string.server_url) + getString(R.string.security_phone_number_validate) + phoneNumber + getString(R.string.security_and_request) + getString(R.string.verification_code_request_param_key_login_captcha) + getString(R.string.security_equal_request) + input;
                String auth = getString(R.string.request_header_key_token);
                String token = sharedPreferences.getString(getString(R.string.user_pref_key_token), "");

                OkHttpUtil.verifyCode(url, auth, token, new OkHttpUtil.OnResponseListener() {
                    @Override
                    public void onSuccess(String response) {
                        // 验证码正确
                        runOnUiThread(() -> {
                            // 跳转到更改手机号界面
                            Intent intent = new Intent(PhoneVerifyActivity.this, PhoneChangeActivity.class);
                            startActivity(intent);
                            finish();
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        // 验证码错误
                        runOnUiThread(() -> {
                            Toast.makeText(PhoneVerifyActivity.this, getString(R.string.security_code_error), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });
    }

    private void sendCode() {
        String url = getString(R.string.server_url) + getString(R.string.verification_code_router_captcha) + "?" + getString(R.string.security_code_send_request) + getString(R.string.security_equal_request) + phoneNumber;
        String auth = getString(R.string.request_header_key_token);
        String token = sharedPreferences.getString(getString(R.string.user_pref_key_token), "");
        OkHttpUtil.getVerificationCode(url, auth, token, new OkHttpUtil.OnResponseListener() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    Toast.makeText(PhoneVerifyActivity.this, getString(R.string.security_verification_code_send), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(PhoneVerifyActivity.this, getString(R.string.security_verification_code_send_failure) + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    private void initViews() {
        tvPhoneVerify = findViewById(R.id.tv_phone_verify);
        edtCode = findViewById(R.id.edt_code);
        btnGetCode = findViewById(R.id.btn_code_get_code);
    }
}