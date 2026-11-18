package cn.agrinav.personal.fragment;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

import cn.agrinav.R;
import cn.agrinav.personal.activity.communication.CommunicationActivity;
import cn.agrinav.personal.activity.info.PersonInfoActivity;
import cn.agrinav.personal.activity.security.SecurityActivity;
import cn.agrinav.personal.activity.feedback.FeedbackActivity;
import cn.agrinav.personal.utils.ImageUtil;
import cn.agrinav.personal.utils.JsonAnalysis;
import cn.agrinav.trajectory.activity.TrajectoryReuploadActivity;
import cn.agrinav.user.ProtocolActivity;
import cn.agrinav.user.login.LoginActivity;
import cn.agrinav.user.login.LoginActivity;
import cn.agrinav.user.login.otherways.FingerprintLoginActivity;
import cn.agrinav.user.login.otherways.GestureActivity;
import cn.agrinav.utils.DialogUtils;

public class FragmentPersonal extends Fragment {

    private Activity activity;
    private SharedPreferences sharedPreferences;
    private JsonAnalysis jsonAnalysis;
    private LinearLayout personInfo;
    private LinearLayout personCompany;
    private LinearLayout personCommunication;
    private LinearLayout personSecurity;
    private LinearLayout personSuggestion;
    private LinearLayout personCommit;
    private LinearLayout personPrivacy;
    private ImageView ivAvatar;
    private TextView tvName;
    private TextView tvPhone;
    private TextView tvPersonCompany;
    private LinearLayout personVersion;
    private TextView tvVersion;
    private Button btnExit;
    private View viewLine;
    private ImageUtil imageUtil;
    private String avatar;

    public FragmentPersonal() {

    }
    public FragmentPersonal(Activity activity, SharedPreferences sharedPreferences) {
        this.activity = activity;
        this.sharedPreferences = sharedPreferences;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal, null);
        // 初始化控件
        initViews(view);
        // 设置布局
        setViews();
        // 设置监听事件
        initEvents();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if(sharedPreferences.getString(getString(R.string.user_pref_key_nick_name), "").isEmpty()) {
            initData();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateData();
    }

    private void loadAvatar(String avatar) {
        if (avatar != null && !avatar.isEmpty()) {
            File avatarFile = new File(activity.getExternalFilesDir(null).getAbsolutePath() + "/imageFile/avatar.png");
            if (avatarFile.exists()) {
                // 添加时间戳以避免Glide缓存
                Glide.with(activity)
                        .load(avatarFile)
                        .apply(RequestOptions.circleCropTransform())
                        .apply(new RequestOptions().signature(new ObjectKey(System.currentTimeMillis())))
                        .into(ivAvatar);
            } else {
                // 如果本地文件不存在，从网络加载
                Glide.with(activity)
                        .load(avatar)
                        .apply(RequestOptions.circleCropTransform())
                        .into(ivAvatar);
            }
        } else {
            imageUtil.saveImageToSDCard(BitmapFactory.decodeResource(activity.getResources(), R.drawable.login_avatar));
            File avatarFile = new File(activity.getExternalFilesDir(null).getAbsolutePath() + "/imageFile/avatar.png");
            // 加载默认头像
            Glide.with(activity)
                    .load(avatarFile)
                    .apply(RequestOptions.circleCropTransform())
                    .into(ivAvatar);
        }
    }

    private void setViews() {
        @SuppressLint("UseCompatLoadingForDrawables") Drawable drawable = activity.getDrawable(R.drawable.style_person_corner);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String departmentName = jsonAnalysis.getDepartmentName();
        // 判断是否具备单位
        if(departmentName.isEmpty()) {
            personCompany.setVisibility(View.GONE);
            viewLine.setVisibility(View.GONE);
            personCommunication.setBackground(drawable);
        }else {
            personCompany.setBackground(drawable);
            personCommunication.setBackgroundColor(activity.getColor(R.color.white));
            tvPersonCompany.setText(departmentName);
            editor.putString(getString(R.string.user_pref_key_dept_name), departmentName);
            editor.apply();
        }
        // 判断是否删除SD卡数据
        if(!sharedPreferences.getBoolean(getString(R.string.user_pref_delete_sdcard), true)) {
            deleteFile();
            editor.putBoolean(getString(R.string.user_pref_delete_sdcard), true);
            editor.apply();
        }
    }

