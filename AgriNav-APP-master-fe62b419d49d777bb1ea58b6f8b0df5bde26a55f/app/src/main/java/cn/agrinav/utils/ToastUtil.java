package cn.agrinav.utils;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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
     * 显示自定义toast
     * @param context 上下文
     * @param content 内容
     */
    public void getToast(Activity context, String content){
        @SuppressLint("InflateParams") View view =context.getLayoutInflater().inflate(R.layout.toast_first,null);
        getSize(context);
        ViewGroup.LayoutParams layoutParams = new LinearLayout.LayoutParams((int)(0.55 * screenWidth),(int)(0.18 * screenHeight));
        view.setLayoutParams(layoutParams);
        TextView toastText = view.findViewById(R.id.tv_toast_first);
        toastText.setText(content);
        Toast toast = new Toast(context);
        toast.setView(view);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    public void getToast(Activity context, String content, int type){
        @SuppressLint("InflateParams") View view =context.getLayoutInflater().inflate(R.layout.toast_first,null);
        getSize(context);
        ViewGroup.LayoutParams layoutParams = new LinearLayout.LayoutParams((int)(0.55 * screenWidth),(int)(0.18 * screenHeight));
        view.setLayoutParams(layoutParams);
        ImageView icon = view.findViewById(R.id.img_toast_icon);
        icon.setImageDrawable(context.getDrawable(type==TOAST_TRUE?R.drawable.toast_true:R.drawable.toast_false));
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