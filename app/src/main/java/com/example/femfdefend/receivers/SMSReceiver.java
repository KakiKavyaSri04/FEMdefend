package com.example.femfdefend.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.example.femfdefend.utils.NotificationHelper;

public class SMSReceiver extends BroadcastReceiver {
    private static final String TAG = "SMSReceiver";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "SMS received with action: " + intent.getAction());
        
        if (intent.getAction() != null && intent.getAction().equals(SMS_RECEIVED)) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                String format = bundle.getString("format");
                
                if (pdus != null) {
                    boolean isEmergencyMessage = false;
                    StringBuilder fullMessage = new StringBuilder();
                    String sender = null;
                    
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                        } else {
                            smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        }
                        
                        sender = smsMessage.getDisplayOriginatingAddress();
                        String messageBody = smsMessage.getMessageBody();
                        fullMessage.append(messageBody);
                        
                        Log.d(TAG, "SMS Received from: " + sender);
                        Log.d(TAG, "Message part: " + messageBody);
                        
                        if (messageBody.contains("EMERGENCY ALERT")) {
                            isEmergencyMessage = true;
                        }
                    }
                    
                    String completeMessage = fullMessage.toString();
                    Log.d(TAG, "Complete message: " + completeMessage);
                    
                    // If this is an emergency message, show a notification
                    if (isEmergencyMessage) {
                        Log.i(TAG, "Emergency alert detected in SMS");
                        // Extract location from Google Maps link if present
                        String locationLink = extractLocationLink(completeMessage);
                        if (locationLink != null) {
                            Log.d(TAG, "Location link found: " + locationLink);
                            double[] coordinates = extractCoordinates(locationLink);
                            if (coordinates != null) {
                                Log.i(TAG, "Showing emergency notification with coordinates: " + coordinates[0] + ", " + coordinates[1]);
                                showEmergencyNotification(context, sender, coordinates[0], coordinates[1]);
                                // Show a toast for immediate visibility
                                String toastMessage = "Emergency alert received from " + sender;
                                Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();
                            } else {
                                Log.e(TAG, "Failed to extract coordinates from location link");
                                // Show notification without coordinates
                                showEmergencyNotification(context, sender, 0, 0);
                            }
                        } else {
                            Log.e(TAG, "No location link found in emergency message");
                            // Show notification without coordinates
                            showEmergencyNotification(context, sender, 0, 0);
                        }
                    }
                }
            }
        }
    }

    private String extractLocationLink(String message) {
        Log.d(TAG, "Extracting location link from message");
        int start = message.indexOf("https://www.google.com/maps");
        if (start != -1) {
            int end = message.indexOf("\n", start);
            if (end == -1) end = message.length();
            String link = message.substring(start, end);
            Log.d(TAG, "Extracted link: " + link);
            return link;
        }
        Log.w(TAG, "No location link found in message");
        return null;
    }

    private double[] extractCoordinates(String locationLink) {
        try {
            Log.d(TAG, "Extracting coordinates from link: " + locationLink);
            int start = locationLink.indexOf("q=") + 2;
            String[] coords = locationLink.substring(start).split(",");
            if (coords.length == 2) {
                double lat = Double.parseDouble(coords[0]);
                double lon = Double.parseDouble(coords[1]);
                Log.d(TAG, "Extracted coordinates: " + lat + ", " + lon);
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting coordinates: " + e.getMessage(), e);
        }
        return null;
    }

    private void showEmergencyNotification(Context context, String sender, double latitude, double longitude) {
        String title = "Emergency Alert!";
        String message = "Emergency alert received from " + sender;
        Log.i(TAG, "Sending panic notification with title: " + title + ", message: " + message);
        
        try {
            NotificationHelper.sendPanicNotification(
                context, title, message, latitude, longitude
            );
            Log.d(TAG, "Emergency notification sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: " + e.getMessage(), e);
        }
    }
} 