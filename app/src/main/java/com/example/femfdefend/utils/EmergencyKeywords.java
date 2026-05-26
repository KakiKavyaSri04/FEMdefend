package com.example.femfdefend.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;

public class EmergencyKeywords {
    private static final Set<String> EMERGENCY_PHRASES = new HashSet<>(Arrays.asList(
        "help",
        "help me",
        "emergency",
        "save",
        "save me",
        "sos",
        "danger",
        "urgent",
        "need help",
        "please help",
        "help please",
        "emergency help",
        "danger help",
        "alert",
        "emergency alert"
    ));

    public static boolean isEmergencyPhrase(String phrase) {
        if (phrase == null || phrase.trim().isEmpty()) {
            return false;
        }
        
        // Convert to lowercase and trim
        String normalizedPhrase = phrase.toLowerCase(Locale.getDefault()).trim();
        
        // Check exact matches
        if (EMERGENCY_PHRASES.contains(normalizedPhrase)) {
            return true;
        }
        
        // Check if any emergency phrase is contained within the input
        return EMERGENCY_PHRASES.stream()
            .anyMatch(normalizedPhrase::contains);
    }
} 