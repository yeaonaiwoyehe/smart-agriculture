package cn.agrinav.front.activity.findfarm.utils;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import java.util.List;
import java.util.Locale;
import cn.agrinav.front.entity.NewLandPlot;


public class UIUtils {

    /**
     * 隐藏软键盘并清除焦点
     */
    public static void hideKeyboardAndClearFocus(Context context, View... views) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        for (View view : views) {
            if (view != null) {
                view.clearFocus();
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    /**
     * 显示Toast消息
     */
    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 更新地块总面积显示
     */
    public static void updateTotalArea(List<NewLandPlot> selectedLandPlots, EditText edtEarthNum) {
        if (selectedLandPlots == null || selectedLandPlots.isEmpty()) {
            setEditTextEditable(edtEarthNum, true);
            return;
        }

        float totalArea = 0;
        for (NewLandPlot landPlot : selectedLandPlots) {
            if (landPlot.getLandArea() != null) {
                totalArea += landPlot.getLandArea();
            }
        }

        edtEarthNum.setText(String.format(Locale.getDefault(), "%.3f", totalArea));
        setEditTextEditable(edtEarthNum, false);
    }

    /**
     * 设置EditText是否可编辑
     */
    private static void setEditTextEditable(EditText editText, boolean editable) {
        editText.setEnabled(editable);
        editText.setFocusable(editable);
        editText.setFocusableInTouchMode(editable);
    }

    public static String getCurrentTime() {
        return String.valueOf(System.currentTimeMillis());
    }
}