    private void initData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // 获取信息
        String userName = jsonAnalysis.getUserName();
        String nickName = jsonAnalysis.getNickName();
        String phone = jsonAnalysis.getPhoneNumber();
        String avatar = jsonAnalysis.getAvatar();
        // 保存到本地
        editor.putString(getString(R.string.user_pref_key_user_name), userName);
        editor.putString(getString(R.string.user_pref_key_nick_name), nickName);
        editor.putString(getString(R.string.user_pref_key_phone), phone);
        editor.putString(getString(R.string.user_pref_key_avatar), avatar);
        editor.apply();
        // 显示信息
        tvName.setText(nickName);
        tvPhone.setText(phone);
        // 读取SD卡中的头像
        new Thread(() -> {
            getBitmapFromURL(avatar);
        }).start();
    }

    private void updateData() {
        String nickName = sharedPreferences.getString(getString(R.string.user_pref_key_nick_name), "");
        String phone = sharedPreferences.getString(getString(R.string.user_pref_key_phone), "");
        avatar = sharedPreferences.getString(getString(R.string.user_pref_key_avatar), "");
        tvName.setText(nickName);
        tvPhone.setText(phone);
        loadAvatar(avatar);
    }

    public void getBitmapFromURL(String urlString) {
        URL url = null;
        InputStream is = null;
        try {
            url = new URL(urlString);
            is = url.openStream();
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            imageUtil.saveImageToSDCard(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(is != null) {
                try{
                    is.close();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void initEvents() {
        // 修改头像、姓名
        personInfo.setOnClickListener(v -> {
            // 跳转到个人信息界面
            Intent intent = new Intent(activity,PersonInfoActivity.class);
            startActivity(intent);
        });
        // 单位信息
        personCompany.setOnClickListener(v -> {
            // 跳转到单位信息界面
            String company = tvPersonCompany.getText().toString();
            Toast.makeText(activity, company, Toast.LENGTH_SHORT).show();
        });
        // 通讯设置
        personCommunication.setOnClickListener(v -> {
            // 显示单位信息
            Intent intent = new Intent(activity, CommunicationActivity.class);
            startActivity(intent);
        });
        // 安全设置
        personSecurity.setOnClickListener(v -> {
            // 跳转到安全设置界面
            Intent intent = new Intent(activity, SecurityActivity.class);
            startActivity(intent);
        });
        // 意见反馈
        personSuggestion.setOnClickListener(v -> {
            // 跳转到意见反馈界面
            Intent intent = new Intent(activity, FeedbackActivity.class);
            startActivityForResult(intent, RESULT_OK);
        });
        // 上传地块
        personCommit.setOnClickListener(v -> {
            // 跳转到上传地块界面
            Intent intent = new Intent(activity, TrajectoryReuploadActivity.class);
            startActivity(intent);
        });
        // 隐私政策
        personPrivacy.setOnClickListener(v -> {
            // 显示隐私政策
            Intent intent = new Intent(activity, ProtocolActivity.class);
            intent.putExtra(getString(R.string.protocol_intent_key),false);
            startActivity(intent);
        });
        // 版本信息
        personVersion.setOnClickListener(v -> {
            // 显示版本信息
            String version = tvVersion.getText().toString();
            Toast.makeText(activity, version, Toast.LENGTH_SHORT).show();
        });
        // 退出登录
        btnExit.setOnClickListener(v -> {
            // 弹窗确认对话框
            showLogoutConfirmationDialog();
        });
    }

    // 显示确认退出登录的对话框
    private void showLogoutConfirmationDialog() {
        DialogUtils dialogUtils = new DialogUtils(activity);
        AlertDialog dialog = dialogUtils.getDialog();
        dialogUtils.setTitle(getString(R.string.personal_exit_title))
                .setContent(getString(R.string.personal_exit_text))
                .setPositiveText(getString(R.string.personal_exit_button_confirm))
                .setNegativeText(getString(R.string.personal_exit_button_cancel))
                .setPositiveListener(v -> {
                    logoutUser();
                    dialog.cancel();
                })
                .setNegativeListener(v -> {
                    dialog.cancel();
                });
        View view = dialogUtils.getView();
        dialog.setContentView(view);
        dialog.show();
    }

    // 退出登录
    private void logoutUser() {
        // 清除登录状态
        SharedPreferences sharedPreferences = activity.getSharedPreferences(getString(R.string.user_pref_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        String phone = sharedPreferences.getString(getString(R.string.user_pref_key_phone), "");
        String token = sharedPreferences.getString(getString(R.string.user_pref_key_token), "");
        boolean isAllowFingerLogin = sharedPreferences.getBoolean(getString(R.string.user_pref_key_finger), false);
        boolean isAllowGestureLogin = sharedPreferences.getBoolean(getString(R.string.user_pref_key_gesture), false);
        String gesturePassword = sharedPreferences.getString(getString(R.string.user_pref_key_gesture_password), "");
        edit.clear();
        edit.putString(getString(R.string.user_pref_key_token), token);
        edit.putString(getString(R.string.user_pref_key_phone), phone);
        edit.putString(getString(R.string.user_pref_key_user_state), getString(R.string.user_state_exit));
        edit.putBoolean(getString(R.string.user_pref_key_finger), isAllowFingerLogin);
        edit.putBoolean(getString(R.string.user_pref_key_gesture), isAllowGestureLogin);
        edit.putString(getString(R.string.user_pref_key_gesture_password), gesturePassword);
        edit.putBoolean(getString(R.string.user_pref_delete_sdcard), false);
        edit.putBoolean(getString(R.string.user_first),false);
        edit.apply();
        // 判断跳转页面
        if(sharedPreferences.getBoolean(getString(R.string.user_pref_key_finger), false)) {
            Intent intent = new Intent(activity, FingerprintLoginActivity.class);
            startActivity(intent);
        }else if(sharedPreferences.getBoolean(getString(R.string.user_pref_key_gesture), false)) {
            Intent intent = new Intent(activity, GestureActivity.class);
            startActivity(intent);
        }else {
            Intent intent = new Intent(activity, LoginActivity.class);
            startActivity(intent);
        }
    }

    private void deleteFile() {
        String imagePath = activity.getExternalFilesDir(null).getAbsolutePath() + "/imageFile";
        deleteFile(imagePath);
    }

    private void deleteFile(String directoryPath) {
        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        file.delete();
                    }
                }
            }
        }
    }


    private void initViews(View view) {
        personInfo = view.findViewById(R.id.person_info);
        ivAvatar = view.findViewById(R.id.iv_person_avatar);
        tvName = view.findViewById(R.id.tv_person_name);
        tvPhone = view.findViewById(R.id.tv_person_phone);
        personCompany = view.findViewById(R.id.person_company);
        tvPersonCompany = view.findViewById(R.id.tv_person_company);
        personCommunication = view.findViewById(R.id.person_communication);
        personSecurity = view.findViewById(R.id.person_security);
        personSuggestion = view.findViewById(R.id.person_suggestion);
        personCommit = view.findViewById(R.id.person_commit);
        personPrivacy = view.findViewById(R.id.person_privacy);
        personVersion = view.findViewById(R.id.person_version);
        tvVersion = view.findViewById(R.id.tv_person_version);
        btnExit = view.findViewById(R.id.btn_person_exit);
        viewLine = view.findViewById(R.id.view_line);
        jsonAnalysis = new JsonAnalysis(sharedPreferences);
        imageUtil = new ImageUtil(activity);
    }
}