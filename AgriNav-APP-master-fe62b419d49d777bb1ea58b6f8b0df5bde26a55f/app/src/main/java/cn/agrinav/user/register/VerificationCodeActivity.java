package cn.agrinav.user.register;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;

import com.bumptech.glide.RequestBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.personal.activity.security.GestureNewActivity;
import cn.agrinav.user.VerificationCodeView;
import cn.agrinav.user.entity.RegisterAndLoginBodyEntity;
import cn.agrinav.user.login.otherways.gesture.BindGestureLockActivity;
import cn.agrinav.utils.OkHttpUtils;
import cn.agrinav.utils.Result;
import cn.agrinav.utils.ToastUtil;
import cn.agrinav.utils.mobile.PhoneNumberUtil;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author 11230
 */
public class VerificationCodeActivity extends AppCompatActivity {

    private VerificationCodeView verificationCodeView;
    private Button btnGetCode;
    private TextView textPhone;
    private Handler handler;
    private static final String TAG="+86";
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    /**接下来的部分是handler的一些标记*/
    private final int handlerLogin= 1;
    private final int handlerRegister =2 ;
    private final int handlerVerification = 3;
    private final int handlerMsg = 4;


    /**用户手机号,从前一个页面传递过来的intent来获取*/
    private String phoneNumber;
    /**用户登陆注册行为,从前一个页面传递过来的intent来获取*/
    private int action;


