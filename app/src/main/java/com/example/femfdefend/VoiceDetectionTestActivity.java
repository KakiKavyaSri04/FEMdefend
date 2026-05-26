package com.example.femfdefend;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.femfdefend.databinding.ActivityVoiceDetectionTestBinding;
import com.example.femfdefend.services.VoiceDetectionService;

public class VoiceDetectionTestActivity extends AppCompatActivity {
    private ActivityVoiceDetectionTestBinding binding;
    private static final int PERMISSION_REQUEST_CODE = 123;
    private boolean isServiceRunning = false;
    private BroadcastReceiver voiceEmergencyReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVoiceDetectionTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();
        registerVoiceEmergencyReceiver();
        checkPermissions();
    }

    private void setupUI() {
        binding.tvStatus.setText("Voice Detection is INACTIVE");
        binding.btnToggleService.setText(R.string.start_voice_detection);
        binding.btnClearLog.setText(R.string.clear_log);

        binding.btnToggleService.setOnClickListener(v -> toggleService());
        binding.btnClearLog.setOnClickListener(v -> binding.tvLog.setText(""));

        // Remove the test microphone button as it's not implemented and confusing
        binding.btnTestMic.setVisibility(android.view.View.GONE);
    }

    private void toggleService() {
        if (!isServiceRunning) {
            startVoiceDetection();
        } else {
            stopVoiceDetection();
        }
    }

    private void startVoiceDetection() {
        Intent intent = new Intent(this, VoiceDetectionService.class);
        startService(intent);
        isServiceRunning = true;
        binding.btnToggleService.setText(R.string.stop_voice_detection);
        binding.tvStatus.setText("Voice Detection is ACTIVE");
        binding.tvStatus.setBackgroundColor(getColor(R.color.green)); // Use a green color for active
        logMessage(getString(R.string.service_started));
    }

    private void stopVoiceDetection() {
        Intent intent = new Intent(this, VoiceDetectionService.class);
        stopService(intent);
        isServiceRunning = false;
        binding.btnToggleService.setText(R.string.start_voice_detection);
        binding.tvStatus.setText("Voice Detection is INACTIVE");
        binding.tvStatus.setBackgroundColor(getColor(android.R.color.darker_gray)); // Reset color
        logMessage(getString(R.string.service_stopped));
    }

    private void registerVoiceEmergencyReceiver() {
        voiceEmergencyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (VoiceDetectionService.ACTION_VOICE_EMERGENCY.equals(intent.getAction())) {
                    String voiceText = intent.getStringExtra(VoiceDetectionService.EXTRA_VOICE_TEXT);
                    logMessage("EMERGENCY TRIGGERED by voice: " + voiceText);
                    binding.tvStatus.setBackgroundColor(getColor(R.color.red));
                    binding.tvStatus.setText("EMERGENCY ACTIVATED");
                }
            }
        };

        IntentFilter filter = new IntentFilter(VoiceDetectionService.ACTION_VOICE_EMERGENCY);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(voiceEmergencyReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(voiceEmergencyReceiver, filter);
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void logMessage(String message) {
        String currentLog = binding.tvLog.getText().toString();
        String timestamp = java.text.DateFormat.getTimeInstance().format(new java.util.Date());
        String newLog = timestamp + ": " + message + "\n" + currentLog;
        binding.tvLog.setText(newLog);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                logMessage(getString(R.string.audio_permission_granted));
            } else {
                logMessage(getString(R.string.audio_permission_denied));
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceEmergencyReceiver != null) {
            unregisterReceiver(voiceEmergencyReceiver);
        }
        if (isServiceRunning) {
            stopVoiceDetection();
        }
        binding = null;
    }
}
