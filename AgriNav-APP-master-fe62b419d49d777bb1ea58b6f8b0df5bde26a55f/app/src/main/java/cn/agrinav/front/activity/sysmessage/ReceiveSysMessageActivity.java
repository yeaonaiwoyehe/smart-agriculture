package cn.agrinav.front.activity.sysmessage;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.agrinav.R;
import cn.agrinav.front.activity.findfarm.FarmDetailActivity;
import cn.agrinav.front.activity.findfarm.utils.UIUtils;
import cn.agrinav.front.activity.sysmessage.utils.SysMessageUtils;
import cn.agrinav.front.entity.NewLandPlot;
import cn.agrinav.front.entity.SysMessage;
import cn.agrinav.measure.utils.ThreadPoolUtils;
import cn.agrinav.utils.OkHttpUtils;
import cn.agrinav.utils.Result;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReceiveSysMessageActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private SysMessageAdapter adapter;
    private List<SysMessage> messageList = new ArrayList<>();
    private SmartRefreshLayout sendMessageRefresh;
    private ImageView ivMarkAll;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;
    private boolean hasMore = true;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_sys_message);
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        initViews();
        initAdapter();
        initEvents();
        loadMessages(true);
    }

    private void initAdapter() {
        adapter = new SysMessageAdapter(messageList);
        recyclerView.setAdapter(adapter);
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_message);
        sendMessageRefresh = findViewById(R.id.send_message_refresh);
        ivMarkAll = findViewById(R.id.iv_mark_all);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initEvents() {
        // 消息点击
//        adapter.setOnItemClickListener((message, position) -> {
            // 处理消息点击事件
//        });

        // 一键已读
        ivMarkAll.setOnClickListener(v -> {
            markAllMessages();
        });

        // 下拉刷新
        sendMessageRefresh.setOnRefreshListener(refresh -> {
            currentPage = 1;
            hasMore = true;
            loadMessages(true);
        });

        // 上拉加载
        sendMessageRefresh.setOnLoadMoreListener(refresh -> {
            if (hasMore) {
                currentPage++;
                loadMessages(false);
            } else {
                sendMessageRefresh.finishLoadMoreWithNoMoreData();
            }
        });

    }

    /**
     * 标记所有消息已读
     */
    private void markAllMessages() {
        SysMessageUtils.markAllMessagesAsRead(this, () -> {
            runOnUiThread(() -> {
                // 清空未读消息状态
                SysMessageUtils.clearUnreadMessageStatus(this);
                // 重新加载消息列表
                loadMessages(true);
                Toast.makeText(this, "已全部标记为已读", Toast.LENGTH_SHORT).show();
            });
        });
    }

    /**
     * 加载通知
     * @param isRefresh
     */
    private void loadMessages(boolean isRefresh) {
        SysMessageUtils.getAllMessages(this, newMessages -> {
            runOnUiThread(() -> {
                if (isRefresh) {
                    messageList.clear();
                }
                messageList.addAll(newMessages);
                adapter.notifyDataSetChanged();

                // 处理刷新和加载更多的状态
                if (isRefresh) {
                    sendMessageRefresh.finishRefresh();
                } else {
                    sendMessageRefresh.finishLoadMore();
                }

                // 判断是否还有更多数据
                hasMore = newMessages.size() >= PAGE_SIZE;
                if (!hasMore) {
                    sendMessageRefresh.setNoMoreData(true);
                }
            });
        });
    }

}