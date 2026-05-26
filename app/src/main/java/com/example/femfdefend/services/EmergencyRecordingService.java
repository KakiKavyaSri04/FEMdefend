package com.example.femfdefend.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;
import com.example.femfdefend.models.EmergencyRecording;
import com.example.femfdefend.models.EmergencyRecordingDao;
import com.example.femfdefend.utils.EmergencyRecordingDatabase;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

public class EmergencyRecordingService extends LifecycleService {
    private static final String TAG = "EmergencyRecordingService";
    private static final String CHANNEL_ID = "emergency_recording_channel";
    private ExecutorService cameraExecutor;
    private Recording activeRecording;
    private EmergencyRecordingDao recordingDao;
    private boolean triedFrontCamera = false;
    private File videoFile;
    private long recordingStartTime;
    private boolean isRecording = false;

    public EmergencyRecordingService() {
        super();
        Log.i("EmergencyRecordingService", "Constructor called");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate called");
        Toast.makeText(getApplicationContext(), "Service onCreate", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "EmergencyRecordingService started");
        cameraExecutor = Executors.newSingleThreadExecutor();
        recordingDao = EmergencyRecordingDatabase.getDatabase(getApplicationContext()).emergencyRecordingDao();
        createNotificationChannel();
        startForeground(1, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: Service started, about to start camera recording");
        startCameraRecording(CameraSelector.DEFAULT_BACK_CAMERA);
        return super.onStartCommand(intent, flags, startId);
    }

    private void startCameraRecording(CameraSelector selector) {
        // Permission check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Camera or audio permission not granted");
            stopSelf();
            return;
        }
        Log.i(TAG, "Permissions granted. Setting up CameraX...");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getApplicationContext());
        cameraProviderFuture.addListener(() -> {
            try {
                Log.i(TAG, "CameraProvider future complete. Getting provider...");
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                // Prepare file
                String fileName = generateFileName();
                File dir = new File(getApplicationContext().getFilesDir(), "emergency_videos");
                if (!dir.exists() && !dir.mkdirs()) {
                    Log.e(TAG, "Failed to create directory for emergency videos");
                    stopSelf();
                    return;
                }
                videoFile = new File(dir, fileName);

                // Recorder
                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build();
                VideoCapture<Recorder> videoCapture = VideoCapture.withOutput(recorder);

                // ImageAnalysis for darkness detection
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    if (isDark(image) && !triedFrontCamera && selector == CameraSelector.DEFAULT_BACK_CAMERA) {
                        triedFrontCamera = true;
                        cameraProvider.unbindAll();
                        startCameraRecording(CameraSelector.DEFAULT_FRONT_CAMERA);
                    }
                    image.close();
                });

                // Preview (not shown in service, but required by CameraX)
                Preview preview = new Preview.Builder().build();

                cameraProvider.bindToLifecycle(
                        this,
                        selector,
                        videoCapture,
                        imageAnalysis,
                        preview
                );

                Log.i(TAG, "CameraX setup complete. About to prepare and start recording...");
                // Start recording
                FileOutputOptions outputOptions = new FileOutputOptions.Builder(videoFile).build();
                try {
                    activeRecording = videoCapture.getOutput()
                            .prepareRecording(this, outputOptions)
                            .withAudioEnabled()
                            .start(cameraExecutor, event -> {
                                Log.i(TAG, "CameraX event: " + event);
                                if (event instanceof VideoRecordEvent.Start) {
                                    isRecording = true;
                                    recordingStartTime = System.currentTimeMillis();
                                    Log.i(TAG, "VideoRecordEvent.Start received");
                                    // Show a notification
                                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                    Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                                            .setContentTitle("Emergency Recording Started")
                                            .setContentText("Your camera and microphone are now recording.")
                                            .setSmallIcon(android.R.drawable.ic_menu_camera)
                                            .setOngoing(true)
                                            .build();
                                    if (manager != null) manager.notify(2, notification);
                                    // Show a Toast (may not always show from a service, but will try)
                                    new android.os.Handler(getMainLooper()).post(() ->
                                        android.widget.Toast.makeText(getApplicationContext(), "Emergency recording started", android.widget.Toast.LENGTH_SHORT).show()
                                    );
                                } else if (event instanceof VideoRecordEvent.Finalize) {
                                    isRecording = false;
                                    long duration = System.currentTimeMillis() - recordingStartTime;
                                    saveRecording(videoFile.getAbsolutePath(), duration, selector == CameraSelector.DEFAULT_BACK_CAMERA ? "back" : "front");
                                    Log.i(TAG, "VideoRecordEvent.Finalize received. Recording stopped and saved.");
                                    stopSelf();
                                }
                            });
                } catch (Exception e) {
                    Log.e(TAG, "Exception during prepareRecording/start: ", e);
                }

            } catch (Exception e) {
                Log.e(TAG, "CameraX setup failed", e);
                stopSelf();
            }
        }, cameraExecutor);
    }

    private boolean isDark(@NonNull ImageProxy image) {
        // Simple average brightness calculation
        ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
        byte[] yBuffer = new byte[yPlane.getBuffer().remaining()];
        yPlane.getBuffer().get(yBuffer);
        long sum = 0;
        for (byte b : yBuffer) sum += (b & 0xFF);
        double avg = sum / (double) yBuffer.length;
        return avg < 50; // Threshold, adjust as needed
    }

    private void saveRecording(String filePath, long duration, String cameraUsed) {
        EmergencyRecording recording = new EmergencyRecording();
        recording.filePath = filePath;
        recording.timestamp = System.currentTimeMillis();
        recording.duration = duration;
        recording.cameraUsed = cameraUsed;
        recordingDao.insert(recording);
        Log.i(TAG, "Saved emergency recording: " + filePath);
    }

    private String generateFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return "EMERGENCY_" + timeStamp + ".mp4";
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Emergency Recording",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Emergency Recording Active")
                .setContentText("Recording video and audio for your safety.")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (activeRecording != null && isRecording) {
            activeRecording.stop();
        }
        cameraExecutor.shutdown();
    }
}
