package cn.agrinav.personal.activity.communication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cn.agrinav.R;

public class GpsActivity extends AppCompatActivity {

    private EditText edtGpsIp;
    private EditText edtGpsPort;
    private Button btnGpsOk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);
        initViews();
        initEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setStatusBarColor(Color.rgb(9,204,125));
        }
    }

    private void initEvents() {
        btnGpsOk.setOnClickListener(v -> {
            String gpsIp = edtGpsIp.getText().toString();
            String gpsPort = edtGpsPort.getText().toString();
            if (gpsIp.isEmpty() || gpsPort.isEmpty()) {
                edtGpsIp.setError(getString(R.string.security_gps_ip_input));
            } else {
                // TODO:判断输入是否合法 & 发送请求
            }
            Intent data = new Intent();
            String jtt808Ip = "http://" + gpsIp + ":" + gpsPort;
            data.putExtra(getString(R.string.jtt808_ip), jtt808Ip);
            setResult(RESULT_OK, data);
            Toast.makeText(this, getString(R.string.security_gps_ip_success), Toast.LENGTH_SHORT).show();
        });
    }

    private void initViews() {
        edtGpsIp = findViewById(R.id.edt_back_gps_url);
        edtGpsPort = findViewById(R.id.edt_back_gps_port);
        btnGpsOk = findViewById(R.id.btn_back_gps_ok);
    }
}