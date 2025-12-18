package cz.vut.fit.vqueue.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

import cz.vut.fit.vqueue.R;
import cz.vut.fit.vqueue.model.Queue;
import cz.vut.fit.vqueue.repo.QueueRepo;
import cz.vut.fit.vqueue.utils.Constants;

public class QueueManagementService extends Service {
    private static final String TAG = "QueueManagementService";
    // Constants used from Constants class

    private QueueRepo queueRepo;
    private BLEAdvertisementManager bleManager;
    private GATTServerManager gattManager;
    private Queue currentQueue;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "QueueManagementService created");
        queueRepo = QueueRepo.getInstance(this);
        bleManager = new BLEAdvertisementManager(this);
        gattManager = new GATTServerManager(this, bleManager);
        currentQueue = queueRepo.getCurrentQueue();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel serviceChannel = new android.app.NotificationChannel(
                    Constants.CHANNEL_ID_MANAGEMENT,
                    "Queue Management Service",
                    android.app.NotificationManager.IMPORTANCE_LOW);
            android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private android.app.Notification createNotification() {
        android.app.PendingIntent pendingIntent = new androidx.navigation.NavDeepLinkBuilder(this)
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.queueManagerFragment)
                .createPendingIntent();

        return new androidx.core.app.NotificationCompat.Builder(this, Constants.CHANNEL_ID_MANAGEMENT)
                .setContentTitle("Queue Active")
                .setContentText("Your queue is currently running.")
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getStringExtra("action");
            if (action != null) {
                switch (action) {
                    case "start_queue":
                        startQueue();
                        break;
                    case "call_next":
                        callNext();
                        break;
                    case "pause_queue":
                        pauseQueue();
                        break;
                    case "resume_queue":
                        resumeQueue();
                        break;
                    case "stop_queue":
                        stopQueue();
                        break;
                }
            }
        }

        return START_STICKY; // Restart service if killed
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    public void startQueue() {
        currentQueue = queueRepo.getCurrentQueue();
        if (currentQueue != null) {
            Log.i(TAG, "Starting queue: " + currentQueue.getName());
            startForeground(Constants.NOTIFICATION_ID_QUEUE_MANAGEMENT, createNotification());

            // Setup GATT server first
            if (gattManager.setupGATTServer()) {
                // Start BLE advertising
                bleManager.startAdvertising(currentQueue);
                Log.i(TAG, "Queue started successfully with BLE advertising and GATT server");
            } else {
                Log.e(TAG, "Failed to setup GATT server");
            }
        } else {
            Log.e(TAG, "No queue to start");
        }
    }

    public void callNext() {
        queueRepo.callNext();
        currentQueue = queueRepo.getCurrentQueue();
        if (currentQueue != null) {
            Log.i(TAG, "Called next number: " + currentQueue.getCurrentNumber());
            bleManager.updateAdvertisement(currentQueue);
        }
    }

    public void pauseQueue() {
        queueRepo.pauseQueue();
        currentQueue = queueRepo.getCurrentQueue();
        if (currentQueue != null) {
            Log.i(TAG, "Paused queue: " + currentQueue.getName());
            bleManager.updateAdvertisement(currentQueue);
        }
    }

    public void resumeQueue() {
        queueRepo.resumeQueue();
        currentQueue = queueRepo.getCurrentQueue();
        if (currentQueue != null) {
            Log.i(TAG, "Resumed queue: " + currentQueue.getName());
            bleManager.updateAdvertisement(currentQueue);
        }
    }

    public void stopQueue() {
        Log.i(TAG, "Stopping queue");
        bleManager.stopAdvertising();
        gattManager.closeGATTServer();
        queueRepo.stopQueue();
        stopSelf(); // Stop the service
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "QueueManagementService destroyed");
        if (bleManager != null) {
            bleManager.stopAdvertising();
        }
        if (gattManager != null) {
            gattManager.closeGATTServer();
        }
    }
}
