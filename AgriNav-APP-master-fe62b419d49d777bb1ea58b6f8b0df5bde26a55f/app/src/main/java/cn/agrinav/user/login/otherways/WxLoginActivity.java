package cn.agrinav.user.login.otherways;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.personal.activity.security.GestureNewActivity;
import cn.agrinav.user.ProtocolActivity;
import cn.agrinav.user.entity.RegisterAndLoginBodyEntity;
import cn.agrinav.user.entity.UserInfoEntity;
import cn.agrinav.user.entity.wx.WxAccessTokenResponse;
import cn.agrinav.user.entity.wx.WxLoginRequestBody;
import cn.agrinav.user.entity.wx.WxLoginResultToken;
import cn.agrinav.user.login.LoginActivity;
import cn.agrinav.user.login.otherways.gesture.BindGestureLockActivity;
import cn.agrinav.user.register.RegisterActivity;
import cn.agrinav.user.register.VerificationCodeActivity;
import cn.agrinav.utils.DialogUtils;
import cn.agrinav.utils.DialogUtilsForChose;
import cn.agrinav.utils.DialogUtilsForPrivacy;
import cn.agrinav.utils.DisplayUtil;
import cn.agrinav.utils.OkHttpUtils;
import cn.agrinav.utils.Result;
import cn.agrinav.utils.ToastUtil;
import cn.agrinav.utils.UuidUtils;
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
public class WxLoginActivity extends AppCompatActivity {

