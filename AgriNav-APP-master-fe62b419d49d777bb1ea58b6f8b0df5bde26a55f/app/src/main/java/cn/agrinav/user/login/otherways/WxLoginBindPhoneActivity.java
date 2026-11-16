package cn.agrinav.user.login.otherways;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import cn.agrinav.R;
import cn.agrinav.user.ProtocolActivity;
import cn.agrinav.user.register.VerificationCodeActivity;
import cn.agrinav.utils.ToastUtil;
import cn.agrinav.utils.mobile.PhoneNumberUtil;

/**
 * @author 11230
 */
public class WxLoginBindPhoneActivity extends AppCompatActivity {
    private Button btnLogin;
    private CheckBox radioButton;
    private TextInputEditText edtPhone;
    private TextView tvReadProtocol;
    private TextView tvReadPrivacy;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_wx_login_bind_phone);
        initViews();
        initEvents();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }
    private void initEvents() {
        //点击按钮会跳转到验证码页面，需要携带验证码行为信息以及填写的手机号信息
        btnLogin.setOnClickListener(v->{
            String userPhone = edtPhone.getText().toString();
            boolean isAgreeProtocol = radioButton.isChecked();
            //优先验证是否同意协议
            if (isAgreeProtocol) {
                //只有用户输入的符合手机号的时候才进行页面的跳转
                if (new PhoneNumberUtil().isPhoneNumber(userPhone)) {
                    Intent intent = new Intent(this, WxLoginBindVerificationActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    //存入要登陆的手机号
                    intent.putExtra(getString(R.string.verification_code_extra_phoneNumber), edtPhone.getText().toString());
                    startActivity(intent);
                } else {
                    //如果不是符合要求的手机号就直接给用户说明这个不是用户手机号
                    new ToastUtil().getToast(this, getString(R.string.toast_first_error_not_phone));
                }
            }else {
                //如果没有同意协议也直接给用户看到效果就是没有同意协议
                new ToastUtil().getToast(this,getString(R.string.toast_first_error_not_protocol));
            }
        });
        //打开协议页面
        tvReadProtocol.setOnClickListener(v->{
            Intent intent = new Intent(this, ProtocolActivity.class);
            intent.putExtra(getString(R.string.protocol_intent_key),true);
            startActivity(intent);
        });
        tvReadPrivacy.setOnClickListener(v->{
            Intent intent = new Intent(this, ProtocolActivity.class);
            intent.putExtra(getString(R.string.protocol_intent_key),false);
            startActivity(intent);
        });
    }

    private void initViews() {
        edtPhone = findViewById(R.id.wx_login_bind_phone);
        btnLogin = findViewById(R.id.wx_login_bind_confirm);
        tvReadProtocol = findViewById(R.id.wx_login_bind_read_protocol);
        radioButton = findViewById(R.id.wx_login_bind_protocol);
        tvReadPrivacy = findViewById(R.id.tv_wx_login_bind_read_personal_protocol);
    }
}