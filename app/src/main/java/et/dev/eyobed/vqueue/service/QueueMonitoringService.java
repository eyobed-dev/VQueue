package et.dev.eyobed.vqueue.service;

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
import et.dev.eyobed.vqueue.R;
import et.dev.eyobed.vqueue.model.AdvertisedQueue;
import et.dev.eyobed.vqueue.utils.Constants;

public class QueueMonitoringService extends Service {
    private static final String TAG = "QueueMonitoringService";
    // Constants used from Constants class

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
                    startForeground(Constants.NOTIFICATION_ID_QUEUE_MONITORING,
                            createNotification(getString(R.string.notification_title_monitoring),
                                    getString(R.string.notification_content_waiting)));
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
                // Match by queue name instead of device address
                if (queue.getName().equals(targetQueueName)) {
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

    private void checkQueueStatus(AdvertisedQueue queue) {
        int currentNumber = queue.getCurrentNumber();
        int waitTime = queue.getWaitTimeMinutes();

        // Broadcast queue status to WaitFragment
        Intent broadcastIntent = new Intent("QUEUE_STATUS_UPDATE");
        broadcastIntent.putExtra("CURRENT_NUMBER", currentNumber);
        broadcastIntent.putExtra("MY_TICKET", myTicketNumber);
        broadcastIntent.putExtra("WAIT_TIME", waitTime);
        sendBroadcast(broadcastIntent);

        if (currentNumber >= myTicketNumber) {
            // It's your turn!
            triggerAlarm();
            stopMonitoring();
            stopSelf();
        } else {
            // Update notification with estimated wait time
            updateNotification(getString(R.string.notification_title_queue_update),
                    getString(R.string.notification_content_update_format, currentNumber, myTicketNumber));
        }
    }

    private void triggerAlarm() {
        Log.i(TAG, "triggerAlarm() called - creating notification");

        android.os.Bundle args = new android.os.Bundle();
        if (targetQueueName != null) {
            args.putString("QUEUE_NAME", targetQueueName);
            args.putInt("TICKET_NUMBER", myTicketNumber);
        }

        PendingIntent pendingIntent = new androidx.navigation.NavDeepLinkBuilder(this)
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.waitFragment)
                .setArguments(args)
                .createPendingIntent();

        Notification notification = new NotificationCompat.Builder(this, Constants.CHANNEL_ID_MONITORING)
                .setContentTitle(getString(R.string.notification_title_turn_alarm))
                .setContentText(getString(R.string.notification_content_turn_alarm))
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_MAX) // Maximum priority
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setVibrate(new long[] { 0, 500, 200, 500, 200, 500 }) // Some crazy vibration
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI) // Sound
                .setDefaults(Notification.DEFAULT_ALL)
                .setOnlyAlertOnce(false) // Alert every time
                .build();

        NotificationManager manager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager = getSystemService(NotificationManager.class);
        }
        if (manager != null) {
            Log.i(TAG, "Sending notification with ID: " + Constants.NOTIFICATION_ID_ALARM);
            manager.notify(Constants.NOTIFICATION_ID_ALARM, notification);
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
        android.os.Bundle args = new android.os.Bundle();
        if (targetQueueName != null) {
            args.putString("QUEUE_NAME", targetQueueName);
            args.putInt("TICKET_NUMBER", myTicketNumber);
        }

        PendingIntent pendingIntent = new androidx.navigation.NavDeepLinkBuilder(this)
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.waitFragment)
                .setArguments(args)
                .createPendingIntent();

        // Create Leave Queue action
        Intent leaveIntent = new Intent(this, QueueMonitoringService.class);
        leaveIntent.setAction("STOP_MONITORING");
        PendingIntent leavePendingIntent = PendingIntent.getService(
                this,
                0,
                leaveIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, Constants.CHANNEL_ID_MONITORING)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher_foreground) // Use valid icon
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_stop, "Leave Queue", leavePendingIntent)  // Leave action
                .setOnlyAlertOnce(true)  // Only alert on first notification, not on updates
                .setSilent(true)  // Silent notification (no sound/vibration)
                .build();
    }

    private void updateNotification(String title, String content) {
        NotificationManager manager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager = getSystemService(NotificationManager.class);
        }
        if (manager != null) {
            manager.notify(Constants.NOTIFICATION_ID_QUEUE_MONITORING, createNotification(title, content));
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    Constants.CHANNEL_ID_MONITORING,
                    getString(R.string.notification_channel_monitoring),
                    NotificationManager.IMPORTANCE_HIGH);  // HIGH for alarm with sound/vibration
            
            // Enable vibration and sound on the channel
            serviceChannel.enableVibration(true);
            serviceChannel.setVibrationPattern(new long[] { 0, 500, 200, 500, 200, 500 });
            serviceChannel.setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            );
            
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
