package cn.agrinav.personal.activity.feedback;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class FeedbackItem {
    @Setter
    @Getter
    private String content;
    @Setter
    @Getter
    private List<String> tags;
    private boolean isChecked;

    public FeedbackItem(String content, List<String> tags, boolean isChecked) {
        this.content = content;
        this.tags = tags;
        this.isChecked = isChecked;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}