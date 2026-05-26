package com.example.femfdefend;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.femfdefend.databinding.ActivityLoginBinding;
import com.example.femfdefend.utils.FirebaseHelper;
import com.example.femfdefend.utils.PermissionManager;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();

        // Check if user is already logged in
        if (firebaseHelper.getCurrentUser() != null) {
            checkPermissionsAndProceed();
            return;
        }

        // Setup click listeners
        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void loginUser() {
        // Add null checks for EditText values
        Editable emailEditable = binding.etEmail.getText();
        Editable passwordEditable = binding.etPassword.getText();
        
        String email = emailEditable != null ? emailEditable.toString().trim() : "";
        String password = passwordEditable != null ? passwordEditable.toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.error_fields_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnLogin.setEnabled(false);

        firebaseHelper.getAuth().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> checkPermissionsAndProceed())
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this,
                            "Login failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void checkPermissionsAndProceed() {
        if (PermissionManager.areAllPermissionsGranted(this)) {
            proceedToDashboard();
        } else {
            showPermissionExplanationDialog();
        }
    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.permission_explanation_title)
            .setMessage(R.string.permission_explanation_message)
            .setPositiveButton("Continue", (dialog, which) -> PermissionManager.checkAndRequestPermissions(this))
            .setNegativeButton("Not Now", (dialog, which) -> showLimitedFunctionalityWarning())
            .setCancelable(false)
            .show();
    }

    private void showLimitedFunctionalityWarning() {
        new AlertDialog.Builder(this)
            .setTitle("Limited Functionality")
            .setMessage(R.string.permission_denied_message)
            .setPositiveButton(R.string.proceed, (dialog, which) -> proceedToDashboard())
            .setCancelable(false)
            .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123) { // Same code as defined in PermissionManager
            proceedToDashboard();
        }
    }

    private void proceedToDashboard() {
        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 