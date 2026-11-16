package cn.agrinav.personal.activity.security;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cn.agrinav.R;
import cn.agrinav.utils.mobile.PhoneNumberUtil;

public class PhoneChangeActivity extends AppCompatActivity {

    private EditText edtPhoneInput;
    private Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_change);
        initViews();
        initEvents();
    }

    private void initEvents() {
        btnNext.setOnClickListener(v -> {
            String phoneNumber = edtPhoneInput.getText().toString();
            if (new PhoneNumberUtil().isPhoneNumber(phoneNumber)) {
                // 跳转到验证码界面
                Intent intent = new Intent(this, AuthCodeActivity.class);
                intent.putExtra(getString(R.string.security_code_send_request), phoneNumber);
                startActivity(intent);
            } else {
                Toast.makeText(this, getString(R.string.security_phone_old_input_correct), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        edtPhoneInput = findViewById(R.id.edt_phone_input);
        btnNext = findViewById(R.id.btn_next);
    }
}