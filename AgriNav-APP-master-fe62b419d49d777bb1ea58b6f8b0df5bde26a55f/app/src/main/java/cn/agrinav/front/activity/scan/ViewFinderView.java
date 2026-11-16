package cn.agrinav.front.activity.scan;

import static android.opengl.ETC1.getHeight;
import static android.opengl.ETC1.getWidth;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class ViewFinderView extends View {
    private static final int DEFAULT_HEIGHT = 800;
    private static final int DEFAULT_WIDTH = 800;
    private static final float CORNER_LENGTH = 40f;
    private static final float CORNER_WIDTH = 8f;
    private static final float SCAN_LINE_WIDTH = 6f;
    private static final float SCAN_SPEED = 5f;

    private Paint paint;
    private float cornerLength;
    private float cornerWidth;
    private float scanLineWidth;
    private float scanLineY;
    private float scanSpeed;
    private float frameWidth;
    private float frameHeight;
    private ValueAnimator animator;

    public ViewFinderView(Context context, AttributeSet attrs, int defStyleAttr, int height, int width) {
        super(context, attrs, defStyleAttr);
        if (height <= 0 || width <= 0) {
            throw new IllegalArgumentException("Height and width must be positive");
        }
        this.frameWidth = height;
        this.frameHeight = width;
        init();
    }

    public ViewFinderView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, DEFAULT_HEIGHT, DEFAULT_WIDTH);
    }

    public ViewFinderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewFinderView(Context context) {
        this(context, null);
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        cornerLength = CORNER_LENGTH;
        cornerWidth = CORNER_WIDTH;
        scanLineWidth = SCAN_LINE_WIDTH;
        scanSpeed = SCAN_SPEED;

        animator = ValueAnimator.ofFloat(0f, frameHeight);
        animator.setDuration(2000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.addUpdateListener(animation -> {
            scanLineY = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float frameLeft = (getWidth() - frameWidth) / 2;
        float frameTop = (getHeight() - frameHeight) / 2;

        // 绘制半透明背景
        paint.setColor(Color.parseColor("#99000000"));
        canvas.drawRect(0, 0, getWidth(), frameTop, paint);
        canvas.drawRect(0, frameTop + frameHeight, getWidth(), getHeight(), paint);
        canvas.drawRect(0, frameTop, frameLeft, frameTop + frameHeight, paint);
        canvas.drawRect(frameLeft + frameWidth, frameTop, getWidth(), frameTop + frameHeight, paint);

        // 绘制扫描框四角
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(cornerWidth);

        // 左上角
        canvas.drawLine(frameLeft, frameTop, frameLeft + cornerLength, frameTop, paint);
        canvas.drawLine(frameLeft, frameTop, frameLeft, frameTop + cornerLength, paint);

        // 右上角
        canvas.drawLine(frameLeft + frameWidth - cornerLength, frameTop, frameLeft + frameWidth, frameTop, paint);
        canvas.drawLine(frameLeft + frameWidth, frameTop, frameLeft + frameWidth, frameTop + cornerLength, paint);

        // 左下角
        canvas.drawLine(frameLeft, frameTop + frameHeight - cornerLength, frameLeft, frameTop + frameHeight, paint);
        canvas.drawLine(frameLeft, frameTop + frameHeight, frameLeft + cornerLength, frameTop + frameHeight, paint);

        // 右下角
        canvas.drawLine(frameLeft + frameWidth - cornerLength, frameTop + frameHeight, frameLeft + frameWidth, frameTop + frameHeight, paint);
        canvas.drawLine(frameLeft + frameWidth, frameTop + frameHeight - cornerLength, frameLeft + frameWidth, frameTop + frameHeight, paint);

        // 绘制扫描线
        paint.setAlpha(100);
        canvas.drawRect(frameLeft, frameTop + scanLineY, frameLeft + frameWidth, frameTop + scanLineY + scanLineWidth, paint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        animator.cancel();
    }
}
