package cn.agrinav.front.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import cn.agrinav.R;
import cn.agrinav.front.activity.front.DemandFarmWorkActivity;
import cn.agrinav.front.activity.front.FreeAgriculturalMachineryActivity;
import cn.agrinav.front.entity.FarmWork;
import cn.agrinav.front.util.TransformUtils;
import cn.agrinav.measure.utils.ImageLoaderUtils;

/**
 * @author 18506
 */
public class GridFarmWorkAdapter extends BaseAdapter {
    //上下文
    private Context context;
    //布局id
    private Integer layoutId;
    //数据
    private List<FarmWork> list;
    private ViewHolder holder;

    public GridFarmWorkAdapter(Context context, Integer layoutId, List<FarmWork> list) {
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
        if (convertView == null){
            convertView = LayoutInflater.from(context).inflate(layoutId,null);
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.farm_work_picture);
            holder.textFarmWorkPlotAreaEdit = convertView.findViewById(R.id.tv_farm_work_plot_area_edit);
            holder.textOperationArea = convertView.findViewById(R.id.tv_operation_area);
            holder.textFarmWorkDeadlineEdit = convertView.findViewById(R.id.tv_farm_work_deadline_edit);
            holder.textOtherDateTime = convertView.findViewById(R.id.tv_other_date_time);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }
        FarmWork farmWork = list.get(position);
        if (!farmWork.getLandPlotImage().isEmpty()){
            ImageLoaderUtils.loadNowImage(context, farmWork.getLandPlotImage().get(0), holder.imageView);
        }
        holder.textFarmWorkPlotAreaEdit.setText(String.valueOf(farmWork.getArea()));
        //可做区域
        String location = TransformUtils.transformRegion(farmWork.getLandAddress());
        if (location.length()>6){
            holder.textOperationArea.setText(location.substring(0,5)+"...");
        }else {
            holder.textOperationArea.setText(location);
        }
        if (farmWork.getEndTime()!=null){
            LocalDate localDate = farmWork.getEndTime().toLocalDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            String deadlineTime = localDate.format(formatter);
            holder.textFarmWorkDeadlineEdit.setText(deadlineTime);
        }
        if (farmWork.getCreateTime()!=null){
            LocalDateTime localDate = farmWork.getCreateTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:ss:mm");
            String time = localDate.format(formatter);
            holder.textOtherDateTime.setText(time);
        }
        initEvents(convertView,position);
        return convertView;
    }

    private void initEvents(View convertView, int position) {
        convertView.setOnClickListener(v ->  {
            Intent intent = new Intent(context, DemandFarmWorkActivity.class);
            intent.putExtra("FarmWork", list.get(position));
            context.startActivity(intent);
        });
    }

    static class ViewHolder{
        ImageView imageView;
        TextView textFarmWorkPlotAreaEdit;
        TextView textOperationArea;
        TextView textFarmWorkDeadlineEdit;
        TextView textOtherDateTime;
    }
    //更新
    public void update(List<FarmWork> farmWorkList){
        if (farmWorkList==null){
            farmWorkList = new ArrayList<>();
        }else{
            this.list.clear();
        }
        this.list.addAll(farmWorkList);
        notifyDataSetChanged();
    }
    //加载更多
    public void loadMore(List<FarmWork> farmWorkList){
        this.list.addAll(farmWorkList);
        notifyDataSetChanged();
    }
}
