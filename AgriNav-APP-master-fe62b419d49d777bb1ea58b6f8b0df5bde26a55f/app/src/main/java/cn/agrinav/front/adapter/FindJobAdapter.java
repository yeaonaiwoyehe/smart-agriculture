package cn.agrinav.front.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import cn.agrinav.R;
import cn.agrinav.front.entity.FindJobDetail;

public class FindJobAdapter extends BaseAdapter {
    private Context context;
    private List<FindJobDetail> data;
    private GridView gridView;
    private OnItemOperationListener listener;

    public interface OnItemOperationListener {
        void onViewDetail(FindJobDetail jobDetail);
        void onViewUpdate(FindJobDetail jobDetail);
        void onRemove(FindJobDetail jobDetail);
    }

    public FindJobAdapter(Context context, List<FindJobDetail> data) {
        this.context = context;
        this.data = data;
    }

    public void setGridView(GridView gridView) {
        this.gridView = gridView;
    }

    public void setOnItemOperationListener(OnItemOperationListener listener) {
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_find_job, parent, false);
            holder = new ViewHolder();
            holder.photo = convertView.findViewById(R.id.iv_photo);
            holder.machineType = convertView.findViewById(R.id.tv_machine_type);
            holder.workArea = convertView.findViewById(R.id.tv_work_area);
            holder.createTime = convertView.findViewById(R.id.tv_create_time);
            holder.operationLayer = convertView.findViewById(R.id.job_operation_layer);
            holder.tvViewDetail = convertView.findViewById(R.id.tv_view_detail);
            holder.tvViewUpdate = convertView.findViewById(R.id.tv_view_update);
            holder.tvRemove = convertView.findViewById(R.id.tv_remove);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        FindJobDetail findJobDetail = data.get(position);
        String[] image = findJobDetail.getImage();
        if (image != null && image.length > 0) {
            Glide.with(context)
                    .load(image[0])
                    .into(holder.photo);
        } else {
            holder.photo.setImageResource(R.drawable.login_avatar);
        }

        holder.machineType.setText(findJobDetail.getMachineType());
        String[] workArea = findJobDetail.getWorkArea();
        StringBuilder workAreaString = new StringBuilder();
        for(int i = 0; i < workArea.length; i++) {
            if(i > 0) {
                workAreaString.append(" ");
            }
            workAreaString.append(workArea[i]);
        }
        holder.workArea.setMaxLines(1);
        holder.workArea.setEllipsize(android.text.TextUtils.TruncateAt.END);
        holder.workArea.setText(workAreaString.toString());
        Date taskCreateTime = findJobDetail.getTaskCreateTime();
        String format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(taskCreateTime);
        holder.createTime.setText(format);

        // 检查是否下架
        if (findJobDetail.getTaskStatus() == 1) {
            convertView.setAlpha(0.5f);
        } else {
            convertView.setAlpha(1.0f);
        }

        convertView.setOnClickListener(v -> {
            // 检查是否有其他悬浮窗显示
            boolean hasVisibleLayer = false;
            if (gridView != null) {
                for (int i = 0; i < gridView.getChildCount(); i++) {
                    View child = gridView.getChildAt(i);
                    View operationLayer = child.findViewById(R.id.job_operation_layer);
                    if (operationLayer != null && operationLayer.getVisibility() == View.VISIBLE) {
                        hasVisibleLayer = true;
                        operationLayer.setVisibility(View.GONE);
                        break;
                    }
                }
            }
            
            // 如果没有其他悬浮窗显示，则显示当前悬浮窗
            // 如果有其他悬浮窗显示，则本次点击只隐藏其他悬浮窗
            if (!hasVisibleLayer) {
                holder.operationLayer.setVisibility(View.VISIBLE);
                // 状态判断
                if(findJobDetail.getTaskStatus() == 1) {
                    holder.tvViewUpdate.setVisibility(View.GONE);
                    holder.tvRemove.setVisibility(View.GONE);
                } else {
                    holder.tvViewUpdate.setVisibility(View.VISIBLE);
                    holder.tvRemove.setVisibility(View.VISIBLE);
                }
            }
        });

        holder.operationLayer.setOnClickListener(v -> {
            holder.operationLayer.setVisibility(View.GONE);
        });

        // 点击查看详情
        holder.tvViewDetail.setOnClickListener(v -> {
            v.setClickable(true);
            if (listener != null) {
                listener.onViewDetail(findJobDetail);
            }
            holder.operationLayer.setVisibility(View.GONE);
        });

        View view = convertView;
        // 修改信息
        holder.tvViewUpdate.setOnClickListener(v -> {
            v.setClickable(true);
            if(listener != null) {
                listener.onViewUpdate(findJobDetail);
            }
            holder.operationLayer.setVisibility(View.GONE);
            // 处理修改逻辑
            holder.operationLayer.setVisibility(View.GONE);
        });
        // 点击下架
        holder.tvRemove.setOnClickListener(v -> {
            v.setClickable(true);
            if (listener != null) {
                listener.onRemove(findJobDetail);
            }
            holder.operationLayer.setVisibility(View.GONE);
            view.setAlpha(0.5f);
            view.setClickable(true);
        });

        return convertView;
    }

    public void addData(List<FindJobDetail> newData) {
        this.data.addAll(newData);
        notifyDataSetChanged();
    }

    public void updateData(List<FindJobDetail> jobList)
    {
        this.data.clear();
        this.data.addAll(jobList);
        notifyDataSetChanged();
    }

    /**
     * 清除所有数据
     */
    public void clear() {
        data.clear();
        notifyDataSetChanged();
    }

    static class ViewHolder {
        ImageView photo;
        TextView machineType;
        TextView workArea;
        TextView createTime;
        View operationLayer;
        TextView tvViewDetail;
        TextView tvViewUpdate;
        TextView tvRemove;
    }
}
