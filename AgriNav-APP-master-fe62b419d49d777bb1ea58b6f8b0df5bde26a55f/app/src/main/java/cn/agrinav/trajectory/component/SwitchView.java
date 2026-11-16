package cn.agrinav.trajectory.component;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import cn.agrinav.R;

/**
 * 滑动按钮
 * @author 11230
 */
public class SwitchView extends FrameLayout {
    /**
     * LeftSwitch
     */
    private  String text_left_switch = "作业轨迹";
    /**
     * RightSwitch
     */
    private String text_right_switch = "原始轨迹";
    /**
     * TYPE - LEFT
     */
    public static final int TYPE_LEFT = 0;
    /**
     * TYPE - Right
     */
    public static final int TYPE_RIGHT = 1;
    /**
     * The best width
     */
    private static final int MAX_WIDTH = 300;
    /**
     * The best height
     */
    private static final int MAX_HEIGHT = 80;
    /**
     * In order to trick your eyes
     */
    private static final int FRAMES = 15;
    /**
     * Used to describe the degree of shaking
     */
    private static final int ARGUMENT_SHAKE = 2;
    /**
     * The offset for the upper view
     */
    private int mOffsetX = 0;
    /**
     * Total width of the floating layer (but I think that may not be required)
     */
    private int mFloatingLayerWidth;
    /**
     * The total width of the view (but I think that may not be required)
     */
    private int mTotalWidth;
    /**
     * Description of the currently checked type
     */
    private int mNowType = TYPE_LEFT;
    /**
     * In order to listen for checked events
     */
    private OnSwitchListener mSwitchListener;
    /**
     * Necessary object
     */
    private int background = R.drawable.switch_background;
    private int layerBackground = R.drawable.switch_select_background;
    private TextView mLeftSwitch;
    private TextView mRightSwitch;
    private ImageView mFloatingLayer;
    private LinearLayout mSwitchParent;
    /**
     * 手指的触摸控制
     */
    private GestureDetector mDetector = null;
    /**
     * 点击状态
     */
    private boolean isOpenClick = true;
    @SuppressLint("Recycle")
    private TypedArray array;

