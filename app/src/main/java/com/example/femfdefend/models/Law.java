package com.example.femfdefend.models;

public class Law implements Comparable<Law> {
    private String id;
    private String title;
    private String shortDescription;
    private String fullDescription;
    private int year;
    private double relevanceScore;

    private Law(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.shortDescription = builder.shortDescription;
        this.fullDescription = builder.fullDescription;
        this.year = builder.year;
        this.relevanceScore = 0.0;
    }

    // Getters only since we're making the class immutable
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getShortDescription() { return shortDescription; }
    public String getFullDescription() { return fullDescription; }
    public int getYear() { return year; }
    public double getRelevanceScore() { return relevanceScore; }

    public void setRelevanceScore(double score) {
        this.relevanceScore = score;
    }

    @Override
    public int compareTo(Law other) {
        // Sort by relevance score in descending order
        return Double.compare(other.relevanceScore, this.relevanceScore);
    }

    // Builder class
    public static class Builder {
        private String id;
        private String title;
        private String shortDescription;
        private String fullDescription;
        private int year;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder shortDescription(String shortDescription) {
            this.shortDescription = shortDescription;
            return this;
        }

        public Builder fullDescription(String fullDescription) {
            this.fullDescription = fullDescription;
            return this;
        }

        public Builder year(int year) {
            this.year = year;
            return this;
        }

        public Law build() {
            return new Law(this);
        }
    }
} 