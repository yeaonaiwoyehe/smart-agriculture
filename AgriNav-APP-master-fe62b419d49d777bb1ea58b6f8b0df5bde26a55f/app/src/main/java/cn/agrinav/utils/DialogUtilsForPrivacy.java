package cn.agrinav.utils;


import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import cn.agrinav.R;
import cn.agrinav.user.ProtocolActivity;
import lombok.Getter;
import lombok.NonNull;

/**
 * @author 11230
 */
public class DialogUtilsForPrivacy {
    private final TextView dialogTextContent;
    private final Button dialogBtnOk;
    private final Button dialogBtnCancel;
    private Context context;
    @Getter
    private View view;
    private String content;
    private String btnOk;
    private String btnCancel;
    public DialogUtilsForPrivacy(Context context){
        //这个是个view
        this.context = context;
        view = View.inflate(context, R.layout.layout_dialog_for_privacy,null);
        dialogTextContent = view.findViewById(R.id.tv_dialog_privacy_content);
        dialogBtnOk = view.findViewById(R.id.btn_dialog_privacy_confirm);
        dialogBtnCancel = view.findViewById(R.id.btn_dialog_privacy_cancel);
        //这里设置初始值
        content = "\t\t\t\t亲爱的用户，感谢您信任并使用田界智航! \n\t\t\t\t田界智航APP主要功能为轨迹测绘、" +
                "地块测量与农商合作，根据《常见类型移动互联网应用程序必要个人信息范围规定》，本应用属于地图导航与网络社区类应用，" +
                "所需必要信息为位置信息、出发地、到达地注册用户移动电话号码.\n\t\t\t\t您同意《田界智航隐私政策》仅代表同意使用定位与网络社区交互功能时收集、处理相关必要信息,不代表同意其他功能收集、处理相关必要信息,其他功能处理个人信息将单独征求您的同意";
        SpannableString spannableString = getSpannableString(context);
        btnOk = "同意";
        btnCancel = "不同意";
        dialogTextContent.setText(spannableString);
                // 必须设置这个属性才能响应点击事件
        dialogTextContent.setMovementMethod(LinkMovementMethod.getInstance());
        dialogBtnOk.setText(btnOk);
        dialogBtnCancel.setText(btnCancel);


    }

    private @androidx.annotation.NonNull SpannableString getSpannableString(Context context) {
        SpannableString spannableString = new SpannableString(content);

        // 定义需要点击的文本及其位置
        String clickableText = "《田界智航隐私政策》";
        int startIndex = content.indexOf(clickableText);
        int endIndex = startIndex + clickableText.length();

        // 创建点击事件
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Intent intent = new Intent(context, ProtocolActivity.class);
                intent.putExtra(context.getString(R.string.protocol_intent_key),false);
                context.startActivity(intent);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                // 设置点击文本的样式
                ds.setColor(context.getResources().getColor(R.color.green_final)); // 设置链接颜色
                ds.setUnderlineText(true); // 设置下划线
            }
        };
        // 应用点击事件到文本
        spannableString.setSpan(clickableSpan, startIndex, endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    public DialogUtilsForPrivacy setPositiveListener(View.OnClickListener listener){
        dialogBtnOk.setOnClickListener(listener);
        return this;
    }
    public DialogUtilsForPrivacy setNegativeListener(View.OnClickListener listener){
        dialogBtnCancel.setOnClickListener(listener);
        return this;
    }
    public DialogUtilsForPrivacy setPositiveText(String text){
        btnOk = text;
        dialogBtnOk.setText(text);
        return this;
    }
    public DialogUtilsForPrivacy setNegativeText(String text){
        btnCancel = text;
        dialogBtnCancel.setText(text);
        return this;
    }
    public AlertDialog getDialog(){
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(context)
                .setCancelable(false)
                .show();
        int width = DisplayUtil.dip2px(context, 340);
        int height = DisplayUtil.dip2px(context, 510);
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
