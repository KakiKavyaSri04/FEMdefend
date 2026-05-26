package com.example.femfdefend;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.femfdefend.databinding.ActivityRegisterBinding;
import com.example.femfdefend.models.User;
import com.example.femfdefend.utils.FirebaseHelper;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();

        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.tvLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String fullName = binding.etFullName.getText() != null ? 
            binding.etFullName.getText().toString().trim() : "";
        String email = binding.etEmail.getText() != null ? 
            binding.etEmail.getText().toString().trim() : "";
        String phone = binding.etPhone.getText() != null ? 
            binding.etPhone.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? 
            binding.etPassword.getText().toString() : "";
        String confirmPassword = binding.etConfirmPassword.getText() != null ? 
            binding.etConfirmPassword.getText().toString() : "";

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || 
            password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, R.string.error_fields_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, R.string.error_passwords_dont_match, Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress and disable register button
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnRegister.setEnabled(false);

        firebaseHelper.getAuth().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() != null) {
                        String userId = authResult.getUser().getUid();
                        User newUser = new User(userId, fullName, email, phone, "");
                        
                        firebaseHelper.saveUser(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                                    finishAffinity();
                                })
                                .addOnFailureListener(e -> {
                                    binding.progressBar.setVisibility(View.GONE);
                                    binding.btnRegister.setEnabled(true);
                                    Toast.makeText(RegisterActivity.this,
                                            "Failed to save user data: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this,
                            "Registration failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 