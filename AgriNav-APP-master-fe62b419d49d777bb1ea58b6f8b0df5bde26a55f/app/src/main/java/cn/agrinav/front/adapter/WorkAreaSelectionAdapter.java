package cn.agrinav.front.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.agrinav.R;

// 新增 WorkAreaSelectionAdapter 内部类
public class WorkAreaSelectionAdapter extends RecyclerView.Adapter<WorkAreaSelectionAdapter.ViewHolder> {
    private List<String> areas;
    private List<String> selectedAreas;

    public WorkAreaSelectionAdapter(List<String> areas, List<String> selectedAreas) {
        this.areas = areas;
        this.selectedAreas = new ArrayList<>(selectedAreas);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_area_selection, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String area = areas.get(position);
        holder.textView.setText(area);
        holder.itemView.setSelected(selectedAreas.contains(area));
        holder.itemView.setOnClickListener(v -> {
            if (selectedAreas.contains(area)) {
                selectedAreas.remove(area);
            } else {
                selectedAreas.add(area);
            }
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return areas.size();
    }

    public List<String> getSelectedWorkAreas() {
        return selectedAreas;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tv_area_name);
        }
    }
}
