package cn.agrinav.user.login.otherways.gesture;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.personal.activity.security.GestureChangeActivity;
import cn.agrinav.user.login.otherways.GestureActivity;
import cn.agrinav.utils.ToastUtil;

/**
 * @author 11230
 */
public class BindGestureLockActivity extends AppCompatActivity {

    private GestureLockViewGroup gestureLockViewGroup;
    private int ciShu;
    private ArrayList<Integer> answerList;
    private ToastUtil toastUtil;
    private SharedPreferences.Editor editor;
    private int[] answer;
    private boolean isFirst;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bind_gesture_lock);
        isFirst = getIntent().getBooleanExtra("isFirst",false);
        initViews();
        initEvents();
    }

    private void initEvents() {
        gestureLockViewGroup.setOnGestureLockViewListener(new GestureLockViewGroup.OnGestureLockViewListener() {
            @Override
            public void onBlockSelected(int cId) {
                if (ciShu ==0){
                    answerList.add(cId);
                }
            }

            @Override
            public void onGestureEvent(boolean matched) {
                if (ciShu==0){
                    answer = new int[answerList.size()];
                    for (int i = 0; i < answerList.size(); i++) {
                        answer[i] = answerList.get(i);
                    }
                    gestureLockViewGroup.setAnswer(answer);
                    ciShu++;
                    answerList.clear();
                    toastUtil.getToast(BindGestureLockActivity.this,"请再绘制一遍");
                }else {
                    //二次验证
                    if (matched){
                        //验证成功
                        StringBuilder sb = new StringBuilder();
                        for (int i : answer) {
                            sb.append(i);
                        }
                        editor.putString(getString(R.string.user_pref_key_gesture_password),sb.toString());
                        editor.putBoolean(getString(R.string.user_pref_key_gesture),true);
                        editor.commit();
                        if (isFirst){
                            Intent intent = new Intent(BindGestureLockActivity.this, BaseActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                        finish();
                    }else {
                        ciShu=0;
                        toastUtil.getToast(BindGestureLockActivity.this,"验证失败,重新设置");
                    }
                }
            }

            @Override
            public void onUnmatchedExceedBoundary() {

            }
        });
    }

    public void initViews(){
        gestureLockViewGroup = findViewById(R.id.id_bindGestureLockViewGroup);
        answerList = new ArrayList<>();
        toastUtil = new ToastUtil();
        editor = getSharedPreferences(getString(R.string.user_pref_name),MODE_PRIVATE).edit();
    }
}