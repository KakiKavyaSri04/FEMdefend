package com.example.femfdefend;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.example.femfdefend.databinding.ActivitySplashBinding;
import com.example.femfdefend.utils.DummyDataGenerator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;
    private FirebaseAuth auth;
    private static final long ANIMATION_DURATION = 1500L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        // Animate title and subtitle
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(ANIMATION_DURATION);
        binding.appTitle.startAnimation(fadeIn);
        binding.appSubtitle.startAnimation(fadeIn);

        // Show Let's Go button after animation using Handler with explicit Looper
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing() && binding != null) {
                binding.btnLetsGo.setVisibility(View.VISIBLE);
                binding.btnLetsGo.setAlpha(0f);
                binding.btnLetsGo.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .start();
            }
        }, ANIMATION_DURATION);

        binding.btnLetsGo.setOnClickListener(v -> {
            // Check if user is already logged in
            if (auth.getCurrentUser() != null) {
                startActivity(new Intent(SplashActivity.this, DashboardActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        });

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DummyDataGenerator.populateEmergencyServices(db);
    }
} 