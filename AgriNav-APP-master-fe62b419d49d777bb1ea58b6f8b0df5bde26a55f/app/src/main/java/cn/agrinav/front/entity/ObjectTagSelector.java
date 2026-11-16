package cn.agrinav.front.entity;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.flexbox.AlignContent;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.flexbox.JustifyContent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cn.agrinav.R;
import cn.agrinav.measure.entity.LandPlot;

public class ObjectTagSelector<T> {
    private static final String TAG = "ObjectTagSelector";
    //上下文
    private final Context context;
    //存放标签的容器
    private final LinearLayout container;
    //显示选中项的输入框
    private final EditText editText;
    //展开或收起的指示器
    private final TextView indicator;
    //所有标签项
    private final List<TagItem<T>> tagItems;
    //已选中的对象集合
    private final Set<T> selectedObjects;
    //是否展开
    private boolean isExpanded = false;
    //是否为单选模式
    private boolean isSingleSelection = false;
    private DisplayTextProvider<T> displayTextProvider;
    private OnSelectionChangedListener<T> selectionChangedListener;

    /**
     * 初始化选择器
     * @param context
     * @param container
     * @param editText
     * @param indicator
     */
    public ObjectTagSelector(Context context, LinearLayout container, EditText editText, TextView indicator) {
        this.context = context;
        this.container = container;
        this.editText = editText;
        this.indicator = indicator;
        this.tagItems = new ArrayList<>();
        this.selectedObjects = new HashSet<>();
    }


    /**
     * 标签项
     * @param <T>
     *     存储显示文本和原始对象
     *     维护选中状态
     *     重写方法支持比较
     */
    private static class TagItem<T> {
        //显示文本
        final String displayText;
        //对象
        final T object;
        //是否被选中
        boolean isSelected;

        TagItem(String displayText, T object) {
            this.displayText = displayText;
            this.object = object;
            this.isSelected = false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TagItem)) {
                return false;
            }
            TagItem<?> tagItem = (TagItem<?>) o;
            return object.equals(tagItem.object);
        }

        @Override
        public int hashCode() {
            return object.hashCode();
        }
    }

    /**
     * 选择变化监听器
     */
    public interface OnSelectionChangedListener<T> {
        void onSelectionChanged(List<T> selectedItems);
    }


    /**
     * 设置选择变化监听器
     * @param listener
     */
    public void setOnSelectionChangedListener(OnSelectionChangedListener<T> listener) {
        this.selectionChangedListener = listener;
    }


    // 显示文本提供者
    public interface DisplayTextProvider<T> {
        String getDisplayText(T object);
    }

    /**
     * 获取已选择的对象列表
     */

    public List<T> getSelectedObjects() {
        return new ArrayList<>(selectedObjects);
    }


    /**
     * 设置选项列表
     *
     * @param objects             对象列表
     * @param displayTextProvider 显示文本提供者
     */
