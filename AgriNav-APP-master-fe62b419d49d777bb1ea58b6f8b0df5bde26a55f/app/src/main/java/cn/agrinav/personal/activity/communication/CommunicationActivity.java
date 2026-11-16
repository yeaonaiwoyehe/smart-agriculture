package cn.agrinav.personal.activity.communication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import cn.agrinav.R;

public class CommunicationActivity extends AppCompatActivity {

    private TextView tvBackInterface;
    private TextView tvGPS;
    private ActivityResultLauncher<Intent> backInterfaceLauncher;
    private ActivityResultLauncher<Intent> gpsLauncher;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication);
        // 初始化控件
        initViews();
        // 设置监听事件
        initEvents();
        // 初始化launcher，接收返回结果
        initLaunchers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setStatusBarColor(Color.rgb(9,204,125));
        }
    }

    private void initLaunchers() {
        backInterfaceLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            if(data != null) {
                                handleBackInterfaceResult(data);
                            }
                        }
                    }
                }
        );
        gpsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null) {
                                handleGpsResult(data);
                            }
                        }
                    }
                }
        );
    }

    private void handleBackInterfaceResult(Intent data) {
        String serverUrl = data.getStringExtra(getString(R.string.server_url));
        tvBackInterface.setText(getString(R.string.communication_encrypt));
        // 更新资源
            preferences.edit().putString(getString(R.string.server_url), serverUrl).apply();
        // 更新后端接口IP
    }

    private void handleGpsResult(Intent data) {
        String gpsUrl = data.getStringExtra(getString(R.string.jtt808_ip));
        tvGPS.setText(getString(R.string.communication_encrypt));
        // 更新资源
        preferences.edit().putString(getString(R.string.jtt808_ip), gpsUrl).apply();
        // 更新GPS轨迹IP
    }

    private void initEvents() {
        tvBackInterface.setOnClickListener(v -> {
            // 跳转至后端接口IP设置界面
            Intent intent = new Intent(this, BackInterfaceActivity.class);
            backInterfaceLauncher.launch(intent);
        });
        tvGPS.setOnClickListener(v -> {
            // 跳转至GPS设置界面
            Intent intent = new Intent(this, GpsActivity.class);
            gpsLauncher.launch(intent);
        });
    }

    private boolean isScatteredUser() {
        // 判断是否为散户
        return preferences.getString(getString(R.string.user_pref_key_dept_name), "").isEmpty();
    }

    private void initViews() {
        preferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        tvBackInterface = findViewById(R.id.tv_communication_back_interface);
        tvGPS = findViewById(R.id.tv_communication_gps);
        // 设置控件是否可点击
        setControlClickable(tvBackInterface, !isScatteredUser());
        setControlClickable(tvGPS, !isScatteredUser());
    }

    private void setControlClickable(TextView textView, boolean clickable) {
        textView.setEnabled(clickable);
        textView.setClickable(clickable);
        textView.setFocusable(clickable);
        textView.setAlpha(clickable ? 1.0f : 0.5f);
    }

}