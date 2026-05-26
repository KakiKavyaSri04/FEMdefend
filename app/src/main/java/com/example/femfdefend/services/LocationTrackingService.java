package com.example.femfdefend.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.femfdefend.LiveTrackingActivity;
import com.example.femfdefend.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.example.femfdefend.utils.FirebaseHelper;
import com.google.firebase.firestore.GeoPoint;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class LocationTrackingService extends Service {
    private static final String CHANNEL_ID = "location_tracking_channel";
    private static final int NOTIFICATION_ID = 1;
    
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseHelper firebaseHelper;
    private List<LatLng> routePath;
    private int nextRoutePointIndex = 0;
    private static final float ROUTE_POINT_REACHED_THRESHOLD = 20; // meters

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firebaseHelper = FirebaseHelper.getInstance();
        
        createNotificationChannel();
        setupLocationCallback();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Used for tracking your location");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    handleLocationUpdate(location);
                }
            }
        };
    }

    private void handleLocationUpdate(Location location) {
        // Update Firebase with current location
        firebaseHelper.updateUserLocation(new GeoPoint(
            location.getLatitude(), 
            location.getLongitude()
        ));

        // Check if following route
        if (routePath != null && nextRoutePointIndex < routePath.size()) {
            LatLng nextPoint = routePath.get(nextRoutePointIndex);
            float[] results = new float[1];
            Location.distanceBetween(
                location.getLatitude(), location.getLongitude(),
                nextPoint.latitude, nextPoint.longitude,
                results
            );

            // If reached next point
            if (results[0] < ROUTE_POINT_REACHED_THRESHOLD) {
                nextRoutePointIndex++;
                if (nextRoutePointIndex >= routePath.size()) {
                    // Reached destination
                    sendBroadcast(new Intent("com.example.femfdefend.DESTINATION_REACHED"));
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("ROUTE_PATH")) {
            routePath = intent.getParcelableArrayListExtra("ROUTE_PATH");
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                buildNotification(), 
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            );
        } else {
            startForeground(NOTIFICATION_ID, buildNotification());
        }
        startLocationUpdates();
        return START_STICKY;
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, LiveTrackingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_active))
            .setContentText(getString(R.string.tracking_notification_text))
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .build();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(10000) // 10 seconds
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMinUpdateDistanceMeters(10) // 10 meters
            .build();

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            );
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
