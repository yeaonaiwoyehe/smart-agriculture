package cn.agrinav.measure.adapter;

import android.content.Context;
import android.content.Intent;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;






import cn.agrinav.R;
import cn.agrinav.measure.activity.PlotInfoActivity;

import cn.agrinav.measure.entity.LandPlot;

import cn.agrinav.measure.utils.ImageLoaderUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 18506
 */
public class MeasureFragmentAdapter extends BaseAdapter {
    //上下文
    private Context context;
    //布局id
    private Integer layoutId;
    //数据
    private List<LandPlot> list;
    //构造方法
    public MeasureFragmentAdapter(Context context, Integer layoutId, List<LandPlot> list) {
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
        ViewHolder holder;
        //判断第一次
        if (convertView==null){
            //填充布局
            convertView= LayoutInflater.from(context).inflate(layoutId,null);
            holder=new ViewHolder();
            holder.imageView=convertView.findViewById(R.id.imageViewMeasure);
            holder.textViewAddress=convertView.findViewById(R.id.textViewAddress);
            holder.textViewDate=convertView.findViewById(R.id.textViewDate);
            holder.textViewArea=convertView.findViewById(R.id.textViewArea);
            convertView.setTag(holder);
        }else{
            holder=(ViewHolder)convertView.getTag();
        }
        //获取图片
        LandPlot landPlot = (LandPlot) getItem(position);
        // 使用修改后的ImageLoaderUtils加载图片
        ImageLoaderUtils.loadNowImage(context, landPlot.getImage(), holder.imageView);
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
        ImageView imageView;
        TextView textViewAddress;
        TextView textViewDate;
        TextView textViewArea;
    }
    private void initEvents(View convertView, int position) {
        //点击每一个item事件
        convertView.setOnClickListener(v ->  {
            Intent intent = new Intent(context, PlotInfoActivity.class);
            intent.putExtra("plotInfo",list.get(position));
            context.startActivity(intent);
        });
    }
    public void update(List<LandPlot> landPlotList){
        if (landPlotList==null){
            landPlotList = new ArrayList<>();
        }else {
            this.list.clear();
        }
        this.list.addAll(landPlotList);
        notifyDataSetChanged();
    }
    public void loadMore(List<LandPlot> landPlotList){
        this.list.addAll(landPlotList);
        notifyDataSetChanged();
    }
}
