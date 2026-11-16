package cn.agrinav.measure.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;


import com.amap.api.maps.AMap;

import cn.agrinav.front.entity.LandPlotExclude;
import cn.agrinav.measure.entity.LandPlot;


/**
 * @author 18506
 */
public class DialogUtils{
    public static void uploadDialog(Activity activity , LandPlot landPlot , String requestUrl, AMap amap, Handler handler, Long phone) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage("是否上传");
        builder.setNegativeButton("取消", null);
        builder.setPositiveButton("确定", (dialog, which) -> {
            //将地块,截图上传到服务器
            PrintUtil.printScreen(activity,requestUrl,landPlot,amap,handler,phone);
        });
        builder.create().show();
    }

}
