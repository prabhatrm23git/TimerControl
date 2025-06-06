package com.example.timercontrol;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "TimerControl";
    private static final String KEY_SECONDS = "saved_seconds";
    private static final String KEY_IS_RUNNING = "is_running";

    private Button startBtn;
    private Button pauseBtn;
    private TextView timerTextView;
    private int seconds = 0;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning.get()) {
                seconds++;
                updateTimer();
                mainHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        restoreState(savedInstanceState);
        setupClickListeners();
        updateTimer();
    }

    private void initializeViews() {
        try {
            startBtn = findViewById(R.id.startBtn);
            pauseBtn = findViewById(R.id.pauseBtn);
            timerTextView = findViewById(R.id.timerTextView);
            
            if (startBtn == null || pauseBtn == null || timerTextView == null) {
                throw new IllegalStateException("One or more views not found in the layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            showToast("Error initializing the app. Please restart.");
        }
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            seconds = savedInstanceState.getInt(KEY_SECONDS, 0);
            boolean wasRunning = savedInstanceState.getBoolean(KEY_IS_RUNNING, false);
            if (wasRunning) {
                startTimer();
            }
        }
    }

    private void setupClickListeners() {
        startBtn.setOnClickListener(v -> startTimer());
        pauseBtn.setOnClickListener(v -> pauseTimer());
    }

    private synchronized void startTimer() {
        if (!isRunning.getAndSet(true)) {
            if (executorService == null || executorService.isShutdown()) {
                executorService = Executors.newSingleThreadExecutor();
            }
            mainHandler.post(timerRunnable);
            updateButtonStates(true);
        }
    }

    private synchronized void pauseTimer() {
        if (isRunning.getAndSet(false)) {
            mainHandler.removeCallbacks(timerRunnable);
            updateButtonStates(false);
        }
    }

    private void updateTimer() {
        try {
            String timeFormat = String.format("%02d:%02d:%02d",
                    seconds / 3600, (seconds % 3600) / 60, seconds % 60);
            timerTextView.setText(timeFormat);
        } catch (Exception e) {
            Log.e(TAG, "Error updating timer", e);
        }
    }

    private void updateButtonStates(boolean isRunning) {
        startBtn.setEnabled(!isRunning);
        pauseBtn.setEnabled(isRunning);
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SECONDS, seconds);
        outState.putBoolean(KEY_IS_RUNNING, isRunning.get());
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause timer when activity is not visible
        if (isRunning.get()) {
            pauseTimer();
        }
    }
// hi prabhat
    @Override
    protected void onDestroy() {
        // Clean up resources
        isRunning.set(false);
        mainHandler.removeCallbacks(timerRunnable);
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        super.onDestroy();
    }
}