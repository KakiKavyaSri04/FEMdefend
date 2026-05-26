package com.example.femfdefend;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.femfdefend.databinding.ActivityVoiceDetectionBinding;
import com.example.femfdefend.services.VoiceRecognitionService;
import com.example.femfdefend.utils.ErrorHandler;

/**
 * Voice Detection Activity for FEMdefend application.
 * Allows users to enable/disable voice detection for emergency activation.
 * Integrates with the main app menu system and triggers panic button functionality.
 * 
 * Features:
 * - Toggle voice detection on/off
 * - Real-time status monitoring
 * - Permission handling
 * - Emergency activation via wake word
 * - Integration with panic button system
 * 
 * @author FEMdefend Team
 * @version 1.0
 */
public class VoiceDetectionActivity extends AppCompatActivity {
    private static final String TAG = "VoiceDetectionActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;
    
    private ActivityVoiceDetectionBinding binding;
    private boolean isServiceRunning = false;
    private BroadcastReceiver voiceEmergencyReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            binding = ActivityVoiceDetectionBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            setupToolbar();
            setupUI();
            registerVoiceEmergencyReceiver();
            checkPermissions();
            updateServiceStatus();

        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "VoiceDetectionActivity initialization");
            finish();
        }
    }

    /**
     * Sets up the toolbar with back navigation
     */
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.voice_detection);
        }
    }

    /**
     * Sets up the user interface components
     */
    private void setupUI() {
        try {
            // Set up toggle switch
            binding.switchVoiceDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    enableVoiceDetection();
                } else {
                    disableVoiceDetection();
                }
            });

            // Set up info button
            binding.btnInfo.setOnClickListener(v -> showInfoDialog());

            // Set up status text
            updateStatusText();
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Setting up UI components");
        }
    }

    /**
     * Registers broadcast receiver for voice emergency events
     */
    private void registerVoiceEmergencyReceiver() {
        try {
            voiceEmergencyReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (VoiceRecognitionService.ACTION_VOICE_EMERGENCY.equals(intent.getAction())) {
                        String voiceText = intent.getStringExtra(VoiceRecognitionService.EXTRA_VOICE_TEXT);
                        handleVoiceEmergency(voiceText);
                    }
                }
            };

            IntentFilter filter = new IntentFilter(VoiceRecognitionService.ACTION_VOICE_EMERGENCY);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(voiceEmergencyReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(voiceEmergencyReceiver, filter);
            }
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Registering voice emergency receiver");
        }
    }

    /**
     * Handles voice emergency activation
     * 
     * @param voiceText The detected voice text
     */
    private void handleVoiceEmergency(String voiceText) {
        try {
            Log.i(TAG, "Voice emergency triggered: " + voiceText);
            
            // Show immediate feedback
            Toast.makeText(this, "Emergency voice command detected!", Toast.LENGTH_LONG).show();
            
            // Update UI to show emergency state
            binding.tvStatus.setText(R.string.emergency_activated);
            binding.tvStatus.setTextColor(getColor(R.color.red));
            
            // Trigger panic button functionality
            triggerPanicButton();
            
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Handling voice emergency");
        }
    }

    /**
     * Triggers the panic button functionality
     */
    private void triggerPanicButton() {
        try {
            // Create intent to activate panic button
            Intent panicIntent = new Intent(this, PanicButtonActivity.class);
            panicIntent.putExtra("voice_triggered", true);
            panicIntent.putExtra("voice_text", "Voice command detected");
            startActivity(panicIntent);
            
            // Close this activity after triggering panic
            finish();
            
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Triggering panic button");
        }
    }

    /**
     * Checks and requests necessary permissions
     */
    private void checkPermissions() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                showPermissionExplanationDialog();
            }
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Checking permissions");
        }
    }

    /**
     * Shows permission explanation dialog
     */
    private void showPermissionExplanationDialog() {
        try {
            new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(R.string.voice_detection_permission_explanation)
                .setPositiveButton(R.string.grant_permission, (dialog, which) -> ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        PERMISSION_REQUEST_CODE))
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    Toast.makeText(this, R.string.voice_detection_requires_permission, Toast.LENGTH_LONG).show();
                    finish();
                })
                .setCancelable(false)
                .show();
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Showing permission dialog");
        }
    }

    /**
     * Enables voice detection service
     */
    private void enableVoiceDetection() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                binding.switchVoiceDetection.setChecked(false);
                showPermissionExplanationDialog();
                return;
            }

            // Battery optimization exclusion prompt
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                new AlertDialog.Builder(this)
                    .setTitle(R.string.battery_optimization_title)
                    .setMessage(R.string.battery_optimization_message)
                    .setPositiveButton(R.string.allow, (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            }

            Intent intent = new Intent(this, VoiceRecognitionService.class);
            startService(intent);
            isServiceRunning = true;
            
            updateServiceStatus();
            updateStatusText();
            
            Toast.makeText(this, R.string.voice_detection_enabled, Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Voice detection service started");
            
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Enabling voice detection");
            binding.switchVoiceDetection.setChecked(false);
        }
    }

    /**
     * Disables voice detection service
     */
    private void disableVoiceDetection() {
        try {
            Intent intent = new Intent(this, VoiceRecognitionService.class);
            stopService(intent);
            isServiceRunning = false;
            
            updateServiceStatus();
            updateStatusText();
            
            Toast.makeText(this, R.string.voice_detection_disabled, Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Voice detection service stopped");
            
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Disabling voice detection");
            binding.switchVoiceDetection.setChecked(true);
        }
    }

    /**
     * Updates the service status display
     */
    private void updateServiceStatus() {
        try {
            isServiceRunning = isServiceRunning(VoiceRecognitionService.class);
            
            // Temporarily disable listener to prevent triggering actions during status updates
            binding.switchVoiceDetection.setOnCheckedChangeListener(null);
            binding.switchVoiceDetection.setChecked(isServiceRunning);
            
            // Re-enable listener
            binding.switchVoiceDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    enableVoiceDetection();
                } else {
                    disableVoiceDetection();
                }
            });
            
            updateStatusText();
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Updating service status");
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        android.app.ActivityManager manager = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Updates the status text based on current state
     */
    private void updateStatusText() {
        try {
            if (isServiceRunning) {
                binding.tvStatus.setText(R.string.voice_detection_active);
                binding.tvStatus.setTextColor(getColor(R.color.green));
            } else {
                binding.tvStatus.setText(R.string.voice_detection_inactive);
                binding.tvStatus.setTextColor(getColor(android.R.color.darker_gray));
            }
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Updating status text");
        }
    }

    /**
     * Shows information dialog about voice detection
     */
    private void showInfoDialog() {
        try {
            new AlertDialog.Builder(this)
                .setTitle(R.string.voice_detection_info_title)
                .setMessage(R.string.voice_detection_info_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Showing info dialog");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        try {
            if (requestCode == PERMISSION_REQUEST_CODE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.audio_permission_granted, Toast.LENGTH_SHORT).show();
                    // If switch is checked, enable voice detection
                    if (binding.switchVoiceDetection.isChecked()) {
                        enableVoiceDetection();
                    }
                } else {
                    Toast.makeText(this, R.string.audio_permission_denied, Toast.LENGTH_LONG).show();
                    binding.switchVoiceDetection.setChecked(false);
                }
            }
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Handling permission result");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        try {
            if (item.getItemId() == android.R.id.home) {
                finish();
                return true;
            }
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Handling menu item selection");
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            updateServiceStatus();
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Activity resume");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (voiceEmergencyReceiver != null) {
                unregisterReceiver(voiceEmergencyReceiver);
            }
            binding = null;
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Activity destruction");
        }
    }
} 