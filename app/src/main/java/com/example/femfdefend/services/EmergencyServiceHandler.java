package com.example.femfdefend.services;

import android.content.Context;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.example.femfdefend.models.EmergencyService;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmergencyServiceHandler {
    private static final String TAG = "EmergencyServiceHandler";
    private final Context context;
    private final FirebaseFirestore db;
    private final Map<String, List<EmergencyService>> emergencyServices;

    public EmergencyServiceHandler(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.emergencyServices = new HashMap<>();
        initializeEmergencyServices();
    }

    private void initializeEmergencyServices() {
        // Fetch emergency services from Firestore
        db.collection("emergency_services")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    queryDocumentSnapshots.forEach(document -> {
                        EmergencyService service = document.toObject(EmergencyService.class);
                        emergencyServices
                            .computeIfAbsent(service.getType(), k -> new ArrayList<>())
                            .add(service);
                    });
                    Log.i(TAG, "Emergency services loaded successfully");
                } else {
                    Log.w(TAG, "No emergency services found in database");
                    // Add default emergency services if none found
                    addDefaultEmergencyServices();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading emergency services: " + e.getMessage());
                // Add default emergency services on failure
                addDefaultEmergencyServices();
            });
    }

    private void addDefaultEmergencyServices() {
        // Police
        List<EmergencyService> policeServices = new ArrayList<>();
        policeServices.add(new EmergencyService("Police Emergency", "100", "POLICE"));
        policeServices.add(new EmergencyService("Women Police Helpline", "1091", "POLICE"));
        emergencyServices.put("POLICE", policeServices);

        // Hospitals
        List<EmergencyService> hospitalServices = new ArrayList<>();
        hospitalServices.add(new EmergencyService("Ambulance", "102", "HOSPITAL"));
        hospitalServices.add(new EmergencyService("Emergency Medical", "108", "HOSPITAL"));
        emergencyServices.put("HOSPITAL", hospitalServices);

        // NGOs
        List<EmergencyService> ngoServices = new ArrayList<>();
        ngoServices.add(new EmergencyService("Women Helpline", "1091", "NGO"));
        ngoServices.add(new EmergencyService("Women in Distress", "181", "NGO"));
        emergencyServices.put("NGO", ngoServices);
    }

    public void handleEmergency(String userName, double latitude, double longitude) {
        Log.i(TAG, "Handling emergency for user: " + userName);
        String googleMapsLink = "https://www.google.com/maps?q=" + latitude + "," + longitude;
        
        // Send to all emergency services
        for (Map.Entry<String, List<EmergencyService>> entry : emergencyServices.entrySet()) {
            String serviceType = entry.getKey();
            List<EmergencyService> services = entry.getValue();
            
            for (EmergencyService service : services) {
                sendEmergencyMessage(service, userName, googleMapsLink);
            }
        }
    }

    private void sendEmergencyMessage(EmergencyService service, String userName, String locationLink) {
        String message = buildEmergencyMessage(service.getType(), userName, locationLink);
        String phoneNumber = formatPhoneNumber(service.getPhoneNumber());
        
        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            
            if (parts.size() > 1) {
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    parts,
                    null,
                    null
                );
            } else {
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    null,
                    null
                );
            }
            
            Log.i(TAG, "Emergency SMS sent to " + service.getName() + " (" + phoneNumber + ")");
            Toast.makeText(context, 
                "Alert sent to " + service.getName(), 
                Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS to " + service.getName() + ": " + e.getMessage());
            Toast.makeText(context, 
                "Failed to send alert to " + service.getName(), 
                Toast.LENGTH_SHORT).show();
        }
    }

    private String buildEmergencyMessage(String serviceType, String userName, String locationLink) {
        StringBuilder message = new StringBuilder();
        message.append("EMERGENCY ALERT! ");
        message.append(userName).append(" needs immediate help!\n");

        switch (serviceType) {
            case "POLICE" -> message.append("Requesting immediate police assistance.\n");
            case "HOSPITAL" -> message.append("Medical emergency - immediate assistance needed.\n");
            case "NGO" -> message.append("Woman in distress - immediate support needed.\n");
        }
        
        message.append("Location: ").append(locationLink);
        return message.toString();
    }

    private String formatPhoneNumber(String phoneNumber) {
        // Remove any non-digit characters
        String cleaned = phoneNumber.replaceAll("\\D", "");
        
        // For Indian numbers
        if (cleaned.length() == 10) {
            cleaned = "+91" + cleaned;
        }
        // For emergency short codes (like 100, 102, 108)
        else if (cleaned.length() <= 4) {
            return cleaned; // Return as is for short emergency numbers
        }
        // For numbers with country code
        else if (!cleaned.startsWith("+")) {
            cleaned = "+" + cleaned;
        }
        
        return cleaned;
    }
}
