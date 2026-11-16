package cn.agrinav.user;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cn.agrinav.R;
import cn.agrinav.utils.res.AssetsUtil;
import io.noties.markwon.Markwon;

/**
 * @author 11230
 */
public class ProtocolActivity extends AppCompatActivity {
    private Button btnRead;
    private WebView webView;
    private String protocolUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_protocol);
        boolean server = getIntent().getBooleanExtra(getString(R.string.protocol_intent_key), true);
        if (server){
            protocolUrl = "http://agrinav.cn/html/protocol.html";
        }else {
            protocolUrl = "http://agrinav.cn/html/privacy.html";
        }
        initViews();
        initEvents();
        setTimeDelay();
    }

    /**
     * 设置时间延迟
     */
    private void setTimeDelay() {
        new CountDownTimer(3000,1000){
            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long millisUntilFinished) {
                //首先按钮内容更改
                btnRead.setEnabled(false);

                btnRead.setText(((int)millisUntilFinished/1000)+1+" s"+getString(R.string.protocol_toast));
            }
            @Override
            public void onFinish() {
                //更改按钮状态
                btnRead.setEnabled(true);
                //更改按钮的内容
                btnRead.setText(getString(R.string.protocol_btn_text));
            }
        }.start();
    }
    /**
     * 设置状态栏
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initEvents() {
        btnRead.setOnClickListener(v->{
            finish();
        });
        webView.loadUrl(protocolUrl);
        webView.setWebViewClient(new WebViewClient());
    }
    private void initViews() {
        btnRead = findViewById(R.id.btn_protocol_read);
        webView = findViewById(R.id.sv_protocol);
    }
}