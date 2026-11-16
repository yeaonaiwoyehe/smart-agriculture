package cn.agrinav.measure.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;




import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * @author 18506
 */
public class FileUtil {
    private static final String MY_PATH = "/Pictures/";
    //将 Bitmap 压缩并保存为文件
    public static File saveBitmapToFile(Activity activity, Object bitmap, Long phone ) {
        //拿到存储路径
        String path = getStorageDirectory(activity);
        File imageFile = new File(path,phone +".jpg");
        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            ((Bitmap)bitmap).compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            return imageFile;
        } catch (IOException e) {
            Log.i("FileUtil", "saveBitmapToFile: " + e.getMessage());
            return imageFile;
        }
    }
    //获取存储文件的目录，如果目录不存在则创建。
    public static String getStorageDirectory(Activity activity) {
        String storageDirectory =activity.getExternalFilesDir(null).getAbsolutePath()+"/";
//        File directory = new File(storageDirectory);
//        if (!directory.exists()) {
//            try {
//                directory.createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        return storageDirectory;
    }
}
