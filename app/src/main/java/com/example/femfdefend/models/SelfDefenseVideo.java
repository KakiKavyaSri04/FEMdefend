package com.example.femfdefend.models;

public class SelfDefenseVideo {
    private String title;
    private String description;
    private String videoPath;
    private String thumbnailPath;

    public SelfDefenseVideo(String title, String description, String videoPath, String thumbnailPath) {
        this.title = title;
        this.description = description;
        this.videoPath = videoPath;
        this.thumbnailPath = thumbnailPath;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getVideoPath() { return videoPath; }
    public String getThumbnailPath() { return thumbnailPath; }
} 