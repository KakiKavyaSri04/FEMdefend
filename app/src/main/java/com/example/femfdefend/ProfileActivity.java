package com.example.femfdefend;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.femfdefend.databinding.ActivityProfileBinding;
import com.example.femfdefend.models.User;
import com.example.femfdefend.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.tasks.Task;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private FirebaseHelper firebaseHelper;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.profile);
        }

        // Handle back button press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        firebaseHelper = FirebaseHelper.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            loadUserProfile();
        }

        binding.btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void loadUserProfile() {
        Task<DocumentSnapshot> profileTask = firebaseHelper.getCurrentUserRef().get();
        profileTask
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null && binding != null) {
                        binding.etFullName.setText(user.getFullName());
                        binding.etPhone.setText(user.getPhone());
                        binding.etEmergencyContact.setText(user.getEmergencyContact());
                    }
                }
            })
            .addOnFailureListener(e -> {
                if (binding != null) {
                    Toast.makeText(this, "Error loading profile: " + e.getMessage(), 
                                 Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void saveProfile() {
        if (currentUser == null || binding == null) return;

        String fullName = binding.etFullName.getText() != null ? 
            binding.etFullName.getText().toString().trim() : "";
        String phone = binding.etPhone.getText() != null ? 
            binding.etPhone.getText().toString().trim() : "";
        String emergencyContact = binding.etEmergencyContact.getText() != null ? 
            binding.etEmergencyContact.getText().toString().trim() : "";

        if (fullName.isEmpty() || phone.isEmpty() || emergencyContact.isEmpty()) {
            Toast.makeText(this, R.string.error_fields_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        User user = new User(
                currentUser.getUid(),
                fullName,
                currentUser.getEmail(),
                phone,
                emergencyContact
        );

        Task<Void> updateTask = firebaseHelper.getCurrentUserRef().set(user);
        updateTask
                .addOnSuccessListener(aVoid -> {
                    if (binding != null) {
                        Toast.makeText(this, R.string.success_profile_updated, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding != null) {
                        Toast.makeText(this,
                                "Failed to update profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
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