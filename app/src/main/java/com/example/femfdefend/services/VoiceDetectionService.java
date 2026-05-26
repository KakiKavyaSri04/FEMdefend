package com.example.femfdefend.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.femfdefend.EmergencyCancelDialogActivity;
import com.example.femfdefend.R;
import com.example.femfdefend.VoiceDetectionActivity;
import com.example.femfdefend.models.EmergencyContact;
import com.example.femfdefend.models.EmergencyServiceModel;
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

import java.util.List;

import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineManager;
import ai.picovoice.porcupine.PorcupineManagerCallback;

public class VoiceDetectionService extends Service {
    private static final String TAG = "VoiceDetectionService";
    private static final String CHANNEL_ID = "voice_detection_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final double NEARBY_RADIUS_KM = 10.0;
    public static final String ACTION_VOICE_EMERGENCY = "com.example.femfdefend.ACTION_VOICE_EMERGENCY";
    public static final String EXTRA_VOICE_TEXT = "com.example.femfdefend.EXTRA_VOICE_TEXT";

    private PowerManager.WakeLock wakeLock;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCache locationCache;
    private PorcupineManager porcupineManager;

    private final PorcupineManagerCallback porcupineManagerCallback = keywordIndex -> {
        Log.i(TAG, "Wake word detected! Keyword index: " + keywordIndex);
        showEmergencyConfirmationDialog();
    };

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCache = LocationCache.getInstance(this);
        acquireWakeLock();
        createNotificationChannel();
        startForegroundService();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                emergencyConfirmedReceiver,
                new IntentFilter("com.example.femfdefend.EMERGENCY_CONFIRMED"),
                Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            registerReceiver(emergencyConfirmedReceiver, new IntentFilter("com.example.femfdefend.EMERGENCY_CONFIRMED"));
        }
    }
    
     @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (checkPermissions()) {
            initializePorcupine();
        } else {
            Log.e(TAG, "Stopping service due to missing RECORD_AUDIO permission.");
            stopSelf();
        }
        return START_STICKY;
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "FEMdefend:VoiceDetectionWakeLock"
            );
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Voice Detection Service",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Running voice detection for emergency phrases");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, VoiceDetectionActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Voice Detection Active")
                .setContentText("Listening for emergency phrases")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void initializePorcupine() {
        // !! IMPORTANT !!
        // 1. Get your AccessKey from Picovoice Console (https://console.picovoice.ai/)
        // 2. Create keyword files (.ppn) for "Help Me" and "Emergency" in the console
        // 3. Add the .ppn files to your project's `assets` directory (`app/src/main/assets`)
        final String accessKey = "DsAaadJIUYp35KOZMQJ9tPCh5/i4FYiWEXsELh2Pder2Yy97cPh3Zw==";

        try {
            porcupineManager = new PorcupineManager.Builder()
                    .setAccessKey(accessKey)
                    .setKeywordPaths(new String[]{"help-me_android.ppn"})
                    .setSensitivity(0.7f)
                    .build(getApplicationContext(), porcupineManagerCallback);
            porcupineManager.start();
            Log.i(TAG, "Porcupine initialized and listening...");
        } catch (PorcupineException e) {
            Log.e(TAG, "Failed to initialize Porcupine: " + e.getMessage());
            handlePorcupineError(e);
        }
    }
    
    private void handlePorcupineError(PorcupineException e) {
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("INVALID_ARGUMENT")) {
                Log.e(TAG, "Error: AccessKey or keyword file is invalid. " +
                    "Please check your AccessKey and ensure the .ppn files are in the assets folder.");
            } else if (message.contains("IO_EXCEPTION")) {
                 Log.e(TAG, "Error: Keyword file not found. " +
                    "Make sure the .ppn files are in `app/src/main/assets`.");
            } else {
                Log.e(TAG, "An unexpected error occurred with Porcupine.", e);
            }
        } else {
            Log.e(TAG, "An unexpected error occurred with Porcupine (null message).", e);
        }
        stopSelf();
    }


    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
               PackageManager.PERMISSION_GRANTED;
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (porcupineManager != null) {
            try {
                porcupineManager.stop();
                porcupineManager.delete();
            } catch (PorcupineException e) {
                Log.e(TAG, "Error stopping Porcupine", e);
            }
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        unregisterReceiver(emergencyConfirmedReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final BroadcastReceiver emergencyConfirmedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            activatePanicButton();
        }
    };

    private void showEmergencyConfirmationDialog() {
        Intent dialogIntent = new Intent(this, EmergencyCancelDialogActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);
        // The dialog will finish itself and you can listen for a broadcast or use another mechanism to trigger activatePanicButton()
    }
}
