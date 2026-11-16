package cn.agrinav.personal.activity.info;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.lang.ref.WeakReference;

import cn.agrinav.R;
import cn.agrinav.personal.utils.ImageUtil;
import cn.agrinav.personal.utils.OkHttpUtil;

public class PersonInfoActivity extends AppCompatActivity {

    private ImageView ivEditAvatar;
    private TextView tvEditName;
    public static final int EDIT_REQUEST_CODE = 1;
    private Handler handler;
    private ImageUtil imageUtil;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> albumLauncher;
    private ActivityResultLauncher<String> permissionLauncher;
    private int id;
    private Snackbar snackbar;
    private PopupMenu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_info);
        // 初始化控件
        initViews();
        // 初始化数
        initData();
        // 设置监听事件
        initEvents();
        // 初始化launcher
        initLauncher();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setStatusBarColor(Color.rgb(9,204,125));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == EDIT_REQUEST_CODE && resultCode == RESULT_OK) {
            // 获取昵称
            String name = data.getStringExtra(getString(R.string.personal_name));
            tvEditName.setText(name);
        }
    }

    private void initLauncher() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // 使用之前创建的uri
                        Intent data = result.getData();
                        if(data != null) {
                            Bundle extras = data.getExtras();
                            Bitmap bitmap = (Bitmap) extras.get(getString(R.string.personal_data));
                            dealData(bitmap);
                        }
                    }
                });

        albumLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // 获取图片
                        Uri uri = result.getData().getData();
                        try {
                            // 通过URL获得一个BitMap
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            dealData(bitmap);
                        } catch (IOException e) {
                            Log.i(getString(R.string.personal_log), getString(R.string.personal_avatar_not_found), e);
                        }
                    }
                });

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if(isGranted) {
                        if(id == R.id.menu_camera) {
                            snackbar.dismiss();
                        cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
                        }
                        if(id == R.id.menu_gallery) {
                            snackbar.dismiss();
                        albumLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
                        }
                    } else {
                        snackbar.setText(getString(R.string.personal_permission_reject));
                        snackbar.setDuration(1000);
                        snackbar.show();
                        new Handler().postDelayed(() -> {
                            if (snackbar != null) {
                                snackbar.dismiss();
                            }
                        }, 1000);
                    }
                });
    }

    public void dealData(Bitmap bitmap) {
        if (bitmap != null) {
            // 使用弱引用处理Bitmap
            WeakReference<Bitmap> weakBitmap = new WeakReference<>(bitmap);
            Bitmap bmp = weakBitmap.get();
            if (bmp != null) {
                Glide.with(PersonInfoActivity.this)
                        .load(bmp)
                        .apply(RequestOptions.circleCropTransform())
                        .into(ivEditAvatar);
                
                // 存储到本地SD卡
                SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getString(R.string.user_pref_key_avatar), getString(R.string.personal_avatar));
                editor.apply();
                
                // 存储到本地SD卡并上传服务器
                File file = imageUtil.saveImageToSDCard(bmp);
                if (file != null) {
                    sendAvatarToServer(file);
                }
            }
        }
    }

    private void sendAvatarToServer(File file) {
        String auth = getString(R.string.request_header_key_token);
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        String token = sharedPreferences.getString(getString(R.string.user_pref_key_token), "");
        String url = getString(R.string.server_url) + getString(R.string.personal_avatar_send_request);
        OkHttpUtil.upLoadAvatar(url, auth, token, file, new OkHttpUtil.OnResponseListener() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    Toast.makeText(PersonInfoActivity.this, getString(R.string.personal_avatar_send_success), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(PersonInfoActivity.this, getString(R.string.personal_avatar_send_failure) + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void initEvents() {
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                // 处理图像与控件绑定
                if(msg.what == 1) {
                    Bitmap bitmap = (Bitmap) msg.obj;
                    // 设置头像为圆形
                    Glide.with(PersonInfoActivity.this)
                            .load(bitmap)
                            .apply(RequestOptions.circleCropTransform())
                            .into(ivEditAvatar);
                }
            }
        };

        ivEditAvatar.setOnClickListener(v -> {
            // 创建并显示 PopupMenu
            menu = new PopupMenu(PersonInfoActivity.this, ivEditAvatar);
            menu.inflate(R.menu.menu_image_picker);

            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    id = item.getItemId();
                    if(id == R.id.menu_camera) {
                        if (checkCameraPermission()) {
                            cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE_SECURE));
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA);
                            snackbar = Snackbar.make(findViewById(R.id.personal_snack_bar_container), getString(R.string.personal_avatar_permission_camera), Snackbar.LENGTH_INDEFINITE).setAnchorView(R.id.personal_snack_bar_container);
                            snackbar.show();
                        }
                        return true;
                    }else if(id == R.id.menu_gallery) {
                        if (checkStoragePermission()) {
                            albumLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
                        } else {
                            String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                                    Manifest.permission.READ_MEDIA_IMAGES :
                                    Manifest.permission.READ_EXTERNAL_STORAGE;
                            permissionLauncher.launch(permission);
                            snackbar = Snackbar.make(findViewById(R.id.personal_snack_bar_container), getString(R.string.personal_avatar_permission_gallery), Snackbar.LENGTH_INDEFINITE).setAnchorView(R.id.personal_snack_bar_container);
                            snackbar.show();
                        }
                        return true;
                    }
                    return false;
                }
            });
            menu.show();
        });

        tvEditName.setOnClickListener(v -> {
            // 更换昵称
            Intent intent = new Intent(PersonInfoActivity.this, EditNameActivity.class);
            intent.putExtra("name", tvEditName.getText());
            startActivityForResult(intent, EDIT_REQUEST_CODE);
        });
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkStoragePermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES :
                Manifest.permission.READ_EXTERNAL_STORAGE;
        return ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void initData() {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        String nickName = sharedPreferences.getString(getString(R.string.user_pref_key_nick_name), "");
        String avatar = sharedPreferences.getString(getString(R.string.user_pref_key_avatar), "");
        tvEditName.setText(nickName);
        Bitmap bitmap = imageUtil.readAvatarFromSDCard(avatar);
        if(bitmap == null) {
            Glide.with(PersonInfoActivity.this)
                    .load(avatar)
                    .apply(RequestOptions.circleCropTransform())
                    .into(ivEditAvatar);
        }else {
            Glide.with(PersonInfoActivity.this)
                    .load(bitmap)
                    .apply(RequestOptions.circleCropTransform())
                    .into(ivEditAvatar);
        }
    }

    private void initViews() {
        ivEditAvatar = findViewById(R.id.iv_edit_avatar);
        tvEditName = findViewById(R.id.tv_edit_name);
        imageUtil = new ImageUtil(this);
    }
}