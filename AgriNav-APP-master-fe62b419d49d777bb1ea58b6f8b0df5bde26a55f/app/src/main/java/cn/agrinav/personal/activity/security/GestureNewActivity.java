package cn.agrinav.personal.activity.security;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Objects;

import cn.agrinav.R;
import cn.agrinav.user.login.otherways.gesture.BindGestureLockActivity;
import cn.agrinav.user.login.otherways.gesture.GestureLockViewGroup;
import cn.agrinav.utils.ToastUtil;

public class GestureNewActivity extends AppCompatActivity {

    private GestureLockViewGroup gestureLockViewGroup;
    private ArrayList<Integer> answerList;
    private ArrayList<Integer> answerListNew;
    private ToastUtil toastUtil;
    private SharedPreferences.Editor editor;
    private int[] answer;
    private SharedPreferences sharedPreferences;
    private boolean flag = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gesture_new);
        initViews();
        initEvents();
    }

    private void initEvents() {
        gestureLockViewGroup.setOnGestureLockViewListener(new GestureLockViewGroup.OnGestureLockViewListener() {
            @Override
            public void onBlockSelected(int cId) {
                answerListNew.add(cId);
            }

            @Override
            public void onGestureEvent(boolean matched) {
                if(answerListNew.size() != answerList.size()) {
                    answerListNew.clear();
                    flag = false;
                    toastUtil.getToast(GestureNewActivity.this, getString(R.string.security_gesture_error));
                } else {
                    int i = 0;
                    for (; i < answerList.size(); i++) {
                        if(!Objects.equals(answerListNew.get(i), answerList.get(i))) {
                            answerListNew.clear();
                            flag = false;
                            toastUtil.getToast(GestureNewActivity.this, getString(R.string.security_gesture_error));
                            break;
                        }
                    }
                    if(i == answerList.size()) {
                        flag = true;
                    }
                }
                if (flag) {
                    // 验证成功，设置新手势
                    Toast.makeText(GestureNewActivity.this, getString(R.string.security_gesture_verify_success), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(GestureNewActivity.this, BindGestureLockActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onUnmatchedExceedBoundary() {

            }
        });
    }

    public void initViews() {
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        gestureLockViewGroup = findViewById(R.id.id_bindGestureLockViewGroup);
        String input = sharedPreferences.getString(getString(R.string.user_pref_key_gesture_password), "");
        answerList = stringToArrayList(input);
        answerListNew = new ArrayList<>();
        toastUtil = new ToastUtil();
        editor = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE).edit();
    }

    public ArrayList<Integer> stringToArrayList(String input) {
        // 创建 ArrayList<Integer>
        ArrayList<Integer> list = new ArrayList<>();

        // 遍历字符串中的每个字符
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            try {
                int number = Character.getNumericValue(c);
                list.add(number);
            } catch (NumberFormatException e) {
                // 处理无法解析为整数的情况
                Log.i(getString(R.string.security_gesture_log), "无法解析为整数的字符：" + e);
            }
        }
        return list;
    }
}