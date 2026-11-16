package cn.agrinav.front.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import cn.agrinav.R;

public class MachineImagesAdapter extends RecyclerView.Adapter<MachineImagesAdapter.ViewHolder> {

    private List<String> images;
    private OnImageRemoveListener listener;
    private Uri defaultImageUri;
    private Context context;

    public interface OnImageRemoveListener {
        void onImageRemove(Uri uri);
    }

    public MachineImagesAdapter(Context context, List<String> images, OnImageRemoveListener listener) {
        this.context = context;
        this.images = images;
        this.listener = listener;
        this.defaultImageUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.drawable.default_machine_image);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_machine_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String imageUrl = images.get(position);

        if (imageUrl.startsWith("http")) {
            Glide.with(context)
                .load(imageUrl)
                .apply(new RequestOptions()
                    .placeholder(R.drawable.default_machine_image)
                    .error(R.drawable.default_machine_image))
                .into(holder.imageView);
        } else {
            holder.imageView.setImageURI(Uri.parse(imageUrl));
        }

        holder.removeButton.setVisibility(View.VISIBLE);
        holder.removeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageRemove(Uri.parse(imageUrl));
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton removeButton;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_machine);
            removeButton = itemView.findViewById(R.id.btn_remove);
        }
    }

    // 更新数据
    public void updateData(List<String> newImages) {
        images.clear();
        images.addAll(newImages);
        notifyDataSetChanged();
    }
}
