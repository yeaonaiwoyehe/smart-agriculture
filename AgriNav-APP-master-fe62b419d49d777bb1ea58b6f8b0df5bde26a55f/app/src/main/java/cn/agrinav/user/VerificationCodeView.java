package cn.agrinav.user;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import android.util.AttributeSet;


import androidx.annotation.NonNull;

import cn.agrinav.R;
import cn.agrinav.utils.DisplayUtil;

/**
 * @author 11230
 */
public class VerificationCodeView extends androidx.appcompat.widget.AppCompatEditText{
    /**间隔*/
    private float space = 45;
    /**验证码长度*/
    private int codeLength = 6;
    /**未输入下划线颜色*/
    private int beforeLineColor = Color.WHITE;
    /**当前输入下划线颜色*/
    private int currentLineColor = Color.WHITE;
    /**文字颜色*/
    private int textColor = Color.WHITE;

    /** 文本字体大小*/
    private float textSize = 70f;

    /** 验证码框下划线宽度*/
    private int lineWidth = 4;
    /**已输入背景框圆角*/
    private float bgBorderRadius = 24;
    /**已输入背景框高度*/
    private float bgBorderHeight = 50;
    /**文本画笔 */
    private Paint mTextPaint;
    /**背景框画笔*/
    private Paint mBorderPaint;
    /** 输入的密码长度*/
    private int mInputLength;
    /** 输入结束监听*/
    private OnInputFinishListener mOnInputFinishListener;
    /** 左右间距*/
    private float paddingStart, paddingEnd, paddingTop, paddingBottom = 0;
    private String text;
    public VerificationCodeView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.VerificationCodeView);
        codeLength = array.getInteger(R.styleable.VerificationCodeView_codeLength, codeLength);
        beforeLineColor = array.getInteger(R.styleable.VerificationCodeView_beforeLineColor, beforeLineColor);
        currentLineColor = array.getInteger(R.styleable.VerificationCodeView_currentLineColor, currentLineColor);
        bgBorderHeight = array.getDimension(R.styleable.VerificationCodeView_bgHeightWidth, bgBorderHeight);
        bgBorderRadius = array.getDimension(R.styleable.VerificationCodeView_bgBorderRadius, bgBorderRadius);
        space = array.getDimension(R.styleable.VerificationCodeView_space, space);
        lineWidth = (int) array.getDimension(R.styleable.VerificationCodeView_lineWidth, lineWidth);

        textSize = array.getDimension(R.styleable.VerificationCodeView_textSize, textSize);
        textColor = array.getInteger(R.styleable.VerificationCodeView_textColor, textColor);

        paddingStart = array.getDimension(R.styleable.VerificationCodeView_paddingStart, paddingStart);
        paddingEnd = array.getDimension(R.styleable.VerificationCodeView_paddingEnd, paddingEnd);
        paddingTop = array.getDimension(R.styleable.VerificationCodeView_paddingTop, paddingTop);
        paddingBottom = array.getDimension(R.styleable.VerificationCodeView_paddingBottom, paddingBottom);

        // 初始化文字画笔
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(textColor);
        mTextPaint.setTextSize(textSize);

        // 初始化下划线
        mBorderPaint = new Paint();
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(beforeLineColor);
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setStrokeWidth(lineWidth);
        mBorderPaint.setStrokeCap(Paint.Cap.ROUND);
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //获取控件宽高的显示模式
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        //获取宽高的尺寸值 固定值的宽度
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        //设置宽高默认为建议的最小宽高
        int width;
        int height;
        // MeasureSpec父布局传递给后代的布局要求 包含 确定大小和三种模式
        // EXACTLY：一般是设置了明确的值或者是MATCH_PARENT
        // AT_MOST：表示子布局限制在一个最大值内，一般为WARP_CONTENT
        // UNSPECIFIED：表示子布局想要多大就多大，很少使用
        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else {
            width = widthSize;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else {
            height = (int) (bgBorderHeight + paddingTop + paddingBottom + lineWidth * 2);
        }
        //设置测量的宽高
        setMeasuredDimension(width, height);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 这三行代码非常关键，大家可以注释点在看看效果
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

        // 计算每个密码框宽度
        int rectWidth = (int) ((getWidth() - space * (codeLength - 1) - paddingStart - paddingEnd) / codeLength);
        int bottom = (int) (paddingStart + bgBorderHeight);

        // 绘制背景
        for (int i = 0; i < codeLength; i++) {
            int left = (int) (paddingStart + lineWidth + (rectWidth + space) * i);
            int right = (int) (left + rectWidth - paddingEnd - lineWidth);
            if (i < mInputLength) {
                //绘制背景色
                mBorderPaint.setColor(currentLineColor);
                //绘制文本
                int cx = left + rectWidth / 2;
                int cy = (int) ((float) bottom / 2 + DisplayUtil.px2dip(getContext(),DisplayUtil.sp2px(getContext(),textSize)));
                String[] c = text.split("");
                mTextPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(c[i], cx, cy, mTextPaint);
                canvas.drawLine(left, bottom, right, bottom, mBorderPaint);

            } else {
                    mBorderPaint.setColor(beforeLineColor);
                canvas.drawLine(left, bottom, right, bottom, mBorderPaint);
            }
        }
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        this.mInputLength = text.toString().length();
        if (mInputLength <= codeLength) {
            this.text = text.toString();
            invalidate();
            if (mInputLength == codeLength && mOnInputFinishListener != null) {
                mOnInputFinishListener.onInputFinish(text.toString());
            }
        }
    }

    public interface OnInputFinishListener {
        /**
         * 密码输入结束监听
         * @param password 验证码的值
         */
        void onInputFinish(String password);
    }
    /**
     * 设置输入完成监听
     */
    public void setOnInputFinishListener(OnInputFinishListener onInputFinishListener) {
        this.mOnInputFinishListener = onInputFinishListener;
    }
}
