package cn.agrinav.front.activity.sysmessage;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.agrinav.R;
import cn.agrinav.front.activity.sysmessage.utils.SysMessageUtils;
import cn.agrinav.front.entity.SysMessage;

public class SysMessageAdapter extends RecyclerView.Adapter<SysMessageAdapter.ViewHolder> {
    private List<SysMessage> messageList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(SysMessage message, int position);
    }

    public SysMessageAdapter(List<SysMessage> messageList) {
        this.messageList = messageList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SysMessage message = messageList.get(position);
        holder.tvMessageTime.setText(message.getCreateTime());
        holder.tvMessageTitle.setText(message.getTitle());
        holder.tvMessageContent.setText(message.getContent().getContent());
        // 根据 isRead 属性控制未读标记的显示状态
        // 使用 readTime 来判断消息是否已读，而不是 isRead
        boolean isRead = message.getReadTime() != null;
        holder.viewUnreadIndicator.setVisibility(
                isRead ? View.GONE : View.VISIBLE
        );
        // 点击整个item进入详情页
        holder.itemView.setOnClickListener(v -> {
            // 跳转到详情页
            Intent intent = new Intent(v.getContext(), SysMsgDetailActivity.class);
            // 传递消息对象
            intent.putExtra("message", message);
            v.getContext().startActivity(intent);

            //如果消息未读，标记为已读
            if (!isRead) {
                SysMessageUtils.markMessageAsRead(v.getContext(), message.getId(), null);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageTime, tvMessageTitle, tvMessageContent;
        View viewUnreadIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageTitle = itemView.findViewById(R.id.tv_message_title);
            tvMessageTime = itemView.findViewById(R.id.tv_message_time);
            tvMessageContent = itemView.findViewById(R.id.tv_message_content);
            viewUnreadIndicator = itemView.findViewById(R.id.view_unread_indicator);
        }
    }
}
