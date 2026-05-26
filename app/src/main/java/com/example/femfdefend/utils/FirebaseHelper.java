package com.example.femfdefend.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.femfdefend.models.EmergencyContact;
import com.example.femfdefend.models.EmergencyServiceModel;
import com.example.femfdefend.models.User;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private static final String USERS_REF = "users";
    private static final String EMERGENCY_CONTACTS_REF = "emergency_contacts";
    private static final String LOCATION_FIELD = "lastLocation";
    private static final String TRACKING_FIELD = "isTracking";
    private static final String EMERGENCY_NOTIFICATIONS_REF = "emergency_notifications";
    private static final String COLLECTION_FCM_TOKENS = "fcm_tokens";
    
    private static FirebaseHelper instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    
    private FirebaseHelper() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }
    
    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }
    
    public FirebaseAuth getAuth() {
        return auth;
    }
    
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }
    
    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    public DocumentReference getCurrentUserRef() {
        String userId = getCurrentUserId();
        return userId != null ? db.collection(USERS_REF).document(userId) : null;
    }
    
    public Task<Void> saveUser(User user) {
        DocumentReference userRef = getCurrentUserRef();
        if (userRef == null) {
            return null;
        }
        return userRef.set(user);
    }
    
    public CollectionReference getEmergencyContactsRef() {
        if (getCurrentUser() == null) {
            return null;
        }
        return getCurrentUserRef().collection(EMERGENCY_CONTACTS_REF);
    }
    
    public Task<Void> addEmergencyContact(EmergencyContact contact) {
        Log.d(TAG, "Adding emergency contact: " + contact.getName());
        
        if (getCurrentUser() == null) {
            Log.e(TAG, "Cannot add emergency contact: User not logged in");
            return Tasks.forException(new Exception("User not logged in"));
        }

        if (contact.getPhone() == null || contact.getPhone().trim().isEmpty()) {
            Log.e(TAG, "Cannot add emergency contact: Phone number is empty");
            return Tasks.forException(new Exception("Phone number cannot be empty"));
        }

        return getCurrentUserRef()
                .collection(EMERGENCY_CONTACTS_REF)
                .document(contact.getId())
                .set(contact)
                .addOnSuccessListener(aVoid -> 
                    Log.i(TAG, "Successfully added emergency contact: " + contact.getName()))
                .addOnFailureListener(e -> 
                    Log.e(TAG, "Failed to add emergency contact: " + e.getMessage(), e));
    }
    
    /**
     * Deletes an emergency contact by its ID
     * @param contactId The ID of the contact to delete
     * @return A Task that completes when the deletion is done
     */
    public Task<Void> deleteEmergencyContact(String contactId) {
        CollectionReference contactsRef = getEmergencyContactsRef();
        if (contactsRef == null) {
            return null;
        }
        return contactsRef.document(contactId).delete();
    }
    
    public void signOut() {
        auth.signOut();
    }
    
    /**
     * Updates the user's location in Firestore
     * @param location The GeoPoint representing the user's current location
     * @return A Task that completes when the update is done
     */
    public Task<Void> updateUserLocation(@NonNull GeoPoint location) {
        DocumentReference userRef = getCurrentUserRef();
        if (userRef == null) {
            return null;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(LOCATION_FIELD, location);
        updates.put(TRACKING_FIELD, true);
        
        return userRef.update(updates);
    }
    
    public void getEmergencyContacts(OnEmergencyContactsLoadedListener listener) {
        Log.d(TAG, "Getting emergency contacts");
        CollectionReference contactsRef = getEmergencyContactsRef();
        if (contactsRef == null) {
            Log.e(TAG, "Cannot get emergency contacts: User not logged in");
            listener.onContactsLoaded(new ArrayList<>());
            return;
        }

        contactsRef
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<EmergencyContact> contacts = new ArrayList<>();
                Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " emergency contacts");
                
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        EmergencyContact contact = document.toObject(EmergencyContact.class);
                        if (contact != null) {
                            contact.setId(document.getId());
                            Log.d(TAG, "Processing contact: " + contact.getName() + ", Phone: " + contact.getPhone());
                            contacts.add(contact);
                        } else {
                            Log.w(TAG, "Null contact object from document: " + document.getId());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error converting document to contact: " + e.getMessage(), e);
                    }
                }
                
                if (contacts.isEmpty()) {
                    Log.w(TAG, "No emergency contacts found in database");
                } else {
                    Log.i(TAG, "Successfully loaded " + contacts.size() + " emergency contacts");
                }
                
                listener.onContactsLoaded(contacts);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading emergency contacts: " + e.getMessage(), e);
                listener.onContactsLoaded(new ArrayList<>());
            });
    }
    
    public interface OnEmergencyContactsLoadedListener {
        void onContactsLoaded(List<EmergencyContact> contacts);
    }
    
    public void updateUserEmergencyStatus(boolean isEmergency, GeoPoint location, long timestamp) {
        if (getCurrentUser() == null) return;

        String userId = getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("isEmergency", isEmergency);
        updates.put("lastLocation", location);
        updates.put("lastEmergencyTime", timestamp);
        
        db.collection("users").document(userId)
            .update(updates)
            .addOnFailureListener(e -> Log.e(TAG, "Error updating emergency status: " + e.getMessage()));
    }
    
    /**
     * Logs an emergency notification
     * @param contactId The ID of the emergency contact
     * @param location The location where the emergency was triggered
     */
    public void logEmergencyNotification(String contactId, GeoPoint location) {
        if (getCurrentUser() == null) return;

        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", getCurrentUserId());
        notification.put("contactId", contactId);
        notification.put("location", location);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("status", "SENT");

        db.collection(EMERGENCY_NOTIFICATIONS_REF)
            .add(notification)
            .addOnSuccessListener(documentReference -> Log.d(TAG, "Emergency notification logged with ID: " + documentReference.getId()))
            .addOnFailureListener(e -> Log.e(TAG, "Error logging emergency notification: " + e.getMessage()));
    }
    
    public void getNearbyNGOs(GeoPoint center, double radiusKm, OnSuccessListener<List<EmergencyServiceModel>> listener) {
        double lat = center.getLatitude();
        double latChange = radiusKm / 111.0; // 1 degree lat = ~111km
        
        db.collection("emergency_services")
            .whereEqualTo("type", "NGO")
            .whereGreaterThan("latitude", lat - latChange)
            .whereLessThan("latitude", lat + latChange)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<EmergencyServiceModel> nearbyNGOs = new ArrayList<>();
                
                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    EmergencyServiceModel ngo = document.toObject(EmergencyServiceModel.class);
                    if (ngo != null && isWithinRadius(center, ngo.getLocation(), radiusKm)) {
                        nearbyNGOs.add(ngo);
                    }
                }
                
                listener.onSuccess(nearbyNGOs);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting nearby NGOs: " + e.getMessage());
                listener.onSuccess(new ArrayList<>());
            });
    }

    private boolean isWithinRadius(GeoPoint center, GeoPoint point, double radiusKm) {
        double lat1 = center.getLatitude();
        double lon1 = center.getLongitude();
        double lat2 = point.getLatitude();
        double lon2 = point.getLongitude();
        
        // Haversine formula
        double R = 6371; // Earth's radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        return (R * c) <= radiusKm;
    }

    /**
     * Gets emergency services of a specific type within a radius
     * @param serviceType The type of service (POLICE, HOSPITAL, etc.)
     * @param center The center point to search from
     * @param radiusKm The radius in kilometers
     * @param listener Callback for the results
     */
    public void getNearbyEmergencyServices(
        String serviceType,
        GeoPoint center,
        double radiusKm,
        OnSuccessListener<List<EmergencyServiceModel>> listener
    ) {
        double lat = center.getLatitude();
        double latChange = radiusKm / 111.0; // 1 degree lat = ~111km
        
        db.collection("emergency_services")
            .whereEqualTo("serviceType", serviceType)
            .whereGreaterThan("latitude", lat - latChange)
            .whereLessThan("latitude", lat + latChange)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<EmergencyServiceModel> services = new ArrayList<>();
                
                queryDocumentSnapshots.getDocuments().forEach(document -> {
                    EmergencyServiceModel service = document.toObject(EmergencyServiceModel.class);
                    if (service != null && isWithinRadius(center, service.getLocation(), radiusKm)) {
                        service.setId(document.getId());
                        services.add(service);
                    }
                });
                
                listener.onSuccess(services);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting nearby emergency services: " + e.getMessage());
                listener.onSuccess(new ArrayList<>()); // Return empty list on failure
            });
    }

    public void updateFCMToken(String token) {
        if (getCurrentUser() != null) {
            db.collection(COLLECTION_FCM_TOKENS)
                    .document(getCurrentUser().getUid())
                    .set(new HashMap<String, Object>() {{
                        put("token", token);
                        put("updatedAt", System.currentTimeMillis());
                    }})
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating FCM token", e));
        }
    }

    public void getFCMToken(String userId, OnTokenRetrievedListener listener) {
        db.collection(COLLECTION_FCM_TOKENS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String token = documentSnapshot.getString("token");
                        listener.onTokenRetrieved(token);
                    } else {
                        listener.onTokenRetrieved(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting FCM token", e);
                    listener.onTokenRetrieved(null);
                });
    }

    public void sendFCMNotification(String userToken, Map<String, String> data) {
        if (userToken == null || userToken.isEmpty()) {
            Log.e(TAG, "Cannot send notification: invalid token");
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("token", userToken);
        message.put("data", data);

        db.collection("notifications")
                .add(message)
                .addOnSuccessListener(documentReference -> 
                    Log.d(TAG, "Notification sent successfully"))
                .addOnFailureListener(e -> 
                    Log.e(TAG, "Error sending notification", e));
    }

    public interface OnTokenRetrievedListener {
        void onTokenRetrieved(String token);
    }

    public interface OnUserDataLoadedListener {
        void onUserDataLoaded(User user);
    }

    public void getCurrentUserData(OnUserDataLoadedListener listener) {
        DocumentReference userRef = getCurrentUserRef();
        if (userRef == null) {
            listener.onUserDataLoaded(null);
            return;
        }

        userRef.get()
            .addOnSuccessListener(documentSnapshot -> {
                User user = documentSnapshot.toObject(User.class);
                listener.onUserDataLoaded(user);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting user data: " + e.getMessage());
                listener.onUserDataLoaded(null);
            });
    }
}