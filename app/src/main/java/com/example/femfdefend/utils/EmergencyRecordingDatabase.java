package com.example.femfdefend.utils;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.femfdefend.models.EmergencyRecording;
import com.example.femfdefend.models.EmergencyRecordingDao;

@Database(entities = {EmergencyRecording.class}, version = 1)
public abstract class EmergencyRecordingDatabase extends RoomDatabase {
    public abstract EmergencyRecordingDao emergencyRecordingDao();

    private static volatile EmergencyRecordingDatabase INSTANCE;

    public static EmergencyRecordingDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (EmergencyRecordingDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            EmergencyRecordingDatabase.class, "emergency_recordings_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
} 