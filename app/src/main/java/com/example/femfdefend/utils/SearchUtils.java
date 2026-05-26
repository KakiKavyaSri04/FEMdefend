package com.example.femfdefend.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchUtils {
    
    /**
     * Calculates the relevance score between a search query and a law name
     * @param query The search query
     * @param lawName The name of the law
     * @return A score between 0 and 1, where 1 is most relevant
     */
    public static double calculateRelevanceScore(String query, String lawName) {
        query = query.toLowerCase();
        lawName = lawName.toLowerCase();
        
        // Direct contains check
        if (lawName.contains(query)) {
            return 1.0;
        }
        
        // Split into words
        Set<String> queryWords = new HashSet<>(Arrays.asList(query.split("\\s+")));
        Set<String> lawWords = new HashSet<>(Arrays.asList(lawName.split("\\s+")));
        
        // Calculate word match ratio
        int matchingWords = 0;
        for (String queryWord : queryWords) {
            for (String lawWord : lawWords) {
                if (lawWord.contains(queryWord) || queryWord.contains(lawWord)) {
                    matchingWords++;
                    break;
                }
            }
        }
        
        // Calculate relevance score
        return queryWords.isEmpty() ? 0 : (double) matchingWords / queryWords.size();
    }

    /**
     * Checks if a law is relevant to the search query
     * @param query The search query
     * @param lawName The name of the law
     * @return true if the law is relevant
     */
    public static boolean isRelevant(String query, String lawName) {
        return calculateRelevanceScore(query, lawName) > 0.3; // Threshold for relevance
    }

    /**
     * Extracts keywords from a law name
     * @param lawName The name of the law
     * @return List of keywords
     */
    public static List<String> extractKeywords(String lawName) {
        List<String> keywords = new ArrayList<>();
        // Common words to exclude
        Set<String> stopWords = new HashSet<>(Arrays.asList(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of"
        ));
        
        // Split and process words
        for (String word : lawName.toLowerCase().split("\\s+")) {
            if (!stopWords.contains(word) && word.length() > 2) {
                keywords.add(word);
            }
        }
        
        return keywords;
    }
} 