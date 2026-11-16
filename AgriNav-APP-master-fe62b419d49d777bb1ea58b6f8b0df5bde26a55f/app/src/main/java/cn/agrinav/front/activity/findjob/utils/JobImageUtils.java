package cn.agrinav.front.activity.findjob.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import cn.agrinav.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class JobImageUtils {
    private static final String APP_FOLDER_NAME = "AgriNav";

    /**
     * 将图片添加到列表中
     */
    public static void addImageToList(Context context, Uri imageUri, List<String> imageList,
                                      boolean hasUserSelectedImage, ImageCallback callback) {
        if (imageUri != null) {
            try {
                // 获取应用专属的外部存储目录
                File storageDir = new File(
                        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                        APP_FOLDER_NAME
                );
                if (!storageDir.exists()) {
                    storageDir.mkdirs();
                }

                // 创建新的图片文件
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(new Date());
                File imageFile = new File(storageDir, "IMG_" + timeStamp + ".jpg");

                // 将Uri对应的图片复制到新文件
                copyUriToFile(context, imageUri, imageFile);

                // 将文件路径添加到列表
                imageList.add(imageFile.getAbsolutePath());

                // 通知回调
                if (callback != null) {
                    callback.onImageAdded();
                }

            } catch (IOException e) {
                Toast.makeText(context,
                        context.getString(R.string.find_job_image_save_failure),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 从Uri复制文件到指定路径
     */
    private static void copyUriToFile(Context context, Uri sourceUri, File destFile)
            throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(destFile)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

    /**
     * 获取Bitmap的Uri
     */
    public static Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(
                context.getContentResolver(),
                bitmap,
                "Title",
                null
        );
        return Uri.parse(path);
    }

    /**
     * 删除指定目录下的所有图片
     */
    public static boolean deleteImages(String directoryPath) {
        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".jpg") ||
                            name.toLowerCase().endsWith(".jpeg") ||
                            name.toLowerCase().endsWith(".png")
            );

            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    public interface ImageCallback {
        void onImageAdded();
    }

    public static void downloadImage(String imageUrl, File imageFile, List<String> machineImages,
                                     CountDownLatch latch) {
        OkHttpClient imageClient = new OkHttpClient.Builder().build();

        Request request = new Request.Builder()
                .url(imageUrl)
                .build();

        imageClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("JobImageUtils", "Download image failed: " + imageUrl, e);
                latch.countDown();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try (InputStream inputStream = response.body().byteStream();
                         FileOutputStream outputStream = new FileOutputStream(imageFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }

                        synchronized (machineImages) {
                            machineImages.add(imageFile.getAbsolutePath());
                        }
                    } catch (IOException e) {
                        Log.e("JobImageUtils", "Save image failed: " + imageUrl, e);
                    }
                }
                latch.countDown();
            }
        });
    }

    /**
     * 生成图片文件名
     * @return 生成的文件名
     */
    public static String generateImageFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault())
                .format(new Date());
        return "IMG_" + timeStamp + "_" + System.nanoTime() + ".jpg";
    }

    /**
     * 创建图片文件
     * @param context 上下文
     * @param fileName 文件名
     * @return 创建的文件
     */
    public static File createImageFile(Context context, String fileName) {
        // 创建存储目录
        File storageDir = new File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "AgriNav"
        );
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        // 创建新的图片文件
        return new File(storageDir, fileName);
    }
}