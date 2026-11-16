package cn.agrinav.trajectory.adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.lang.ref.WeakReference;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import cn.agrinav.R;
import cn.agrinav.trajectory.entity.Trajectory;
import cn.agrinav.trajectory.util.DataUtil;
/**
 * @author 11230
 */
public class ReloadAdapter extends BaseAdapter {
    private int layoutId;
    private List<Trajectory> trajectories;
    private AppCompatActivity context;
    public ReloadAdapter(int layoutId, List<Trajectory> trajectories, AppCompatActivity context) {
        this.layoutId = layoutId;
        this.trajectories = trajectories;
        this.context = context;
    }
    @Override
    public int getCount() {
        return trajectories.size();
    }

    @Override
    public Object getItem(int position) {
        return trajectories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ReloadAdapter.ViewHolder holder;
        if (convertView == null){
            //第一步获取视图
            convertView = LayoutInflater.from(context).inflate(layoutId,null);
            holder = new ReloadAdapter.ViewHolder();
            holder.startTime = convertView.findViewById(R.id.reload_time_start);
            holder.endTime = convertView.findViewById(R.id.reload_time_end);
            holder.workType = convertView.findViewById(R.id.reload_time_work_type);
            holder.button = convertView.findViewById(R.id.btn_reload);
            convertView.setTag(holder);
        }else {
            holder = (ReloadAdapter.ViewHolder) convertView.getTag();
        }
        //设置结束时间
        if (trajectories.get(position).getEndTime() != null) {
            holder.endTime.setText(LocalDateTime.parse(trajectories.get(position).getEndTime()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }else {
            holder.endTime.setText("");
        }
        //设置开始时间
        if (trajectories.get(position).getStartTime()!=null){
            holder.startTime.setText(LocalDateTime.parse(trajectories.get(position).getStartTime()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }else {
            holder.startTime.setText("");
        }
        //设置工作类型
        if (trajectories.get(position).getWorkType()!=null){
            holder.workType.setText(trajectories.get(position).getWorkType());
        }else {
            holder.workType.setText("");
        }
        //设置按钮内容和样式
        if (trajectories.get(position).getIsUpload()==null||!trajectories.get(position).getIsUpload()){
            //这里说明还没有上传
            holder.button.setText("重新上传");
            holder.button.setEnabled(true);
        }else {
            //这里说明已经上传了就不让他上传了
            holder.button.setText("已上传");
            holder.button.setEnabled(false);
        }
        holder.button.setOnClickListener(v -> {
            new DataUtil(context).reloadTrajectory(new WeakReference<>(context), holder.button, this,trajectories.get(position));
        });
        return convertView;
    }
    static class ViewHolder{
        TextView startTime;
        TextView endTime;
        TextView workType;
        Button button;
    }
    public void notified() {
        super.notifyDataSetChanged();
    }
}
