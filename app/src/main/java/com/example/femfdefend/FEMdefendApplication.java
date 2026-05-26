package com.example.femfdefend;

import android.app.Application;
import com.example.femfdefend.utils.DatabaseInitializer;
import com.example.femfdefend.utils.NotificationHelper;

public class FEMdefendApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firestore collections
        new DatabaseInitializer().initializeCollections();
        
        // Create notification channels
        NotificationHelper.createNotificationChannels(this);
    }
} 