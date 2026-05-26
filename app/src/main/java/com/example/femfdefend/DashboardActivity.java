package com.example.femfdefend;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import com.example.femfdefend.databinding.ActivityDashboardBinding;
import com.example.femfdefend.dialogs.CancelEmergencyDialog;
import com.example.femfdefend.models.EmergencyContact;
import com.example.femfdefend.models.EmergencyServiceModel;
import com.example.femfdefend.services.EmergencyServiceHandler;
import com.example.femfdefend.utils.ErrorHandler;
import com.example.femfdefend.utils.FirebaseHelper;
import com.example.femfdefend.utils.LocationCache;
import com.example.femfdefend.utils.NotificationHelper;
import com.example.femfdefend.utils.SMSHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.GeoPoint;

import java.util.List;

/**
 * Main dashboard activity for FEMdefend application.
 * Provides access to all safety features including emergency contacts, laws, 
 * self-defense resources, live tracking, and panic button functionality.
 * 
 * Features:
 * - Emergency panic button with shake detection
 * - Navigation to all safety modules
 * - Theme switching (dark/light mode)
 * - User profile management
 * - Emergency contact management
 * 
 * @author FEMdefend Team
 * @version 1.0
 */
public class DashboardActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "DashboardActivity";
    
    // UI Components
    private ActivityDashboardBinding binding;
    
    // Sensor Management
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime;
    private static final float SHAKE_THRESHOLD = 20.0f;
    private static final int MIN_TIME_BETWEEN_SHAKES = 1000;
    
    // Location Services
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCache locationCache;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final double NEARBY_RADIUS_KM = 10.0; // 10km radius for nearby services
    
    // Emergency State Management
    private boolean isEmergencyActive = false;
    private EmergencyServiceHandler emergencyServiceHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            binding = ActivityDashboardBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            setSupportActionBar(binding.toolbar);

            initializeComponents();
            setupClickListeners();
            checkPermissions();
            validateUserAuthentication();

        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Dashboard initialization");
            finish();
        }
    }

    /**
     * Initializes all components and services
     */
    private void initializeComponents() {
        try {
            // Initialize location services
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            locationCache = LocationCache.getInstance(this);
            emergencyServiceHandler = new EmergencyServiceHandler(this);

            // Initialize sensor manager for shake detection
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (sensorManager != null) {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (accelerometer == null) {
                    Log.w(TAG, "Accelerometer sensor not available on this device");
                }
            } else {
                Log.e(TAG, "SensorManager not available");
            }
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Component initialization");
        }
    }

    /**
     * Sets up click listeners for all UI elements
     */
    private void setupClickListeners() {
        try {
            binding.cardEmergencyContacts.setOnClickListener(v -> 
                startActivity(new Intent(this, EmergencyContactsActivity.class)));

            binding.cardLaws.setOnClickListener(v -> 
                startActivity(new Intent(this, WomenLawsActivity.class)));

            binding.cardSelfDefense.setOnClickListener(v -> 
                startActivity(new Intent(this, SelfDefenseActivity.class)));

            binding.cardLiveTracking.setOnClickListener(v -> 
                startActivity(new Intent(this, LiveTrackingActivity.class)));

            binding.btnPanic.setOnClickListener(v -> activatePanicMode("Button pressed"));
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Setting up click listeners");
        }
    }

    /**
     * Checks and requests necessary permissions
     */
    private void checkPermissions() {
        try {
            String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            };

            boolean allPermissionsGranted = true;
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (!allPermissionsGranted) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
            }
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Permission checking");
        }
    }

    /**
     * Validates user authentication and redirects to login if needed
     */
    private void validateUserAuthentication() {
        try {
            if (FirebaseHelper.getInstance().getCurrentUser() == null) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "User authentication validation");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            int id = item.getItemId();
            if (id == R.id.action_theme) {
                toggleTheme();
                return true;
            } else if (id == R.id.action_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            } else if (id == R.id.action_logout) {
                performLogout();
                return true;
            } else if (id == R.id.action_voice_detection) {
                startActivity(new Intent(this, VoiceDetectionActivity.class));
                return true;
            } else if (id == R.id.action_camera_x_test) {
                startActivity(new Intent(this, CameraXTestActivity.class));
                return true;
            } else if (id == R.id.action_emergency_recordings) {
                startActivity(new Intent(this, EmergencyRecordingsActivity.class));
                return true;
            }
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Menu item selection");
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Toggles between light and dark themes
     */
    private void toggleTheme() {
        try {
            if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Theme switching");
        }
    }

    /**
     * Performs user logout and redirects to login screen
     */
    private void performLogout() {
        try {
            FirebaseHelper helper = FirebaseHelper.getInstance();
            helper.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "User logout");
        }
    }

    /**
     * Activates emergency mode with the specified trigger
     * 
     * @param trigger The trigger that activated the emergency mode
     */
    private void activatePanicMode(String trigger) {
        if (isEmergencyActive) {
            Toast.makeText(this, "Emergency alert already active!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Log.i(TAG, "Activating emergency protocol. Trigger: " + trigger);
            isEmergencyActive = true;

            // Disable panic button temporarily
            binding.btnPanic.setEnabled(false);

            // Provide haptic feedback
            provideHapticFeedback();

            // Show immediate feedback
            Toast.makeText(this, "Emergency protocol activated! Alerting contacts and emergency services...", Toast.LENGTH_LONG).show();

            // Get current user information
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String userName = user != null ? user.getDisplayName() : "Unknown User";

            // Get current location and send alerts
            getCurrentLocationAndSendAlerts(userName);

        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Emergency activation");
            // Re-enable panic button on error
            binding.btnPanic.setEnabled(true);
            isEmergencyActive = false;
        }
    }

    /**
     * Provides haptic feedback for emergency activation
     */
    private void provideHapticFeedback() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(500);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to provide haptic feedback: " + e.getMessage());
        }
    }

    /**
     * Gets current location and sends emergency alerts
     * 
     * @param userName The name of the user in emergency
     */
    private void getCurrentLocationAndSendAlerts(String userName) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        try {
                            double latitude = 0;
                            double longitude = 0;

                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                locationCache.updateLastLocation(latitude, longitude);
                            } else {
                                // Use cached location if current location is null
                                Location cachedLocation = locationCache.getLastKnownLocation();
                                if (cachedLocation != null) {
                                    latitude = cachedLocation.getLatitude();
                                    longitude = cachedLocation.getLongitude();
                                }
                            }

                            sendEmergencyAlerts(userName, latitude, longitude);
                        } catch (Exception e) {
                            ErrorHandler.handleException(this, e, "Location processing");
                            sendEmergencyAlerts(userName, 0, 0); // Send without location
                        }
                    })
                    .addOnFailureListener(e -> {
                        ErrorHandler.handleException(this, e, "Getting current location");
                        sendEmergencyAlerts(userName, 0, 0); // Send without location
                    });
            } else {
                ErrorHandler.handleError(this, ErrorHandler.ErrorType.PERMISSION_ERROR, 
                    "Location access", "Location permission not granted");
                sendEmergencyAlerts(userName, 0, 0); // Send without location
            }
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Location retrieval");
            sendEmergencyAlerts(userName, 0, 0); // Send without location
        }
    }

    /**
     * Sends emergency alerts to contacts and services
     * 
     * @param userName The name of the user in emergency
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     */
    private void sendEmergencyAlerts(String userName, double latitude, double longitude) {
        try {
            // Send to emergency contacts
            FirebaseHelper.getInstance().getEmergencyContacts(contacts -> {
                try {
                    for (EmergencyContact contact : contacts) {
                        Location lastLocation = locationCache.getLastKnownLocation();
                        if (lastLocation != null) {
                            double latitudeToSend = lastLocation.getLatitude();
                            double longitudeToSend = lastLocation.getLongitude();
                            SMSHelper.sendLocationSMS(this, contact.getPhone(), latitudeToSend, longitudeToSend, true);
                        }
                    }
                } catch (Exception e) {
                    ErrorHandler.handleException(this, e, "Sending SMS to emergency contacts");
                }
            });

            // Send to emergency services
            emergencyServiceHandler.handleEmergency(userName, latitude, longitude);

            // Re-enable panic button after delay
            new Handler().postDelayed(() -> {
                binding.btnPanic.setEnabled(true);
                isEmergencyActive = false;
                Toast.makeText(this, "Emergency alerts sent. You can press again if needed.", Toast.LENGTH_SHORT).show();
            }, 5000);

        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Sending emergency alerts");
            // Re-enable panic button on error
            binding.btnPanic.setEnabled(true);
            isEmergencyActive = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Register sensor listener for shake detection
            if (sensorManager != null && accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Sensor registration");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            // Unregister sensor listener
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Sensor unregistration");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastShakeTime > MIN_TIME_BETWEEN_SHAKES) {
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    float acceleration = (float) Math.sqrt(x * x + y * y + z * z);
                    if (acceleration > SHAKE_THRESHOLD) {
                        lastShakeTime = currentTime;
                        activatePanicMode("Shake detected");
                    }
                }
            }
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Sensor event processing");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used in this implementation
    }

    /**
     * Shows emergency cancellation dialog
     */
    private void showCancellationDialog() {
        try {
            CancelEmergencyDialog dialog = new CancelEmergencyDialog(this, new CancelEmergencyDialog.OnEmergencyActionListener() {
                @Override
                public void onEmergencyConfirmed() {
                    // Handle emergency confirmed
                    Log.i(TAG, "Emergency confirmed by user");
                }
                @Override
                public void onEmergencyCancelled() {
                    // Handle emergency cancelled
                    Log.i(TAG, "Emergency cancelled by user");
                    isEmergencyActive = false;
                    binding.btnPanic.setEnabled(true);
                }
            });
            dialog.show();
        } catch (Exception e) {
            ErrorHandler.handleException(this, e, "Showing cancellation dialog");
        }
    }
} 