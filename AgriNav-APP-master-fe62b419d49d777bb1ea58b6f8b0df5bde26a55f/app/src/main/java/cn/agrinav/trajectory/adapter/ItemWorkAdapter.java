package cn.agrinav.trajectory.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import cn.agrinav.R;
import cn.agrinav.trajectory.activity.ItemActivity;
import cn.agrinav.trajectory.entity.ItemWork;

/**
 * @author 11230
 */
public class ItemWorkAdapter extends BaseAdapter {
    private ArrayList<ItemWork> list;
    private int layout;
    private Context context;

    public ItemWorkAdapter(ArrayList<ItemWork> list, int layout, Context context) {
        this.list = list;
        this.layout = layout;
        this.context = context;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null){
            //第一步获取视图
            convertView = LayoutInflater.from(context).inflate(layout,null);
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.iv_trajectory_item_work_show);
            holder.tvAddress = convertView.findViewById(R.id.tv_trajectory_font_item_work_address);
            holder.tvTimeBegin = convertView.findViewById(R.id.tv_trajectory_font_item_work_time_begin);
            holder.tvTimeEnd = convertView.findViewById(R.id.tv_trajectory_font_item_work_time_end);
            holder.tvWidth = convertView.findViewById(R.id.tv_trajectory_font_item_work_width);
            holder.tvWorkType = convertView.findViewById(R.id.tv_trajectory_font_item_work_type);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }
        convertView.setOnClickListener(v->{
            Intent intent = new Intent(context, ItemActivity.class);
            intent.putExtra(context.getString(R.string.item_extra_key),list.get(position).getTrajectoryId());
            intent.putExtra(context.getString(R.string.item_extra_begin_time),list.get(position).getTimeBegin());
            intent.putExtra(context.getString(R.string.item_extra_end_time),list.get(position).getTimeEnd());
            intent.putExtra(context.getString(R.string.item_extra_phone),list.get(position).getPhone());
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
        Glide.with(context)
                .load(list.get(position).getImage())
                .placeholder(R.drawable.loading_img)
                .fallback(R.drawable.img_load_err)
                .error(R.drawable.img_load_err)
                .into(holder.imageView);
        holder.tvAddress.setText(list.get(position).getAddress());
        holder.tvTimeBegin.setText(LocalDateTime.parse(list.get(position).getTimeBegin()).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
        holder.tvTimeEnd.setText(LocalDateTime.parse(list.get(position).getTimeEnd()).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
        holder.tvWidth.setText(String.valueOf(list.get(position).getArea()).concat(context.getString(R.string.trajectory_area_unit)));
        holder.tvWorkType.setText(list.get(position).getWorkType());
        return convertView;
    }
    public void notifyDataSetChanged(ArrayList<ItemWork> list) {
        this.list = list;
        super.notifyDataSetChanged();
    }
    static class ViewHolder{
        ImageView imageView;
        TextView tvAddress;
        TextView tvTimeBegin;
        TextView tvTimeEnd;
        TextView tvWidth;
        TextView tvWorkType;
    }
}
