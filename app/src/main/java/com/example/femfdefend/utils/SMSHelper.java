package com.example.femfdefend.utils;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SMSHelper {
    private static final String TAG = "SMSHelper";
    private static final int SMS_PERMISSION_REQUEST = 123;
    private static final String SMS_SENT = "SMS_SENT";
    private static final String SMS_DELIVERED = "SMS_DELIVERED";

    public static void sendLocationSMS(Context context, String phoneNumber, double latitude, double longitude, boolean isEmergency) {
        Log.d(TAG, "Attempting to send SMS to: " + phoneNumber);
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            Log.e(TAG, "Cannot send SMS: Phone number is null or empty");
            return;
        }

        // Format phone number
        phoneNumber = formatPhoneNumber(phoneNumber);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SMS permission not granted");
            if (context instanceof Activity) {
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.SEND_SMS},
                        SMS_PERMISSION_REQUEST);
            }
            Toast.makeText(context, "SMS permission required to send emergency messages", 
                    Toast.LENGTH_LONG).show();
            return;
        }

        try {
            // If current location is (0,0), try to get last known location
            if (latitude == 0 && longitude == 0) {
                Log.w(TAG, "Location is (0,0), attempting to use last known location");
                Location lastLocation = LocationCache.getInstance(context).getLastKnownLocation();
                if (lastLocation != null) {
                    latitude = lastLocation.getLatitude();
                    longitude = lastLocation.getLongitude();
                    
                    // Format the last update time
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());
                    String lastUpdateTime = sdf.format(new Date(lastLocation.getTime()));
                    Log.d(TAG, "Using last known location from: " + lastUpdateTime);
                    
                    sendSMSWithLastKnownLocation(context, phoneNumber, latitude, longitude, lastUpdateTime, isEmergency);
                    return;
                } else {
                    Log.e(TAG, "No last known location available");
                }
            } else {
                // Update the location cache with current location
                LocationCache.getInstance(context).updateLastLocation(latitude, longitude);
                Log.d(TAG, "Updated location cache with current location");
            }

            String googleMapsLink = "https://www.google.com/maps?q=" + latitude + "," + longitude;
            String message;
            
            if (isEmergency) {
                message = "EMERGENCY ALERT! Your contact needs immediate help!\n" +
                         "Current Location: " + googleMapsLink + "\n" +
                         "Please respond immediately!";
            } else {
                message = "Location Update: Your contact is sharing their location with you.\n" +
                         "Track them here: " + googleMapsLink;
            }

            Log.i(TAG, "Sending " + (isEmergency ? "emergency" : "location update") + " SMS to: " + phoneNumber);
            sendMultipartSMSWithDeliveryReport(context, phoneNumber, message, isEmergency);
            Log.d(TAG, "SMS sending initiated to " + phoneNumber);
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS: " + e.getMessage(), e);
            Toast.makeText(context, "Failed to send SMS: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }

    private static void sendSMSWithLastKnownLocation(Context context, String phoneNumber, 
            double latitude, double longitude, String lastUpdateTime, boolean isEmergency) {
        String googleMapsLink = "https://www.google.com/maps?q=" + latitude + "," + longitude;
        String message;
        
        if (isEmergency) {
            message = "EMERGENCY ALERT! Your contact needs immediate help!\n" +
                     "⚠️ Current location unavailable. Last known location:\n" +
                     "📍 " + googleMapsLink + "\n" +
                     "⏰ Last updated: " + lastUpdateTime + "\n" +
                     "This might not be their current location.\n" +
                     "Please respond immediately!";
        } else {
            message = "Location Update: Your contact is sharing their location.\n" +
                     "⚠️ Current location unavailable. Last known location:\n" +
                     "📍 " + googleMapsLink + "\n" +
                     "⏰ Last updated: " + lastUpdateTime;
        }

        Log.i(TAG, "Sending SMS with last known location to: " + phoneNumber);
        sendMultipartSMSWithDeliveryReport(context, phoneNumber, message, isEmergency);
    }

    private static void sendMultipartSMSWithDeliveryReport(Context context, String phoneNumber, String message, boolean isEmergency) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            
            Log.d(TAG, "Message will be sent in " + parts.size() + " parts");

            ArrayList<PendingIntent> sentIntents = new ArrayList<>();
            ArrayList<PendingIntent> deliveredIntents = new ArrayList<>();

            // Register for SMS sent and delivered intents
            for (int i = 0; i < parts.size(); i++) {
                String SENT = SMS_SENT + "_" + i;
                String DELIVERED = SMS_DELIVERED + "_" + i;

                PendingIntent sentPI = PendingIntent.getBroadcast(context, i,
                    new Intent(SENT), PendingIntent.FLAG_IMMUTABLE);
                PendingIntent deliveredPI = PendingIntent.getBroadcast(context, i,
                    new Intent(DELIVERED), PendingIntent.FLAG_IMMUTABLE);

                sentIntents.add(sentPI);
                deliveredIntents.add(deliveredPI);

                // Register broadcast receivers
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            switch (getResultCode()) {
                                case Activity.RESULT_OK:
                                    Log.i(TAG, "SMS sent successfully to " + phoneNumber);
                                    if (isEmergency) {
                                        Toast.makeText(context, "Emergency alert sent", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                                    Log.e(TAG, "Generic failure sending SMS to " + phoneNumber);
                                    Toast.makeText(context, "SMS sending failed", Toast.LENGTH_SHORT).show();
                                    break;
                                case SmsManager.RESULT_ERROR_NO_SERVICE:
                                    Log.e(TAG, "No service available for sending SMS to " + phoneNumber);
                                    Toast.makeText(context, "No service available", Toast.LENGTH_SHORT).show();
                                    break;
                                case SmsManager.RESULT_ERROR_NULL_PDU:
                                    Log.e(TAG, "Null PDU error sending SMS to " + phoneNumber);
                                    break;
                                case SmsManager.RESULT_ERROR_RADIO_OFF:
                                    Log.e(TAG, "Radio off error sending SMS to " + phoneNumber);
                                    Toast.makeText(context, "Phone radio is off", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        }
                    }, new IntentFilter(SENT), android.content.Context.RECEIVER_EXPORTED);
                } else {
                    context.registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            switch (getResultCode()) {
                                case Activity.RESULT_OK:
                                    Log.i(TAG, "SMS sent successfully to " + phoneNumber);
                                    if (isEmergency) {
                                        Toast.makeText(context, "Emergency alert sent", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                                    Log.e(TAG, "Generic failure sending SMS to " + phoneNumber);
                                    Toast.makeText(context, "SMS sending failed", Toast.LENGTH_SHORT).show();
                                    break;
                                case SmsManager.RESULT_ERROR_NO_SERVICE:
                                    Log.e(TAG, "No service available for sending SMS to " + phoneNumber);
                                    Toast.makeText(context, "No service available", Toast.LENGTH_SHORT).show();
                                    break;
                                case SmsManager.RESULT_ERROR_NULL_PDU:
                                    Log.e(TAG, "Null PDU error sending SMS to " + phoneNumber);
                                    break;
                                case SmsManager.RESULT_ERROR_RADIO_OFF:
                                    Log.e(TAG, "Radio off error sending SMS to " + phoneNumber);
                                    Toast.makeText(context, "Phone radio is off", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        }
                    }, new IntentFilter(SENT));
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            switch (getResultCode()) {
                                case Activity.RESULT_OK:
                                    Log.i(TAG, "SMS delivered successfully to " + phoneNumber);
                                    break;
                                case Activity.RESULT_CANCELED:
                                    Log.e(TAG, "SMS delivery failed to " + phoneNumber);
                                    if (isEmergency) {
                                        Toast.makeText(context, "Emergency alert not delivered", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                            }
                        }
                    }, new IntentFilter(DELIVERED), android.content.Context.RECEIVER_EXPORTED);
                } else {
                    context.registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            switch (getResultCode()) {
                                case Activity.RESULT_OK:
                                    Log.i(TAG, "SMS delivered successfully to " + phoneNumber);
                                    break;
                                case Activity.RESULT_CANCELED:
                                    Log.e(TAG, "SMS delivery failed to " + phoneNumber);
                                    if (isEmergency) {
                                        Toast.makeText(context, "Emergency alert not delivered", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                            }
                        }
                    }, new IntentFilter(DELIVERED));
                }
            }
            
            // Send the SMS
            if (parts.size() > 1) {
                Log.d(TAG, "Sending multipart SMS");
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    parts,
                    sentIntents,
                    deliveredIntents
                );
            } else {
                Log.d(TAG, "Sending single part SMS");
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    sentIntents.get(0),
                    deliveredIntents.get(0)
                );
            }
            Log.i(TAG, "SMS sending initiated to: " + phoneNumber);
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS: " + e.getMessage(), e);
            Toast.makeText(context, "Failed to send SMS: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }

    private static String formatPhoneNumber(String phoneNumber) {
        // Remove any non-digit characters
        String cleaned = phoneNumber.replaceAll("[^\\d]", "");
        
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
        
        Log.d(TAG, "Formatted phone number from " + phoneNumber + " to " + cleaned);
        return cleaned;
    }

    public static void sendPeriodicLocationSMS(Context context, String phoneNumber, 
            double latitude, double longitude) {
        sendLocationSMS(context, phoneNumber, latitude, longitude, false);
    }
} 