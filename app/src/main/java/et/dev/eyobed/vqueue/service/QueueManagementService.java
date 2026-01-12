package et.dev.eyobed.vqueue.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;

import android.content.Context;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

import et.dev.eyobed.vqueue.R;
import et.dev.eyobed.vqueue.model.Queue;
import et.dev.eyobed.vqueue.repo.QueueRepo;
import et.dev.eyobed.vqueue.utils.Constants;

public class QueueManagementService extends Service {
    private static final String TAG = "QueueManagementService";

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
            NotificationChannel serviceChannel = new NotificationChannel(
                    Constants.CHANNEL_ID_MANAGEMENT,
                    getString(R.string.notification_channel_management),
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        PendingIntent pendingIntent = new androidx.navigation.NavDeepLinkBuilder(this)
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.queueManagerFragment)
                .createPendingIntent();

        PendingIntent pauseIntent = PendingIntent.getService(this, 101,
                new Intent(this, QueueManagementService.class).setAction(Constants.ACTION_PAUSE_QUEUE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent resumeIntent = PendingIntent.getService(this, 102,
                new Intent(this, QueueManagementService.class).setAction(Constants.ACTION_RESUME_QUEUE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent stopIntent = PendingIntent.getService(this, 103,
                new Intent(this, QueueManagementService.class).setAction(Constants.ACTION_STOP_QUEUE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this,
                Constants.CHANNEL_ID_MANAGEMENT)
                .setContentTitle(getString(R.string.notification_title_queue_active))
                .setContentText(getString(R.string.notification_content_queue_running))
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        if (currentQueue != null) {
            if (currentQueue.isPaused()) {
                builder.addAction(android.R.drawable.ic_media_play, getString(R.string.notification_action_resume),
                        resumeIntent);
            } else {
                builder.addAction(android.R.drawable.ic_media_pause, getString(R.string.notification_action_pause),
                        pauseIntent);
            }
        }

        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notification_action_stop),
                stopIntent);

        return builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case Constants.ACTION_START_QUEUE:
                    startQueue();
                    break;
                case Constants.ACTION_CALL_NEXT:
                    callNext();
                    break;
                case Constants.ACTION_PAUSE_QUEUE:
                    pauseQueue();
                    break;
                case Constants.ACTION_RESUME_QUEUE:
                    resumeQueue();
                    break;
                case Constants.ACTION_UPDATE_ADVERTISEMENT:
                    updateAdvertisement();
                    break;
                case Constants.ACTION_STOP_QUEUE:
                    stopQueue();
                    break;
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
            updateNotification();
        }
    }

    public void resumeQueue() {
        queueRepo.resumeQueue();
        currentQueue = queueRepo.getCurrentQueue();
        if (currentQueue != null) {
            Log.i(TAG, "Resumed queue: " + currentQueue.getName());
            bleManager.updateAdvertisement(currentQueue);
            updateNotification();
        }
    }

    public void updateAdvertisement() {
        currentQueue = queueRepo.getCurrentQueue();
        if (currentQueue != null) {
            Log.i(TAG, "Updating advertisement for queue: " + currentQueue.getName());
            bleManager.updateAdvertisement(currentQueue);
            updateNotification();
        }
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(Constants.NOTIFICATION_ID_QUEUE_MANAGEMENT, createNotification());
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
        if (bleManager != null) {
            bleManager.stopAdvertising();
        }
        if (gattManager != null) {
            gattManager.closeGATTServer();
        }
    }
}
