package cn.agrinav.wxapi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import cn.agrinav.R;
import cn.agrinav.user.login.LoginActivity;
import cn.agrinav.utils.ToastUtil;

/**
 * @author 11230
 */
public class WXEntryActivity extends AppCompatActivity implements IWXAPIEventHandler {
    private IWXAPI api;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //创建服务api
        api = WXAPIFactory.createWXAPI(this,getString(R.string.wx_app_id), false);
        sharedPreferences = getSharedPreferences(getString(R.string.wx_pref_name), MODE_PRIVATE);
        try {
            Intent intent = getIntent();
            api.handleIntent(intent, this);
        } catch (Exception e) {
           Log.e(getString(R.string.trajectory_tag), e.toString());
           new ToastUtil().getToast(this, "微信登录授权失败，稍后再试");
           finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        api.handleIntent(intent, this);
    }


    @Override
    public void onResp(BaseResp baseResp) {
        if (baseResp.getType() == ConstantsAPI.COMMAND_SENDAUTH) {
            SendAuth.Resp authResp = (SendAuth.Resp)baseResp;
            //首先判断是否用户同意了授权
            if (baseResp.errCode == BaseResp.ErrCode.ERR_OK) {
                //用户同意授权
                String code = authResp.code;
                //TODO:如果后期需要access_token那么我就要拿到这些信息
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            } else {
                //用户拒绝授权
                new ToastUtil().getToast(this, "未授权，请重试");
            }
        }
        finish();
    }


    @Override
    public void onReq(BaseReq baseReq) {
        Log.i("aaaaa","------------------code: " + baseReq.getType());
        switch (baseReq.getType()) {
            case ConstantsAPI.COMMAND_GETMESSAGE_FROM_WX:
                break; case ConstantsAPI.COMMAND_SHOWMESSAGE_FROM_WX: break; default: break;
        }
        finish();
    }
}