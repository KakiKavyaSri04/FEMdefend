package com.example.femfdefend.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.femfdefend.EmergencyCancelDialogActivity;
import com.example.femfdefend.R;
import com.example.femfdefend.models.EmergencyContact;
import com.example.femfdefend.models.EmergencyServiceModel;
import com.example.femfdefend.utils.EmergencyKeywords;
import com.example.femfdefend.utils.FirebaseHelper;
import com.example.femfdefend.utils.LocationCache;
import com.example.femfdefend.utils.NotificationHelper;
import com.example.femfdefend.utils.SMSHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VoiceRecognitionService extends Service {
    private static final String TAG = "VoiceRecognitionService";
    public static final String ACTION_VOICE_EMERGENCY = "com.example.femfdefend.VOICE_EMERGENCY";
    public static final String EXTRA_VOICE_TEXT = "voice_text";
    private static final String CHANNEL_ID = "VoiceRecognitionChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final double NEARBY_RADIUS_KM = 10.0;

    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private boolean isListening = false;
    private Handler restartHandler;
    private int errorCount = 0;
    private static final int MAX_ERRORS = 3;
    private static final long RESTART_DELAY_MS = 1000;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCache locationCache;

    private final BroadcastReceiver emergencyConfirmedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            activatePanicButton();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        restartHandler = new Handler();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCache = LocationCache.getInstance(this);

        createNotificationChannel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            );
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                emergencyConfirmedReceiver,
                new IntentFilter("com.example.femfdefend.EMERGENCY_CONFIRMED"),
                Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            registerReceiver(emergencyConfirmedReceiver, new IntentFilter("com.example.femfdefend.EMERGENCY_CONFIRMED"));
        }

        initializeSpeechRecognizer();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Voice Recognition Service",
                NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Running voice recognition for emergency detection");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Recognition Active")
            .setContentText("Listening for emergency commands")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private void initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition is not available on this device");
            Toast.makeText(this, "Speech recognition is not available on this device", 
                Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Log.d(TAG, "Speech recognizer created");

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                Log.d(TAG, "Ready for speech");
                errorCount = 0; // Reset error count when successfully ready
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "Speech begun");
            }

            @Override
            public void onRmsChanged(float v) {
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
                Log.d(TAG, "Buffer received");
            }

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "Speech ended");
            }

            @Override
            public void onError(int errorCode) {
                String errorMessage = getErrorMessage(errorCode);
                Log.e(TAG, "Speech recognition error: " + errorMessage);
                
                errorCount++;
                if (errorCount >= MAX_ERRORS) {
                    Log.w(TAG, "Too many errors, restarting speech recognizer");
                    restartSpeechRecognizer();
                } else if (isListening) {
                    restartHandler.postDelayed(() -> startListening(), RESTART_DELAY_MS);
                }
            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    for (String result : matches) {
                        Log.d(TAG, "Speech result: " + result);
                        if (EmergencyKeywords.isEmergencyPhrase(result)) {
                            Log.i(TAG, "Emergency phrase detected: " + result);
                            
                            // Trigger the 30-second cancellation dialog
                            showEmergencyConfirmationDialog();
                            break;
                        }
                    }
                } else {
                    Log.d(TAG, "No speech results");
                }
                
                // Continue listening
                if (isListening) {
                    restartHandler.postDelayed(() -> startListening(), RESTART_DELAY_MS);
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    for (String result : matches) {
                        Log.d(TAG, "Partial result: " + result);
                        if (EmergencyKeywords.isEmergencyPhrase(result)) {
                            Log.i(TAG, "Emergency phrase detected in partial result: " + result);
                            
                            // Trigger the 30-second cancellation dialog
                            showEmergencyConfirmationDialog();
                            break;
                        }
                    }
                }
            }

            @Override
            public void onEvent(int i, Bundle bundle) {
                Log.d(TAG, "Recognition event: " + i);
            }
        });
    }

    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No recognition match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Unknown error";
        }
    }

    private void restartSpeechRecognizer() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        initializeSpeechRecognizer();
        if (isListening) {
            startListening();
        }
        errorCount = 0;
    }

    private void showEmergencyConfirmationDialog() {
        Intent dialogIntent = new Intent(this, EmergencyCancelDialogActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);
    }

    private void activatePanicButton() {
        Log.i(TAG, "PANIC BUTTON ACTIVATED VIA VOICE! Notifying contacts and services.");
        new Handler(Looper.getMainLooper()).post(() ->
            Toast.makeText(this, "Emergency command recognized! Alerting contacts.", Toast.LENGTH_LONG).show()
        );

        Intent intent = new Intent(ACTION_VOICE_EMERGENCY);
        intent.putExtra(EXTRA_VOICE_TEXT, "Voice command detected");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userName = (user != null && user.getDisplayName() != null) ?
                user.getDisplayName() : "A user";

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && 
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
             Log.e(TAG, "Location permission not granted.");
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    double latitude = 0, longitude = 0;
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        locationCache.updateLastLocation(latitude, longitude);
                    } else {
                        Location lastLocation = locationCache.getLastKnownLocation();
                        if (lastLocation != null) {
                            latitude = lastLocation.getLatitude();
                            longitude = lastLocation.getLongitude();
                        } else {
                            Log.e(TAG, "No location available to send alerts.");
                        }
                    }
                    sendEmergencyAlerts(latitude, longitude, userName);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting location for alert", e);
                     Location lastLocation = locationCache.getLastKnownLocation();
                        if (lastLocation != null) {
                           sendEmergencyAlerts(lastLocation.getLatitude(), lastLocation.getLongitude(), userName);
                        } else {
                            Log.e(TAG, "No location available to send alerts.");
                        }
                });
    }

    private void sendEmergencyAlerts(double latitude, double longitude, String userName) {
        GeoPoint geoPoint = new GeoPoint(latitude, longitude);

        // 1. Alert Emergency Contacts
        FirebaseHelper.getInstance().getEmergencyContacts(contacts -> {
            if (contacts == null || contacts.isEmpty()) {
                Log.w(TAG, "No emergency contacts found to alert.");
                return;
            }
            for (EmergencyContact contact : contacts) {
                String phoneNumber = contact.getPhone();
                if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                    SMSHelper.sendLocationSMS(this, phoneNumber, latitude, longitude, true);
                    FirebaseHelper.getInstance().getFCMToken(contact.getId(), token -> {
                        if (token != null && !token.isEmpty()) {
                            NotificationHelper.sendFCMNotification(
                                    token, "EMERGENCY ALERT", userName + " needs immediate help!",
                                    latitude, longitude, true
                            );
                        }
                    });
                    FirebaseHelper.getInstance().logEmergencyNotification(contact.getId(), geoPoint);
                }
            }
        });

        // 2. Alert Nearby Emergency Services
        FirebaseHelper.getInstance().getNearbyEmergencyServices("POLICE", geoPoint, NEARBY_RADIUS_KM,
            policeStations -> handleEmergencyServices(policeStations, "Police", latitude, longitude));
        FirebaseHelper.getInstance().getNearbyEmergencyServices("HOSPITAL", geoPoint, NEARBY_RADIUS_KM,
            hospitals -> handleEmergencyServices(hospitals, "Hospital", latitude, longitude));
        FirebaseHelper.getInstance().getNearbyNGOs(geoPoint, NEARBY_RADIUS_KM,
            ngos -> handleEmergencyServices(ngos, "NGO", latitude, longitude));

        // 3. Update user's emergency status
        FirebaseHelper.getInstance().updateUserEmergencyStatus(true, geoPoint, System.currentTimeMillis());
    }

    private void handleEmergencyServices(List<EmergencyServiceModel> services, String type, double latitude, double longitude) {
        if (services.isEmpty()) {
            Log.w(TAG, "No nearby " + type + " services found");
            return;
        }
        Log.i(TAG, "Found " + services.size() + " nearby " + type + " services");
        for (EmergencyServiceModel service : services) {
            String phone = service.getContactPhone();
            if (phone == null || phone.isEmpty()) phone = service.getEmergencyNumber();
            if (phone != null && !phone.isEmpty()) {
                SMSHelper.sendLocationSMS(this, phone, latitude, longitude, true);
            }
        }
    }

    public void startListening() {
        if (speechRecognizer != null && !SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition became unavailable");
            restartSpeechRecognizer();
            return;
        }

        try {
            speechRecognizer.startListening(recognizerIntent);
            Log.d(TAG, "Started listening");
        } catch (Exception e) {
            Log.e(TAG, "Error starting speech recognition: " + e.getMessage());
            restartSpeechRecognizer();
        }
    }

    public void stopListening() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
                Log.d(TAG, "Stopped listening");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping speech recognition: " + e.getMessage());
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service starting");
        isListening = true;
        startListening();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service being destroyed");
        isListening = false;
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (restartHandler != null) {
            restartHandler.removeCallbacksAndMessages(null);
        }
        try {
            unregisterReceiver(emergencyConfirmedReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}