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

public class BackInterfaceActivity extends AppCompatActivity {

    private EditText edtBackInterface;
    private Button btnInterfaceOk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_back_interface);
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
        btnInterfaceOk.setOnClickListener(v -> {
            String serverUrl = edtBackInterface.getText().toString();
            if (serverUrl.isEmpty()) {
                edtBackInterface.setError(getString(R.string.security_interface_url_input));
            } else {
                // TODO:发送请求
            }
            Intent data = new Intent();
            data.putExtra(getString(R.string.server_url), serverUrl);
            setResult(RESULT_OK, data);
            Toast.makeText(this, getString(R.string.security_interface_url_success), Toast.LENGTH_SHORT).show();
        });
    }

    private void initViews() {
        edtBackInterface = findViewById(R.id.edt_back_ip_address);
        btnInterfaceOk = findViewById(R.id.btn_back_interface_ok);
    }
}