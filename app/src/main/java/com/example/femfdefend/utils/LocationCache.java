package com.example.femfdefend.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

public class LocationCache {
    private static final String TAG = "LocationCache";
    private static final String PREFS_NAME = "LocationCache";
    private static final String KEY_LAST_LATITUDE = "last_latitude";
    private static final String KEY_LAST_LONGITUDE = "last_longitude";
    private static final String KEY_LAST_UPDATE_TIME = "last_update_time";
    
    private static LocationCache instance;
    private final SharedPreferences prefs;
    
    private LocationCache(Context context) {
        prefs = context.getApplicationContext()
                      .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized LocationCache getInstance(Context context) {
        if (instance == null) {
            instance = new LocationCache(context);
        }
        return instance;
    }
    
    public void updateLastLocation(double latitude, double longitude) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(KEY_LAST_LATITUDE, (float) latitude);
        editor.putFloat(KEY_LAST_LONGITUDE, (float) longitude);
        editor.putLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis());
        editor.apply();
        
        Log.d(TAG, "Updated last known location: " + latitude + ", " + longitude);
    }
    
    public Location getLastKnownLocation() {
        float latitude = prefs.getFloat(KEY_LAST_LATITUDE, 0);
        float longitude = prefs.getFloat(KEY_LAST_LONGITUDE, 0);
        long lastUpdateTime = prefs.getLong(KEY_LAST_UPDATE_TIME, 0);
        
        if (lastUpdateTime == 0) {
            Log.d(TAG, "No last known location available");
            return null;
        }
        
        Location location = new Location("cache");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setTime(lastUpdateTime);
        
        Log.d(TAG, "Retrieved last known location: " + latitude + ", " + longitude);
        return location;
    }
    
    public long getLastUpdateTime() {
        return prefs.getLong(KEY_LAST_UPDATE_TIME, 0);
    }
    
    public void clear() {
        prefs.edit().clear().apply();
        Log.d(TAG, "Location cache cleared");
    }
} 