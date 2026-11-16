package cn.agrinav.front.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import com.google.android.material.imageview.ShapeableImageView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import cn.agrinav.R;
import cn.agrinav.front.activity.front.FreeAgriculturalMachineryActivity;
import cn.agrinav.front.entity.AgriculturalMachinery;
import cn.agrinav.front.util.TransformUtils;
import cn.agrinav.measure.utils.ImageLoaderUtils;

/**
 * @author 18506
 */
public class GridMachineryAdapter extends BaseAdapter {
    //上下文
    private Context context;
    //布局id
    private Integer layoutId;
    //数据
    private List<AgriculturalMachinery> list;
    public GridMachineryAdapter(Context context, Integer layoutId, List<AgriculturalMachinery> list) {
        this.context = context;
        this.layoutId = layoutId;
        this.list = list;
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null){
            convertView = LayoutInflater.from(context).inflate(layoutId,null);
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.machinery_picture);
            holder.textViewMachineryModelNumber = convertView.findViewById(R.id.tv_machinery_model_number_edit);
            holder.textViewRegion = convertView.findViewById(R.id.tv_machinery_region);
            holder.textViewDate = convertView.findViewById(R.id.tv_date_time);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }
        AgriculturalMachinery agriculturalMachinery = list.get(position);
        //默认为第一个
        if (!agriculturalMachinery.getImage().isEmpty()) {
            ImageLoaderUtils.loadNowImage(context, agriculturalMachinery.getImage().get(0), holder.imageView);
        }
        holder.textViewMachineryModelNumber.setText(agriculturalMachinery.getTypeName());
        String location = TransformUtils.transformRegion(agriculturalMachinery.getLocation());
        if (location.length()>4){
            holder.textViewRegion.setText(location.substring(0,3)+"...");
        }else {
            holder.textViewRegion.setText(location);
        }
        if(agriculturalMachinery.getCreateTime()!=null){
            LocalDateTime localDate = agriculturalMachinery.getCreateTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:ss:mm");
            String time = localDate.format(formatter);
            holder.textViewDate.setText(time);
        }
        initEvents(convertView,position);
        return convertView;
    }

    private void initEvents(View convertView, int position) {
        convertView.setOnClickListener(v ->  {
            Intent intent = new Intent(context, FreeAgriculturalMachineryActivity.class);
            intent.putExtra("AgriculturalMachinery", list.get(position));
            context.startActivity(intent);
        });
    }

    static class ViewHolder{
        ShapeableImageView imageView;
        TextView textViewMachineryModelNumber;
        TextView textViewDate;
        TextView textViewRegion;
    }
    //更新
    public void update(List<AgriculturalMachinery> agriculturalMachineryList){
        if (agriculturalMachineryList==null){
            agriculturalMachineryList = new ArrayList<>();
        }else {
            this.list.clear();
        }
        this.list.addAll(agriculturalMachineryList);
        notifyDataSetChanged();
    }
    //加载更多
    public void loadMore(List<AgriculturalMachinery> agriculturalMachineryList){
        this.list.addAll(agriculturalMachineryList);
        notifyDataSetChanged();
    }
}
