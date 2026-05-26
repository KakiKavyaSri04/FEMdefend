package com.example.femfdefend;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.femfdefend.adapters.SelfDefenseVideoAdapter;
import com.example.femfdefend.databinding.ActivitySelfDefenseBinding;
import com.example.femfdefend.models.SelfDefenseVideo;

import java.util.ArrayList;
import java.util.List;

public class SelfDefenseActivity extends AppCompatActivity {
    private ActivitySelfDefenseBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySelfDefenseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.self_defense);
        }

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        binding.recyclerVideos.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerVideos.setAdapter(new SelfDefenseVideoAdapter(getVideoList()));
    }

    private List<SelfDefenseVideo> getVideoList() {
        List<SelfDefenseVideo> videos = new ArrayList<>();
        
        // Add the self-defense videos
        videos.add(new SelfDefenseVideo(
            getString(R.string.basic_techniques_title),
            getString(R.string.basic_techniques_desc),
            String.valueOf(R.raw.self_defense_basics),
            "thumbnails/basics_thumb.jpg"
        ));
        
        videos.add(new SelfDefenseVideo(
            getString(R.string.escape_techniques_title),
            getString(R.string.escape_techniques_desc),
            String.valueOf(R.raw.escape_techniques),
            "thumbnails/escape_thumb.jpg"
        ));
        
        videos.add(new SelfDefenseVideo(
            getString(R.string.defense_techniques_title),
            getString(R.string.defense_techniques_desc),
            String.valueOf(R.raw.defense_attacks),
            "thumbnails/defence_thumb.jpg"
        ));
        
        videos.add(new SelfDefenseVideo(
            getString(R.string.emergency_techniques_title),
            getString(R.string.emergency_techniques_desc),
            String.valueOf(R.raw.emergency_response),
            "thumbnails/emergency_thumb.jpg"
        ));

        return videos;
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