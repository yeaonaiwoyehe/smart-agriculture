package cn.agrinav.personal.activity.feedback;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

import cn.agrinav.R;

public class FeedbackAdapter extends RecyclerView.Adapter<FeedbackAdapter.ViewHolder> {
    private List<FeedbackItem> feedbackList;

    public FeedbackAdapter(List<FeedbackItem> feedbackList) {
        this.feedbackList = feedbackList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feedback, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FeedbackItem item = feedbackList.get(position);
        
        if (item.isChecked()) {
            holder.tvStatus.setText("已解决");
            holder.tvStatus.setTextColor(Color.parseColor("#63B0F5"));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_solved);
        } else {
            holder.tvStatus.setText("未解决");
            holder.tvStatus.setTextColor(Color.parseColor("#F18336"));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status);
        }
        
        holder.chipGroup.removeAllViews();
        List<String> tags = item.getTags();
        
        String content = item.getContent();
        if (content.length() > 20) {
            content = content.substring(0, 20) + "...";
        }
        
        if (tags != null && !tags.isEmpty()) {
            holder.chipGroup.setVisibility(View.VISIBLE);
            holder.chipGroup.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (48 * holder.itemView.getContext().getResources().getDisplayMetrics().density)
            ));
            holder.tvContent.setMaxLines(1);
            holder.tvContent.setEllipsize(TextUtils.TruncateAt.END);
            holder.tvContent.setText(content);
            holder.tvContent.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            addTagsToGroup(holder.chipGroup, tags);
        } else {
            holder.chipGroup.setVisibility(View.INVISIBLE);
            holder.chipGroup.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
            ));
            
            holder.tvContent.setMaxLines(3);
            holder.tvContent.setEllipsize(TextUtils.TruncateAt.END);
            holder.tvContent.setText(content);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            holder.tvContent.setLayoutParams(params);
        }
        
        holder.itemView.setOnClickListener(v -> showDetailDialog(v.getContext(), item));
    }

    private void addTagsToGroup(ChipGroup chipGroup, List<String> tags) {
        Context context = chipGroup.getContext();
        int maxTags = Math.min(2, tags.size());
        
        for (int i = 0; i < maxTags; i++) {
            Chip chip = createChip(context, tags.get(i));
            chipGroup.addView(chip);
        }
        
        if (tags.size() > 2) {
            Chip moreChip = createChip(context, "...");
            chipGroup.addView(moreChip);
        }
    }

    private Chip createChip(Context context, String text) {
        Chip chip = new Chip(context);
        chip.setText(text);
        chip.setClickable(false);
        chip.setTextSize(14);
        chip.setTextColor(Color.parseColor("#646566"));
        chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EEF5F7")));
        chip.setChipStrokeWidth(0);  // 移除边框
        chip.setEnsureMinTouchTargetSize(false);
        
        // 设置内边距
        int paddingHorizontal = (int) (8 * context.getResources().getDisplayMetrics().density);
        int paddingVertical = (int) (4 * context.getResources().getDisplayMetrics().density);
        chip.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
        
        return chip;
    }

    private void showDetailDialog(Context context, FeedbackItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.TransparentDialog);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_feedback_detail, null);
        
        TextView contentView = dialogView.findViewById(R.id.tv_feedback_detail_content);
        contentView.setText(item.getContent());
        TextView titleView = dialogView.findViewById(R.id.tv_feedback_detail_title);
        titleView.setTextSize(14);
        ChipGroup dialogChipGroup = dialogView.findViewById(R.id.dialog_chip_group);
        List<String> tags = item.getTags();
        if (tags != null && !tags.isEmpty()) {
            dialogChipGroup.setVisibility(View.VISIBLE);
            titleView.setVisibility(View.VISIBLE);
            for (String tag : tags) {
                Chip chip = createChip(context, tag);
                dialogChipGroup.addView(chip);
            }
        }
        
        AlertDialog dialog = builder.setView(dialogView).create();
        
        dialogView.setOnClickListener(v -> dialog.dismiss());
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 
                                       ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return feedbackList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ChipGroup chipGroup;
        TextView tvContent;
        TextView tvStatus;

        ViewHolder(View itemView) {
            super(itemView);
            chipGroup = itemView.findViewById(R.id.chip_group_tags);
            tvContent = itemView.findViewById(R.id.tv_feedback_content);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
} 