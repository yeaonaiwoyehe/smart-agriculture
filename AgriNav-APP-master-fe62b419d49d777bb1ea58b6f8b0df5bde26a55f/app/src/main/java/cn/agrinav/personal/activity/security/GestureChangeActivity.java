package cn.agrinav.personal.activity.security;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;

import cn.agrinav.R;
import cn.agrinav.user.login.otherways.gesture.BindGestureLockActivity;

public class GestureChangeActivity extends AppCompatActivity {

    private MaterialSwitch msGestureLogin;
    private TextView tvGestureChange;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture_change);
        initViews();
        initEvents();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 禁用按钮事件监听器
        msGestureLogin.setOnCheckedChangeListener(null);
        // 设置按钮状态
        msGestureLogin.setChecked(sharedPreferences.getBoolean(getString(R.string.user_pref_key_gesture), false));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setStatusBarColor(Color.rgb(9,204,125));
        }
        // 重写开启监听事件
        msGestureLogin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    // 开启手势登录（二次确认）
                    Intent intent = new Intent(GestureChangeActivity.this, BindGestureLockActivity.class);
                    startActivity(intent);
                }else {
                    // 关闭手势登录
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(getString(R.string.user_pref_key_gesture), false);
                    editor.apply();
                }
            }
        });
    }

    private void initEvents() {
        // 修改手势
        tvGestureChange.setOnClickListener(v -> {
            // 判断手势登录是否开启
            if(sharedPreferences.getBoolean(getString(R.string.user_pref_key_gesture), false)) {
                // 修改手势（确认原手势）
                Intent intent = new Intent(GestureChangeActivity.this, GestureNewActivity.class);
                startActivity(intent);
            }else {
                // 提示用户开启手势登录
                Toast.makeText(this, getString(R.string.user_gesture_open), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        msGestureLogin = findViewById(R.id.ms_gesture_login);
        tvGestureChange = findViewById(R.id.tv_gesture_change);
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
    }
}