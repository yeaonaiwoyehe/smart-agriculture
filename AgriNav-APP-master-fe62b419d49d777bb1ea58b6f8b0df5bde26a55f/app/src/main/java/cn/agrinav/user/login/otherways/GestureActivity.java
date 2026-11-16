package cn.agrinav.user.login.otherways;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.PixelCopy;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.user.entity.RegisterAndLoginBodyEntity;
import cn.agrinav.user.login.LoginActivity;
import cn.agrinav.user.login.otherways.gesture.GestureLockViewGroup;
import cn.agrinav.user.register.RegisterActivity;
import cn.agrinav.user.register.VerificationCodeActivity;
import cn.agrinav.utils.OkHttpUtils;
import cn.agrinav.utils.ToastUtil;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author 11230
 */
public class GestureActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private Handler handler;
    private ThreadPoolExecutor executor;
    private OkHttpUtils okHttpUtils;
    private SharedPreferences.Editor editor;
    private final int handlerLogin=1;
    private final int handlerMsg=2;
    private final int handlerVerification=3;
    private GestureLockViewGroup mGestureLockViewGroup;
    private ToastUtil toastUtil;
    private int[] answer;
    private TextView gestureOtherWay;
    private TextView gestureMoreWay;
    private BottomSheetDialog bottomSheetDialog;
    private View bottomPopWindowMore;
    private View bottomPopWindowChange;
    private TextView tvMoreChange;
    private TextView tvMoreRegister;
    private TextView tvMoreCancel;
    private TextView tvChangeWechat;
    private TextView tvChangeMessage;
    private TextView tvChangeCancel;
    private RelativeLayout gesturePaintOnLayout;
    private int maxNum = 5;
    private int time = 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gesture);
        initViews();
        initValues();
        initEvents();
        //这里先获取手势密码
        String string = sharedPreferences.getString(getString(R.string.user_pref_key_gesture_password), null);
        //判断手势密码是否能正常获取到，如果没有正常获取到就直接退出该页面
        if (string==null){
            toastUtil.getToast(this,getString(R.string.toast_first_error_not_gesture));
            finish();
        }else {
            answer = new int[string.length()];
            for (int i = 0; i < string.length(); i++){
                answer[i]=Integer.parseInt(String.valueOf(string.charAt(i)));
                Log.i("password", string.charAt(i)+"");
            }
            mGestureLockViewGroup.setAnswer(answer);
            mGestureLockViewGroup.setUnMatchExceedBoundary(maxNum);
            mGestureLockViewGroup.setOnGestureLockViewListener(new GestureLockViewGroup.OnGestureLockViewListener() {
                        @Override
                        public void onUnmatchedExceedBoundary() {
                            toastUtil.getToast(GestureActivity.this, getString(R.string.toast_first_error_false_num));
                            //首先设置时间限制，一段时间内不允许输入手势，过了这段时间才可以输入手势
                            gesturePaintOnLayout.setVisibility(View.VISIBLE);
                            executor.execute(()->{
                                while(time>0){
                                    time--;
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        Log.i("sleep",e.getMessage());
                                    }
                                }
                                handler.post(()->{
                                    gesturePaintOnLayout.setVisibility(View.GONE);
                                    //重新设置最大次数和最大时间
                                    maxNum+=maxNum;
                                    mGestureLockViewGroup.setUnMatchExceedBoundary(maxNum);
                                    time = 60*(maxNum/5);
                                });
                            });
                        }

                        @Override
                        public void onGestureEvent(boolean matched) {
                            //TODO:这里是登陆的逻辑
                            //当匹配成功的时候直接免密登录
                            if (matched) {
                                loginWithoutPassword();
                            }else {
                                toastUtil.getToast(GestureActivity.this, getString(R.string.toast_first_error_gesture));
                            }
                        }
                        @Override
                        public void onBlockSelected(int cId) {
                        }
                    });
        }
    }

    private void initEvents() {
        gesturePaintOnLayout.setVisibility(View.GONE);
        gesturePaintOnLayout.setOnClickListener(v->{
            toastUtil.getToast(GestureActivity.this, "请"+time+"s后重试");
        });
        // 点击更多
        gestureMoreWay.setOnClickListener(v -> {
            bottomPopWindowMore = LayoutInflater.from(this).inflate(R.layout.layout_more_bottom_sheet, null);
            tvMoreChange = bottomPopWindowMore.findViewById(R.id.tv_login_finger_more_change);
            tvMoreRegister = bottomPopWindowMore.findViewById(R.id.tv_login_finger_more_register);
            tvMoreCancel = bottomPopWindowMore.findViewById(R.id.tv_login_finger_more_cancel);
            moreBottomDialog();
            // 点击更多中换个账号登录
            tvMoreChange.setOnClickListener(view -> {
                // 跳转到LoginActivity
                Intent intent = new Intent(GestureActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
            // 点击更多中注册
            tvMoreRegister.setOnClickListener(view -> {
                // 跳转到RegisterActivity
                Intent intent = new Intent(GestureActivity.this, RegisterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
            // 点击更多中取消
            tvMoreCancel.setOnClickListener(view -> {
                bottomSheetDialog.dismiss();
            });
        });
        // 点击其他登录方式
        gestureOtherWay.setOnClickListener(v -> {
            bottomPopWindowChange = LayoutInflater.from(this).inflate(R.layout.layout_change_bottom_sheet, null);
            tvChangeWechat = bottomPopWindowChange.findViewById(R.id.tv_login_finger_change_wechat);
            tvChangeMessage = bottomPopWindowChange.findViewById(R.id.tv_login_finger_change_message);
            tvChangeCancel = bottomPopWindowChange.findViewById(R.id.tv_login_finger_change_cancel);
            // 底部弹窗
            changeBottomDialog();
            // 点击换个登录方式中的微信登录
            tvChangeWechat.setOnClickListener(view -> {
                // 跳转到WechatLoginActivity
                Intent intent = new Intent(this, WxLoginActivity.class);
                startActivity(intent);
            });
            // 点击换个登录方式中短信验证码登录
            tvChangeMessage.setOnClickListener(view -> {
                // 跳转到VerificationCodeActivity
                Intent intent = new Intent(this, VerificationCodeActivity.class);
                intent.putExtra(getResources().getString(R.string.verification_code_extra_action), getResources().getInteger(R.integer.action_login));
                intent.putExtra(getResources().getString(R.string.verification_code_extra_phoneNumber), sharedPreferences.getString(getString(R.string.user_pref_key_phone), null));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
            // 点击换个登录方式中取消
            tvChangeCancel.setOnClickListener(view -> {
                bottomSheetDialog.dismiss();
            });
        });
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

    private void initViews() {
        mGestureLockViewGroup = (GestureLockViewGroup) findViewById(R.id.id_gestureLockViewGroup);
        gestureMoreWay = findViewById(R.id.tv_login_gesture_more_protocol);
        gestureOtherWay = findViewById(R.id.gesture_other_way);
        gesturePaintOnLayout = findViewById(R.id.gesture_on);

    }
    /**绵密登录*/
    private void loginWithoutPassword() {
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
    private void initValues(){
        toastUtil = new ToastUtil();
        //初始化信息
        sharedPreferences = GestureActivity.this.getSharedPreferences(getString(R.string.user_pref_name), Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        //获取我的工具包
        okHttpUtils = new OkHttpUtils();
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
                        toastUtil.getToast(GestureActivity.this,registerAndLoginBodyEntity.getMsg());
                    }else {
                        getUserInfo(registerAndLoginBodyEntity);
                    }
                }else if(msg.what==handlerVerification){
                    //这个进行的处理是发送请求故障的时候的错误处理
                    toastUtil.getToast(GestureActivity.this,(String)msg.obj);
                } else if (msg.what==handlerMsg) {
                    //把用的参数全部填入进去
                    editor.putString(getString(R.string.user_pref_key_user_info),(String) msg.obj);
                    //提交
                    editor.commit();
                    //跳转到主页面
                    Intent intent = new Intent(GestureActivity.this, BaseActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            }
        };
    }
    private void getUserInfo(RegisterAndLoginBodyEntity registerAndLoginBodyEntity) {
        //刚才只是及逆行用户信息的获取，这里要负责进入下一个页面前的跳转准备，要存入用户手机号，token,还有当前的用户状态
        editor.putString(getString(R.string.user_pref_key_phone),sharedPreferences.getString(getString(R.string.user_pref_key_phone),null));
        editor.putString(getString(R.string.user_pref_key_token),registerAndLoginBodyEntity.getToken());
        editor.putString(getString(R.string.user_pref_key_user_state),getString(R.string.user_state_login));
        editor.putString(getString(R.string.user_pref_key_rsa_public),registerAndLoginBodyEntity.getRsa_public_key());
        //存入之后记得要提交存入记录
        editor.commit();
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

    }
}