package com.example.femfdefend.utils;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import java.util.HashMap;
import java.util.Map;

public class DummyDataGenerator {
    private static final String TAG = "DummyDataGenerator";

    public static void populateEmergencyServices(FirebaseFirestore db) {
        // Police Stations
        addPoliceStation(db, "Central Police Station", "123-456-7890", 
            "123 Main St", new GeoPoint(12.9716, 77.5946), true);
        addPoliceStation(db, "North Police Station", "123-456-7891", 
            "456 North Ave", new GeoPoint(12.9850, 77.5938), true);
        addPoliceStation(db, "South Police Station", "123-456-7892", 
            "789 South Rd", new GeoPoint(12.9542, 77.5946), true);
        addPoliceStation(db, "East Police Station", "123-456-7893", 
            "321 East Blvd", new GeoPoint(12.9716, 77.6100), true);
        addPoliceStation(db, "West Police Station", "123-456-7894", 
            "654 West St", new GeoPoint(12.9716, 77.5800), true);

        // Hospitals
        addHospital(db, "City General Hospital", "234-567-8901", 
            "100 Hospital Dr", new GeoPoint(12.9716, 77.5946), true);
        addHospital(db, "North Medical Center", "234-567-8902", 
            "200 Medical Pkwy", new GeoPoint(12.9900, 77.5946), true);
        addHospital(db, "South Community Hospital", "234-567-8903", 
            "300 Health Ave", new GeoPoint(12.9500, 77.5946), true);
        addHospital(db, "East Emergency Hospital", "234-567-8904", 
            "400 Care Rd", new GeoPoint(12.9716, 77.6200), true);
        addHospital(db, "West Medical Institute", "234-567-8905", 
            "500 Wellness Blvd", new GeoPoint(12.9716, 77.5700), true);

        // NGOs
        addNGO(db, "Women's Safety Network", "345-678-9012", 
            "111 NGO St", new GeoPoint(12.9716, 77.5946), false);
        addNGO(db, "Safe Haven Foundation", "345-678-9013", 
            "222 Help Ave", new GeoPoint(12.9800, 77.5946), false);
        addNGO(db, "Emergency Support Group", "345-678-9014", 
            "333 Support Rd", new GeoPoint(12.9600, 77.5946), true);
        addNGO(db, "Crisis Center", "345-678-9015", 
            "444 Crisis Ln", new GeoPoint(12.9716, 77.6050), true);
        addNGO(db, "Women's Help Organization", "345-678-9016", 
            "555 Help St", new GeoPoint(12.9716, 77.5850), false);
    }

    private static void addPoliceStation(FirebaseFirestore db, String name, String phone, 
            String address, GeoPoint location, boolean isOpen24Hours) {
        Map<String, Object> data = createServiceData("POLICE", name, phone, address, location, isOpen24Hours);
        data.put("emergencyNumber", "100");
        addEmergencyService(db, data);
    }

    private static void addHospital(FirebaseFirestore db, String name, String phone, 
            String address, GeoPoint location, boolean isOpen24Hours) {
        Map<String, Object> data = createServiceData("HOSPITAL", name, phone, address, location, isOpen24Hours);
        data.put("emergencyNumber", "102");
        addEmergencyService(db, data);
    }

    private static void addNGO(FirebaseFirestore db, String name, String phone, 
            String address, GeoPoint location, boolean isOpen24Hours) {
        Map<String, Object> data = createServiceData("NGO", name, phone, address, location, isOpen24Hours);
        data.put("website", "www." + name.toLowerCase().replace(" ", "") + ".org");
        data.put("email", "contact@" + name.toLowerCase().replace(" ", "") + ".org");
        addEmergencyService(db, data);
    }

    private static Map<String, Object> createServiceData(String type, String name, String phone, 
            String address, GeoPoint location, boolean isOpen24Hours) {
        Map<String, Object> data = new HashMap<>();
        data.put("serviceType", type);
        data.put("name", name);
        data.put("contactPhone", phone);
        data.put("address", address);
        data.put("location", location);
        data.put("isOpen24Hours", isOpen24Hours);
        return data;
    }

    private static void addEmergencyService(FirebaseFirestore db, Map<String, Object> data) {
        db.collection("emergency_services")
            .add(data)
            .addOnSuccessListener(documentReference -> 
                Log.d(TAG, "Emergency service added with ID: " + documentReference.getId()))
            .addOnFailureListener(e -> 
                Log.e(TAG, "Error adding emergency service", e));
    }
} 