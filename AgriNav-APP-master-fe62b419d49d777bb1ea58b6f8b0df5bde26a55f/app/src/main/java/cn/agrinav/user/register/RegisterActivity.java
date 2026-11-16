package cn.agrinav.user.register;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.material.snackbar.Snackbar;
import cn.agrinav.R;
import cn.agrinav.user.ProtocolActivity;
import cn.agrinav.utils.ToastUtil;
import cn.agrinav.utils.mobile.PhoneNumberUtil;

/**
 * @author 11230
 */
public class RegisterActivity extends AppCompatActivity {

    private TextView registerPhone;
    private SubscriptionManager subscriptionManager;
    private String userPhone;
    private Button registerThis;
    private Button registerOther;
    private CheckBox checkBox;
    private Resources resources;
    private TextView textProtocol;
    private TextView tvReadProtocolPersonal;
    private Snackbar snackbar;
    private String activeDataPhoneNumber;

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
        setContentView(R.layout.activity_register);
        //首先获取到注册页面的控件
        initViews();

        initEvents();
    }

    private void initEvents() {
        //给一键注册按钮绑定事件
        registerThis.setOnClickListener(v->{
            //首先判断是否同意了协议
            if (checkBox.isChecked()) {
                initPhoneNumber();
            }else {
                new ToastUtil().getToast(this,resources.getString(R.string.toast_first_error_not_protocol));
            }

        });
        //给其他方式注册绑定事件
        registerOther.setOnClickListener(v->{
            Intent intent = new Intent(this,RegisterOtherActivity.class);
            startActivity(intent);
        });
        //打开协议页面
        textProtocol.setOnClickListener(v->{
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

    @SuppressLint("HardwareIds")
    private void initPhoneNumber() {
        //首先获取到管理器
        subscriptionManager = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        //在这里判断用户是否授予了电话权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED&&ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            //判断版本号
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                String simCard1 = "";
                String simCard2 = "";
                int activeDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
                activeDataPhoneNumber = subscriptionManager.getPhoneNumber(activeDataSubscriptionId);
                for (int i=0;i<2;i++) {
                    SubscriptionInfo subscriptionInfo = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(i);
                    if (subscriptionInfo == null) {
                        continue;
                    }
                    int subscriptionId = subscriptionInfo.getSubscriptionId();
                    String phoneNumber = subscriptionManager.getPhoneNumber(subscriptionId);
                    if (i==0){ simCard1 = phoneNumber;}else {simCard2 = phoneNumber;};
                    Log.e("phone",phoneNumber)  ;
                }
                setPhoneNumber(simCard1, simCard2);
            }else {
                //说明两张卡都获取不到
                Intent intent = new Intent(this,RegisterOtherActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                this.finish();
            }
        }else {
            //如果没给权限就去找他要权限
            snackbar = Snackbar.make(findViewById(R.id.snack_bar_container_register), "需要使用电话权限获取手机号", Snackbar.LENGTH_INDEFINITE);
            snackbar.setAnchorView(R.id.snack_bar_container_register);
            snackbar.setBackgroundTint(Color.WHITE);
            snackbar.setTextColor(Color.BLACK);
            snackbar.show();
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.READ_PHONE_STATE}, getResources().getInteger(R.integer.permission_phone_request_code));
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        snackbar.dismiss();
        if (requestCode == getResources().getInteger(R.integer.permission_phone_request_code)) {
            if (PackageManager.PERMISSION_GRANTED == grantResults[0]&&grantResults[1]==PackageManager.PERMISSION_GRANTED) {
                //这里是授予权限之后的逻辑
                //判断版本号
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED) {
                    //判断版本号
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        String simCard1 = "";
                        String simCard2 = "";
                        int activeDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
                        activeDataPhoneNumber = subscriptionManager.getPhoneNumber(activeDataSubscriptionId);
                        for (int i=0;i<2;i++) {
                            SubscriptionInfo subscriptionInfo = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(i);
                            if (subscriptionInfo == null) {
                                continue;
                            }
                            int subscriptionId = subscriptionInfo.getSubscriptionId();
                            String phoneNumber = subscriptionManager.getPhoneNumber(subscriptionId);
                            if (i==0){ simCard1 = phoneNumber;}else {simCard2 = phoneNumber;};
                            Log.e("phone",phoneNumber)  ;
                        }
                        setPhoneNumber(simCard1, simCard2);
                    }else {
                        //说明两张卡都获取不到
                        Intent intent = new Intent(this,RegisterOtherActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        this.finish();
                    }
                }
            } else {
                //如果还是不给我权限就给他跳走让他自己去找地方注册去
                Intent intent = new Intent(this,RegisterOtherActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                this.finish();
            }
        }
    }
    private void setPhoneNumber(String simCard1, String simCard2) {
        boolean firstResult = !simCard1.isEmpty();
        boolean secondResult = !simCard2.isEmpty();
        //判断是否能够获取到卡一或者卡二
        if (firstResult&&secondResult){
            if (simCard1.equals(activeDataPhoneNumber)) {
                userPhone = simCard1;
                registerPhone.setText(PhoneNumberUtil.changePhoneNumber(simCard1));
            }else {
                userPhone = simCard2;
                registerPhone.setText(PhoneNumberUtil.changePhoneNumber(simCard2));
            }
            Intent intent = new Intent(this, VerificationCodeActivity.class);
            intent.putExtra(getResources().getString(R.string.verification_code_extra_action),getResources().getInteger(R.integer.action_register));
            intent.putExtra(getResources().getString(R.string.verification_code_extra_phoneNumber),userPhone);
            startActivity(intent);
            finish();
        }else if(firstResult||secondResult){
            if (firstResult){
                userPhone = simCard1;
                registerPhone.setText(PhoneNumberUtil.changePhoneNumber(simCard1));
            }else {
                userPhone = simCard2;
                registerPhone.setText(PhoneNumberUtil.changePhoneNumber(simCard2));
            }
            Intent intent = new Intent(this, VerificationCodeActivity.class);
            intent.putExtra(getResources().getString(R.string.verification_code_extra_action),getResources().getInteger(R.integer.action_register));
            intent.putExtra(getResources().getString(R.string.verification_code_extra_phoneNumber),userPhone);
            startActivity(intent);
        }else {
            //说明两张卡都获取不到
            Intent intent = new Intent(this,RegisterOtherActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            this.finish();
        }
    }
    private void initViews() {
        registerPhone = findViewById(R.id.tv_register_phone);
        registerThis = findViewById(R.id.btn_register_this);
        registerOther = findViewById(R.id.btn_register_other);
        checkBox = findViewById(R.id.rb_register_protocol);
        textProtocol = findViewById(R.id.tv_register_read_protocol);
        resources = getResources();
        tvReadProtocolPersonal = findViewById(R.id.tv_register_read_protocol_personal);

    }
}