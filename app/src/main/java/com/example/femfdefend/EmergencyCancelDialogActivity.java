package com.example.femfdefend;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class EmergencyCancelDialogActivity extends Activity {
    public static final String EXTRA_CONFIRMED = "confirmed";
    private CountDownTimer timer;
    private TextView timerTextView;
    private Button cancelButton;
    private int secondsLeft = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(false);
        getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        setContentView(R.layout.dialog_cancel_emergency);

        timerTextView = findViewById(R.id.timerTextView);
        cancelButton = findViewById(R.id.cancelButton);

        timerTextView.setText("Emergency will be triggered in 30 seconds!");

        cancelButton.setOnClickListener(v -> cancel());

        timer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                secondsLeft = (int) (millisUntilFinished / 1000);
                timerTextView.setText("Emergency will be triggered in " + secondsLeft + " seconds!");
            }
            @Override
            public void onFinish() {
                confirm();
            }
        };
        timer.start();
    }

    private void confirm() {
        Intent intent = new Intent("com.example.femfdefend.EMERGENCY_CONFIRMED");
        sendBroadcast(intent);
        // Stop emergency recording service
        stopService(new Intent(this, com.example.femfdefend.services.EmergencyRecordingService.class));
        finishWithResult(true);
    }
    private void cancel() {
        // Stop emergency recording service
        stopService(new Intent(this, com.example.femfdefend.services.EmergencyRecordingService.class));
        finishWithResult(false);
    }
    private void finishWithResult(boolean confirmed) {
        if (timer != null) timer.cancel();
        Intent result = new Intent();
        result.putExtra(EXTRA_CONFIRMED, confirmed);
        setResult(Activity.RESULT_OK, result);
        finish();
    }
    // @Override
    // protected void onDestroy() {
    //     if (timer != null) timer.cancel();
    //     super.onDestroy();
    // }
} 