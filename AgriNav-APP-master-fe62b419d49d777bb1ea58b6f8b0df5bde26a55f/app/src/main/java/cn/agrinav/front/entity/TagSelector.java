package cn.agrinav.front.entity;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.flexbox.AlignContent;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.flexbox.JustifyContent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.agrinav.R;

public class TagSelector {
    // 上下文对象，用于访问资源和进行Toast提示
    private Context context;
    // 包含FlexboxLayout的容器
    private LinearLayout container;
    // 显示选中标签的EditText
    private EditText editText;
    // 展开/收起标签选择器的指示器
    private TextView indicator;
    // 可供选择的标签数组
    private String[] options;
    // 保存选中的标签列表
    private List<String> selectedTags;
    // 标签选择器是否处于展开状态
    private boolean isExpanded = false;
    // 是否是单选模式
    private boolean isSingleSelection = false;

    // 添加标签选择监听器接口
    public interface OnTagSelectedListener {
        void onTagSelected(List<String> selectedTags);
    }

    // 添加监听器字段
    private OnTagSelectedListener onTagSelectedListener;

    /**
     * 构造方法，初始化标签选择器
     *
     * @param context   上下文对象
     * @param container 标签容器
     * @param editText  显示选中标签的EditText
     * @param indicator 展开/收起指示器
     * @param options   可供选择的标签数组
     */
    public TagSelector(Context context, LinearLayout container, EditText editText, TextView indicator, String[] options) {
        this.context = context;
        this.container = container;
        this.editText = editText;
        this.indicator = indicator;
        this.options = options;
        this.selectedTags = new ArrayList<>();
        setupTags();
    }

    /**
     * 设置新的标签选项并重新配置标签选择器
     *
     * @param newOptions 新的标签选项数组
     */
    public void setOptions(String[] newOptions) {
        this.options = newOptions;

        setupTags();
        if (isExpanded) {
            expand();
        }
    }

    /**
     * 设置选择模式为单选或多选
     *
     * @param singleSelection true为单选，false为多选
     */
    public void setSingleSelection(boolean singleSelection) {
        this.isSingleSelection = singleSelection;
    }

    /**
     * 初始化和更新标签视图
     */
    private void setupTags() {
        container.removeAllViews(); // 清除现有的标签
        FlexboxLayout flexboxLayout = new FlexboxLayout(context);
        flexboxLayout.setFlexWrap(FlexWrap.WRAP);
        flexboxLayout.setAlignItems(AlignItems.STRETCH);
        flexboxLayout.setAlignContent(AlignContent.STRETCH);
        flexboxLayout.setJustifyContent(JustifyContent.FLEX_START);

        for (String option : options) {
            TextView tag = createTag(option);
            tag.setSelected(selectedTags.contains(option));
            flexboxLayout.addView(tag);
        }
        container.addView(flexboxLayout);
    }

    /**
     * 创建并返回一个标签视图
     *
     * @param text 标签上显示的文本
     * @return 创建的TextView对象
     */
    private TextView createTag(String text) {
        TextView tag = new TextView(context);
        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 8, 8, 8);
        tag.setLayoutParams(params);
        tag.setPadding(20, 10, 20, 10);
        tag.setText(text);
        tag.setTextSize(16);
        tag.setBackgroundResource(R.drawable.selector_tag_bg);
        tag.setTextColor(context.getResources().getColorStateList(R.color.selector_tag_color));

