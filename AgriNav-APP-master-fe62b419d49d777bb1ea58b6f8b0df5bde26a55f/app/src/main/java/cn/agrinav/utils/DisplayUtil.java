package cn.agrinav.utils;

import android.content.Context;

/**
 * @author 11230
 */
public class DisplayUtil{
    /**
     * px转化为dp或者dip
     */
    public static int px2dip(Context context, float pxValue) {
        //得到换算比例
        float scale = context.getResources().getDisplayMetrics().density;
        //加上0.5f是为了结果四舍五入
        return (int) (pxValue / scale + 0.5f);
    }
    /**
     * dip或者dp转换为px
     */
    public static int dip2px(Context context, float dpValue) {
        //得到换算比例
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
    /**
     * px转换为sp
     */
    public static int px2sp(Context context, float pxValue) {
        //得到换算比例
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
    /**
     * sp转化为px
     */
    public static int sp2px(Context context, float spValue) {
        //得到换算比例
        float fontScale = context.getResources().getDisplayMetrics().density;
        return (int) (spValue * fontScale + 0.5f);
    }
}