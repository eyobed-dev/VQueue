package et.dev.eyobed.vqueue.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

import et.dev.eyobed.vqueue.R;
import et.dev.eyobed.vqueue.model.Queue;
import et.dev.eyobed.vqueue.repo.QueueRepo;
import et.dev.eyobed.vqueue.utils.Constants;

public class QueueManagementService extends Service {
    private static final String TAG = "QueueManagementService";

    public static final String ACTION_START_QUEUE = "et.dev.eyobed.vqueue.START_QUEUE";
    public static final String ACTION_STOP_QUEUE = "et.dev.eyobed.vqueue.STOP_QUEUE";
    public static final String ACTION_CALL_NEXT = "et.dev.eyobed.vqueue.CALL_NEXT";
    public static final String ACTION_PAUSE_QUEUE = "et.dev.eyobed.vqueue.PAUSE_QUEUE";
    public static final String ACTION_RESUME_QUEUE = "et.dev.eyobed.vqueue.RESUME_QUEUE";
    public static final String ACTION_UPDATE_ADVERTISEMENT = "et.dev.eyobed.vqueue.UPDATE_ADVERTISEMENT";

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
                    getString(R.string.notification_channel_management),
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

        android.app.PendingIntent pauseIntent = android.app.PendingIntent.getService(this, 101,
                new Intent(this, QueueManagementService.class).setAction(ACTION_PAUSE_QUEUE),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        android.app.PendingIntent resumeIntent = android.app.PendingIntent.getService(this, 102,
                new Intent(this, QueueManagementService.class).setAction(ACTION_RESUME_QUEUE),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        android.app.PendingIntent stopIntent = android.app.PendingIntent.getService(this, 103,
                new Intent(this, QueueManagementService.class).setAction(ACTION_STOP_QUEUE),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

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
        if (intent != null) {
            String action = intent.getAction();
            if (action == null) {
                // Fallback for old "action" extra if still used anywhere
                action = intent.getStringExtra("action");
            }

            if (action != null) {
                switch (action) {
                    case ACTION_START_QUEUE:
                    case "start_queue":
                        startQueue();
                        break;
                    case ACTION_CALL_NEXT:
                    case "call_next":
                        callNext();
                        break;
                    case ACTION_PAUSE_QUEUE:
                    case "pause_queue":
                        pauseQueue();
                        break;
                    case ACTION_RESUME_QUEUE:
                    case "resume_queue":
                        resumeQueue();
                        break;
                    case ACTION_STOP_QUEUE:
                    case "stop_queue":
                        stopQueue();
                        break;
                    case ACTION_UPDATE_ADVERTISEMENT:
                    case "update_advertisement":
                        updateAdvertisement();
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
        android.app.NotificationManager manager = (android.app.NotificationManager) getSystemService(
                android.content.Context.NOTIFICATION_SERVICE);
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
        Log.i(TAG, "QueueManagementService destroyed");
        if (bleManager != null) {
            bleManager.stopAdvertising();
        }
        if (gattManager != null) {
            gattManager.closeGATTServer();
        }
    }
}
