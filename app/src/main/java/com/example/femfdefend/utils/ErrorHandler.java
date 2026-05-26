package com.example.femfdefend.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.io.IOException;

/**
 * Centralized error handling utility for FEMdefend application.
 * Provides consistent error handling, logging, and user feedback across all components.
 * 
 * @author FEMdefend Team
 * @version 1.0
 */
public class ErrorHandler {
    
    private static final String TAG = "ErrorHandler";
    
    /**
     * Error types for categorization and appropriate handling
     */
    public enum ErrorType {
        NETWORK_ERROR,
        AUTHENTICATION_ERROR,
        PERMISSION_ERROR,
        LOCATION_ERROR,
        SMS_ERROR,
        CAMERA_ERROR,
        VOICE_ERROR,
        DATABASE_ERROR,
        VALIDATION_ERROR,
        UNKNOWN_ERROR
    }
    
    /**
     * Handles exceptions with appropriate logging and user feedback
     * 
     * @param context Application context for showing user feedback
     * @param exception The exception to handle
     * @param operation Description of the operation that failed
     */
    public static void handleException(@NonNull Context context, 
                                     @NonNull Exception exception, 
                                     @NonNull String operation) {
        ErrorType errorType = categorizeException(exception);
        String userMessage = getUserFriendlyMessage(errorType);
        String logMessage = getDetailedLogMessage(exception, operation);
        
        // Log the error with appropriate level
        logError(errorType, logMessage, exception);
        
        // Show user-friendly message
        showUserMessage(context, userMessage, errorType);
    }
    
    /**
     * Handles specific error types with custom messages
     * 
     * @param context Application context
     * @param errorType Type of error
     * @param operation Operation that failed
     * @param customMessage Custom error message
     */
    public static void handleError(@NonNull Context context, 
                                 @NonNull ErrorType errorType, 
                                 @NonNull String operation, 
                                 @Nullable String customMessage) {
        String userMessage = customMessage != null ? customMessage : 
                           getUserFriendlyMessage(errorType);
        
        logError(errorType, "Operation failed: " + operation + " - " + customMessage, null);
        showUserMessage(context, userMessage, errorType);
    }
    
    /**
     * Categorizes exceptions into error types for appropriate handling
     */
    private static ErrorType categorizeException(@NonNull Exception exception) {
        if (exception instanceof FirebaseAuthException) {
            return ErrorType.AUTHENTICATION_ERROR;
        } else if (exception instanceof FirebaseFirestoreException) {
            return ErrorType.DATABASE_ERROR;
        } else if (exception instanceof IOException) {
            return ErrorType.NETWORK_ERROR;
        } else if (exception instanceof SecurityException) {
            return ErrorType.PERMISSION_ERROR;
        } else if (exception instanceof IllegalArgumentException) {
            return ErrorType.VALIDATION_ERROR;
        } else {
            return ErrorType.UNKNOWN_ERROR;
        }
    }
    
    /**
     * Provides user-friendly error messages based on error type
     */
    private static String getUserFriendlyMessage(@NonNull ErrorType errorType) {
        switch (errorType) {
            case NETWORK_ERROR -> {
                return "Network connection issue. Please check your internet connection and try again.";
            }
            case AUTHENTICATION_ERROR -> {
                return "Authentication failed. Please log in again.";
            }
            case PERMISSION_ERROR -> {
                return "Permission denied. Please grant the required permissions in settings.";
            }
            case LOCATION_ERROR -> {
                return "Unable to get your location. Please enable location services.";
            }
            case SMS_ERROR -> {
                return "Unable to send SMS. Please check your phone settings.";
            }
            case CAMERA_ERROR -> {
                return "Camera access failed. Please check camera permissions.";
            }
            case VOICE_ERROR -> {
                return "Voice recognition failed. Please check microphone permissions.";
            }
            case DATABASE_ERROR -> {
                return "Data sync failed. Please check your connection and try again.";
            }
            case VALIDATION_ERROR -> {
                return "Invalid input. Please check your information and try again.";
            }
            default -> {
                return "An unexpected error occurred. Please try again.";
            }
        }
    }
    
    /**
     * Creates detailed log messages for debugging
     */
    private static String getDetailedLogMessage(@NonNull Exception exception, 
                                              @NonNull String operation) {
        StringBuilder message = new StringBuilder();
        message.append("Operation: ").append(operation).append("\n");
        message.append("Exception: ").append(exception.getClass().getSimpleName()).append("\n");
        message.append("Message: ").append(exception.getMessage()).append("\n");
        
        if (exception.getCause() != null) {
            message.append("Cause: ").append(exception.getCause().getMessage());
        }
        
        return message.toString();
    }
    
    /**
     * Logs errors with appropriate severity levels
     */
    private static void logError(@NonNull ErrorType errorType, 
                               @NonNull String message, 
                               @Nullable Exception exception) {
        switch (errorType) {
            case NETWORK_ERROR, DATABASE_ERROR -> Log.w(TAG, message, exception);
            case UNKNOWN_ERROR -> Log.e(TAG, message, exception);
            default -> {
                // This case should never be reached
            }
        }
    }
    
    /**
     * Shows user-friendly error messages
     */
    private static void showUserMessage(@NonNull Context context, 
                                      @NonNull String message, 
                                      @NonNull ErrorType errorType) {
        // Use appropriate duration based on error type
        int duration = (errorType == ErrorType.NETWORK_ERROR || 
                       errorType == ErrorType.PERMISSION_ERROR) ? 
                       Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        
        Toast.makeText(context, message, duration).show();
    }
} 