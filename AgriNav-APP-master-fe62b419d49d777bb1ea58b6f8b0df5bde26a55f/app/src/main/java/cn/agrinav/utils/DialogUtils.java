package cn.agrinav.utils;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import cn.agrinav.R;
import lombok.Getter;

/**
 * @author 11230
 */
public class DialogUtils {
    private final TextView dialogTextTitle;
    private final TextView dialogTextContent;
    private final Button dialogBtnOk;
    private final Button dialogBtnCancel;
    private Context context;
    @Getter
    private View view;
    private String title;
    private String content;
    private String btnOk;
    private String btnCancel;
    public DialogUtils(Context context){
        //这个是个view
        this.context = context;
        view = View.inflate(context, R.layout.layout_dialog,null);
        dialogTextTitle = view.findViewById(R.id.dialog_text_title);
        dialogTextContent = view.findViewById(R.id.dialog_text_content);
        dialogBtnOk = view.findViewById(R.id.dialog_button_yes);
        dialogBtnCancel = view.findViewById(R.id.dialog_button_no);
        //这里设置初始值
        title = "温馨提示";
        content = "该页面需要开启定位功能获取周围地块，请打开定位功能再使用";
        btnOk = "去设置";
        btnCancel = "取消";
        dialogTextTitle.setText(title);
        dialogTextContent.setText(content);
        dialogBtnOk.setText(btnOk);
        dialogBtnCancel.setText(btnCancel);
    }
    public DialogUtils setTitle(String title){
        this.title = title;
        dialogTextTitle.setText(title);
        return this;
    }
    public DialogUtils setContent(String content){
        this.content = content;
        dialogTextContent.setText(content);
        return this;
    }
    public DialogUtils setPositiveListener(View.OnClickListener listener){
        dialogBtnOk.setOnClickListener(listener);
        return this;
    }
    public DialogUtils setNegativeListener(View.OnClickListener listener){
        dialogBtnCancel.setOnClickListener(listener);
        return this;
    }
    public DialogUtils setPositiveText(String text){
        btnOk = text;
        dialogBtnOk.setText(text);
        return this;
    }
    public DialogUtils setNegativeText(String text){
        btnCancel = text;
        dialogBtnCancel.setText(text);
        return this;
    }
    public AlertDialog getDialog(){
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(context)
                .setCancelable(false)
                .show();
        int width = DisplayUtil.dip2px(context, 340);
        int height = DisplayUtil.dip2px(context, 390);
        // 获取弹窗的Window对象
        Window window = alertDialog.getWindow();
        window.setLayout(width,height);
        // 设置弹窗的宽度和高度
        WindowManager.LayoutParams params = window.getAttributes();
        // 设置弹窗宽度为屏幕宽度
        params.width = width;
        // 设置弹窗高度为自适应内容
        params.height =height;
        window.setAttributes(params);
        return alertDialog;
    }
}
