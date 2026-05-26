package com.example.femfdefend.adapters;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.femfdefend.R;
import com.example.femfdefend.VideoPlayerActivity;
import com.example.femfdefend.models.SelfDefenseVideo;

import java.util.List;

public class SelfDefenseVideoAdapter extends RecyclerView.Adapter<SelfDefenseVideoAdapter.VideoViewHolder> {
    private static final String TAG = "SelfDefenseVideoAdapter";
    private final List<SelfDefenseVideo> videos;

    public SelfDefenseVideoAdapter(List<SelfDefenseVideo> videos) {
        this.videos = videos;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_self_defense_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        holder.bind(videos.get(position));
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivThumbnail;
        private final TextView tvTitle;
        private final TextView tvDescription;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }

        public void bind(SelfDefenseVideo video) {
            tvTitle.setText(video.getTitle());
            tvDescription.setText(video.getDescription());

            // Load thumbnail using Glide with error handling
            try {
                RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.placeholder_video)
                    .error(R.drawable.error_video)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop();

                Glide.with(itemView.getContext())
                    .load("file:///android_asset/" + video.getThumbnailPath())
                    .apply(options)
                    .into(ivThumbnail);
            } catch (Exception e) {
                Log.e(TAG, "Error loading thumbnail: " + e.getMessage());
                ivThumbnail.setImageResource(R.drawable.error_video);
            }

            // Set click listener to play video
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), VideoPlayerActivity.class);
                intent.putExtra("video_path", video.getVideoPath());
                intent.putExtra("video_title", video.getTitle());
                itemView.getContext().startActivity(intent);
            });
        }
    }
} 