package com.example.femfdefend;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.femfdefend.databinding.ActivityLawDetailBinding;

public class LawDetailActivity extends AppCompatActivity {
    private ActivityLawDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLawDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get law details from intent
        String title = getIntent().getStringExtra("law_title");
        String description = getIntent().getStringExtra("law_description");
        int year = getIntent().getIntExtra("law_year", 0);

        // Set the title and details
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        binding.tvYear.setText(getString(R.string.year, year));
        binding.tvDescription.setText(description);
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