package com.example.femfdefend;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.femfdefend.databinding.ActivityPanicButtonBinding;
import com.example.femfdefend.models.EmergencyContact;
import com.example.femfdefend.services.EmergencyServiceHandler;
import com.example.femfdefend.services.VoiceRecognitionService;
import com.example.femfdefend.utils.FirebaseHelper;
import com.example.femfdefend.utils.LocationCache;
import com.example.femfdefend.utils.NotificationHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.ArrayList;

public class PanicButtonActivity extends AppCompatActivity {
    private static final String TAG = "PanicButtonActivity";
    private static final String SENT = "SMS_SENT";
    private static final String DELIVERED = "SMS_DELIVERED";
    
    private ActivityPanicButtonBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCache locationCache;
    private EmergencyServiceHandler emergencyServiceHandler;
    private BroadcastReceiver voiceEmergencyReceiver;
    private ArrayList<BroadcastReceiver> smsReceivers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPanicButtonBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCache = LocationCache.getInstance(this);
        emergencyServiceHandler = new EmergencyServiceHandler(this);
        smsReceivers = new ArrayList<>();

        // Check if user is authenticated
        if (FirebaseHelper.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupUI();
        checkPermissions();
        setupVoiceRecognition();
    }

    private void setupUI() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.panic_button);
        }

        binding.btnPanic.setOnClickListener(v -> activateEmergencyProtocol());
    }

    private void setupVoiceRecognition() {
        // Register voice emergency receiver
        voiceEmergencyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String voiceText = intent.getStringExtra(VoiceRecognitionService.EXTRA_VOICE_TEXT);
                Log.i(TAG, "Emergency voice command received: " + voiceText);
                
                // Visual feedback
                binding.btnPanic.setPressed(true);
                new Handler().postDelayed(() -> {
                    binding.btnPanic.setPressed(false);
                    activateEmergencyProtocol();
                }, 500);
                
                // Vibrate to confirm recognition
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(200);
                    }
                }
                
                // Show toast with recognized command
                Toast.makeText(PanicButtonActivity.this, 
                    "Voice command recognized: " + voiceText, 
                    Toast.LENGTH_SHORT).show();
            }
        };

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(voiceEmergencyReceiver, 
                new IntentFilter(VoiceRecognitionService.ACTION_VOICE_EMERGENCY));

        // Start voice recognition service
        if (checkVoicePermission()) {
            startService(new Intent(this, VoiceRecognitionService.class));
            Toast.makeText(this, "Voice recognition activated", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkVoicePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.RECORD_AUDIO}, 
                VOICE_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private static final int VOICE_PERMISSION_REQUEST_CODE = 101;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == VOICE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Start voice service after permission granted
                startService(new Intent(this, VoiceRecognitionService.class));
                Toast.makeText(this, "Voice recognition activated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, 
                    "Voice recognition requires microphone permission", 
                    Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    Log.w(TAG, "Permission not granted: " + permissions[i]);
                    
                    // Show specific message based on the permission
                    String message = switch (permissions[i]) {
                        case Manifest.permission.ACCESS_FINE_LOCATION ->
                                "Location permission is required for emergency alerts";
                        case Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS ->
                                "SMS permissions are required for emergency alerts";
                        case Manifest.permission.RECORD_AUDIO ->
                                "Microphone permission is required for voice recognition";
                        case Manifest.permission.POST_NOTIFICATIONS ->
                                "Notification permission is required for emergency alerts";
                        default -> "Required permission was not granted";
                    };
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                }
            }
            
            if (allGranted) {
                Log.i(TAG, "All required permissions granted");
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkPermissions() {
        ArrayList<String> permissionsToRequest = new ArrayList<>();

        // Location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // SMS permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECEIVE_SMS);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_SMS);
        }

        // Audio permission for voice recognition
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO);
        }

        // Notification permission for Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            Log.i(TAG, "Requesting permissions: " + permissionsToRequest);
            ActivityCompat.requestPermissions(this, 
                permissionsToRequest.toArray(new String[0]), 
                PERMISSIONS_REQUEST_CODE);
        }
    }

    private static final int PERMISSIONS_REQUEST_CODE = 100;

    private void activateEmergencyProtocol() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted");
            Toast.makeText(this, "Location permission required for emergency alerts", Toast.LENGTH_LONG).show();
            checkPermissions();
            return;
        }

        // Launch EmergencyCameraActivity to start recording automatically
        Intent cameraIntent = new Intent(this, EmergencyCameraActivity.class);
        startActivity(cameraIntent);

        Log.i("PanicButtonActivity", "Starting EmergencyRecordingService...");
        Intent recordingIntent = new Intent(this, com.example.femfdefend.services.EmergencyRecordingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(recordingIntent);
        } else {
            startService(recordingIntent);
        }

        // Show immediate feedback to user
        binding.btnPanic.setEnabled(false);
        Log.i(TAG, "Emergency protocol activated - attempting to get current location");
        Toast.makeText(this, "Emergency protocol activated! Alerting all contacts and emergency services...", Toast.LENGTH_LONG).show();

        // Vibrate to confirm activation
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(500);
            }
        }

        // Try to get current location with high accuracy
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener(this, location -> {
                final LocationData locationData = new LocationData();
                
                if (location != null) {
                    Log.i(TAG, "Current location obtained - Lat: " + location.getLatitude() + ", Long: " + location.getLongitude());
                    locationData.latitude = location.getLatitude();
                    locationData.longitude = location.getLongitude();
                    locationData.isUsingLastKnown = false;
                    // Cache the current location
                    locationCache.updateLastLocation(locationData.latitude, locationData.longitude);
                    Log.d(TAG, "Location cached successfully");
                } else {
                    // Try to get last known location
                    Log.w(TAG, "Current location not available, trying last known location");
                    Location lastLocation = locationCache.getLastKnownLocation();
                    if (lastLocation != null) {
                        locationData.latitude = lastLocation.getLatitude();
                        locationData.longitude = lastLocation.getLongitude();
                        locationData.isUsingLastKnown = true;
                        Log.i(TAG, "Using last known location - Lat: " + locationData.latitude + ", Long: " + locationData.longitude);
                    } else {
                        Log.e(TAG, "No location available");
                        Toast.makeText(this, "Unable to get location. Emergency alerts will be sent without location.", Toast.LENGTH_SHORT).show();
                    }
                }

                // Get user name for emergency messages
                String userName = FirebaseHelper.getInstance().getCurrentUser() != null ? 
                    FirebaseHelper.getInstance().getCurrentUser().getDisplayName() : "Unknown User";

                // Send emergency alerts
                notifyEmergencyContacts(locationData, userName);
                
                // Handle emergency services
                emergencyServiceHandler.handleEmergency(userName, locationData.latitude, locationData.longitude);

                // Re-enable panic button after delay
                new Handler().postDelayed(() -> {
                    binding.btnPanic.setEnabled(true);
                    Toast.makeText(PanicButtonActivity.this, "Emergency alerts sent. You can press again if needed.", Toast.LENGTH_SHORT).show();
                }, 5000);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting location: " + e.getMessage());
                Toast.makeText(this, "Location error: " + e.getMessage() + ". Sending alerts without location.", Toast.LENGTH_SHORT).show();
                
                // Send alerts without location if location fails
                String userName = FirebaseHelper.getInstance().getCurrentUser() != null ? 
                    FirebaseHelper.getInstance().getCurrentUser().getDisplayName() : "Unknown User";
                LocationData noLocationData = new LocationData();
                notifyEmergencyContacts(noLocationData, userName);
                emergencyServiceHandler.handleEmergency(userName, 0, 0);
                
                binding.btnPanic.setEnabled(true);
            });
    }

    private void notifyEmergencyContacts(LocationData locationData, String userName) {
        FirebaseHelper.getInstance().getEmergencyContacts(contacts -> {
            if (contacts.isEmpty()) {
                Log.w(TAG, "No emergency contacts found");
                Toast.makeText(this, "No emergency contacts found. Please add contacts in settings.", Toast.LENGTH_LONG).show();
                return;
            }

            int totalContacts = contacts.size();
            final int[] successfulSends = {0};

            for (EmergencyContact contact : contacts) {
                String phoneNumber = contact.getPhone();
                if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                    Log.e(TAG, "Invalid phone number for contact: " + contact.getName());
                    continue;
                }

                // Send SMS using SMSHelper
                com.example.femfdefend.utils.SMSHelper.sendLocationSMS(
                    this,
                    phoneNumber,
                    locationData.latitude,
                    locationData.longitude,
                    true // isEmergency = true
                );

                // Send FCM notification if token available
                FirebaseHelper.getInstance().getFCMToken(contact.getId(), token -> {
                    if (token != null && !token.isEmpty()) {
                        NotificationHelper.sendFCMNotification(
                            token,
                            "EMERGENCY ALERT",
                            userName + " needs immediate help!",
                            locationData.latitude,
                            locationData.longitude,
                            true
                        );
                    }
                });

                successfulSends[0]++;
                if (successfulSends[0] == totalContacts) {
                    Log.i(TAG, "Emergency alerts sent to all " + totalContacts + " contacts");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister all SMS receivers
        for (BroadcastReceiver receiver : smsReceivers) {
            try {
                unregisterReceiver(receiver);
            } catch (Exception e) {
                Log.w(TAG, "Error unregistering receiver: " + e.getMessage());
            }
        }
        smsReceivers.clear();
        
        if (voiceEmergencyReceiver != null) {
            LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(voiceEmergencyReceiver);
        }
        stopService(new Intent(this, VoiceRecognitionService.class));
        // Stop emergency recording service if running
        stopService(new Intent(this, com.example.femfdefend.services.EmergencyRecordingService.class));
        binding = null;
        // Send broadcast to stop EmergencyCameraActivity recording if running
        Intent stopIntent = new Intent("com.example.femfdefend.STOP_EMERGENCY_RECORDING");
        sendBroadcast(stopIntent);
    }

    // Helper class to hold location data that can be used in lambda expressions
    private static class LocationData {
        double latitude = 0;
        double longitude = 0;
        boolean isUsingLastKnown = false;
    }
}
