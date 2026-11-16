package cn.agrinav.personal.activity.feedback;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.text.InputFilter;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import cn.agrinav.R;
import cn.agrinav.personal.fragment.FragmentPersonal;
import cn.agrinav.personal.utils.OkHttpUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FeedbackActivity extends AppCompatActivity {

    private EditText edtFeedback;
    private Button btnSend;
    private SharedPreferences sharedPreferences;
    private ChipGroup chipGroupAllTags;
    private List<String> selectedTags = new ArrayList<>();
    private Map<Integer, String> tagMap = new HashMap<>();
    private String[] allTags = null;
    private TextView tvFeedBackMine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        initViews();
        initEvents();
    }

    private void initEvents() {
        btnSend.setOnClickListener(v -> {
            String feedback = edtFeedback.getText().toString().trim();
            if (feedback.isEmpty()) {
                Toast.makeText(FeedbackActivity.this, getString(R.string.feedback_input_please), Toast.LENGTH_SHORT).show();
            } else if (feedback.length() > 300) {
                Toast.makeText(FeedbackActivity.this, getString(R.string.feedback_input_limit), Toast.LENGTH_SHORT).show();
            } else {
                sendFeedBack(feedback);
            }
        });
        tvFeedBackMine.setOnClickListener(v -> {
            Intent intent = new Intent(FeedbackActivity.this, ShowFeedbackActivity.class);
            startActivity(intent);
        });
    }

    private void sendFeedBack(String feedback) {
        String auth = getString(R.string.request_header_key_token);
        String token = sharedPreferences.getString(getString(R.string.user_pref_key_token), "");
        String url = getString(R.string.server_url) + getString(R.string.feedback_request_send);

        // 获取选中标签对应的 code 列表
        List<Integer> selectedCodes = new ArrayList<>();
        for (String selectedTag : selectedTags) {
            for (Map.Entry<Integer, String> entry : tagMap.entrySet()) {
                if (entry.getValue().equals(selectedTag)) {
                    selectedCodes.add(entry.getKey());
                    break;
                }
            }
        }

        // 将反馈内容和标签 code 传递给后端
        OkHttpUtil.sendFeedBack(url, auth, token, feedback, selectedCodes, new OkHttpUtil.OnResponseListener() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    Toast.makeText(FeedbackActivity.this, getString(R.string.feedback_thanks), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(FeedbackActivity.this, FragmentPersonal.class);
                    setResult(RESULT_OK, intent);
                    finish();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(FeedbackActivity.this, getString(R.string.feedback_failure), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void initViews() {
        edtFeedback = findViewById(R.id.edt_feedback);
        btnSend = findViewById(R.id.btn_send);
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        chipGroupAllTags = findViewById(R.id.chip_group_all_tags);
        tvFeedBackMine = findViewById(R.id.tv_feedback_mine);
        // 获取标签数据
        getTags();
        
        // 为根布局添加触摸监听器以清除焦点
        View rootLayout = findViewById(R.id.root_layout);
        rootLayout.setOnTouchListener((v, event) -> {
            clearEditTextFocus();
            return false;
        });
        
        // 配置EditText的焦点变化行为
        edtFeedback.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                edtFeedback.setCursorVisible(false);
            } else {
                edtFeedback.setCursorVisible(true);
            }
        });
        
        // 配置EditText的输入限制
        edtFeedback.setFilters(new InputFilter[]{
            // 限制最大长度为300
            new InputFilter.LengthFilter(300),
            // 过滤空格
            (source, start, end, dest, dstart, dend) -> {
                if (source.equals(" ")) {
                    return "";
                }
                return null;
            }
        });

        // 添加文本变化监听器
        edtFeedback.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (s.length() >= 300) {
                    Toast.makeText(FeedbackActivity.this, getString(R.string.feedback_input_limit), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void getTags() {
        String auth = getString(R.string.request_header_key_token);
        String token = sharedPreferences.getString(getString(R.string.user_pref_key_token), "");
        String url = getString(R.string.server_url) + getString(R.string.feedback_request_get_tags);
        OkHttpUtil.getTags(url, auth, token, new OkHttpUtil.OnResponseListener() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray dataArray = jsonObject.getJSONArray(getString(R.string.feedback_request_data));
                    List<String> tagList = new ArrayList<>();
                    
                    // 遍历数据数组，获取dictLabel
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject item = dataArray.getJSONObject(i);
                        int dictCode = item.getInt(getString(R.string.feedback_request_dictCode));
                        String dictLabel = item.getString(getString(R.string.feedback_request_dictLabel));
                        tagList.add(dictLabel);
                        tagMap.put(dictCode, dictLabel);
                    }
                    
                    // 转换为数组
                    allTags = tagList.toArray(new String[0]);
                    
                    // 在UI线程中更新标签
                    runOnUiThread(() -> {
                        // 清空现有标签
                        chipGroupAllTags.removeAllViews();
                        
                        // 添加新的标签
                        for (String tag : allTags) {
                            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_choice, chipGroupAllTags, false);
                            chip.setText(tag);
                            chip.setCheckable(true);
                            chip.setChecked(selectedTags.contains(tag));
                            
                            // 添加点击事件
                            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                clearEditTextFocus();
                                if (isChecked) {
                                    if (!selectedTags.contains(tag)) {
                                        selectedTags.add(tag);
                                    }
                                } else {
                                    selectedTags.remove(tag);
                                }
                            });
                            
                            chipGroupAllTags.addView(chip);
                        }
                    });
                    
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(FeedbackActivity.this, getString(R.string.feedback_analysis_failure), Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(FeedbackActivity.this, getString(R.string.feedback_acquire_failure) + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // 辅助方法：清除EditText焦点并隐藏键盘
    private void clearEditTextFocus() {
        edtFeedback.clearFocus();
        edtFeedback.setCursorVisible(false);
        
        // 获取InputMethodManager实例
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        // 隐藏软键盘
        if (imm != null) {
            imm.hideSoftInputFromWindow(edtFeedback.getWindowToken(), 0);
        }
    }
}