    private CheckBox radioButton;
    private Button btnLogin;
    private TextView tvRegister;
    private TextView tvReadProtocol;
    private IWXAPI api;
    private Handler handler;
    private final int HANDLER_MESSAGE_TAG_LOGIN = 1;
    private TextView tvLoginOther;
    private final String TAG = "WxLoginActivity";
    private ThreadPoolExecutor executor;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private TextView tvPersonalProtocol;
    private RelativeLayout relativeLayout;
    private ImageView ivWxLoginLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wx_login);
        api = WXAPIFactory.createWXAPI(this,getString(R.string.wx_app_id),false);
        initViews();
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), Context.MODE_PRIVATE);
        boolean state = sharedPreferences.getBoolean(getString(R.string.user_first), true);
        if (state){
            WebView webView = new WebView(this);
//            webView.loadUrl("http://agrinav.cn/html/protocol.html");
            webView.loadUrl("file:///android_asset/protocol.html");
            DialogUtilsForPrivacy dialogUtilsForChose = new DialogUtilsForPrivacy(this);
            AlertDialog dialogForPrivacy = dialogUtilsForChose.getDialog();
            dialogUtilsForChose
                    .setPositiveListener(v -> {
                        sharedPreferences.edit().putBoolean(getString(R.string.user_first), false).apply();
                        dialogForPrivacy.dismiss();
                    })
                    .setNegativeListener(v -> {
                        finish();
                        dialogForPrivacy.dismiss();
                    });
            dialogForPrivacy.setContentView(dialogUtilsForChose.getView());
            dialogForPrivacy.show();
        }
        initEvents();
        initSomething();
        initHandler();
    }

    private void initSomething() {
        executor = new ThreadPoolExecutor(
                10,
                20,
                10,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        preferences = getSharedPreferences(getString(R.string.user_pref_name), Context.MODE_PRIVATE);
        //刚才只是及逆行用户信息的获取，这里要负责进入下一个页面前的跳转准备，要存入用户手机号，token,还有当前的用户状态
        editor = preferences.edit();
    }

    private void initHandler() {
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what==HANDLER_MESSAGE_TAG_LOGIN){
                    //判断该微信是否已经绑定过信息
                    checkHaveBind((String)msg.obj);
                }
            }
        };
    }
    public void checkHaveBind(String result){
        final int noPhoneResultCode = 601;
        Gson gson = new Gson();
        WxLoginResultToken resultEntity = gson.fromJson(result,new TypeToken<WxLoginResultToken>(){}.getType());
        if (resultEntity.getCode()==getResources().getInteger(R.integer.request_success)){
            //说明是成功的已经绑定过的账号就可以拿到token了
            //得到token之后可以按照验证码页面的逻辑获取用户信息
            getUserInfo(resultEntity);
        } else if (resultEntity.getCode()==noPhoneResultCode) {
            //说明是账号从来没有绑定过，
            // 就应该先保存数据到临时表中，然后跳转到绑定手机号的页面
            Log.e(TAG,"WxLogin____:这个微信没有绑定过手机号，该去绑定了");
            editor.putString(getString(R.string.wx_login_temp_message),result);
            editor.putString(getString(R.string.user_pref_key_rsa_public),resultEntity.getRsa_public_key());
            editor.commit();
            Log.e(TAG,"WxLogin____:这个微信获取到的rsakey是");
            //防止有什么问题，在这里解析一下
            if (resultEntity.getUserDetails()==null){
                new ToastUtil().getToast(this,resultEntity.getMsg());
            }else {
                Intent intent = new Intent(this, WxLoginBindPhoneActivity.class);
                startActivity(intent);
            }
        }else{
            //这里说明的事出错了
            new ToastUtil().getToast(this,resultEntity.getMsg());
        }
    }

    private void getUserInfo(WxLoginResultToken registerAndLoginBodyEntity) {
        editor.putString(getString(R.string.user_pref_key_rsa_public),registerAndLoginBodyEntity.getRsa_public_key());
        editor.putString(getString(R.string.user_pref_key_token),registerAndLoginBodyEntity.getToken());
        editor.commit();
        Log.e(TAG,"WxLogin____:"+"这个微信绑定过手机号");
        Log.e(TAG,"wxRSA:"+preferences.getString(getString(R.string.user_pref_key_rsa_public),"aaa"));
        Log.e(TAG,"wxKeyWx:"+registerAndLoginBodyEntity.getRsa_public_key());
        executor.execute(() -> {
            //创建出一个request对象
            OkHttpUtils okHttpUtils = new OkHttpUtils();
            Request request=okHttpUtils
                    .createBuilder(getString(R.string.server_url),getString(R.string.verification_code_router_getInfo))
                    .addHeader(getString(R.string.request_header_key_token),registerAndLoginBodyEntity.getToken())
                    .build();
            okHttpUtils.sendRequest(request, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    showTips(getString(R.string.toast_err_net_or_server));
                    // 添加日志记录
                    Log.e(TAG, getString(R.string.toast_err_net_or_server), e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    //这里解析
                    if(response.isSuccessful()){
                        //然后解析响应
                        String json = response.body().string();
                        Log.e(TAG,"WxLogin____:userInfoResult:->   "+json);
                        Gson gson = new Gson();
                        UserInfoEntity userInfoEntity = gson.fromJson(json,new TypeToken<UserInfoEntity>(){}.getType());
                        if (userInfoEntity.getCode()==getResources().getInteger(R.integer.request_success)){
                            //刚才只是及逆行用户信息的获取，这里要负责进入下一个页面前的跳转准备，要存入用户手机号，token,还有当前的用户状态
                            editor.putString(getString(R.string.user_pref_key_user_info),json);
                            editor.putString(getString(R.string.user_pref_key_phone),userInfoEntity.getUser().getPhonenumber());
                            editor.putString(getString(R.string.user_pref_key_user_state),getResources().getString(R.string.user_state_login));
                            Log.e(TAG,"WxLogin____:用户信息获取成功");
                            //判断是否第一次登录
                            if(preferences.getString(getString(R.string.user_pref_key_user_state),null)==null) {
                                //如果查看是否是在这个手机上初次登录，如果是就直接判断指纹如果没有指纹就直接让用户绑定手势密码
                                Log.e(TAG,"WxLogin____:初次登录开始绑定指纹或者手势");
                                handler.post(()->{
                                    bindFingerOrGesture();
                                    //提交
                                    editor.commit();
                                });
                            }else {
                                //跳转到主页面
                                Log.e(TAG,"WxLogin____:直接跳转");
                                Intent intent = new Intent(WxLoginActivity.this, BaseActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                //提交
                                editor.commit();
                            }
                        }else {
                            showTips(userInfoEntity.getMsg());
                        }
                    }else {
                        showTips(response.code()+response.body().string());
                        showTips(getString(R.string.toast_err_request));
                    }
                }
                public void showTips(String tips){
                    handler.post(()->{
                       new ToastUtil().getToast(WxLoginActivity.this,tips);
                    });
                }
            });
        });
    }
    /**这里绑定指纹信息以及进行也面的跳转*/
    private void bindFingerOrGesture() {
        //判定是否有指纹的权限
        if (!checkSupport()) {
            //TODO：在这里要跳转到手势密码绑定页面
            //如果不可以使用指纹就直接强制绑定手势密码，跳转到手势密码绑定页面
            DialogUtils dialogUtils = new DialogUtils(this);
            AlertDialog dialog = dialogUtils.getDialog();
            dialogUtils.setTitle("温馨提示")
                    .setContent("是否开启手势密码，有助于下次便捷登录")
                    .setPositiveText("去开启")
                    .setNegativeText("取消")
                    .setPositiveListener(v -> {
                        //如果要重新授权就直接让他重新给权限重新调用一遍直到他给我权限为止
                        Intent intent = new Intent(WxLoginActivity.this, BindGestureLockActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra("isFirst",true);
                        startActivity(intent);
                        dialog.dismiss();
                    })
                    .setNegativeListener(v -> {
                        //不给权限不让用这个功能，结束这个页面
                        Intent intent = new Intent(WxLoginActivity.this, BaseActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        dialog.dismiss();
                    });
            View view = dialogUtils.getView();
            dialog.setContentView(view);
            dialog.show();
        }else{
            //如果有指纹的权限直接跳转到正常的页面，此时用户的信息已经填充进去了，直接跳转就行了
            editor.putBoolean(getString(R.string.user_pref_key_finger),true);
            Intent intent = new Intent(WxLoginActivity.this, BaseActivity.class);
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

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        String code = getIntent().getStringExtra(getString(R.string.wx_intent_code));
        String state = getIntent().getStringExtra(getString(R.string.wx_intent_state));
        //这里相当于回调，因为自己不能保证进入那个页面之后的处理时机，不如回到这个页面进行处理
        if (code!= null&&!code.isEmpty()&&state!= null&&!state.isEmpty()) {
            relativeLayout.setVisibility(View.VISIBLE);
            Glide.with(this).asGif().load(R.drawable.img_loading).into(ivWxLoginLoading);
            //TODO:执行真正的微信登录逻辑
            WxLoginRequestBody wxLoginRequestBody = new WxLoginRequestBody();
            wxLoginRequestBody.setCode(code);
            wxLoginRequestBody.setState(state);
            //向后端发送登录请求
            loginByWx(wxLoginRequestBody);
//            getMessage(code);
        }
    }
    private void initEvents() {
        btnLogin.setOnClickListener(v->{
            boolean isAgreeProtocol = radioButton.isChecked();
            //优先验证是否同意协议
            if (isAgreeProtocol) {
                sendAuthRequest();
            }else {
                //如果没有同意协议也直接给用户看到效果就是没有同意协议
                //todo:
                new ToastUtil().getToast(this,getString(R.string.toast_first_error_not_protocol));
            }
        });
        //点击按钮会跳转到注册首页
        tvRegister.setOnClickListener(v->{
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });
        //打开协议页面
        tvReadProtocol.setOnClickListener(v->{
            Intent intent = new Intent(this, ProtocolActivity.class);
            intent.putExtra(getString(R.string.protocol_intent_key),true);
            startActivity(intent);
        });
        tvLoginOther.setOnClickListener(v->{
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });
        tvPersonalProtocol.setOnClickListener(v->{
            Intent intent = new Intent(this, ProtocolActivity.class);
            intent.putExtra(getString(R.string.protocol_intent_key),false);
            startActivity(intent);
        });
    }
    private void sendAuthRequest() {
        SendAuth.Req req = new SendAuth.Req();
        req.scope = getString(R.string.wx_login_request_scope);
        req.state = UuidUtils.getUUID();
        boolean b = api.sendReq(req);
        if (!b){
            new ToastUtil().getToast(this, "微信登录请求失败,请检查网络");
        }
    }
    private void loginByWx(WxLoginRequestBody wxLoginRequestBody) {
        //首先创建请求
        OkHttpUtils okHttpUtils = new OkHttpUtils();
        HashMap<String,String> params = new HashMap<>();
        params.put(getString(R.string.wx_intent_code),wxLoginRequestBody.getCode());
        params.put(getString(R.string.wx_intent_state),wxLoginRequestBody.getState());
        Request.Builder builder = okHttpUtils.createBuilder(getString(R.string.server_url), getString(R.string.wx_login_path),params);
        Request build = builder.build();
        okHttpUtils.sendRequest(build, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                showToast(getString(R.string.toast_err_net_or_server));
                // 添加日志记录
                Log.e(TAG, getString(R.string.toast_err_net_or_server), e);
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    if(response.body() != null) {
                        String result = response.body().string();
                        Log.e(TAG,"*******/loginWechatMobile得到的result:     "+result);
                        handler.sendMessage(handler.obtainMessage(HANDLER_MESSAGE_TAG_LOGIN,result));
                    }
                }else{
                    showToast(getString(R.string.toast_err_request));
                    Log.e(TAG,response.message());
                }
            }
            private void showToast(String message) {
                // 使用 Handler 在主线程中显示 Toast
                handler.post(() -> new ToastUtil().getToast(WxLoginActivity.this, message));
            }
        });
    }

    private void initViews() {
        radioButton = findViewById(R.id.rb_wx_login_protocol);
        btnLogin = findViewById(R.id.btn_wx_login_confirm);
        tvRegister = findViewById(R.id.tv_wx_login_to_register);
        tvReadProtocol = findViewById(R.id.tv_wx_login_read_protocol);
        tvLoginOther = findViewById(R.id.tv_wx_login_other);
        tvPersonalProtocol = findViewById(R.id.tv_wx_login_read_personal_protocol);
        relativeLayout = findViewById(R.id.wx_login_mask_layer);
        ivWxLoginLoading = findViewById(R.id.iv_wx_login_loading);
    }
    //
