package com.example.femfdefend.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.femfdefend.BuildConfig;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TravelMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SafeRouteCalculator {
    private static final String TAG = "SafeRouteCalculator";
    private final OnRouteCalculatedListener listener;

    public interface OnRouteCalculatedListener {
        void onRouteCalculated(List<List<LatLng>> routes, List<String> durations, List<String> distances);
        void onRouteError(String error);
    }

    public SafeRouteCalculator(Context context, OnRouteCalculatedListener listener) {
        this.listener = listener;
    }

    public void calculateRoute(LatLng origin, LatLng destination) {
        new CalculateRouteTask(listener).execute(origin, destination);
    }

    private static class CalculateRouteTask extends AsyncTask<LatLng, Void, List<RouteInfo>> {
        private final OnRouteCalculatedListener listener;

        CalculateRouteTask(OnRouteCalculatedListener listener) {
            this.listener = listener;
        }

        @Override
        protected List<RouteInfo> doInBackground(LatLng... points) {
            final String DIRECTIONS_API_KEY = BuildConfig.MAPS_API_KEY;
            try {
                GeoApiContext geoApiContext = new GeoApiContext.Builder()
                    .apiKey(DIRECTIONS_API_KEY)
                    .build();

                DirectionsResult result = DirectionsApi.newRequest(geoApiContext)
                    .origin(new com.google.maps.model.LatLng(points[0].latitude, points[0].longitude))
                    .destination(new com.google.maps.model.LatLng(points[1].latitude, points[1].longitude))
                    .mode(TravelMode.WALKING)
                    .alternatives(true)
                    .await();

                if (result != null && result.routes.length > 0) {
                    List<RouteInfo> routes = new ArrayList<>();
                    int limit = Math.min(result.routes.length, 2);
                    for (int i = 0; i < limit; i++) {
                        DirectionsRoute route = result.routes[i];
                        List<LatLng> path = new ArrayList<>();
                        for (com.google.maps.model.LatLng point : route.overviewPolyline.decodePath()) {
                            path.add(new LatLng(point.lat, point.lng));
                        }
                        routes.add(new RouteInfo(path, route.legs[0].duration.humanReadable, route.legs[0].distance.humanReadable, route.legs[0].distance.inMeters));
                    }
                    return routes;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating route: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<RouteInfo> routes) {
            if (routes != null && !routes.isEmpty()) {
                // Sort routes by distance (shortest first)
                routes.sort(Comparator.comparingLong(RouteInfo::distanceInMeters));

                List<List<LatLng>> sortedRoutes = new ArrayList<>();
                List<String> sortedDurations = new ArrayList<>();
                List<String> sortedDistances = new ArrayList<>();
                for (RouteInfo route : routes) {
                    sortedRoutes.add(route.path());
                    sortedDurations.add(route.duration());
                    sortedDistances.add(route.distance());
                }
                listener.onRouteCalculated(sortedRoutes, sortedDurations, sortedDistances);
            } else {
                listener.onRouteError("Could not calculate any routes");
            }
        }
    }

    /**
     * Helper class to hold route info, including the exact distance for sorting.
     */
    private static class RouteInfo {
        private final List<LatLng> path;
        private final String duration;
        private final String distance;
        private final long distanceInMeters;

        RouteInfo(List<LatLng> path, String duration, String distance, long distanceInMeters) {
            this.path = path;
            this.duration = duration;
            this.distance = distance;
            this.distanceInMeters = distanceInMeters;
        }

        public List<LatLng> path() {
            return path;
        }

        public String duration() {
            return duration;
        }

        public String distance() {
            return distance;
        }

        public long distanceInMeters() {
            return distanceInMeters;
        }
    }
} 