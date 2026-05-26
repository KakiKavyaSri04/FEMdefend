package com.example.femfdefend;

import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.femfdefend.databinding.ActivityVideoPlayerBinding;

import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.WindowMetrics;

public class VideoPlayerActivity extends AppCompatActivity {
    private ActivityVideoPlayerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getIntent().getStringExtra("video_title"));
        }

        String videoPath = getIntent().getStringExtra("video_path");
        setupVideoPlayer(videoPath);
    }

    private void setupVideoPlayer(String videoPath) {
        VideoView videoView = binding.videoView;
        String path = "android.resource://" + getPackageName() + "/" + videoPath;
        videoView.setVideoURI(Uri.parse(path));

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        
        // Handle video sizing for portrait videos
        videoView.setOnPreparedListener(mp -> {
            int videoWidth = mp.getVideoWidth();
            int videoHeight = mp.getVideoHeight();
            float videoProportion = (float) videoWidth / (float) videoHeight;
            
            // Get screen dimensions using WindowMetrics API
            int screenWidth;
            int screenHeight;
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();
                Rect bounds = windowMetrics.getBounds();
                screenWidth = bounds.width();
                screenHeight = bounds.height();
            } else {
                // For older versions, use DisplayMetrics
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                screenWidth = displayMetrics.widthPixels;
                screenHeight = displayMetrics.heightPixels;
            }
            
            float screenProportion = (float) screenWidth / (float) screenHeight;
            
            android.view.ViewGroup.LayoutParams lp = videoView.getLayoutParams();
            
            if (videoProportion > screenProportion) {
                lp.width = screenWidth;
                lp.height = (int) ((float) screenWidth / videoProportion);
            } else {
                lp.width = (int) (videoProportion * (float) screenHeight);
                lp.height = screenHeight;
            }
            videoView.setLayoutParams(lp);
            
            videoView.start();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 