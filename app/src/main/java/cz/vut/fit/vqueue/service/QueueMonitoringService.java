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
    private String targetQueueName;
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
                // Changed to use QUEUE_NAME instead of DEVICE_ADDRESS
                targetQueueName = intent.getStringExtra("QUEUE_NAME");
                myTicketNumber = intent.getIntExtra("TICKET_NUMBER", -1);

                Log.i(TAG, "Starting monitoring - Queue: " + targetQueueName + ", Ticket: " + myTicketNumber);

                if (targetQueueName != null && myTicketNumber != -1) {
                    startForeground(NOTIFICATION_ID,
                            createNotification("Monitoring Queue", "Waiting for your turn..."));
                    startMonitoring();
                } else {
                    Log.e(TAG, "Missing queue name or ticket number");
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
                Log.i(TAG, "onQueueFound - Queue: " + queue.getName() + 
                    ", Current#: " + queue.getCurrentNumber() +
                    ", Target queue: " + targetQueueName);
                    
                // Match by queue name instead of device address
                if (queue.getName().equals(targetQueueName)) {
                    Log.i(TAG, "Queue name matches! Checking queue status...");
                    checkQueueStatus(queue);
                } else {
                    Log.d(TAG, "Queue name doesn't match, ignoring");
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


    private void checkQueueStatus(AdvertisedQueue queue) {
        int currentNumber = queue.getCurrentNumber();
        int waitTime = queue.getWaitTimeMinutes();

        Log.i(TAG, "checkQueueStatus - Current: " + currentNumber + ", MyTicket: " + myTicketNumber);

        // Broadcast queue status to WaitFragment
        Intent broadcastIntent = new Intent("QUEUE_STATUS_UPDATE");
        broadcastIntent.putExtra("CURRENT_NUMBER", currentNumber);
        broadcastIntent.putExtra("MY_TICKET", myTicketNumber);
        broadcastIntent.putExtra("WAIT_TIME", waitTime);
        sendBroadcast(broadcastIntent);
        Log.i(TAG, "Broadcast sent - Current: " + currentNumber);

        if (currentNumber >= myTicketNumber) {
            // It's your turn!
            Log.i(TAG, "IT'S YOUR TURN! Triggering alarm...");
            triggerAlarm();
            stopMonitoring();
            stopSelf();
        } else {
            // Update notification with estimated wait time
            Log.i(TAG, "Not your turn yet. Updating notification...");
            updateNotification("Queue Update", "Currently serving: #" + currentNumber + ". You are #" + myTicketNumber);
        }
    }

    private void triggerAlarm() {
        Log.i(TAG, "triggerAlarm() called - creating notification");
        
        // Create an intent to open the app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🔔 It's Your Turn!")
                .setContentText("Please proceed to the counter now!")
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_MAX)  // Maximum priority
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500, 200, 500})  // Vibration pattern
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)  // Sound
                .setDefaults(Notification.DEFAULT_ALL)  // All defaults (sound, vibrate, lights)
                .setOnlyAlertOnce(false)  // Alert every time
                .build();

        NotificationManager manager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager = getSystemService(NotificationManager.class);
        }
        if (manager != null) {
            Log.i(TAG, "Sending notification with ID: " + (NOTIFICATION_ID + 1));
            manager.notify(NOTIFICATION_ID + 1, notification);
        } else {
            Log.e(TAG, "NotificationManager is null!");
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
