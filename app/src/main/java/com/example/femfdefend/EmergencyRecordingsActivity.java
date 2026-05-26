package com.example.femfdefend;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.femfdefend.models.EmergencyRecording;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EmergencyRecordingsActivity extends AppCompatActivity implements EmergencyRecordingAdapter.OnItemClickListener {
    private EmergencyRecordingAdapter adapter;
    private List<EmergencyRecording> recordings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_recordings);
        RecyclerView recyclerView = findViewById(R.id.recyclerViewRecordings);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recordings = getAllMp4Recordings();
        adapter = new EmergencyRecordingAdapter(recordings, this);
        recyclerView.setAdapter(adapter);
    }

    private List<EmergencyRecording> getAllMp4Recordings() {
        List<EmergencyRecording> list = new ArrayList<>();
        File dir = getFilesDir();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".mp4"));
        if (files != null) {
            for (File file : files) {
                EmergencyRecording rec = new EmergencyRecording();
                rec.filePath = file.getAbsolutePath();
                rec.timestamp = file.lastModified();
                rec.duration = 0; // Unknown unless you parse the file
                rec.cameraUsed = file.getName().contains("EMERGENCY") ? "emergency" : "test";
                list.add(rec);
            }
        }
        return list;
    }

    @Override
    public void onPlayClick(EmergencyRecording recording) {
        File file = new File(recording.filePath);
        Uri uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            getApplicationContext().getPackageName() + ".provider",
            file
        );
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "No app found to play video", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteClick(EmergencyRecording recording) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Recording")
            .setMessage("Are you sure you want to delete this recording?")
            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    File file = new File(recording.filePath);
                    if (file.exists()) file.delete();
                    adapter.removeRecording(recording);
                    Toast.makeText(EmergencyRecordingsActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
} 