package cn.agrinav.measure.utils;


import android.app.Activity;

import android.graphics.Bitmap;
import android.os.Handler;


import com.amap.api.maps.AMap;

import cn.agrinav.front.entity.LandPlotExclude;
import cn.agrinav.measure.entity.LandPlot;


import java.io.File;


/**
 * @author 18506
 */
public class PrintUtil {
    public static void printScreen(Activity activity, String requestUrl, LandPlot landPlot, AMap aMap, Handler handler,Long phone) {
        aMap.getMapScreenShot(new AMap.OnMapScreenShotListener() {
            @Override
            public void onMapScreenShot(Bitmap bitmap) {
                File imageFile = FileUtil.saveBitmapToFile(activity,bitmap,phone);
                if (imageFile != null) {
                    ThreadPoolUtils.initThreadPool();
                    ThreadPoolUtils.threadPoolExecutor.execute(()-> UploadUtils.upload(activity, requestUrl, landPlot, imageFile,handler));
                    ThreadPoolUtils.shutdownThreadPool();
                }
            }

            @Override
            public void onMapScreenShot(Bitmap bitmap, int i) {

            }
        });
    }

    public static void printScreen(Activity activity, String requestUrl, LandPlotExclude landPlot, AMap aMap, Handler handler, Long phone) {
        aMap.getMapScreenShot(new AMap.OnMapScreenShotListener() {
            @Override
            public void onMapScreenShot(Bitmap bitmap) {
                File imageFile = FileUtil.saveBitmapToFile(activity,bitmap,phone);
                if (imageFile != null) {
                    ThreadPoolUtils.initThreadPool();
                    ThreadPoolUtils.threadPoolExecutor.execute(()-> UploadUtils.upload(activity, requestUrl, landPlot, imageFile,handler));
                    ThreadPoolUtils.shutdownThreadPool();
                }
            }

            @Override
            public void onMapScreenShot(Bitmap bitmap, int i) {

            }
        });
    }
}
