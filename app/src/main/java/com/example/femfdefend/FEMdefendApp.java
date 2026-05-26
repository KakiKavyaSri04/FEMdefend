package com.example.femfdefend;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class FEMdefendApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
} 