    public SwitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        array = getContext().obtainStyledAttributes(attrs, R.styleable.SwitchView);
        if (array.getString(R.styleable.SwitchView_text_left) != null) {
            text_left_switch = array.getString(R.styleable.SwitchView_text_left);
        }
        if (array.getString(R.styleable.SwitchView_text_right) != null) {
            text_right_switch = array.getString(R.styleable.SwitchView_text_right);
        }
        background = array.getResourceId(R.styleable.SwitchView_background, background);
        layerBackground = array.getResourceId(R.styleable.SwitchView_background_layer, layerBackground);
        init();
    }

    public SwitchView(Context context) {
        super(context);
        init();
    }

    private void init() {
        // 加载switch父视图
        mSwitchParent = new LinearLayout(getContext());
        mSwitchParent.setOrientation(LinearLayout.HORIZONTAL);
        //TODO：设置背景
        setBackgroundResource(background);
        // 添加一个覆盖的滑块的图片视图
        mFloatingLayer = new ImageView(getContext());
        //TODO:给滑块指定样式
        mFloatingLayer.setBackgroundResource(layerBackground);
        //给滑块指定大小
        LayoutParams flowLayoutParams = new LayoutParams( LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(mFloatingLayer, flowLayoutParams);
        //TODO:添加左右两侧文本框
        mLeftSwitch = new TextView(getContext());
        mRightSwitch = new TextView(getContext());
        //加载文本资源
        mLeftSwitch.setText(text_left_switch);
        mRightSwitch.setText(text_right_switch);
        //设定文本背景色
        mLeftSwitch.setBackgroundColor(Color.TRANSPARENT);
        mRightSwitch.setBackgroundColor(Color.TRANSPARENT);
        mLeftSwitch.setTextColor(Color.BLACK);
        mRightSwitch.setTextColor(Color.BLACK);
        //设定文本在文本框内居中显示
        mLeftSwitch.setGravity(Gravity.CENTER);
        mRightSwitch.setGravity(Gravity.CENTER);
        //构建文本的参数视图
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        lp.weight = 1;
        mLeftSwitch.setLayoutParams(lp);
        mRightSwitch.setLayoutParams(lp);
        //把两个文本框添加到layout父视图里
        mSwitchParent.addView(mLeftSwitch);
        mSwitchParent.addView(mRightSwitch);
        // 添加视图到这个玩意里面
        addView(mSwitchParent, LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        // 设定手势控制器
        mDetector = new GestureDetector(getContext(), new FlowGesture());

    }

    /**
     * 设置当这个开关被点击之后的事件
     *
     * @param sl 监听器
     */
    public void setOnSwitchListener(OnSwitchListener sl) {
        this.mSwitchListener = sl;
    }

    /**
     * 改变当前被选中的状态从做导游或者从右到左
     */
    public void switching() {
        int type = getType();
        if (type == TYPE_LEFT) {
            sliding(TYPE_RIGHT);
        } else {
            sliding(TYPE_LEFT);
        }
    }

    /**
     *指定划到某个状态下
     * @param type 当前状态
     * @see #TYPE_LEFT
     * @see #TYPE_RIGHT
     */
    public void switching(int type) {
        sliding(type);
    }

    /**
     * 如果现在选择的是左边，那么就划到右边，反之亦然
     *
     * @param isCheckedLeft 是否选择的是左边
     */
    public void switching(boolean isCheckedLeft) {
        System.out.println(isCheckedLeft);
        sliding(isCheckedLeft ? TYPE_LEFT : TYPE_RIGHT);
    }
    /**获取当前的选中状态类型*/
    public int getType() {
        if (mOffsetX == 0) {
            return TYPE_LEFT;
        } else {
            return TYPE_RIGHT;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 解析宽度和高度的测量规范
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        // 调整宽度和高度（在实际尺寸和最大尺寸之间挑选出来一个比较小的值）
        width = getBestWidth(width);
        height = getBestHeight(height);
        // 更新总宽度
        mTotalWidth = width;
        // 重新创建宽度和高度的测量规范
        int wSpec = MeasureSpec.makeMeasureSpec(width, wMode);
        int hSpec = MeasureSpec.makeMeasureSpec(height, hMode);
        // 计算浮动层的宽度
        mFloatingLayerWidth = getFlowWidth(width);
        // 测量浮动层的尺寸，高度应该和父视图相同但是宽度应该要按照自己的宽度来
        mFloatingLayer.measure( MeasureSpec.makeMeasureSpec(mFloatingLayerWidth, wMode), hSpec);
        // 测量开关组件的尺寸
        mSwitchParent.measure(wSpec, hSpec);
        // 设置最终的测量尺寸
        setMeasuredDimension(wSpec, hSpec);
    }

    private int getBestWidth(int width) {
        return Math.max(width, MAX_WIDTH);
    }

    private int getBestHeight(int height) {
        return Math.max(height, MAX_HEIGHT);
    }

    private int getFlowWidth(int total) {
        // expression (can make all the changes)
        return total / 2 + total / 50;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        setup(mFloatingLayer, mOffsetX);
    }

    private void setup(View child, int offset) {
        child.layout(offset, 0, offset + child.getMeasuredWidth(), child.getMeasuredHeight());
    }

    public void closeClickEvent() {
        isOpenClick = false;
    }

    public void openClickEvent() {
        isOpenClick = true;
    }

    private void sliding(int type) {
        // 获取当前 X 坐标
        final int nowPosX = mOffsetX;
        // 获取目标 X 坐标
        final int targetX = getX(type);
        if (nowPosX == targetX) {
            // 如果当前位置等于目标位置，则无需处理
            return;
        }
        // 计算移动距离
        final int distance = targetX - nowPosX;
        // 创建动画对象
        final FakeAnimation fake = new FakeAnimation(distance, 250);
        // 在主线程中执行动画更新
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (fake.isEnd()) {
                    // 如果动画结束，更新 mOffsetX 并请求重新布局
                    mOffsetX = targetX;
                    requestLayout();
                } else {
                    // 如果动画未结束，更新 mOffsetX 并请求重新布局
                    mOffsetX = fake.getValue() + mOffsetX;
                    requestLayout();
                    // 延迟执行此 Runnable，将当前的 Runnable 对象延迟 FRAMES 毫秒后再次执行。
                    postDelayed(this, FRAMES);
                }
            }
        });
        // 设置当前类型
        mNowType = type;
        // 执行检查操作
        performOnCheck(mNowType);
    }

    private int getX(int type) {
        if (type == TYPE_LEFT) {
            return 0;
        } else if (type == TYPE_RIGHT) {
            return mTotalWidth - mFloatingLayerWidth;
        } else {
            return 0;
        }
    }

    /**
     * 检查状态
     */
    private void performOnCheck(int type) {
        if (mSwitchListener == null) {
            return;
        }
        if (type == TYPE_LEFT) {
            mSwitchListener.onCheck(this, true, false);
        } else if (type == TYPE_RIGHT) {
            mSwitchListener.onCheck(this, false, true);
        } else {
            // i don't know really will run here?
            mSwitchListener.onCheck(this, false, false);
        }
    }

    public TextView getLeftView() {
        return mLeftSwitch;
    }

    public TextView getRightView() {
        return mRightSwitch;
    }

    private int getFlowXWithLimit(double nowX, double flowX) {
        if (nowX < 0) {
            shake(TYPE_LEFT);
            return 0;
        } else if (nowX > (mTotalWidth - mFloatingLayerWidth)) {
            shake(TYPE_RIGHT);
            return (mTotalWidth - mFloatingLayerWidth);
        }
        return (int) flowX;
    }

    private void shake(int type) {
        int amplitude;
        if (type == TYPE_LEFT) {
            amplitude = ARGUMENT_SHAKE;
        } else if (type == TYPE_RIGHT) {
            amplitude = -ARGUMENT_SHAKE;
        } else {
            amplitude = 0;
        }
        mOffsetX = mOffsetX + amplitude;
        requestLayout();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        //获取触摸点的坐标
        final int x = (int) event.getX();
        if (event.getAction() == MotionEvent.ACTION_UP) {
            //当手松开的时候重置视图类型
            int type = x > mTotalWidth / 2 ? TYPE_RIGHT : TYPE_LEFT;
            sliding(type);
        }
        return mDetector.onTouchEvent(event);
    }
    /**
     * onScroll 方法：
     *      onScroll 方法处理滚动事件。
     *      getFlowXWithLimit(mOffsetX, mOffsetX - distanceX) 更新 mOffsetX 并限制其范围。
     *      requestLayout() 请求重新布局以反映新的位置。
     *      返回 true 表示事件已处理。
     * onDown 方法：
     *      onDown 方法处理触摸开始事件。
     *      获取触摸点的 X 和 Y 坐标。
     *      获取浮动层的点击区域。
     *      如果点击点不在浮动层内，并且允许点击，则触发切换操作，并返回 false 表示事件未处理。
     *      否则返回 true 表示事件已处理。
     * onSingleTapUp 方法：
     *      onSingleTapUp 方法处理单击事件。
     *      如果允许点击，则触发切换操作。
     *      返回父类的处理结果。
     **/
    class FlowGesture extends SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            // 处理普通的滚动事件
            mOffsetX = getFlowXWithLimit(mOffsetX, mOffsetX - distanceX);
            requestLayout();
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            final int x = (int) e.getX();
            final int y = (int) e.getY();

            Rect hitRect = new Rect();
            mFloatingLayer.getHitRect(hitRect);
            if (!hitRect.contains(x, y)) {
                if (isOpenClick) {
                    switching();
                }
                return false;
            }
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (isOpenClick) {
                switching();
            }
            return super.onSingleTapUp(e);
        }

    }

    /**
     * 定义一个私有的静态内部类用来操作动画
     */
    private static class FakeAnimation {

        private int times;
        private int nowTimes;
        private int ceil;

        FakeAnimation(int distance, long time) {
            times = (int) (time / FRAMES);
            ceil = distance / times;
            nowTimes = 0;
        }

        public boolean isEnd() {
            return nowTimes == times;
        }

        public int getValue() {
            nowTimes++;
            return ceil;
        }
    }
    /**
     * 接口定义，用于在视图被选中时调用回调。
     */
    public static interface OnSwitchListener {
        /**
         * 当视图被选中时调用。
         * @param sv            被选中的视图。
         * @param checkLeft     如果左开关被选中，则为 true，否则为 false。
         * @param checkRight    如果右开关被选中，则为 true，否则为 false。
         */
        void onCheck(SwitchView sv, boolean checkLeft, boolean checkRight);
    }
}
