package cn.agrinav.utils;

import android.content.Context;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import cn.agrinav.R;
import lombok.Getter;

/**
 * @author 11230
 */
public class DialogUtilsForChose {
    private final TextView dialogBtnOk;
    private final TextView dialogBtnCancel;
    private WebView dialogContentView;
    private Context context;
    @Getter
    private View view;
    private String btnOk;
    private String btnCancel;
    private String contentViewUrl;
    public DialogUtilsForChose(Context context){
        //这个是个view
        this.context = context;
        view = View.inflate(context, R.layout.layout_dialog_for_choose,null);
        dialogBtnOk = view.findViewById(R.id.tv_dialog_for_choose_cancel);
        dialogBtnCancel = view.findViewById(R.id.tv_dialog_for_choose_confirm);
        dialogContentView = view.findViewById(R.id.view_dialog_for_choose);
        //这里设置初始值
        btnOk = "确认";
        btnCancel = "取消";
        dialogBtnOk.setText(btnOk);
        dialogBtnCancel.setText(btnCancel);
    }
    public DialogUtilsForChose setPositiveListener(View.OnClickListener listener){
        dialogBtnOk.setOnClickListener(listener);
        return this;
    }
    public DialogUtilsForChose setNegativeListener(View.OnClickListener listener){
        dialogBtnCancel.setOnClickListener(listener);
        return this;
    }
    public DialogUtilsForChose setContentViewUrl(String viewUrl){
        contentViewUrl = viewUrl;
        dialogContentView.loadUrl(viewUrl);
        return this;
    }
    public DialogUtilsForChose setPositiveText(String text){
        btnOk = text;
        dialogBtnOk.setText(text);
        return this;
    }
    public DialogUtilsForChose setNegativeText(String text){
        btnCancel = text;
        dialogBtnCancel.setText(text);
        return this;
    }
    public AlertDialog getDialog() {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(context)
                .setCancelable(false)
                .show();
        int width = DisplayUtil.dip2px(context, 300);
        int height = DisplayUtil.dip2px(context, 520);
        // 获取弹窗的Window对象
        Window window = alertDialog.getWindow();
        window.setLayout(width, height);
        // 设置弹窗的宽度和高度
        WindowManager.LayoutParams params = window.getAttributes();
        // 设置弹窗宽度为屏幕宽度
        params.width = width;
        // 设置弹窗高度为自适应内容
        params.height = height;
        window.setAttributes(params);
        return alertDialog;
    }
}