//    public void setOptions(List<T> objects, DisplayTextProvider<T> displayTextProvider) {
//        this.displayTextProvider = displayTextProvider;
//        if (objects == null || displayTextProvider == null) {
//            return;
//        }
//
//        try {
//            // 保存当前选中项的ID
//            Set<String> selectedIds = selectedObjects.stream()
//                    .map(obj -> ((NewLandPlot)obj).getId())
//                    .collect(Collectors.toSet());
//
//            tagItems.clear();
//
//            for (T object : objects) {
//                if (object == null) {
//                    continue;
//                }
//
//                String displayText = displayTextProvider.getDisplayText(object);
//                if (displayText == null || displayText.trim().isEmpty()) {
//                    continue;
//                }
//
//                TagItem<T> item = new TagItem<>(displayText.trim(), object);
//                // 检查是否应该被选中
//                if (selectedIds.contains(((NewLandPlot)object).getId())) {
//                    item.isSelected = true;
//                }
//                tagItems.add(item);
//                Log.i(TAG, "setOptions: adding tag " + item.displayText);
//            }
//
//            setupTags();
//            updateEditText();
//
//            // 通知监听器
//            if (selectionChangedListener != null) {
//                selectionChangedListener.onSelectionChanged(getSelectedObjects());
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "setOptions: error setting options", e);
//        }
//    }

    /**
     * 设置标签布局
     */
    private void setupTags() {
        container.removeAllViews();
        FlexboxLayout flexboxLayout = createFlexboxLayout();

        for (TagItem<T> item : tagItems) {
            TextView tag = createTag(item);
            flexboxLayout.addView(tag);
        }

        container.addView(flexboxLayout);
    }

    /**
     *  创建FlexboxLayout
     */
    private FlexboxLayout createFlexboxLayout() {
        FlexboxLayout flexboxLayout = new FlexboxLayout(context);
        flexboxLayout.setFlexWrap(FlexWrap.WRAP);
        flexboxLayout.setAlignItems(AlignItems.STRETCH);
        flexboxLayout.setAlignContent(AlignContent.STRETCH);
        flexboxLayout.setJustifyContent(JustifyContent.FLEX_START);
        return flexboxLayout;
    }

    /**
     * 创建标签
     * @param item
     * @return
     */

    private TextView createTag(TagItem<T> item) {
        TextView tag = new TextView(context);
        setupTagLayout(tag);
        setupTagAppearance(tag, item);
        setupTagClickListener(tag, item);
        return tag;
    }

    /**
     * 设置标签的布局参数
     */
    private void setupTagLayout(TextView tag) {
        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 8, 8, 8);
        tag.setLayoutParams(params);
        tag.setPadding(20, 10, 20, 10);
    }

    /**
     * 设置标签的样式
     * @param tag
     * @param item
     */
    private void setupTagAppearance(TextView tag, TagItem<T> item) {
        tag.setText(item.displayText);
        tag.setTextSize(16);
        tag.setBackgroundResource(R.drawable.selector_tag_bg);
        tag.setTextColor(context.getResources().getColorStateList(R.color.selector_tag_color));
        tag.setSelected(item.isSelected);
        tag.setTag(item);
    }

    /**
     * 设置标签的点击监听器
     * @param tag
     * @param item
     */
    private void setupTagClickListener(TextView tag, TagItem<T> item) {
        tag.setOnClickListener(v -> handleTagClick(item));
    }

    /**
     * 处理标签点击事件
     * @param item
     */
    private void handleTagClick(TagItem<T> item) {
        item.isSelected = !item.isSelected;

        if (isSingleSelection) {
            handleSingleSelection(item);
        } else {
            handleMultiSelection(item);
        }

        updateTagViews();
        updateEditText();

        // 通知监听器
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(getSelectedObjects());
        }
    }

