package cn.agrinav.personal.utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;

import cn.agrinav.R;
import cn.agrinav.measure.utils.GlideApp;
import cn.agrinav.personal.activity.info.PersonInfoActivity;

public class ImageUtil {
    private Activity activity;

    public ImageUtil(Activity activity) {
        this.activity = activity;
    }

    // 添加压缩图片的方法
    private Bitmap compressBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        
        // 计算压缩比例
        int maxSize = 1024; // 设置最大边长为1024像素
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = 1.0f;
        
        if (width > height && width > maxSize) {
            scale = (float) maxSize / width;
        } else if (height > width && height > maxSize) {
            scale = (float) maxSize / height;
        }
        
        if (scale < 1) {
            int newWidth = Math.round(width * scale);
            int newHeight = Math.round(height * scale);
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        }
        
        return bitmap;
    }

    public File saveImageToSDCard(Bitmap bitmap) {
        // 在保存前压缩图片
        Bitmap compressedBitmap = compressBitmap(bitmap);
        
        String storageState = Environment.getExternalStorageState();
        if(storageState.equals(Environment.MEDIA_MOUNTED)) {
            String path = activity.getExternalFilesDir(null).getAbsolutePath() + "/imageFile/";
            String fileName = "avatar.png";
            File dir = new File(path);
            if(!dir.exists()) {
                dir.mkdir();
            }
            File file = new File(dir, fileName);
            try {
                FileOutputStream fos = new FileOutputStream(file);
                // 使用压缩后的图片，并降低保存质量到80%
                compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                fos.flush();
                fos.close();
                // 更新媒体库
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(file);
                mediaScanIntent.setData(contentUri);
                activity.sendBroadcast(mediaScanIntent);
                return file;
            }catch (Exception e) {
                e.printStackTrace();
            } finally {
                // 及时回收Bitmap
                if (compressedBitmap != null && compressedBitmap != bitmap) {
                    compressedBitmap.recycle();
                }
            }
        }
        return null;
    }

    public Bitmap readAvatarFromSDCard(String avatar) {
        Bitmap avatarBitmap = null;
        String storageState = Environment.getExternalStorageState();
        if (storageState.equals(Environment.MEDIA_MOUNTED)) {
            if(avatar != null && !avatar.isEmpty()) {
                String path = Objects.requireNonNull(activity.getExternalFilesDir(null)).getAbsolutePath() + "/imageFile/avatar.png";
                File avatarFile = new File(path);
                avatarBitmap = BitmapFactory.decodeFile(avatarFile.getAbsolutePath());
            }else {
                avatarBitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.login_avatar);
            }
        }
        return avatarBitmap;
    }

}
