package cn.agrinav;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;

import android.os.Environment;
import android.util.Log;
import android.widget.Button;

import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;


import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.File;

import cn.agrinav.base.BaseActivity;

import cn.agrinav.front.activity.ScanCodeActivity;
import cn.agrinav.user.login.otherways.FingerprintLoginActivity;
import cn.agrinav.user.login.otherways.WxLoginActivity;


public class MainActivity extends AppCompatActivity {
    /** 计时器 */
    private CountDownTimer countDownTimer;
    /** 倒计时5秒 */
    private int countDownTime = 5;
    /** 倒计时按钮 */
    private Button btnJumper;
    /** 判断标志 */
    private boolean flag = true;
    private ImageView imgTop;
    private IWXAPI api;
    private String APP_ID ;
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

    }
    private void regToWx() {
        // 通过WXAPIFactory工厂，获取IWXAPI的实例
        APP_ID = getString(R.string.wx_app_id);
        api = WXAPIFactory.createWXAPI(this, APP_ID,false);
        // 将应用的appId注册到微信
        api.registerApp(APP_ID);
        //动态监听微信启动广播进行注册到微信

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // 将该app注册到微信
                    api.registerApp(APP_ID);
                }
            }, new IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP), Context.RECEIVER_EXPORTED);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Intent intent = new Intent(MainActivity.this, ScanCodeActivity.class);
//        startActivity(intent);
//        finish();
        //注册到微信
        regToWx();
        // 初始化控件
        initViews();
        // 加载布局
        loadImage();
        // 设置倒计时事件
        countDownTimer = new CountDownTimer(countDownTime * 1000L, 1000) {
            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                btnJumper.setText("跳过 " + (seconds + 1));
            }

            @Override
            public void onFinish() {
                // 判断是否已跳过
                if (flag) {
                    // 跳转
                    jumpActivity();
                    // 销毁
                    finish();
                }
            }
        };
        countDownTimer.start();
        // 设置监听器
        initEvents();
        // 创建目录
        createDir();
    }

    private void createDir() {
        // 检查外部存储是否可用
        String state = android.os.Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // 创建外部存储目录
            File dir = new File(getExternalFilesDir(null), "imageFile");
            if (!dir.exists()) {
                boolean isCreated = dir.mkdir();
                if (isCreated) {
                    Log.d("MainActivity", "目录创建成功: " + dir.getAbsolutePath());
                } else {
                    Log.e("MainActivity", "目录创建失败");
                }
            } else {
                Log.i("MainActivity", "目录已存在: " + dir.getAbsolutePath());
            }
        } else {
            Log.w("MainActivity", "外部存储不可用");
        }
    }

    private void loadImage() {
        imgTop.setImageResource(R.drawable.guide_image);
    }

    /**跳转至登录页*/
    private void jumpActivity() {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), Context.MODE_PRIVATE);
        String state = sharedPreferences.getString(getString(R.string.user_pref_key_user_state),null);
        //当用户退出之后state置为exit,token仍然保留，如果是初次登录，那么就会是null
        if(state!=null) {
            //不是初次登录会有login和exit两个状态
            if(state.equals(getResources().getString(R.string.user_state_login))){
                //当是登录状态的时候直接进去
                Intent intent = new Intent(this, BaseActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else if(getString(R.string.user_state_logout).equals(state)){
                //等于logout说明刚刚注销了
                Intent intent = new Intent(MainActivity.this, WxLoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }else {
               //如果是退出状态说明token仍然保存,这只是状态改变了,就调入指纹登录页
                Intent intent = new Intent(this, FingerprintLoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        }else {
            //等于null说明初次登录
            Intent intent = new Intent(MainActivity.this, WxLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void initEvents() {
        btnJumper.setOnClickListener(v -> {
            // 修改判断
            flag = false;
            // 跳转
            jumpActivity();
        });
    }

    private void initViews() {
        btnJumper = findViewById(R.id.btn_jumper);
        imgTop = findViewById(R.id.img_top);
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }
}