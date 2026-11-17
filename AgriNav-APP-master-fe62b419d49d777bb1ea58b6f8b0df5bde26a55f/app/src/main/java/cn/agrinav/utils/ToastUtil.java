package cn.agrinav.utils;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import cn.agrinav.R;
/**
 * @author 11230
 */
public class ToastUtil  {
    private int screenWidth;
    private int screenHeight;
    private DisplayMetrics dm;
    public static final int TOAST_TRUE = 1;
    public static final int TOAST_FALSE = 0;

    /**
     * 保证 Toast 的构建与显示在主线程执行。
     * 原因：Toast 内部需要 Looper，在线程池/网络回调等没有 Looper 的后台线程
     * 直接 new Toast() 会抛 NullPointerException（Can't toast on a thread
     * that has not called Looper.prepare()）。
     */
    private void runOnMainThread(Activity context, Runnable action) {
        if (context == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (context.isFinishing() || context.isDestroyed()) {
                    return;
                }
                action.run();
            });
        }
    }

    /**
     * 显示自定义toast
     * @param context 上下文
     * @param content 内容
     */
    public void getToast(Activity context, String content){
        runOnMainThread(context, () -> buildAndShow(context, content, false, TOAST_FALSE));
    }

    /**
     * 显示带图标的自定义toast
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    public void getToast(Activity context, String content, int type){
        runOnMainThread(context, () -> buildAndShow(context, content, true, type));
    }

    /**
     * 实际构建并显示自定义 Toast，始终在主线程被调用。
     */
    @SuppressLint("InflateParams")
    private void buildAndShow(Activity context, String content, boolean withIcon, int type) {
        View view = context.getLayoutInflater().inflate(R.layout.toast_first, null);
        getSize(context);
        ViewGroup.LayoutParams layoutParams = new LinearLayout.LayoutParams((int)(0.55 * screenWidth),(int)(0.18 * screenHeight));
        view.setLayoutParams(layoutParams);
        if (withIcon) {
            ImageView icon = view.findViewById(R.id.img_toast_icon);
            icon.setImageDrawable(context.getDrawable(type==TOAST_TRUE?R.drawable.toast_true:R.drawable.toast_false));
        }
        TextView toastText = view.findViewById(R.id.tv_toast_first);
        toastText.setText(content);
        Toast toast = new Toast(context);
        toast.setView(view);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();
    }

    /**度量屏幕尺寸*/
    public void getSize(Activity activity) {
        if (dm==null){
            WindowManager windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            dm = new DisplayMetrics();
            display.getMetrics(dm);
            screenWidth = dm.widthPixels;
            screenHeight = dm.heightPixels;
        }
    }
}
