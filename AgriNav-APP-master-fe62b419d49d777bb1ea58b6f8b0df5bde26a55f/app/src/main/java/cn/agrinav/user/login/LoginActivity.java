package cn.agrinav.user.login;


import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import cn.agrinav.R;
import cn.agrinav.user.ProtocolActivity;
import cn.agrinav.user.register.RegisterActivity;
import cn.agrinav.user.register.VerificationCodeActivity;
import cn.agrinav.utils.ToastUtil;
import cn.agrinav.utils.mobile.PhoneNumberUtil;

/**
 * @author 11230
 */
public class LoginActivity extends AppCompatActivity {
    private Button btnLogin;
    private CheckBox radioButton;
    private TextInputEditText edtPhone;
    private TextView tvReadProtocol;
    private TextView tvRegister;
    private TextView tvReadProtocolPersonal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //加载组件
        initViews();
        //加载事件
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
                    Intent intent = new Intent(this, VerificationCodeActivity.class);
                    //存入行为
                    intent.putExtra(getString(R.string.verification_code_extra_action), getResources().getInteger(R.integer.action_login));
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
        //点击按钮会跳转到注册首页
        tvRegister.setOnClickListener(v->{
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });
        //打开协议页面
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
        tvRegister = findViewById(R.id.tv_login_to_register);
        tvReadProtocol = findViewById(R.id.tv_login_read_protocol);
        edtPhone = findViewById(R.id.edt_login_phone);
        btnLogin = findViewById(R.id.btn_login_confirm);
        radioButton = findViewById(R.id.rb_login_protocol);
        tvReadProtocolPersonal = findViewById(R.id.tv_login_read_protocol_personal);
    }


}