    private OkHttpUtils okHttpUtils;
    private ThreadPoolExecutor executor;
    private ToastUtil toastUtil;
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_code);
        //初始化组件
        initViews();
        //初始化值
        setValues();
        //初始化事件
        initEvents();
        btnGetCode.performClick();
    }
    /**用于给一些值进行初始化*/
    private void setValues() {
        //这里首先获取到preferences
        preferences = getSharedPreferences(getString(R.string.user_pref_name), Context.MODE_PRIVATE);
        //获取编辑器
        editor = preferences.edit();
        //获取一个OkHttpUtil的工具类对象
        okHttpUtils = new OkHttpUtils();
        //初始化一个toast工具类
        toastUtil = new ToastUtil();
        //初始化线程池(核心线程数,  最大线程数,程空闲超时时间（单位：秒） ,工作队列大小, 拒绝策略)
        executor = new ThreadPoolExecutor(
                10,
                20,
                10,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        //首先设定子线程的回调方法
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if(msg.what==handlerRegister){//这个是登陆场注册的逻辑
                    //既然能拿到这个就说明没问题，直接获取就行了不需要报错
                    String content = (String)msg.obj;
                    //返回的响应体当中包含了token
                    Gson gson = new Gson();
                    //解析成了请求体的实体
                    RegisterAndLoginBodyEntity registerAndLoginBodyEntity = gson.fromJson(content,new TypeToken<RegisterAndLoginBodyEntity>(){}.getType());
                    //这里检测是否后台进行了正确的处理
                    if(registerAndLoginBodyEntity.getCode()!=getResources().getInteger(R.integer.request_success)){
                        //如果后台没有正确处理就打印后台的报错给用户看
                        toastUtil.getToast(VerificationCodeActivity.this,registerAndLoginBodyEntity.getMsg());
                    }else {
                        getUserInfo(registerAndLoginBodyEntity);
                    }
                }else if(msg.what==handlerLogin){
                    //既然能拿到这个就说明没问题，直接获取就行了不需要报错
                    String content = (String)msg.obj;
                    //返回的响应体当中包含了token
                    Gson gson = new Gson();
                    //解析成了请求体的实体
                    RegisterAndLoginBodyEntity registerAndLoginBodyEntity = gson.fromJson(content,new TypeToken<RegisterAndLoginBodyEntity>(){}.getType());
                    //先打印一下
                    if(registerAndLoginBodyEntity.getCode()!=getResources().getInteger(R.integer.request_success)){
                        toastUtil.getToast(VerificationCodeActivity.this,registerAndLoginBodyEntity.getMsg());
                    }else {
                        getUserInfo(registerAndLoginBodyEntity);
                    }
                }else if(msg.what==handlerVerification){
                    //这个进行的处理是发送请求故障的时候的错误处理
                    toastUtil.getToast(VerificationCodeActivity.this,(String)msg.obj);
                } else if (msg.what==handlerMsg) {
                    //把后端传入用户信息参数全部填入进去
                    editor.putString(getString(R.string.user_pref_key_user_info),(String) msg.obj);
                    editor.putString(getString(R.string.user_pref_key_user_state),getResources().getString(R.string.user_state_login));
                    //判断是否第一次登录
                    if(preferences.getString(getString(R.string.user_pref_key_user_state),null)==null) {
                        handler.post(()->{
                            //第一次登录直接开始绑定指纹或者手势密码
                            bindFingerOrGesture();
                             //提交
                            editor.commit();
                        });
                    }else {
                        //跳转到主页面
                        Intent intent = new Intent(VerificationCodeActivity.this, BaseActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        //提交
                        editor.commit();
                    }
                }
            }
        };
        //获取行为信息
        action = getIntent().getIntExtra(getString(R.string.verification_code_extra_action), getResources().getInteger(R.integer.action_login));
        //TODO:nullPointerException获取用户手机号
        phoneNumber = getIntent().getStringExtra(getString(R.string.verification_code_extra_phoneNumber));
        //获取用户真实手机号
        if(phoneNumber!=null) {
            if (phoneNumber.startsWith(TAG)) {
                phoneNumber = phoneNumber.substring(3);
            }
            //获取用户手机号后进行的处理
            textPhone.setText(PhoneNumberUtil.changePhoneNumber(phoneNumber));
        }else{
            Intent intent = new Intent(VerificationCodeActivity.this,RegisterOtherActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }
    /**这里绑定指纹信息以及进行也面的跳转*/
    private void bindFingerOrGesture() {
        //判定是否有指纹的权限
        if (!checkSupport()) {
            //TODO：在这里要跳转到手势密码绑定页面
            //如果不可以使用指纹就直接强制绑定手势密码，跳转到手势密码绑定页面
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setPositiveButton("开启", (dialog, which) -> {
                        Intent intent = new Intent(VerificationCodeActivity.this, BindGestureLockActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra("isFirst",true);
                        startActivity(intent);
                    })
                    .setNegativeButton("取消", (dialog, which) -> {
                        Intent intent = new Intent(VerificationCodeActivity.this, BaseActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setTitle("是否开启手势密码，有助于下次便捷登录")
                    .show();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }else{
            //如果有指纹的权限直接跳转到正常的页面，此时用户的信息已经填充进去了，直接跳转就行了
            editor.putBoolean(getString(R.string.user_pref_key_finger),true);
            Intent intent = new Intent(VerificationCodeActivity.this, BaseActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }

    }
    /**检查指纹权限*/
    public boolean checkSupport() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            BiometricManager biometricManager = BiometricManager.from(this);
            return biometricManager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS;
        }else {
            return false;
        }
    }

    /** 输入验证码完成之后的事件，点击获取验证码之后的事件*/
    private void initEvents() {
        //设定当输入六位验证码之后会产生什么效果
        verificationCodeView.setOnInputFinishListener(v->{
            //这里获取到发来的参数信息包括进入验证码页面要进行的操作和传入进来的手机号
            if (action == getResources().getInteger(R.integer.action_register)) {
                //给后端发送注册请求
                registerOrLogin(v,handlerRegister);
            }else {
                //给后端发送登录请求
                registerOrLogin(v,handlerLogin);
            }
        });
        //这里是请求验证码按钮
        btnGetCode.setOnClickListener(v->{
            Log.e("TAGErrorCodeAgriNav", "onClick: "+new Date().toString());
            sendCode();
            //在这段事时间内设定按钮状态不可使用
            btnGetCode.setEnabled(false);
            //这个时候显示出来手机号
            textPhone.setVisibility(View.VISIBLE);
            //这里进行按钮的计时并且定时更改文本内容
            new CountDownTimer(60000,1000){
                @SuppressLint("SetTextI18n")
                @Override
                public void onTick(long millisUntilFinished) {
                    //首先按钮内容更改
                    btnGetCode.setText(getString(R.string.verification_code_gets)+"("+(((int)millisUntilFinished/1000)+1)+"s)");
                }
                @Override
                public void onFinish() {
                    //更改按钮状态
                    btnGetCode.setEnabled(true);
                    //更改按钮的内容
                    btnGetCode.setText(getString(R.string.verification_code_gets));
                }
            }.start();
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
                try(Response response = okHttpUtils.sendRequest(request)) {
                    //response获取到了内容
                    if (response.body() != null) {
                        //获取到response的内容
                        String string = response.body().string();
                        //发送给主线程response的内容
                        handler.sendMessage(handler.obtainMessage(handlerMsg, string));
                    }
                }
            } catch (IOException e) {
                //如果有异常说明在获取内容的时候遇到了问题，应该要提示用户”TODO:网络异常“
                handler.sendMessage(handler.obtainMessage(handlerVerification,getString(R.string.toast_first_error_verification_code_request_false_server_text)));
            }
        });
        //刚才只是及逆行用户信息的获取，这里要负责进入下一个页面前的跳转准备，要存入用户手机号，token,还有当前的用户状态
        editor.putString(getString(R.string.user_pref_key_phone),phoneNumber);
        editor.putString(getString(R.string.user_pref_key_token), registerAndLoginBodyEntity.getToken());
        editor.putString(getString(R.string.user_pref_key_rsa_public),registerAndLoginBodyEntity.getRsa_public_key());
        //存入之后记得要提交存入记录
        editor.commit();
    }
    private void sendCode() {
        executor.execute(() -> {
            //首先初始化一个工具类
            OkHttpUtils okHttpUtils = new OkHttpUtils();
            //这里构造一条请求
            HashMap<String,String> hashMap = new HashMap<>();
            //添加手机号字段这个参数
            hashMap.put(getString(R.string.verification_code_request_param_key_login_phone),phoneNumber);
            //这里的地址是服务器ip+captcha
            Request request = okHttpUtils
                    .createBuilder(getString(R.string.server_url),getString(R.string.verification_code_router_captcha),hashMap)
                    .build();
            try {
                Response response = okHttpUtils.sendRequest(request);
                //当验证码发送成功之后就直接让用户自己去操作
                if (response.code() != getResources().getInteger(R.integer.request_success)) {
                    //TODO:请求异常
                    handler.sendMessage(handler.obtainMessage(handlerVerification, getString(R.string.toast_first_error_verification_code_request_false_server_text)));
                }else{
                    // 这里说明成功了
                    if (response.body() != null) {
                        String string = response.body().string();
                        //发送给主线程
                        Result result = new Gson().fromJson(string, new TypeToken<Result>(){}.getType());
                        if (result.getCode()!=getResources().getInteger(R.integer.request_success)){
                            handler.post(()->{
                                new ToastUtil().getToast(VerificationCodeActivity.this,result.getMsg());
                            });
                        }
                    }
                }
            } catch (IOException e) {
                //TODO:请求IO异常
                //这处异常是服务器问题也可能是自身网络原因
                handler.sendMessage(handler.obtainMessage(handlerVerification,getString(R.string.toast_first_error_verification_code_request_false_text)));
            }
        });
    }
    private void registerOrLogin(String v,int handlerMessageTag) {
        executor.execute(() -> {
            //在子线程中新建一个工具类
            OkHttpUtils okHttpUtils = new OkHttpUtils();
            //这里填写请求参数
            HashMap<String,String> hashMap = new HashMap<>(1);
            //在参数hashMap中添加验证码字段
            hashMap.put(getString(R.string.verification_code_request_param_key_login_captcha), v);
            //在参数hashMap中添加手机号字段
            hashMap.put(getString(R.string.verification_code_request_param_key_login_phone),phoneNumber);
            Request.Builder builder = okHttpUtils
                    .createBuilder(getString(R.string.server_url),getString(R.string.verification_code_router_login),hashMap);
            builder.setMethod$okhttp("POST");
            Request request = builder.build();
            try {
                Response response = okHttpUtils.sendRequest(request);
                //判断是否可以获取到正确的响应码
                if (response.code()==getResources().getInteger(R.integer.request_success)) {
                    //如果获取到了请求体，请求体中应当包含请求体
                    if (response.body() != null) {
                        //把请求体的内容发送到主线程有主线程来获取验证码
                        handler.sendMessage(handler.obtainMessage(handlerMessageTag, response.body().string()));
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
    private void initViews() {
        //获取验证码组件
        verificationCodeView = findViewById(R.id.edt_verification_code);
        //获取验证码按钮
        btnGetCode = findViewById(R.id.btn_verification_code_get_code);
        //手机号展示口
        textPhone = findViewById(R.id.tv_verification_code_phone);
    }
}