package cn.agrinav.front.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import cn.agrinav.R;

public class JobPhotoAdapter extends RecyclerView.Adapter<JobPhotoAdapter.PhotoViewHolder> {
    private Context context;
    private String[] photos;

    public JobPhotoAdapter(Context context) {
        this.context = context;
    }

    public void setPhotos(String[] photos) {
        this.photos = photos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_job_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        if (photos != null && position < photos.length) {
            Glide.with(context)
                    .load(photos[position])
                    .into(holder.ivPhoto);
        }
    }

    @Override
    public int getItemCount() {
        return photos != null ? photos.length : 0;
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
        }
    }
} 