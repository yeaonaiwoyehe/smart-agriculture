package cn.agrinav.personal.activity.security;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;

import cn.agrinav.R;
import cn.agrinav.utils.OkHttpUtils;
import cn.agrinav.utils.mobile.PhoneNumberUtil;
import okhttp3.Request;
import okhttp3.Response;

public class PhoneInputActivity extends AppCompatActivity {

    private EditText edtOldInput;
    private Button btnOldNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_input);
        initViews();
        initEvents();
    }

    private void initEvents() {
        btnOldNext.setOnClickListener(v -> {
           String phone = edtOldInput.getText().toString();
           if (new PhoneNumberUtil().isPhoneNumber(phone)) {
               // 跳转到验证码界面
               Intent intent = new Intent(this, PhoneVerifyActivity.class);
               startActivity(intent);
           }else {
               Toast.makeText(this, getString(R.string.security_phone_old_input_correct), Toast.LENGTH_SHORT).show();
           }
        });
    }
    private void initViews() {
        edtOldInput = findViewById(R.id.edt_phone_old_input);
        btnOldNext = findViewById(R.id.btn_old_next);
    }
}