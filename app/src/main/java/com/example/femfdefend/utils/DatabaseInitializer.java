package com.example.femfdefend.utils;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;
import java.util.Map;

public class DatabaseInitializer {
    private static final String TAG = "DatabaseInitializer";
    private final FirebaseFirestore db;

    public DatabaseInitializer() {
        db = FirebaseFirestore.getInstance();
    }

    public void initializeCollections() {
        // Initialize required collections with a dummy document
        initializeCollection("users");
        initializeCollection("emergency_services");
        initializeCollection("emergency_notifications");
        
        Log.i(TAG, "Database collections initialization started");
    }

    private void initializeCollection(String collectionName) {
        CollectionReference collectionRef = db.collection(collectionName);
        
        // Create a temporary document to initialize the collection
        Map<String, Object> dummyData = new HashMap<>();
        dummyData.put("initialized", true);
        dummyData.put("timestamp", System.currentTimeMillis());
        
        DocumentReference dummyDoc = collectionRef.document("init_doc");
        dummyDoc.set(dummyData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Collection " + collectionName + " initialized");
                // Delete the dummy document after initialization
                dummyDoc.delete()
                    .addOnSuccessListener(aVoid2 -> Log.d(TAG, "Dummy document deleted from " + collectionName))
                    .addOnFailureListener(e -> Log.w(TAG, "Error deleting dummy document from " + collectionName, e));
            })
            .addOnFailureListener(e -> Log.e(TAG, "Error initializing collection " + collectionName, e));
    }
} 