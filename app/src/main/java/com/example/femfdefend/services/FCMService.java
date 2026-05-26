package com.example.femfdefend.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.femfdefend.utils.NotificationHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class FCMService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Store the new token in Firebase for the current user
        com.example.femfdefend.utils.FirebaseHelper.getInstance().updateFCMToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        if (data.isEmpty()) {
            return;
        }

        String title = data.get("title");
        String message = data.get("message");
        double latitude = Double.parseDouble(data.getOrDefault("latitude", "0"));
        double longitude = Double.parseDouble(data.getOrDefault("longitude", "0"));
        boolean isPanic = Boolean.parseBoolean(data.getOrDefault("isPanic", "false"));

        Log.d(TAG, "Message received: " + title + " - " + message);

        if (isPanic) {
            NotificationHelper.sendPanicNotification(this, title, message, latitude, longitude);
        } else {
            NotificationHelper.sendTrackingNotification(this, title, message, latitude, longitude);
        }
    }
} 