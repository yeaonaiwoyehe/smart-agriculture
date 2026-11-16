package cn.agrinav.personal.activity.security;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.io.File;

import cn.agrinav.R;
import cn.agrinav.personal.utils.OkHttpUtil;
import cn.agrinav.user.login.LoginActivity;
import cn.agrinav.utils.DialogUtils;
import cn.agrinav.utils.ToastUtil;

public class SecurityActivity extends AppCompatActivity {

    private MaterialSwitch msFingerLogin;
    private TextView tvGestureLogin;
    private TextView tvChangePhone;
    private BiometricPrompt.PromptInfo promptInfo;
    private BiometricPrompt biometricPrompt;
    private SharedPreferences sharedPreferences;
    // 标记是否正在验证
    private boolean isVerifying;
    private TextView tvAccountCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);
        initViews();
        initEvents();
        initBiometricPrompt();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 禁用按钮事件监听器
        msFingerLogin.setOnCheckedChangeListener(null);
        // 设置按钮状态
        msFingerLogin.setChecked(sharedPreferences.getBoolean(getString(R.string.user_pref_key_finger), false));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setStatusBarColor(Color.rgb(9,204,125));
        }
        // 重新启用按钮事件监听器
        msFingerLogin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !isVerifying) {
                    isVerifying = true;
                    boolean agree = checkSupport();
                    if (agree) {
                        // 指纹弹窗
                        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                                .setTitle(getString(R.string.security_finger_verify))
                                .setSubtitle(null)
                                .setNegativeButtonText(getString(R.string.security_finger_exit))
                                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                                .build();
                        biometricPrompt.authenticate(promptInfo);
                    } else {
                        msFingerLogin.setChecked(false);
                        isVerifying = false;
                    }
                } else {
                    // 关闭指纹登录
                    handleVerificationResult(false);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(getString(R.string.user_pref_key_finger), false);
                    editor.apply();
                }
            }
        });
    }

    private void initBiometricPrompt() {
        biometricPrompt = new BiometricPrompt(this, new BiometricPrompt.AuthenticationCallback() {
            // 错误退出
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                msFingerLogin.setChecked(false);
            }
            // 指纹成功
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // 设置允许指纹登录
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(getString(R.string.user_pref_key_finger), true);
                editor.apply();
            }

            // 指纹有误
            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });
    }

    private void initEvents() {
        // 手势登录
        tvGestureLogin.setOnClickListener(v -> {
           // 跳转至手势登录修改界面
            Intent intent = new Intent(this, GestureChangeActivity.class);
            startActivity(intent);
        });
        // 修改手机号
        tvChangePhone.setOnClickListener(v -> {
            // 弹窗，用户确认是否修改手机号
            showConfirmationDialog();
        });
        // 账号注销
        tvAccountCancel.setOnClickListener(v -> {
            // 弹窗，用户确认是否注销账号
            showCancelDialog();
        });
    }

    private void showCancelDialog() {
        DialogUtils dialogUtils = new DialogUtils(this);
        AlertDialog dialog = dialogUtils.getDialog();
        dialogUtils.setTitle(getString(R.string.security_account_cancel_title))
                .setContent(getString(R.string.security_account_cancel_message))
                .setPositiveText(getString(R.string.confirmation_positive_button))
                .setNegativeText(getString(R.string.confirmation_negative_button))
                .setPositiveListener(v -> {
                    // 用户点击了“确认”，执行注销账号的操作
                    String userName = sharedPreferences.getString(getString(R.string.user_pref_key_user_name), "");
                    String url = getString(R.string.server_url) + getString(R.string.security_account_cancel_request) + userName;
                    String auth = getString(R.string.request_header_key_token);
                    String token = sharedPreferences.getString(getString(R.string.user_pref_key_token), "");
                    OkHttpUtil.logout(url, auth, token, new OkHttpUtil.OnResponseListener() {
                        @Override
                        public void onSuccess(String response) {
                            runOnUiThread(() -> {
                                Toast.makeText(SecurityActivity.this, getString(R.string.security_account_cancel_success), Toast.LENGTH_SHORT).show();
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                String logout = sharedPreferences.getString(getString(R.string.user_pref_key_user_state), "");
                                editor.clear();
                                editor.putString(getString(R.string.user_state_logout), logout);
                                editor.putBoolean(getString(R.string.user_first), false);
                                editor.apply();
                                Intent intent = new Intent(SecurityActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            });
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            runOnUiThread(() -> {
                                Toast.makeText(SecurityActivity.this, getString(R.string.security_account_cancel_failure), Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                    dialog.cancel();
                })
                .setNegativeListener(v -> {
                    // 用户点击了“取消”，不执行任何操作
                    dialog.cancel();
                });
        View view = dialogUtils.getView();
        dialog.setContentView(view);
        dialog.show();
    }

    private void showConfirmationDialog() {
        DialogUtils dialogUtils = new DialogUtils(this);
        AlertDialog dialog = dialogUtils.getDialog();
        dialogUtils.setTitle(getString(R.string.confirmation_title))
                .setContent(getString(R.string.confirmation_message))
                .setPositiveText(getString(R.string.confirmation_positive_button))
                .setNegativeText(getString(R.string.confirmation_negative_button))
                .setPositiveListener(v -> {
                    // 用户点击了“确认”，跳转到手机号修改界面
                    Intent intent = new Intent(SecurityActivity.this, PhoneChangeActivity.class);
                    startActivity(intent);
                })
                .setNegativeListener(v -> {
                    // 用户点击了“取消”，不执行任何操作
                    dialog.cancel();
                });
        View view = dialogUtils.getView();
        dialog.setContentView(view);
        dialog.show();
    }

    private void handleVerificationResult(boolean success) {
        if (success) {
            // 验证成功后保持按钮状态
            msFingerLogin.setChecked(true);
        } else {
            // 验证失败后恢复按钮状态
            msFingerLogin.setChecked(false);
        }
        isVerifying = false;
    }

    public boolean checkSupport() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            BiometricManager biometricManager = BiometricManager.from(this);
            ToastUtil toastUtil = new ToastUtil();
            switch (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
                case BiometricManager.BIOMETRIC_SUCCESS:
                    return true;
                case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                    //生物识别设备不存在
                    toastUtil.getToast(this,getString(R.string.security_finger_not_support));
                    return false;
                case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                    //生物识别不可使用
                    toastUtil.getToast(this,getString(R.string.security_finger_not_use));
                    return false;
                default:
                    toastUtil.getToast(this,getString(R.string.security_finger_not_have));
                    return false;
            }
        }else {
            return false;
        }
    }

    private void initViews() {
        msFingerLogin = findViewById(R.id.ms_finger_login);
        tvGestureLogin = findViewById(R.id.tv_gesture_login);
        tvChangePhone = findViewById(R.id.tv_change_phone);
        tvAccountCancel = findViewById(R.id.tv_account_cancel);
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
    }
}