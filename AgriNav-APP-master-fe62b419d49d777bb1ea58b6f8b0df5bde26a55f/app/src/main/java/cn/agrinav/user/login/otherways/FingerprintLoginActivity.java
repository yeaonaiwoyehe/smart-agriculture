package cn.agrinav.user.login.otherways;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.personal.utils.ImageUtil;
import cn.agrinav.user.ProtocolActivity;
import cn.agrinav.user.entity.RegisterAndLoginBodyEntity;
import cn.agrinav.user.login.LoginActivity;
import cn.agrinav.user.register.RegisterActivity;
import cn.agrinav.user.register.VerificationCodeActivity;
import cn.agrinav.utils.OkHttpUtils;
import cn.agrinav.utils.ToastUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.agrinav.utils.mobile.PhoneNumberUtil;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author 11230
 */
public class FingerprintLoginActivity extends AppCompatActivity {

    private TextView tvMore;
    private TextView tvPhone;
    private TextView tvMoreChange;
    private TextView tvMoreRegister;
    private TextView tvMoreCancel;
    private TextView tvChangeWechat;
    private TextView tvChangeMessage;
    private TextView tvChangeCancel;
    private Button btnChange;
    private TextView tvRead;
    private BiometricPrompt.PromptInfo promptInfo;
    private BiometricPrompt biometricPrompt;
    private BottomSheetDialog bottomSheetDialog;
    private View bottomPopWindowMore;
    private View bottomPopWindowChange;
    private Button btnLogin;
    private CheckBox radioButton;
    private Resources resources;
    private SharedPreferences sharedPreferences;
    private Handler handler;
    private ThreadPoolExecutor executor;
    private OkHttpUtils okHttpUtils;
    private SharedPreferences.Editor editor;
    private final int handlerLogin=1;
    private final int handlerMsg=2;
    private final int handlerVerification=3;
    private ImageView ivAvatar;
    private TextView tvReadProtocolPersonal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint_login);
        // 指纹 or 手势 or 手机号
        judge();
        // 初始化控件
        initViews();
        //初始化一些数据
        initValues();
        // 设置监听器
        initEvents();

        biometricPrompt = new BiometricPrompt(this, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode == BiometricPrompt.ERROR_LOCKOUT) {
                    // 指纹传感器已被锁定
                    new ToastUtil().getToast(FingerprintLoginActivity.this,"指纹传感器已被锁定，请稍后再试或使用其他登录方式");
                }
            }
            /**
             * 这是指纹成功之后的逻辑*/
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                //开始登录
                loginByFingerprint();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });
    }

    private void judge() {
        sharedPreferences = FingerprintLoginActivity.this.getSharedPreferences(getString(R.string.user_pref_name), Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        boolean finger = sharedPreferences.getBoolean(getString(R.string.user_pref_key_finger), false);
        boolean gesture = sharedPreferences.getBoolean(getString(R.string.user_pref_key_gesture), false);
        if(finger) {
            // do nothing
        } else if (gesture) {
            // 如果不允许指纹登录跳转至手势
            startActivity(new Intent(FingerprintLoginActivity.this, GestureActivity.class));
        }else {
            // 如果指纹与手势均不允许，跳转手机号登录
            startActivity(new Intent(FingerprintLoginActivity.this, LoginActivity.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void loginByFingerprint() {
        executor.execute(() -> {
            //在子线程中新建一个工具类
            OkHttpUtils okHttpUtils = new OkHttpUtils();
            Request request = okHttpUtils
                    .createBuilder(getString(R.string.server_url),getString(R.string.verification_code_router_login_finger))
                    .addHeader(getString(R.string.request_header_key_token), Objects.requireNonNull(sharedPreferences.getString(getString(R.string.user_pref_key_token), null)))
                    .build();
            try {
                Response response = okHttpUtils.sendRequest(request);
                //判断是否可以获取到正确的响应码
                if (response.code()==getResources().getInteger(R.integer.request_success)) {
                    //如果获取到了请求体，请求体中应当包含请求体
                    if (response.body() != null) {
                        //把请求体的内容发送到主线程有主线程来获取验证码
                        handler.sendMessage(handler.obtainMessage(handlerLogin, response.body().string()));
                    }
                }else {
                    //如果获取到的不是正确的请求体这里应该要报一个//TODO:请求异常
                    handler.sendMessage(handler.obtainMessage(handlerVerification,getString(R.string.toast_first_error_verification_code_request_false_server_text)));
                }
            } catch (IOException e) {
                //抛一个异常//TODO:请求IO异常
                handler.sendMessage(handler.obtainMessage(handlerVerification,getString(R.string.toast_first_error_verification_code_request_false_text)));
            }
        });
    }
    private void getUserInfo(RegisterAndLoginBodyEntity registerAndLoginBodyEntity) {
        executor.execute(() -> {
            //创建出一个request对象
            Request request=okHttpUtils
                    .createBuilder(getString(R.string.server_url),getString(R.string.verification_code_router_getInfo))
                    .addHeader(getString(R.string.request_header_key_token), registerAndLoginBodyEntity.getToken())
                    .build();
            try {
                //发送request请求并获取结果
                Response response = okHttpUtils.sendRequest(request);
                //response获取到了内容
                if(response.body() != null){
                    //获取到response的内容
                    String string = response.body().string();
                    //发送给主线程response的内容
                    handler.sendMessage(handler.obtainMessage(handlerMsg, string));
                }
            } catch (IOException e) {
                //如果有异常说明在获取内容的时候遇到了问题，应该要提示用户”TODO:网络异常“
                handler.sendMessage(handler.obtainMessage(handlerVerification,getString(R.string.toast_first_error_verification_code_request_false_server_text)));
            }
        });
        //刚才只是及逆行用户信息的获取，这里要负责进入下一个页面前的跳转准备，要存入用户手机号，token,还有当前的用户状态
        editor.putString(getString(R.string.user_pref_key_phone),sharedPreferences.getString(getString(R.string.user_pref_key_phone),null));
        editor.putString(getString(R.string.user_pref_key_token),registerAndLoginBodyEntity.getToken());
        editor.putString(getString(R.string.user_pref_key_user_state),getString(R.string.user_state_login));
        //存入之后记得要提交存入记录
        editor.commit();
    }
    private void initValues() {
        // 初始化头像
        String path = getExternalFilesDir(null).getAbsolutePath() + "/imageFile/avatar.png";
        File avatarFile = new File(path);
        Bitmap bitmap = BitmapFactory.decodeFile(avatarFile.getAbsolutePath());
        Glide.with(this)
                .load(bitmap)
                .apply(RequestOptions.circleCropTransform())
                .into(ivAvatar);
        //获取我的工具包
        okHttpUtils = new OkHttpUtils();
        //初始化手机号
        String phone = sharedPreferences.getString(getString(R.string.user_pref_key_phone),null);
        if (phone !=null) {
            String phoneNumber = PhoneNumberUtil.changePhoneNumber(phone);
            tvPhone.setText(phoneNumber);
        }else {
            //如果出问题直接给他跳到登录的普通页面
            new ToastUtil().getToast(this,getString(R.string.login_finger_app_error));
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        executor = new ThreadPoolExecutor(
                10,
                20,
                10,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        //初始化handler
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                ToastUtil toastUtil = new ToastUtil();
                if(msg.what==handlerLogin){
                    //既然能拿到这个就说明没问题，直接获取就行了不需要报错
                    String content = (String)msg.obj;
                    //返回的响应体当中包含了token
                    Gson gson = new Gson();
                    //解析成了请求体的实体
                    RegisterAndLoginBodyEntity registerAndLoginBodyEntity = gson.fromJson(content,new TypeToken<RegisterAndLoginBodyEntity>(){}.getType());
                    //先打印一下
                    if(registerAndLoginBodyEntity.getCode()!=getResources().getInteger(R.integer.request_success)){
                        toastUtil.getToast(FingerprintLoginActivity.this,registerAndLoginBodyEntity.getMsg());
                    }else {
                        //先存一下rsa
                        editor.putString(getString(R.string.user_pref_key_rsa_public),registerAndLoginBodyEntity.getRsa_public_key());
                        editor.commit();
                        getUserInfo(registerAndLoginBodyEntity);
                    }
                }else if(msg.what==handlerVerification){
                    //这个进行的处理是发送请求故障的时候的错误处理
                    toastUtil.getToast(FingerprintLoginActivity.this,(String)msg.obj);
                } else if (msg.what==handlerMsg) {
                    //把用的参数全部填入进去
                    editor.putString(getString(R.string.user_pref_key_user_info),(String) msg.obj);
                    //提交
                    editor.commit();
                    //跳转到主页面
                    Intent intent = new Intent(FingerprintLoginActivity.this, BaseActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            }
        };
    }

    private void initEvents() {
        // 点击更多
        tvMore.setOnClickListener(v -> {
            bottomPopWindowMore = LayoutInflater.from(this).inflate(R.layout.layout_more_bottom_sheet, null);
            tvMoreChange = bottomPopWindowMore.findViewById(R.id.tv_login_finger_more_change);
            tvMoreRegister = bottomPopWindowMore.findViewById(R.id.tv_login_finger_more_register);
            tvMoreCancel = bottomPopWindowMore.findViewById(R.id.tv_login_finger_more_cancel);
            // 底部弹窗
            moreBottomDialog();
            // 点击更多中换个账号登录
            tvMoreChange.setOnClickListener(view -> {
                // 跳转到LoginActivity
                Intent intent = new Intent(FingerprintLoginActivity.this, LoginActivity.class);
                startActivity(intent);
            });
            // 点击更多中注册
            tvMoreRegister.setOnClickListener(view -> {
                // 跳转到RegisterActivity
                Intent intent = new Intent(FingerprintLoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            });
            // 点击更多中取消
            tvMoreCancel.setOnClickListener(view -> {
                bottomSheetDialog.dismiss();
            });
        });
        // 点击指纹登录
        btnLogin.setOnClickListener(v -> {
            //第一步检查是否手机支持指纹
            boolean agreeProtocol = radioButton.isChecked();
            if (agreeProtocol) {
                boolean agree = checkSupport();
                if (agree) {
                    //第二步设置指纹弹窗的相关内容
                    promptInfo = new BiometricPrompt.PromptInfo.Builder()
                            .setTitle(getString(R.string.login_finger_message_title))
                            .setSubtitle(null)
                            .setNegativeButtonText(getString(R.string.login_finger_negative))
                            .setAllowedAuthenticators(BIOMETRIC_STRONG)
                            .build();
                    biometricPrompt.authenticate(promptInfo);
                }
            }else {
                //如果没有同意协议就通知他让他同意
                new ToastUtil().getToast(this,resources.getString(R.string.toast_first_error_not_protocol));
            }
        });
        //切换方式
        btnChange.setOnClickListener(v -> {
            bottomPopWindowChange = LayoutInflater.from(this).inflate(R.layout.layout_change_bottom_sheet, null);
            tvChangeWechat = bottomPopWindowChange.findViewById(R.id.tv_login_finger_change_wechat);
            tvChangeMessage = bottomPopWindowChange.findViewById(R.id.tv_login_finger_change_message);
            tvChangeCancel = bottomPopWindowChange.findViewById(R.id.tv_login_finger_change_cancel);
            // 底部弹窗
            changeBottomDialog();
            // 点击换个登录方式中的手机号登录
            tvChangeWechat.setOnClickListener(view -> {
                // 跳转到手机号登录页
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
            });
            // 点击换个登录方式中短信验证码登录
            tvChangeMessage.setOnClickListener(view -> {
                // 跳转到VerificationCodeActivity
                Intent intent = new Intent(this, VerificationCodeActivity.class);
                intent.putExtra(getResources().getString(R.string.verification_code_extra_action), getResources().getInteger(R.integer.action_login));
                intent.putExtra(getResources().getString(R.string.verification_code_extra_phoneNumber), sharedPreferences.getString(getString(R.string.user_pref_key_phone), null));
                startActivity(intent);
            });
            // 点击换个登录方式中取消
            tvChangeCancel.setOnClickListener(view -> {
                bottomSheetDialog.dismiss();
            });
        });
        tvRead.setOnClickListener(v->{
            Intent intent = new Intent(this, ProtocolActivity.class);
            intent.putExtra(getString(R.string.protocol_intent_key),true);
            startActivity(intent);
        });
        tvReadProtocolPersonal.setOnClickListener(v->{
            Intent intent = new Intent(this, ProtocolActivity.class);
            intent.putExtra(getString(R.string.protocol_intent_key),false);
            startActivity(intent);
        });

    }
    /**
     * 检查设别是否支持指纹识别
     *
     */
    public boolean checkSupport() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            BiometricManager biometricManager = BiometricManager.from(this);
            ToastUtil toastUtil = new ToastUtil();
            switch (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
                case BiometricManager.BIOMETRIC_SUCCESS:
                    return true;
                case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                    //生物识别设备不存在
                    toastUtil.getToast(this,getString(R.string.login_finger_no_device));
                    return false;
                case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                    //生物识别不可使用
                    toastUtil.getToast(this,getString(R.string.login_finger_no_support));
                    return false;
                default:
                    toastUtil.getToast(this,getString(R.string.login_finger_no_finger));
                    return false;
            }
        }else {
            return false;
        }
    }
    /**点击换个登录方式底部弹窗*/
    private void changeBottomDialog() {
        bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialog);
        bottomSheetDialog.setContentView(bottomPopWindowChange);
        bottomSheetDialog.show();
    }

    /**点击更多底部弹窗*/
    private void moreBottomDialog() {
        bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialog);
        bottomSheetDialog.setContentView(bottomPopWindowMore);
        bottomSheetDialog.show();
    }
    /**初始化控件*/
    private void initViews() {
        ivAvatar = findViewById(R.id.iv_login_finger_avatar_protocol);
        tvMore = findViewById(R.id.tv_login_finger_more_protocol);
        tvPhone = findViewById(R.id.tv_login_finger_phone_protocol);
        btnLogin = findViewById(R.id.btn_login_finger_protocol);
        btnChange = findViewById(R.id.btn_login_change_protocol);
        radioButton = findViewById(R.id.rb_login_finger_protocol);
        tvRead = findViewById(R.id.tv_login_finger_protocol_read);
        tvReadProtocolPersonal = findViewById(R.id.tv_login_finger_read_protocol_personal);
        resources = getResources();
    }
}