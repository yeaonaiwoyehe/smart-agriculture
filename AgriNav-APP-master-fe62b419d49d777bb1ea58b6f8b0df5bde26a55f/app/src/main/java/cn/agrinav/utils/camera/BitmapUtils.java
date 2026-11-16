package cn.agrinav.utils.camera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;

/**
 * @author 11230
 */
public class BitmapUtils {
    public static byte[] compressBitmap(Context context,byte[] imageBytes, int width, int height, Matrix matrix){
        BitmapFactory.Options options = new BitmapFactory.Options();
        //解析出来原始的bitmap
        Bitmap bitmapOrigin = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
        Toast.makeText(context, "原始：" + imageBytes.length/1000000f+"MB", Toast.LENGTH_SHORT).show();
        saveBitmapToAlbum(context,bitmapOrigin);
        //根据原始bitmap进行放缩和旋转
        Bitmap bitmapMatrix =  Bitmap.createBitmap(bitmapOrigin, 0, 0, bitmapOrigin.getWidth(), bitmapOrigin.getHeight(),matrix,true);
        Bitmap bitmapScaled =  Bitmap.createScaledBitmap(bitmapMatrix, width, height,true);
        //回收原始bitmap和旋转后的bitmap
        bitmapOrigin.recycle();
        bitmapMatrix.recycle();
        //创建新的bitmap
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // 压缩图片把新图片的流传输到 outputStream 中
            boolean compress = bitmapScaled.compress(Bitmap.CompressFormat.PNG, 85, outputStream);
            // 获取到字节数组
            byte[] result =  outputStream.toByteArray();
            //回收位图
            bitmapScaled.recycle();
            //返回压缩后的图片
            return result;
        } catch (IOException e) {
            Log.e("TAG", "图片压缩异常", e);
            return null;
        }
    }
    private static void saveBitmapToAlbum(Context context,Bitmap bitmap) {
        // 获取当前时间作为文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        // 创建 ContentValues 对象
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName);
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyApp");

        // 获取内容解析器
        ContentResolver contentResolver = context.getContentResolver();

        // 插入图片到媒体库
        Uri imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        if (imageUri != null) {
            try (OutputStream outputStream = contentResolver.openOutputStream(imageUri)) {
                if (outputStream != null) {
                    // 压缩图片并写入输出流
                    boolean isSaved = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    if (isSaved) {
                        Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "保存图片失败", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (IOException e) {
                Log.e("TAG", "保存图片异常", e);
                Toast.makeText(context, "保存图片失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "保存图片失败", Toast.LENGTH_SHORT).show();
        }
    }
}
