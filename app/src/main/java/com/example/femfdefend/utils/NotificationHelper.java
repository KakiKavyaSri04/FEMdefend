package com.example.femfdefend.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.femfdefend.LiveTrackingActivity;
import com.example.femfdefend.R;

import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID_TRACKING = "tracking_channel";
    private static final String CHANNEL_ID_PANIC = "panic_channel";
    private static final int NOTIFICATION_ID_TRACKING = 1;
    private static final int NOTIFICATION_ID_PANIC = 2;

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

            // Create tracking channel
            NotificationChannel trackingChannel = new NotificationChannel(
                    CHANNEL_ID_TRACKING,
                    "Live Tracking",
                    NotificationManager.IMPORTANCE_HIGH);
            trackingChannel.setDescription("Notifications for live location tracking");
            
            // Create panic button channel with high importance and special settings
            NotificationChannel panicChannel = new NotificationChannel(
                    CHANNEL_ID_PANIC,
                    "Emergency Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            panicChannel.setDescription("Emergency panic button alerts");
            panicChannel.enableVibration(true);
            panicChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            panicChannel.setBypassDnd(true); // Bypass Do Not Disturb
            panicChannel.setShowBadge(true); // Show badge on app icon
            
            try {
                notificationManager.createNotificationChannel(trackingChannel);
                notificationManager.createNotificationChannel(panicChannel);
                Log.i(TAG, "Notification channels created successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channels: " + e.getMessage());
            }
        }
    }

    public static void sendTrackingNotification(Context context, String title, String message, double latitude, double longitude) {
        Intent intent = new Intent(context, LiveTrackingActivity.class);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        intent.putExtra("isPanic", false);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_TRACKING)
                .setSmallIcon(R.drawable.ic_notification_location)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NOTIFICATION_ID_TRACKING, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Error sending tracking notification: " + e.getMessage());
        }
    }

    public static void sendPanicNotification(Context context, String title, String message, double latitude, double longitude) {
        Log.d(TAG, "Preparing to send panic notification");
        
        // Create intent for notification tap action
        Intent intent = new Intent(context, LiveTrackingActivity.class);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        intent.putExtra("isPanic", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            1, 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification with high priority settings
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_PANIC)
                .setSmallIcon(R.drawable.ic_notification_emergency)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 1000, 500, 1000})
                .setOngoing(true) // Make it persistent until user interacts
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC); // Show on lock screen

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NOTIFICATION_ID_PANIC, builder.build());
            Log.i(TAG, "Panic notification sent successfully");
        } catch (SecurityException e) {
            Log.e(TAG, "Error sending notification: " + e.getMessage());
        }
    }

    public static void sendFCMNotification(String userToken, String title, String message, 
                                         double latitude, double longitude, boolean isPanic) {
        Map<String, String> data = new HashMap<>();
        data.put("title", title);
        data.put("message", message);
        data.put("latitude", String.valueOf(latitude));
        data.put("longitude", String.valueOf(longitude));
        data.put("isPanic", String.valueOf(isPanic));

        FirebaseHelper.getInstance().sendFCMNotification(userToken, data);
    }
} 