package com.example.femfdefend;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class EmergencyCameraActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 11;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    private static final String TAG = "EmergencyCameraActivity";
    private PreviewView previewView;
    private VideoCapture<Recorder> videoCapture;
    private Recording activeRecording;
    private BroadcastReceiver stopReceiver;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "EmergencyCameraActivity onCreate");
        setContentView(R.layout.activity_camera_x_test); // Reuse the CameraX test layout
        previewView = findViewById(R.id.previewView);

        // Register broadcast receiver to stop recording
        stopReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "Received STOP_EMERGENCY_RECORDING broadcast");
                Toast.makeText(EmergencyCameraActivity.this, "Stopping emergency recording in 3 seconds...", Toast.LENGTH_SHORT).show();
                handler.postDelayed(() -> {
                    stopRecording();
                    finish();
                }, 3000); // 3 second delay before stopping
            }
        };
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopReceiver, new IntentFilter("com.example.femfdefend.STOP_EMERGENCY_RECORDING"), android.content.Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stopReceiver, new IntentFilter("com.example.femfdefend.STOP_EMERGENCY_RECORDING"));
        }

        if (allPermissionsGranted()) {
            Log.i(TAG, "Permissions granted, starting camera");
            startCamera();
        } else {
            Log.i(TAG, "Requesting permissions");
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.i(TAG, "Permissions granted after request, starting camera");
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Permissions denied, finishing activity");
                finish();
            }
        }
    }

    private void startCamera() {
        Log.i(TAG, "startCamera called");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                Log.i(TAG, "CameraProvider future complete");
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        videoCapture
                );
                Log.i(TAG, "CameraX started successfully, starting recording");
                startRecording(); // Automatically start recording
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "CameraX start failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startRecording() {
        Log.i(TAG, "startRecording called");
        if (videoCapture == null) {
            Log.e(TAG, "videoCapture is null, cannot start recording");
            return;
        }
        File file = new File(getFilesDir(), "EMERGENCY_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".mp4");
        Log.i(TAG, "Recording to file: " + file.getAbsolutePath());
        FileOutputOptions outputOptions = new FileOutputOptions.Builder(file).build();
        activeRecording = videoCapture.getOutput()
                .prepareRecording(this, outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this), event -> {
                    Log.i(TAG, "VideoRecordEvent: " + event);
                    if (event instanceof VideoRecordEvent.Start) {
                        Toast.makeText(this, "Emergency recording started", Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "Emergency recording started");
                    } else if (event instanceof VideoRecordEvent.Finalize) {
                        Toast.makeText(this, "Recording stopped. Saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        Log.i(TAG, "Recording stopped. Saved: " + file.getAbsolutePath());
                        finish(); // Close the activity when done
                    }
                });
    }

    // Optionally, provide a way to stop recording programmatically
    public void stopRecording() {
        Log.i(TAG, "stopRecording called");
        if (activeRecording != null) {
            activeRecording.stop();
            activeRecording = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
        if (stopReceiver != null) {
            unregisterReceiver(stopReceiver);
        }
    }
} 