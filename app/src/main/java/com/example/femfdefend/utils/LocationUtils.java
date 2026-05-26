package com.example.femfdefend.utils;

import com.google.firebase.firestore.GeoPoint;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import com.example.femfdefend.models.EmergencyServiceModel;

public class LocationUtils {
    private static final double EARTH_RADIUS_KM = 6371.0;

    public static double calculateDistance(GeoPoint point1, GeoPoint point2) {
        double lat1 = Math.toRadians(point1.getLatitude());
        double lon1 = Math.toRadians(point1.getLongitude());
        double lat2 = Math.toRadians(point2.getLatitude());
        double lon2 = Math.toRadians(point2.getLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dLon/2) * Math.sin(dLon/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return EARTH_RADIUS_KM * c;
    }

    public static List<EmergencyServiceModel> sortServicesByDistance(
            List<EmergencyServiceModel> services, 
            final GeoPoint userLocation) {
        
        List<EmergencyServiceModel> sortedServices = new ArrayList<>(services);
        Collections.sort(sortedServices, (s1, s2) -> {
            double dist1 = calculateDistance(userLocation, s1.getLocation());
            double dist2 = calculateDistance(userLocation, s2.getLocation());
            return Double.compare(dist1, dist2);
        });
        return sortedServices;
    }

    public static List<EmergencyServiceModel> filterServicesByRadius(
            List<EmergencyServiceModel> services,
            GeoPoint center,
            double radiusKm) {
        
        List<EmergencyServiceModel> nearbyServices = new ArrayList<>();
        for (EmergencyServiceModel service : services) {
            if (calculateDistance(center, service.getLocation()) <= radiusKm) {
                nearbyServices.add(service);
            }
        }
        return nearbyServices;
    }
} 