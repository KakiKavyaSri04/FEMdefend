package com.example.femfdefend.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.femfdefend.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CancelEmergencyDialog {
    private static final long TIMER_DURATION = 30000; // 30 seconds
    private static final long TIMER_INTERVAL = 1000; // 1 second
    private static final long VIBRATION_PATTERN[] = {0, 1000, 500}; // Start, Vibrate, Sleep

    private final Context context;
    private AlertDialog dialog;
    private TextView timerTextView;
    private CountDownTimer countDownTimer;
    private Vibrator vibrator;
    private final OnEmergencyActionListener listener;

    public interface OnEmergencyActionListener {
        void onEmergencyConfirmed();
        void onEmergencyCancelled();
    }

    public CancelEmergencyDialog(Context context, OnEmergencyActionListener listener) {
        this.context = context;
        this.listener = listener;
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void show() {
        // Inflate custom layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_cancel_emergency, null);
        
        // Initialize views
        timerTextView = dialogView.findViewById(R.id.timerTextView);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Create dialog
        dialog = new MaterialAlertDialogBuilder(context, R.style.AlertDialog_Emergency)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Set window properties
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        // Set up cancel button
        cancelButton.setOnClickListener(v -> {
            stopEverything();
            // Stop emergency recording service
            context.stopService(new Intent(context, com.example.femfdefend.services.EmergencyRecordingService.class));
            listener.onEmergencyCancelled();
        });

        // Start continuous vibration
        startContinuousVibration();

        // Start the countdown timer
        startTimer();

        // Show the dialog
        dialog.show();
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(TIMER_DURATION, TIMER_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                updateTimerDisplay(secondsRemaining);
            }

            @Override
            public void onFinish() {
                stopEverything();
                listener.onEmergencyConfirmed();
            }
        }.start();
    }

    private void updateTimerDisplay(int seconds) {
        if (timerTextView != null) {
            String text = String.format("Emergency will be triggered in %d seconds", seconds);
            timerTextView.setText(text);
            // Make text red when less than 10 seconds remain
            if (seconds < 10) {
                timerTextView.setTextColor(Color.RED);
            }
        }
    }

    private void startContinuousVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                VibrationEffect effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0); // 0 means repeat indefinitely
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(VIBRATION_PATTERN, 0); // 0 means repeat indefinitely
            }

        }

    }

    private void stopEverything() {
        // Stop timer
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Stop vibration
        if (vibrator != null) {
            vibrator.cancel();
        }

        // Dismiss dialog
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public void dismiss() {
        stopEverything();
    }
} 