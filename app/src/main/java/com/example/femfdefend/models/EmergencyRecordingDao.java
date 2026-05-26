package com.example.femfdefend.models;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EmergencyRecordingDao {
    @Insert
    void insert(EmergencyRecording recording);

    @Delete
    void delete(EmergencyRecording recording);

    @Query("SELECT * FROM emergency_recordings ORDER BY timestamp DESC")
    List<EmergencyRecording> getAllRecordings();
} 