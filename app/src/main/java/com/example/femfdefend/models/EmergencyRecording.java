package com.example.femfdefend.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "emergency_recordings")
public class EmergencyRecording {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String filePath;
    public long timestamp;
    public long duration;
    public String cameraUsed; // "back" or "front"
} 