package cn.agrinav.front.adapter;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import cn.agrinav.R;
import cn.agrinav.front.activity.findfarm.FarmDetailActivity;
import cn.agrinav.front.activity.findfarm.FarmItemDetailActivity;
import cn.agrinav.front.entity.FindFarmDetail;
import cn.agrinav.front.entity.FindJobDetail;
import cn.agrinav.measure.entity.LandPlot;
import cn.agrinav.utils.DialogUtils;
import cn.agrinav.utils.OkHttpUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FindFarmAdapter extends BaseAdapter {
    private Context context;
    private List<FindFarmDetail> farmDetails;
    private FindFarmAdapter.OnItemOperationListener listener;
    private GridView gridView;

    public interface OnItemOperationListener {
        void onViewDetail(FindFarmDetail jobDetail);
        void onViewUpdate(FindFarmDetail jobDetail);
        void onRemove(FindFarmDetail jobDetail);
    }
    public void setOnItemOperationListener(FindFarmAdapter.OnItemOperationListener listener) {
        this.listener = listener;
    }
    public void setGridView(GridView gridView) {
        this.gridView = gridView;
    }

    public FindFarmAdapter(Context context) {
        this.context = context;
        this.farmDetails = new ArrayList<>();
    }
    // 更新所有数据（用于刷新）
    public void update(List<FindFarmDetail> newData) {
        if (newData == null) {
            newData = new ArrayList<>();
        }
        this.farmDetails.clear();
        this.farmDetails.addAll(newData);
        notifyDataSetChanged();
    }

    // 添加更多数据（用于加载更多）
    public void loadMore(List<FindFarmDetail> newData) {
        if (newData != null && !newData.isEmpty()) {
            this.farmDetails.addAll(newData);
            notifyDataSetChanged();
        }
    }

    // 清除所有数据
    public void clearData() {
        farmDetails.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return farmDetails.size();
    }

    @Override
    public Object getItem(int position) {
        return farmDetails.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_farm_detail, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        FindFarmDetail detail = farmDetails.get(position);

        holder.tvTaskName.setText(detail.getTaskName());

        // 设置地块面积
        holder.tvFindFarmSize.setText(detail.getLandPlotArea());

        // 设置作物
        String crops = String.join(" ", detail.getCrops());
        holder.tvCrop.setText(crops);

        // 设置作业类型
        String operations = String.join(" ", detail.getOperationTypes());
        holder.tvOperationType.setText(operations);

        // 设置有效期
        holder.tvOperationDate.setText(detail.getOperationDate());
        //设置图片
        if (detail.getImageList() != null && !detail.getImageList().isEmpty()) {
            String imageUrl = detail.getImageList().get(0);
            Log.i("imageUrl", imageUrl);
            // 使用Glide加载图片
            Glide.with(context)
                    .load(imageUrl)
                    .into(holder.ivFindFarmImage);
        } else {
            // 如果没有图片，则显示默认图片
            holder.ivFindFarmImage.setImageResource(R.drawable.plot_use);
        }
        // 设置发布时间
        String publishTime = detail.getPublishDate();
        holder.tvPublishTime.setText(publishTime);

        // 添加整个条目的点击事件
        convertView.setOnClickListener(v -> {
            // 检查是否有其他悬浮窗显示
            boolean hasVisibleLayer = false;
            if (gridView != null) {
                for (int i = 0; i < gridView.getChildCount(); i++) {
                    View child = gridView.getChildAt(i);
                    View operationLayer = child.findViewById(R.id.farm_choose_open);
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
                holder.farmChooseOpen.setVisibility(View.VISIBLE);
                // 根据发布状态判断按钮显示
                if(detail.getPublishStatus() == 1) {
                    holder.tvUpdateFarmDetail.setVisibility(View.GONE);
                    holder.tvRemove.setVisibility(View.GONE);
                } else {
                    holder.tvUpdateFarmDetail.setVisibility(View.VISIBLE);
                    holder.tvRemove.setVisibility(View.VISIBLE);
                }
            }
        });

        // 点击悬浮窗本身时关闭
        holder.farmChooseOpen.setOnClickListener(v -> {
            holder.farmChooseOpen.setVisibility(View.GONE);
        });

        // 设置详情按钮点击事件
        holder.tvViewFarmDetail.setOnClickListener(v -> {
            v.setClickable(true);
            if (listener != null) {
                listener.onViewDetail(detail);
            }
            holder.farmChooseOpen.setVisibility(View.GONE);
        });

        //设置修改按钮点击事件
        holder.tvUpdateFarmDetail.setOnClickListener(v -> {
            v.setClickable(true);
            if (listener != null) {
                listener.onViewUpdate(detail);
            }
            holder.farmChooseOpen.setVisibility(View.GONE);
        });

        //设置下架按钮点击事件
        holder.tvRemove.setOnClickListener(v -> {
            v.setClickable(true);
            if (listener != null) {
                listener.onRemove(detail);
            }
            holder.farmChooseOpen.setVisibility(View.GONE);

        });

        if(detail.getPublishStatus() == 1) {
            convertView.setAlpha(0.5f);
        } else {
            convertView.setAlpha(1.0f);
        }

        // 设置下架按钮点击事件
        View finalConvertView = convertView;
        holder.tvRemove.setOnClickListener(v -> {
            DialogUtils dialogUtils = new DialogUtils(context);
            AlertDialog dialog = dialogUtils.getDialog();
            dialogUtils.setTitle("确认下架")
                    .setContent("您确定要下架该任务吗？")
                    .setPositiveText("确定")
                    .setNegativeText("取消")
                    .setPositiveListener(view -> {
                        if(listener != null){
                            listener.onRemove(detail);
                        }
                        holder.farmChooseOpen.setVisibility(View.GONE);
                        finalConvertView.setAlpha(0.5f);
                        dialog.cancel();
                    })
                    .setNegativeListener(view -> {
                        holder.farmChooseOpen.setVisibility(View.GONE);
                        dialog.cancel();
                    });
            View view = dialogUtils.getView();
            dialog.setContentView(view);
            dialog.show();

        });

        return convertView;
    }

    // 替换所有数据
    public void setData(List<FindFarmDetail> newData) {
        if (newData != null) {
            this.farmDetails.clear();
            this.farmDetails.addAll(newData);
            notifyDataSetChanged();
        }
    }


    // 检查是否已存在相同的详情
    private boolean containsDetail(FindFarmDetail newDetail) {
        for (FindFarmDetail detail : farmDetails) {
            // 根据业务需求定义相等条件，这里假设任务名称相同则认为是同一条数据
            if (detail.getTaskName().equals(newDetail.getTaskName())) {
                return true;
            }
        }
        return false;
    }

    // 获取当前数据量
    public int getDataSize() {
        return farmDetails.size();
    }

    private static class ViewHolder {
        TextView tvTaskName;
        TextView tvCrop, tvOperationType, tvOperationDate, tvPublishTime, tvFindFarmSize;
        ImageView ivFindFarmImage;
        TextView tvViewFarmDetail, tvUpdateFarmDetail;
        TextView tvRemove;
        View farmChooseOpen;

        ViewHolder(View view) {
            ivFindFarmImage= view.findViewById(R.id.iv_find_farm_image);
            tvTaskName = view.findViewById(R.id.tv_task_name);
            tvFindFarmSize = view.findViewById(R.id.tv_find_farm_size);
            tvCrop = view.findViewById(R.id.tv_crop);
            tvOperationType = view.findViewById(R.id.tv_operation_type);
            tvOperationDate = view.findViewById(R.id.tv_operation_date);
            tvPublishTime = view.findViewById(R.id.tv_publish_time);
            tvViewFarmDetail = view.findViewById(R.id.tv_view_farm_detail);
            tvUpdateFarmDetail = view.findViewById(R.id.tv_update_farm_detail);
            tvRemove = view.findViewById(R.id.tv_remove_farm);
            farmChooseOpen = view.findViewById(R.id.farm_choose_open);
        }
    }
}