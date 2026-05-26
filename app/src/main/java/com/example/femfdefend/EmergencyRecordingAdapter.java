package com.example.femfdefend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.femfdefend.models.EmergencyRecording;
import java.util.List;

public class EmergencyRecordingAdapter extends RecyclerView.Adapter<EmergencyRecordingAdapter.ViewHolder> {
    public interface OnItemClickListener {
        void onPlayClick(EmergencyRecording recording);
        void onDeleteClick(EmergencyRecording recording);
    }

    private List<EmergencyRecording> recordings;
    private OnItemClickListener listener;

    public EmergencyRecordingAdapter(List<EmergencyRecording> recordings, OnItemClickListener listener) {
        this.recordings = recordings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_emergency_recording, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmergencyRecording recording = recordings.get(position);
        holder.textViewTitle.setText("Recording: " + recording.timestamp);
        holder.textViewCamera.setText("Camera: " + recording.cameraUsed);
        holder.buttonPlay.setOnClickListener(v -> listener.onPlayClick(recording));
        holder.buttonDelete.setOnClickListener(v -> listener.onDeleteClick(recording));
    }

    @Override
    public int getItemCount() {
        return recordings.size();
    }

    public void removeRecording(EmergencyRecording recording) {
        int pos = recordings.indexOf(recording);
        if (pos != -1) {
            recordings.remove(pos);
            notifyItemRemoved(pos);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle, textViewCamera;
        ImageButton buttonPlay, buttonDelete;
        ViewHolder(View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewCamera = itemView.findViewById(R.id.textViewCamera);
            buttonPlay = itemView.findViewById(R.id.buttonPlay);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
} 