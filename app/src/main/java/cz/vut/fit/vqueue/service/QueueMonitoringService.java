package cz.vut.fit.vqueue.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import cz.vut.fit.vqueue.R;
import cz.vut.fit.vqueue.activities.MainActivity;
import cz.vut.fit.vqueue.model.AdvertisedQueue;

public class QueueMonitoringService extends Service {
    private static final String TAG = "QueueMonitoringService";
    private static final String CHANNEL_ID = "QueueMonitoringChannel";
    private static final int NOTIFICATION_ID = 2;

    private BLEScanner bleScanner;
    private String targetDeviceAddress;
    private int myTicketNumber;
    private boolean isMonitoring = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        bleScanner = new BLEScanner(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("START_MONITORING".equals(action)) {
                targetDeviceAddress = intent.getStringExtra("DEVICE_ADDRESS");
                myTicketNumber = intent.getIntExtra("TICKET_NUMBER", -1);

                if (targetDeviceAddress != null && myTicketNumber != -1) {
                    startForeground(NOTIFICATION_ID,
                            createNotification("Monitoring Queue", "Waiting for your turn..."));
                    startMonitoring();
                } else {
                    stopSelf();
                }
            } else if ("STOP_MONITORING".equals(action)) {
                stopMonitoring();
                stopSelf();
            }
        }
        return START_STICKY;
    }

    private void startMonitoring() {
        if (isMonitoring)
            return;
        isMonitoring = true;

        bleScanner.setOnQueueFoundListener(new BLEScanner.OnQueueFoundListener() {
            @Override
            public void onQueueFound(AdvertisedQueue queue) {
                if (queue.getDeviceAddress().equals(targetDeviceAddress)) {
                    checkQueueStatus(queue);
                }
            }

            @Override
            public void onScanComplete() {
                // Restart scan if monitoring is still active
                if (isMonitoring) {
                    bleScanner.startScan();
                }
            }
        });

        bleScanner.startScan();
    }

    public static final String ACTION_QUEUE_UPDATE = "cz.vut.fit.vqueue.ACTION_QUEUE_UPDATE";
    public static final String EXTRA_CURRENT_NUMBER = "EXTRA_CURRENT_NUMBER";
    public static final String EXTRA_WAIT_TIME = "EXTRA_WAIT_TIME";
    public static final String EXTRA_STATUS = "EXTRA_STATUS";

    private void checkQueueStatus(AdvertisedQueue queue) {
        int currentNumber = queue.getCurrentNumber();
        int waitTime = queue.getWaitTimeMinutes();

        // Broadcast update to UI
        Intent updateIntent = new Intent(ACTION_QUEUE_UPDATE);
        updateIntent.putExtra(EXTRA_CURRENT_NUMBER, currentNumber);
        updateIntent.putExtra(EXTRA_WAIT_TIME, waitTime);
        sendBroadcast(updateIntent);

        if (currentNumber >= myTicketNumber) {
            // It's your turn!
            triggerAlarm();
            stopMonitoring();
            stopSelf();
        } else {
            // Update notification with estimated wait time
            updateNotification("Queue Update", "Currently serving: #" + currentNumber + ". You are #" + myTicketNumber);
        }
    }

    private void triggerAlarm() {
        // Create an intent to open the app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("It's Your Turn!")
                .setContentText("Please proceed to the counter.")
                .setSmallIcon(R.mipmap.ic_launcher_foreground) // Use valid icon
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .build();

        NotificationManager manager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager = getSystemService(NotificationManager.class);
        }
        if (manager != null) {
            manager.notify(NOTIFICATION_ID + 1, notification);
        }
    }

    private void stopMonitoring() {
        isMonitoring = false;
        if (bleScanner != null) {
            bleScanner.stopScan();
        }
    }

    private Notification createNotification(String title, String content) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher_foreground) // Use valid icon
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(String title, String content) {
        NotificationManager manager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager = getSystemService(NotificationManager.class);
        }
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(title, content));
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Queue Monitoring Service",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMonitoring();
    }
}
