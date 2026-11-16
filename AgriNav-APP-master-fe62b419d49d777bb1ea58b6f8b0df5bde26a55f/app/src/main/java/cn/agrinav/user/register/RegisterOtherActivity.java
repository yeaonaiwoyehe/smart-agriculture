package cn.agrinav.user.register;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


import cn.agrinav.R;
import cn.agrinav.user.ProtocolActivity;
import cn.agrinav.utils.ToastUtil;
import cn.agrinav.utils.mobile.PhoneNumberUtil;

/**
 * @author 11230
 */
public class RegisterOtherActivity extends AppCompatActivity {

    private Button btnOk;
    private EditText edtPhone;
    private CheckBox checkBox;
    private Resources resources;
    private TextView tvReadProtocol;
    private TextView tvReadProtocolPersonal;

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
        setContentView(R.layout.activity_register_other);
        initViews();
        initEvents();
    }

    private void initEvents() {
        btnOk.setOnClickListener(v->{
            String phoneNumber = edtPhone.getText().toString();
            //只有同意了协议才可以跳转到其他页面
            if (checkBox.isChecked()) {
                //想要跳转的前提是输入的是个手机号
                if (new PhoneNumberUtil().isPhoneNumber(phoneNumber)) {
                    Intent intent = new Intent(this, VerificationCodeActivity.class);
                    intent.putExtra(resources.getString(R.string.verification_code_extra_action), resources.getInteger(R.integer.action_register));
                    intent.putExtra(resources.getString(R.string.verification_code_extra_phoneNumber), "+86"+phoneNumber);
                    startActivity(intent);
                }else {
                    new ToastUtil().getToast(this,resources.getString(R.string.toast_first_error_not_phone));
                }
            }else {
                new ToastUtil().getToast(this,resources.getString(R.string.toast_first_error_not_protocol));
            }
        });
        tvReadProtocol.setOnClickListener(v->{
            Intent intent = new Intent(this, ProtocolActivity.class);
            intent.putExtra(getString(R.string.protocol_intent_key),true);
            startActivity(intent);
        });
        tvReadProtocolPersonal.setOnClickListener(v->{
            Intent intent = new Intent(this, ProtocolActivity.class);
            intent.putExtra(getString(R.string.protocol_intent_key),false);
            startActivity(intent);
        });
    }

    private void initViews() {
        btnOk = findViewById(R.id.btn_register_other_ok);
        edtPhone = findViewById(R.id.edt_register_other);
        checkBox = findViewById(R.id.rb_register_other_protocol);
        tvReadProtocol = findViewById(R.id.tv_register_other_read_protocol);
        resources = getResources();
        tvReadProtocolPersonal = findViewById(R.id.tv_register_other_read_protocol_personal);
    }
}