package com.example.femfdefend;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.femfdefend.adapters.EmergencyContactsAdapter;
import com.example.femfdefend.databinding.ActivityLiveTrackingBinding;
import com.example.femfdefend.models.EmergencyContact;
import com.example.femfdefend.services.LocationTrackingService;
import com.example.femfdefend.utils.FirebaseHelper;
import com.example.femfdefend.utils.NotificationHelper;
import com.example.femfdefend.utils.SMSHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class LiveTrackingActivity extends AppCompatActivity implements OnMapReadyCallback {
    private ActivityLiveTrackingBinding binding;
    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isTracking = false;
    private final List<EmergencyContact> selectedContacts = new ArrayList<>();
    private MarkerOptions currentLocationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLiveTrackingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize marker options
        currentLocationMarker = new MarkerOptions()
                .title("My Current Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        // Request location permission at startup
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.live_tracking);
        }

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set up Google Maps
        setupMap();
        setupButtons();
        setupBackPressHandler();
    }

    private void setupMap() {
        try {
            Log.d("MapDebug", "Setting up map fragment in LiveTrackingActivity");
            SupportMapFragment mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.map, mapFragment)
                    .commit();
            mapFragment.getMapAsync(this);
            Log.d("MapDebug", "Map fragment setup completed");
        } catch (Exception e) {
            Log.e("MapDebug", "Error setting up map fragment: " + e.getMessage());
            Toast.makeText(this, "Error setting up map: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupButtons() {
        binding.btnStartTracking.setOnClickListener(v -> showContactSelectionDialog());
        binding.btnFindSafeRoute.setOnClickListener(v -> {
            Intent intent = new Intent(this, SafeRouteActivity.class);
            startActivity(intent);
        });
    }

    private void showContactSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_contacts, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.contactsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Clear previous selections when showing the dialog
        selectedContacts.clear();

        FirebaseHelper.getInstance().getEmergencyContacts(contacts -> {
            EmergencyContactsAdapter adapter = new EmergencyContactsAdapter(contacts, selectedContacts);
            recyclerView.setAdapter(adapter);

            builder.setView(dialogView)
                    .setTitle(R.string.select_contacts)
                    .setPositiveButton(R.string.start_tracking, (dialog, which) -> {
                        // Get the latest selected contacts from the adapter
                        if (selectedContacts.isEmpty()) {
                            Toast.makeText(this, R.string.error_no_contacts_selected, Toast.LENGTH_SHORT).show();
                            // Show the dialog again
                            showContactSelectionDialog();
                            return;
                        }
                        startTracking();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
    }

    private void startTracking() {
        isTracking = true;
        binding.btnStartTracking.setText(R.string.stop_tracking);
        
        // Get current location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    
                    // Send initial location to all selected contacts
                    for (EmergencyContact contact : selectedContacts) {
                        // Send FCM notification
                        FirebaseHelper.getInstance().getFCMToken(contact.getId(), token -> {
                            if (token != null) {
                                NotificationHelper.sendFCMNotification(
                                    token,
                                    "Location Update",
                                    "Your contact has started sharing their location",
                                    latitude,
                                    longitude,
                                    false
                                );
                            }
                        });
                        
                        // Send SMS with location link
                        SMSHelper.sendLocationSMS(this, contact.getPhone(), latitude, longitude, false);
                    }
                }
            });
        }
        
        // Start location tracking service
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        startService(serviceIntent);
        
        // Start periodic location updates
        startLocationUpdates();
        
        Toast.makeText(this, R.string.success_tracking_started, Toast.LENGTH_SHORT).show();
    }

    private void stopTracking() {
        isTracking = false;
        binding.btnStartTracking.setText(R.string.start_tracking);
        
        // Stop location tracking service
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        stopService(serviceIntent);
        
        selectedContacts.clear();
        Toast.makeText(this, R.string.success_tracking_stopped, Toast.LENGTH_SHORT).show();
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isTracking) {
                    showStopTrackingDialog();
                } else {
                    finish();
                }
            }
        });
    }

    private void showStopTrackingDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.stop_tracking_title)
                .setMessage(R.string.stop_tracking_message)
                .setPositiveButton(R.string.stop_tracking, (dialog, which) -> {
                    stopTracking();
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        
        try {
            // Set map settings
            this.map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            this.map.getUiSettings().setZoomControlsEnabled(true);
            this.map.getUiSettings().setMyLocationButtonEnabled(true);
            this.map.getUiSettings().setCompassEnabled(true);
            this.map.getUiSettings().setMapToolbarEnabled(true);
            this.map.getUiSettings().setZoomGesturesEnabled(true);
            this.map.getUiSettings().setScrollGesturesEnabled(true);
            this.map.getUiSettings().setTiltGesturesEnabled(true);
            this.map.getUiSettings().setRotateGesturesEnabled(true);

            // Set initial camera position to India (in case location is not available)
            LatLng india = new LatLng(20.5937, 78.9629);
            this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(india, 5f));
            
            // Check and request location permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Enable the my-location layer
                this.map.setMyLocationEnabled(true);
                
                // Get current location and add marker
                fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            
                            // Add marker for current location with custom marker
                            currentLocationMarker.position(currentLocation);
                            this.map.addMarker(currentLocationMarker);
                            
                            // Move camera to current location with zoom
                            this.map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));
                            
                            // Debug log
                            Log.d("MapDebug", "Map camera moved to location: " + currentLocation.latitude + ", " + currentLocation.longitude);
                        } else {
                            Log.e("MapDebug", "Location is null, starting location updates");
                            Toast.makeText(this, "Could not get current location", Toast.LENGTH_SHORT).show();
                            startLocationUpdates(); // Try to get location updates instead
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("MapDebug", "Error getting location: " + e.getMessage());
                        Toast.makeText(this, "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        startLocationUpdates(); // Try to get location updates instead
                    });
            } else {
                Log.e("MapDebug", "Location permission not granted");
                // Request location permission if not granted
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
            }
        } catch (Exception e) {
            Log.e("MapDebug", "Error initializing map: " + e.getMessage());
            Toast.makeText(this, "Error initializing map: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            LocationRequest locationRequest = new LocationRequest.Builder(5000) // 5 seconds
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateDistanceMeters(5) // 5 meters
                .build();

            LocationCallback locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    if (locationResult.getLastLocation() != null && isTracking) {
                        Location location = locationResult.getLastLocation();
                        updateMapLocation(location);
                        
                        // Send periodic updates to all selected contacts
                        for (EmergencyContact contact : selectedContacts) {
                            // Send FCM notification
                            FirebaseHelper.getInstance().getFCMToken(contact.getId(), token -> {
                                if (token != null) {
                                    NotificationHelper.sendFCMNotification(
                                        token,
                                        "Location Update",
                                        "Your contact's location has been updated",
                                        location.getLatitude(),
                                        location.getLongitude(),
                                        false
                                    );
                                }
                            });
                            
                            // Send SMS with updated location
                            SMSHelper.sendPeriodicLocationSMS(
                                LiveTrackingActivity.this,
                                contact.getPhone(),
                                location.getLatitude(),
                                location.getLongitude()
                            );
                        }
                    }
                }
            };

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            );
        } catch (SecurityException e) {
            Log.e("MapDebug", "Error requesting location updates: " + e.getMessage());
            Toast.makeText(this, "Error requesting location updates: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMapLocation(Location location) {
        if (location != null && map != null) {
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            
            // Update marker position
            map.clear(); // Clear previous markers
            currentLocationMarker.position(currentLocation);
            map.addMarker(currentLocationMarker);
            
            // Animate camera to follow the marker
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));
            
            // Enable the my-location layer
            if (ActivityCompat.checkSelfPermission(LiveTrackingActivity.this, 
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);
            }
            
            // Log the update
            Log.d("MapDebug", "Updated marker position to: " + currentLocation.latitude + ", " + currentLocation.longitude);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isTracking) {
            stopTracking();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start location updates
                if (map != null) {
                    checkLocationPermission();
                }
            } else {
                Toast.makeText(this, "Location permission is required for tracking", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        } else {
            if (map != null) {
                map.setMyLocationEnabled(true);
                startLocationUpdates();
            }
        }
    }
} 