//    /**
//     * 设置选项并同时设置选中项
//     * @param objects 所有可选对象
//     * @param selectedObjects 预选中的对象
//     * @param displayTextProvider 显示文本提供者
//     */
//    public void setOptionsAndSelected(List<T> objects, List<T> selectedObjects, DisplayTextProvider<T> displayTextProvider) {
//        this.displayTextProvider = displayTextProvider;
//        if (objects == null) {
//            return;
//        }
//
//        tagItems.clear();
//        this.selectedObjects.clear();
//
//
//        // 收集所有选中对象的ID
//        List<String> selectedIds = new ArrayList<>();
//        for(T object : selectedObjects){
//            // 添加已有的选中项ID
//            if (selectedObjects != null && !selectedObjects.isEmpty()) {
//                selectedIds.add(((NewLandPlot)object).getId().toString());
//            }
//        }
//
//
//
//        // 创建所有标签项
//        for (T object : objects) {
//            if (object == null) {
//                continue;
//            }
//
//            String displayText = displayTextProvider.getDisplayText(object);
//            if (displayText == null || displayText.trim().isEmpty()) {
//                continue;
//            }
//
//            TagItem<T> item = new TagItem<>(displayText.trim(), object);
//
//            //如果对象ID在选中集合中，设置为选中状态
//            //循环selectedIds
//            for(String id : selectedIds){
//                if(id.toString().equals(((NewLandPlot)object).getId().toString())){
//                    item.isSelected = true;
//                    this.selectedObjects.add(object);
//                }
//            }
//            tagItems.add(item);
//        }
//
//        setupTags();
//        updateEditText();
//
//        if (selectionChangedListener != null) {
//            selectionChangedListener.onSelectionChanged(getSelectedObjects());
//        }
//    }
     /**
     * 设置所有可选项
     */
    public void setOptions(List<T> objects, DisplayTextProvider<T> displayTextProvider) {
        this.displayTextProvider = displayTextProvider;
        if (objects == null || displayTextProvider == null) {
            return;
        }

        try {
            tagItems.clear();
            for (T object : objects) {
                if (object == null) {
                    continue;
                }

                String displayText = displayTextProvider.getDisplayText(object);
                if (displayText == null || displayText.trim().isEmpty()) {
                    continue;
                }

                TagItem<T> item = new TagItem<>(displayText.trim(), object);
                tagItems.add(item);
            }
            setupTags();

        } catch (Exception e) {
            Log.e(TAG, "setOptions: error setting options", e);
        }
    }

    /**
     * 设置选中项
     */
    public void setSelected(List<T> selectedItems) {
        if (selectedItems == null) {
            return;
        }

        try {
            // 清除之前的选中状态
            for (TagItem<T> item : tagItems) {
                item.isSelected = false;
            }
            selectedObjects.clear();

            // 设置新的选中状态
            for (T selectedItem : selectedItems) {
                for (TagItem<T> item : tagItems) {
                    if (item.object.equals(selectedItem)) {
                        item.isSelected = true;
                        selectedObjects.add(selectedItem);
                        break;
                    }
                }
            }

            updateTagViews();
            updateEditText();

            // 通知监听器
            if (selectionChangedListener != null) {
                selectionChangedListener.onSelectionChanged(getSelectedObjects());
            }
        } catch (Exception e) {
            Log.e(TAG, "setSelected: error setting selected items", e);
        }
    }
    /**
     * 单选逻辑
     * @param item
     */
    private void handleSingleSelection(TagItem<T> item) {
        for (TagItem<T> otherItem : tagItems) {
            if (otherItem != item) {
                otherItem.isSelected = false;
            }
        }
        selectedObjects.clear();
        if (item.isSelected) {
            selectedObjects.add(item.object);
        }
    }

    /**
     * 多选逻辑
     * @param item
     */
    private void handleMultiSelection(TagItem<T> item) {
        if (item.isSelected) {
            selectedObjects.add(item.object);
        } else {
            selectedObjects.remove(item.object);
        }
    }

    /**
     * 更新输入框
     */
    private void updateEditText() {
        List<String> displayTexts = new ArrayList<>();
        // 遍历tagItems列表以保持原始顺序
        for (TagItem<T> item : tagItems) {
            if (item.isSelected) {
                displayTexts.add(item.displayText);
            }
        }
        editText.setText(String.join(" ", displayTexts));
    }

    /**
     * 更新所有标签的选中状态
     */
    private void updateTagViews() {
        View flexboxLayoutView = container.getChildAt(0);
        if (!(flexboxLayoutView instanceof FlexboxLayout)) {
            return;
        }
        FlexboxLayout flexboxLayout = (FlexboxLayout) flexboxLayoutView;
        for (int i = 0; i < flexboxLayout.getChildCount(); i++) {
            View child = flexboxLayout.getChildAt(i);
            if (child instanceof TextView && child.getTag() instanceof TagItem) {
                updateTagView((TextView) child);
            }
        }
    }

    /**
     * 更新标签的选中状态
     * @param textView
     */
    private void updateTagView(TextView textView) {
        TagItem<T> item = (TagItem<T>) textView.getTag();
        textView.setSelected(item.isSelected);
    }

    /**
     * 展开标签容器
     */
    public void expand() {
        container.setVisibility(View.VISIBLE);
        indicator.setRotation(90);
        isExpanded = true;
    }

    /**
     * 收起标签容器
     */
    public void collapse() {
        container.setVisibility(View.GONE);
        indicator.setRotation(0);
        isExpanded = false;
    }

    /**
     * 切换展开/收起状态
     */
    public void toggle() {
        if (isExpanded) {
            collapse();
        } else {
            expand();
        }
    }

    /**
     * 清空所有选择
     */
    public void clearSelection() {
        // 清空所有标签项的选中状态
        for (TagItem<T> item : tagItems) {
            item.isSelected = false;
        }

        // 清空选中对象集合
        selectedObjects.clear();

        // 更新标签视图
        updateTagViews();

        // 清空输入框
        editText.setText("");

        // 隐藏标签容器
        container.setVisibility(View.GONE);

        // 重置指示器方向
        indicator.setRotation(0);
        isExpanded = false;

        // 通知监听器
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(getSelectedObjects());
        }
    }


    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }
}