package cn.agrinav.front.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import cn.agrinav.R;
import cn.agrinav.front.activity.plot.ItemPlotDetail;
import cn.agrinav.front.database.entity.LandPlot;
import cn.agrinav.measure.activity.PlotInfoActivity;

/**
 * @author 11230
 */
public class PlotDetailListAdapter extends BaseAdapter {
    //上下文
    private Context context;
    //布局id
    private Integer layoutId;
    //数据
    private List<LandPlot> list;
    //构造方法
    public PlotDetailListAdapter(Context context, Integer layoutId, List<LandPlot> list) {
        this.context = context;
        this.layoutId = layoutId;
        this.list = list;
    }
    //获取数据源的长度
    @Override
    public int getCount() {
        return list.size();
    }
    //获取数据源的元素
    @Override
    public Object getItem(int position) {
        return list.get(position);
    }
    //获取数据源的id
    @Override
    public long getItemId(int position) {
        return position;
    }
    //填充数据
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PlotDetailListAdapter.ViewHolder holder;
        //判断第一次
        if (convertView==null){
            //填充布局
            convertView= LayoutInflater.from(context).inflate(layoutId,null);
            holder = new PlotDetailListAdapter.ViewHolder();
            holder.imageView=convertView.findViewById(R.id.iv_plot_detail_list);
            holder.textViewAddress=convertView.findViewById(R.id.tv_address_plot_detail_list);
            holder.textViewDate=convertView.findViewById(R.id.tv_date_plot_detail_list);
            holder.textViewArea=convertView.findViewById(R.id.tv_area_plot_detail_list);
            convertView.setTag(holder);
        }else{
            holder=(ViewHolder)convertView.getTag();
        }
        //获取图片
        LandPlot landPlot = (LandPlot) getItem(position);
        //加载图片
        Glide.with(context)
                .load(landPlot.getImage())
                .placeholder(R.drawable.loading_img)
                .fallback(R.drawable.img_load_err)
                .error(R.drawable.img_load_err)
                .into(holder.imageView);
        //填充面积信息
        holder.textViewArea.setText(String.valueOf(landPlot.getLandArea()));
        //填充地址信息
        holder.textViewAddress.setText(landPlot.getAddress());
        if(landPlot.getCreateTime()!=null){
            LocalDate localDate = landPlot.getCreateTime().toLocalDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            String time = localDate.format(formatter);
            holder.textViewDate.setText(time);
        }
        //跳转
        initEvents(convertView,position);
        return convertView;
    }
    static class ViewHolder{
        ShapeableImageView imageView;
        TextView textViewAddress;
        TextView textViewDate;
        TextView textViewArea;
    }
    private void initEvents(View convertView, int position) {
        //点击每一个item事件
        convertView.setOnClickListener(v ->  {
            Intent intent = new Intent(context, ItemPlotDetail.class);
            intent.putExtra(context.getString(R.string.font_plot_detail_list_intent_plot_id),list.get(position).getId());
            context.startActivity(intent);
        });
    }
    public void notifyDataSetChanged(ArrayList<LandPlot> list) {
        this.list = list;
        super.notifyDataSetChanged();
    }
}
