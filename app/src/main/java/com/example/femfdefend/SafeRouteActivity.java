package com.example.femfdefend;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.femfdefend.databinding.ActivitySafeRouteBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.femfdefend.utils.SafeRouteCalculator;

public class SafeRouteActivity extends AppCompatActivity implements OnMapReadyCallback {
    private ActivitySafeRouteBinding binding;
    private GoogleMap map;
    private List<Polyline> currentPolylines = new ArrayList<>();
    private SafeRouteCalculator routeCalculator;
    private Geocoder geocoder;
    private MarkerOptions startMarker;
    private MarkerOptions destinationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySafeRouteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize markers
        startMarker = new MarkerOptions()
                .title("Start Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        
        destinationMarker = new MarkerOptions()
                .title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.find_safe_route);
        }

        // Initialize geocoder
        geocoder = new Geocoder(this, Locale.getDefault());

        // Set up map
        try {
            Log.d("MapDebug", "Setting up map fragment in SafeRouteActivity");
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
                Log.d("MapDebug", "Map fragment setup completed");
            } else {
                Log.e("MapDebug", "Map fragment is null");
                Toast.makeText(this, "Error: Could not initialize map", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("MapDebug", "Error setting up map fragment: " + e.getMessage());
            Toast.makeText(this, "Error setting up map: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        // Initialize route calculator
        routeCalculator = new SafeRouteCalculator(this, new SafeRouteCalculator.OnRouteCalculatedListener() {
            @Override
            public void onRouteCalculated(List<List<LatLng>> routes, List<String> durations, List<String> distances) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    drawRoutes(routes);
                    if (!durations.isEmpty() && !distances.isEmpty()) {
                        showRouteInfo(durations.get(0), distances.get(0));
                    }
                });
            }

            @Override
            public void onRouteError(String error) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(SafeRouteActivity.this, 
                        "Error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });

        // Set up find route button
        binding.btnCalculateRoute.setOnClickListener(v -> calculateRoute());
    }

    private void calculateRoute() {
        String startLocation = "";
        String endLocation = "";
        
        if (binding.startLocationInput.getText() != null) {
            startLocation = binding.startLocationInput.getText().toString().trim();
        }
        if (binding.endLocationInput.getText() != null) {
            endLocation = binding.endLocationInput.getText().toString().trim();
        }

        if (startLocation.isEmpty() || endLocation.isEmpty()) {
            Toast.makeText(this, "Please enter both locations", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        // Convert addresses to coordinates
        try {
            LatLng startPoint = getLocationFromAddress(startLocation);
            LatLng endPoint = getLocationFromAddress(endLocation);

            if (startPoint == null || endPoint == null) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Could not find one or both locations", Toast.LENGTH_SHORT).show();
                return;
            }

            // Add markers with custom icons
            map.clear();
            startMarker.position(startPoint);
            destinationMarker.position(endPoint);
            
            map.addMarker(startMarker);
            map.addMarker(destinationMarker);

            // Show both markers in view
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(startPoint);
            builder.include(endPoint);
            LatLngBounds bounds = builder.build();
            
            // Add padding to the bounds
            int padding = 100;
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));

            // Calculate route
            routeCalculator.calculateRoute(startPoint, endPoint);

        } catch (IOException e) {
            binding.progressBar.setVisibility(View.GONE);
            Log.e("MapDebug", "Error finding location: " + e.getMessage());
            Toast.makeText(this, "Error finding location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private LatLng getLocationFromAddress(String address) throws IOException {
        List<Address> addresses = geocoder.getFromLocationName(address, 1);
        if (addresses != null && !addresses.isEmpty()) {
            Address location = addresses.get(0);
            return new LatLng(location.getLatitude(), location.getLongitude());
        }
        return null;
    }

    private void drawRoutes(List<List<LatLng>> routes) {
        // Clear previous routes
        for (Polyline polyline : currentPolylines) {
            polyline.remove();
        }
        currentPolylines.clear();

        if (routes.isEmpty()) return;

        // Draw the primary "safest" route (the first one in the sorted list) in green
        PolylineOptions primaryRouteOptions = new PolylineOptions()
                .addAll(routes.get(0))
                .width(15f)
                .color(Color.GREEN)
                .zIndex(1); // Make it draw on top
        currentPolylines.add(map.addPolyline(primaryRouteOptions));

        // Draw all other routes in red
        for (int i = 1; i < routes.size(); i++) {
            PolylineOptions alternativeRouteOptions = new PolylineOptions()
                    .addAll(routes.get(i))
                    .width(10f)
                    .color(Color.RED);
            currentPolylines.add(map.addPolyline(alternativeRouteOptions));
        }

        // Adjust camera to show all routes
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (List<LatLng> route : routes) {
            for (LatLng point : route) {
                builder.include(point);
            }
        }
        // Also include markers
        builder.include(startMarker.getPosition());
        builder.include(destinationMarker.getPosition());

        final LatLngBounds bounds = builder.build();
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100)); // 100px padding
    }

    private void showRouteInfo(String duration, String distance) {
        if (duration != null && distance != null) {
            String info = String.format("Duration: %s\nDistance: %s", duration, distance);
            binding.routeInfoTextView.setText(info);
            binding.routeInfoCard.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        try {
            this.map = googleMap;
            Log.d("MapDebug", "Map instance created in SafeRouteActivity");
            
            // Set map settings
            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            map.getUiSettings().setZoomControlsEnabled(true);
            map.getUiSettings().setCompassEnabled(true);
            map.getUiSettings().setMapToolbarEnabled(true);
            map.getUiSettings().setZoomGesturesEnabled(true);
            map.getUiSettings().setScrollGesturesEnabled(true);
            map.getUiSettings().setTiltGesturesEnabled(true);
            map.getUiSettings().setRotateGesturesEnabled(true);
            
            // Set default camera position to India
            LatLng defaultLocation = new LatLng(20.5937, 78.9629); // India center coordinates
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 5f));
            Log.d("MapDebug", "Map camera set to default location");
            
        } catch (Exception e) {
            Log.e("MapDebug", "Error initializing map in SafeRouteActivity: " + e.getMessage());
            Toast.makeText(this, "Error initializing map: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (Polyline polyline : currentPolylines) {
            polyline.remove();
        }
    }
} 