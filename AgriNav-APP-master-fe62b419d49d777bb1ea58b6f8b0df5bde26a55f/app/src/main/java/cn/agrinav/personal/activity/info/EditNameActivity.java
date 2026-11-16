package cn.agrinav.personal.activity.info;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import cn.agrinav.R;
import cn.agrinav.personal.utils.OkHttpUtil;

public class EditNameActivity extends AppCompatActivity {

    private EditText edtName;
    private Button btnOk;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_name);
        initViews();
        initData();
        initEvents();
    }

    private void initData() {
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
    }

    private void initEvents() {
        btnOk.setOnClickListener(v -> {
            String nickName = edtName.getText().toString();
            
            // 验证昵称
            if (nickName.length() > 15) {
                Toast.makeText(EditNameActivity.this, getString(R.string.personal_edit_name_length_limit), Toast.LENGTH_SHORT).show();
                return;
            }
            if (nickName.contains(" ")) {
                Toast.makeText(EditNameActivity.this, getString(R.string.personal_edit_name_blank_limit), Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(getString(R.string.user_pref_key_nick_name), nickName);
            editor.apply();
            // 将昵称传给后端
            new Thread(() -> {
                try{
                    sendNameToServer(nickName);
                }catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditNameActivity.this, getString(R.string.personal_edit_name_failure), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
            // 跳转到个人信息页
            Intent intent = new Intent();
            intent.putExtra("name", nickName);
            setResult(RESULT_OK, intent);
            finish();
        });
    }

    private void sendNameToServer(String nickName) {
        String auth = getString(R.string.request_header_key_token);
        String token = sharedPreferences.getString(getString(R.string.user_pref_key_token), "");
        String url = getString(R.string.server_url) + getString(R.string.personal_edit_name_request);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(getString(R.string.user_pref_key_nick_name), nickName);
        Gson gson = new Gson();
        String body = gson.toJson(jsonObject);
        // 异步请求
        OkHttpUtil.putNickName(url, auth, token, body, new OkHttpUtil.OnResponseListener() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    Toast.makeText(EditNameActivity.this, getString(R.string.personal_name_edit_success), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(EditNameActivity.this, getString(R.string.personal_name_edit_failure) + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void initViews() {
        edtName = findViewById(R.id.edt_name);
        btnOk = findViewById(R.id.btn_ok);
    }
}