        tag.setOnClickListener(v -> {
            selectTag(text);
            updateTagViews(); // 确保在点击后更新所有标签的视觉状态
        });
        return tag;
    }

    /**
     * 更新EditText中显示的选中标签文本
     */
    private void updateEditText() {
        // 修改为按照原始顺序显示选中标签
        List<String> orderedSelectedTags = new ArrayList<>();
        // 遍历原始选项数组，保持顺序
        for (String option : options) {
            if (selectedTags.contains(option)) {
                orderedSelectedTags.add(option);
            }
        }
        editText.setText(String.join(" ", orderedSelectedTags));
    }

    /**
     * 展开标签选择器
     */
    public void expand() {
        container.setVisibility(View.VISIBLE);
        indicator.setRotation(90);
        isExpanded = true;
    }

    /**
     * 收起标签选择器
     */
    public void collapse() {
        container.setVisibility(View.GONE);
        indicator.setRotation(0);
        isExpanded = false;
    }

    /**
     * 切换标签选择器的展开/收起状态
     */
    public void toggle() {
        if (isExpanded) {
            collapse();
        } else {
            expand();
        }
    }

    /**
     * 获取选中的标签列表
     *
     * @return 选中的标签列表
     */
    public List<String> getSelectedTags() {
        return selectedTags;
    }

    /**
     * 设置标签选择监听器
     *
     * @param listener 标签选择监听器
     */
    public void setOnTagSelectedListener(OnTagSelectedListener listener) {
        this.onTagSelectedListener = listener;
    }

    /**
     * 选中或取消选中一个标签
     *
     * @param tag 要操作的标签文本
     */
    public void selectTag(String tag) {
        if (isSingleSelection) {
            selectedTags.clear();
            selectedTags.add(tag);
        } else {
            if (selectedTags.contains(tag)) {
                selectedTags.remove(tag);
            } else {
                selectedTags.add(tag);
            }
        }
        updateEditText();
        updateTagViews();
        
        // 触发选择监听器回调
        if (onTagSelectedListener != null) {
            onTagSelectedListener.onTagSelected(selectedTags);
        }
    }

    /**
     * 更新所有标签的视觉状态，以反映它们是否被选中
     */
    private void updateTagViews() {
        FlexboxLayout flexboxLayout = (FlexboxLayout) container.getChildAt(0);
        for (int i = 0; i < flexboxLayout.getChildCount(); i++) {
            View child = flexboxLayout.getChildAt(i);
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                textView.setSelected(selectedTags.contains(textView.getText().toString()));
            }
        }
    }

    /**
     * 设置选中的标签
     *
     * @param tags 要设置的标签列表
     */
    public void setSelectedTags(List<String> tags) {
        selectedTags.clear();
        selectedTags.addAll(tags);
        updateEditText();
        updateTagViews();
        
        // 触发选择监听器回调
        if (onTagSelectedListener != null) {
            onTagSelectedListener.onTagSelected(selectedTags);
        }
    }

    /**
     * 设置选中的标签
     *
     * @param tags 要设置的标签数组
     */
    public void setSelectedTags(String[] tags) {
        selectedTags.clear();
        Collections.addAll(selectedTags, tags);
        updateEditText();
        updateTagViews();
        
        // 触发选择监听器回调
        if (onTagSelectedListener != null) {
            onTagSelectedListener.onTagSelected(selectedTags);
        }
    }
    /**
     * 清除所有选中状态
     */
    public void clearSelection() {
        selectedTags.clear();
        editText.setText("");

        // 获取 FlexboxLayout
        if (container.getChildCount() > 0 && container.getChildAt(0) instanceof FlexboxLayout) {
            FlexboxLayout flexboxLayout = (FlexboxLayout) container.getChildAt(0);

            // 清除所有标签的选中状态
            for (int i = 0; i < flexboxLayout.getChildCount(); i++) {
                View child = flexboxLayout.getChildAt(i);
                if (child instanceof TextView) {
                    TextView tagView = (TextView) child;
                    tagView.setSelected(false);
                }
            }
        }

        // 隐藏标签容器
        container.setVisibility(View.GONE);
        // 重置指示器方向
        indicator.setRotation(0);
        isExpanded = false;

        // 触发选择监听器回调
        if (onTagSelectedListener != null) {
            onTagSelectedListener.onTagSelected(selectedTags);
        }
    }
}