//    private void getMessage(String code) {
//        OkHttpUtils okHttpUtils = new OkHttpUtils();
//        HashMap<String,String> params = new HashMap<>();
//        params.put("appid",getString(R.string.wx_app_id));
//        params.put("secret",getString(R.string.wx_secret));
//        params.put("code", code);
//        params.put("grant_type",getString(R.string.wx_login_grant_type));
//        Request.Builder builder = okHttpUtils.createBuilder(getString(R.string.wx_url), getString(R.string.wx_request_login_path_get_access_token), params);
//        okHttpUtils.sendRequest(builder.build(), new Callback() {
//            // 创建一个 Handler，用于在主线程中处理 UI 更新
//            final Handler handler = new Handler(Looper.getMainLooper());
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                showToast(getString(R.string.toast_err_net_or_server));
//                // 添加日志记录
//                Log.e("getCornCallBack", "网络请求失败", e);
//            }
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                if (response.isSuccessful()) {
//                    String json = response.body().string();
//                    WxAccessTokenResponse responseEntity = new Gson().fromJson(json,new TypeToken<WxAccessTokenResponse>(){}.getType());
//                    if (responseEntity.getErrCode()==null||responseEntity.getErrCode()==0){
//                        //这里说明真的收到了结果
//                        Log.i("getCornCallBack", "aaaa");
//                        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
//                        SharedPreferences.Editor edit = sharedPreferences.edit();
//                        edit.putString(getString(R.string.wx_pref_key_access_token),responseEntity.getAccessToken());
//                        edit.putInt(getString(R.string.wx_pref_key_expires_in),responseEntity.getExpiresIn());
//                        edit.putString(getString(R.string.wx_pref_key_refresh_token),responseEntity.getRefreshToken());
//                        edit.putString(getString(R.string.wx_pref_key_openid),responseEntity.getOpenid());
//                        edit.putString(getString(R.string.wx_pref_key_scope),responseEntity.getScope());
//                        edit.apply();
//                        showToast(responseEntity.getAccessToken());
//                        showToast(responseEntity.getOpenid());
//                        Log.i("access_token", responseEntity.getAccessToken());
//                        Log.i("openId", responseEntity.getOpenid());
////                        getPhoneNumber(code,responseEntity.getAccessToken(),responseEntity.getOpenid());
//                    }else {
//                        showToast(responseEntity.getErrMsg());
//                    }
//                }else {
//                    showToast(getString(R.string.toast_err_request));
//                }
//            }
//            private void showToast(String message) {
//                // 使用 Handler 在主线程中显示 Toast
//                handler.post(() -> new ToastUtil().getToast(WxLoginActivity.this, message));
//            }
//        });
//    }
//
//    private void getPhoneNumber(String code,String accessToken,String openId) {
//        OkHttpUtils okHttpUtils = new OkHttpUtils();
//        HashMap<String,String> params = new HashMap<>();
//        params.put("code", code);
//        params.put("access_token",accessToken);
//        params.put("openid",openId);
//        Log.i("getCornCallBack", "bbb");
//        Request.Builder builder = okHttpUtils.createBuilder(getString(R.string.wx_url), "/wxa/business/getuserphonenumber", params).method("POST", new RequestBody() {
//            @Nullable
//            @Override
//            public MediaType contentType() {
//                return null;
//            }
//
//            @Override
//            public void writeTo(@NonNull BufferedSink bufferedSink) throws IOException {
//
//            }
//        });
//        okHttpUtils.sendRequest(builder.build(), new Callback() {
//            // 创建一个 Handler，用于在主线程中处理 UI 更新
//            final Handler handler = new Handler(Looper.getMainLooper());
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                showToast(getString(R.string.toast_err_net_or_server));
//                // 添加日志记录
//                Log.e("getCornCallBack", "网络请求失败", e);
//            }
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                if (response.isSuccessful()) {
//                    String json = response.body().string();
//                    Log.i("aa",json);
//                    WxAccessTokenResponse responseEntity = new Gson().fromJson(json,new TypeToken<WxAccessTokenResponse>(){}.getType());
//                    if (responseEntity.getErrCode()!=null&&responseEntity.getErrCode()!=0){
//                        //这里说明真的收到了结果
//                        Log.i("aa",json);
//                    }else {
//                        showToast(responseEntity.getErrMsg());
//                    }
//                }else{
//                    Log.i("aa",response.message());
//                }
//            }
//            private void showToast(String message) {
//                // 使用 Handler 在主线程中显示 Toast
//                handler.post(() -> new ToastUtil().getToast(LoginActivity.this, message));
//            }
//        });
//    }
}