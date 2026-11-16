package cn.agrinav.personal.activity.feedback;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import cn.agrinav.R;
import cn.agrinav.personal.utils.OkHttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ShowFeedbackActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private List<FeedbackItem> feedbackList = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private static final int PAGE_SIZE = 10;
    private int pageNum = 1;
    private boolean isLoading = false;  // 是否正在加载数据
    private boolean hasMoreData = true; // 是否还有更多数据

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_feedback);
        
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        
        getFeedbackHistory();
        
        // 添加滚动监听器实现分页
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && hasMoreData) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        pageNum++;
                        getFeedbackHistory();
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setStatusBarColor(Color.rgb(9,204,125));
        }
    }

    private void getFeedbackHistory() {
        if (isLoading) return;
        isLoading = true;
        
        String auth = getString(R.string.request_header_key_token);
        String token = sharedPreferences.getString(getString(R.string.user_pref_key_token), "");
        String url = getString(R.string.server_url) + getString(R.string.feedback_request_get) + getString(R.string.feedback_request_pageNum) + pageNum + getString(R.string.feedback_request_pageSize) + PAGE_SIZE;

        OkHttpUtil.getFeedbackHistory(url, auth, token, new OkHttpUtil.OnResponseListener() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray dataArray = jsonObject.getJSONArray(getString(R.string.feedback_request_data));
                    
                    // 如果返回的数据少于页面大小，说明没有更多数据了
                    if (dataArray.length() < PAGE_SIZE) {
                        hasMoreData = false;
                    }
                    
                    List<FeedbackItem> newItems = new ArrayList<>();
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject item = dataArray.getJSONObject(i);
                        String content = item.getString(getString(R.string.feedback_request_content));
                        boolean isChecked = item.getInt(getString(R.string.feedback_request_isChecked)) == 1;
                        List<String> tags = new ArrayList<>();
                        
                        if (!item.isNull(getString(R.string.feedback_request_labels))) {
                            JSONArray labelsArray = item.getJSONArray(getString(R.string.feedback_request_labels));
                            for (int j = 0; j < labelsArray.length(); j++) {
                                JSONObject labelObj = labelsArray.getJSONObject(j);
                                if (!labelObj.isNull(getString(R.string.feedback_request_dictLabel))) {
                                    String dictValue = labelObj.getString(getString(R.string.feedback_request_dictLabel));
                                    if (dictValue != null && !dictValue.isEmpty()) {
                                        tags.add(dictValue);
                                    }
                                }
                            }
                        }
                        
                        newItems.add(new FeedbackItem(content, tags, isChecked));
                    }
                    feedbackList.addAll(newItems);

                    runOnUiThread(() -> {
                        if (recyclerView.getAdapter() == null) {
                            // 首次加载创建适配器
                            FeedbackAdapter adapter = new FeedbackAdapter(feedbackList);
                            recyclerView.setAdapter(adapter);
                        } else {
                            // 加载更多数据时只需要通知数据变化
                            recyclerView.getAdapter().notifyDataSetChanged();
                        }
                        isLoading = false;
                    });
                    
                } catch (JSONException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(ShowFeedbackActivity.this, getString(R.string.feedback_analysis_failure), Toast.LENGTH_SHORT).show();
                        isLoading = false;
                    });
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(ShowFeedbackActivity.this, getString(R.string.feedback_acquire_failure) + errorMessage, Toast.LENGTH_SHORT).show();
                    isLoading = false;
                });
            }
        });
    }
}