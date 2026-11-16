package cn.agrinav.front.activity.findjob.utils;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cn.agrinav.R;

public class JobUIUtils {
    
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
     * 生成默认任务名称
     */
    public static String generateDefaultTaskName(Context context) {
        String format = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
        return context.getString(R.string.find_job_task_name_default) + format;
    }

    /**
     * 检查字符串是否为空
     */
    public static boolean isEmpty(String... strings) {
        for (String str : strings) {
            if (str == null || str.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 设置视图可见性
     */
    public static void setViewVisibility(int visibility, View... views) {
        for (View view : views) {
            if (view != null) {
                view.setVisibility(visibility);
            }
        }
    }

    /**
     * 处理选择器的展开和收起
     * @param currentSelector 当前选择器布局
     * @param filterManager 筛选管理器
     * @param hideEditText 隐藏输入框的回调
     */
    public static void handleSelectorToggle(View currentSelector, 
                                          JobFilterManager filterManager,
                                          Runnable hideEditText) {
        // 编辑框隐藏光标
        if (hideEditText != null) {
            hideEditText.run();
        }
        
        // 如果当前选择器已展开，则只需收起
        if (currentSelector.getVisibility() == View.VISIBLE) {
            filterManager.hideAllSelectors();
            return;
        }
        
        // 收起其他选择器并展开当前选择器
        filterManager.hideAllSelectors();
        currentSelector.setVisibility(View.VISIBLE);
    }
} 