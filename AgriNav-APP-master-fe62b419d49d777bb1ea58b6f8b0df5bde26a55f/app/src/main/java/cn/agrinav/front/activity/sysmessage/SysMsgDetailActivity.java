package cn.agrinav.front.activity.sysmessage;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import cn.agrinav.R;
import cn.agrinav.front.entity.SysMessage;

public class SysMsgDetailActivity extends AppCompatActivity {
    private TextView tvTitle;
    private TextView tvTime;
    private TextView tvContent;
    private SysMessage message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sys_msg_detail);

        // 初始化视图
        initViews();
        // 获取传递的消息对象
        message = (SysMessage) getIntent().getSerializableExtra("message");
        if (message != null) {
            // 显示消息详情
            displayMessageDetails();
        }

    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_msg_title);
        tvTime = findViewById(R.id.tv_msg_time);
        tvContent = findViewById(R.id.tv_msg_content);
    }

    private void displayMessageDetails() {
        tvTitle.setText(message.getTitle());
        tvTime.setText(message.getCreateTime());
        tvContent.setText(message.getContent().getContent());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }


    /**
     * 设置状态栏颜色
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().setStatusBarColor(Color.rgb(255,255,255));
        }
    }
}