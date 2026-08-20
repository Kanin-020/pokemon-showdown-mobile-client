package com.pokemonshowdown.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

/**
 * Foreground service that keeps the app alive in the background.
 * Shows a persistent notification with play/pause mute control.
 */
public class KeepAliveService extends Service {

    private static final String CHANNEL_ID = "pokemon_showdown_media";
    private static final int NOTIFICATION_ID = 1001;
    public static final String ACTION_TOGGLE_MUTE = "com.pokemonshowdown.app.ACTION_TOGGLE_MUTE";
    public static final String EXTRA_MUTED = "extra_muted";

    private PowerManager.WakeLock wakeLock;
    private BroadcastReceiver muteReceiver;
    private static boolean isMuted = false;
    private static boolean isRunning = false;

    // Callback for communicating mute state to MainActivity
    public interface MuteCallback {
        void onMuteChanged(boolean muted);
    }
    private static MuteCallback muteCallback;

    public static void setMuteCallback(MuteCallback callback) {
        muteCallback = callback;
    }

    public static boolean isMuted() {
        return isMuted;
    }

    public static boolean isServiceRunning() {
        return isRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        createNotificationChannel();
        setupMuteReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_TOGGLE_MUTE.equals(intent.getAction())) {
            isMuted = !isMuted;
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.notify(NOTIFICATION_ID, buildNotification());
            }
            if (muteCallback != null) {
                muteCallback.onMuteChanged(isMuted);
            }
        }

        if (intent != null && intent.hasExtra(EXTRA_MUTED)) {
            isMuted = intent.getBooleanExtra(EXTRA_MUTED, false);
        }

        startForeground(NOTIFICATION_ID, buildNotification());
        acquireWakeLock();
        return START_STICKY;
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Mute/Unmute action (shown as a transport control button)
        Intent muteIntent = new Intent(this, KeepAliveService.class);
        muteIntent.setAction(ACTION_TOGGLE_MUTE);
        PendingIntent mutePendingIntent = PendingIntent.getService(
            this, 1, muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String muteText = isMuted ? getString(R.string.notification_action_enable_sound) : getString(R.string.notification_action_mute);
        int muteIcon = isMuted
            ? android.R.drawable.ic_media_play
            : android.R.drawable.ic_media_pause;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(isMuted ? getString(R.string.notification_muted) : getString(R.string.notification_active))
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(muteIcon, muteText, mutePendingIntent);

        return builder.build();
    }

    private void setupMuteReceiver() {
        muteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_TOGGLE_MUTE.equals(intent.getAction())) {
                    isMuted = !isMuted;
                    NotificationManager nm = getSystemService(NotificationManager.class);
                    if (nm != null) {
                        nm.notify(NOTIFICATION_ID, buildNotification());
                    }
                    if (muteCallback != null) {
                        muteCallback.onMuteChanged(isMuted);
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_TOGGLE_MUTE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(muteReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(muteReceiver, filter);
        }
    }

    private void acquireWakeLock() {
        if (wakeLock == null || !wakeLock.isHeld()) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "PokemonShowdown::KeepAlive"
            );
            wakeLock.acquire(60 * 60 * 1000L); // 1 hour max, service restarts with START_STICKY
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        releaseWakeLock();
        if (muteReceiver != null) {
            unregisterReceiver(muteReceiver);
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.notification_channel_description));
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
