package cn.agrinav.front.util;

import android.content.Context;
import android.widget.Toast;

import java.util.List;

/**
 * @author lya
 * @date 2024/11/11
 * @description 一般工具类
 */
public class CommonUtils {
    /**
     * 等待数据获取
     *
     * @param data    数据源
     * @param context 上下文
     */
    public static void waitDataGetting(List data, Context context) {
        while (data == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public static void waitDataGetting(Integer data, Context context) {
        while (data == 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void waitDataGetting(Float data, Context context) {
        while (data == 0.0F) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /***
     * 与权限有关
     */

}
