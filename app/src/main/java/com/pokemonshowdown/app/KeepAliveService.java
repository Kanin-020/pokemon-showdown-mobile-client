package com.pokemonshowdown.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

/**
 * Foreground service that keeps the app alive in the background.
 * Shows a persistent notification with play/pause mute control.
 */
public class KeepAliveService extends Service {

    public static final String ACTION_TOGGLE_MUTE =
            "com.pokemonshowdown.app.ACTION_TOGGLE_MUTE";
    public static final String EXTRA_MUTED = "extra_muted";

    private PowerManager.WakeLock wakeLock;
    private BroadcastReceiver muteReceiver;
    private static boolean isMuted = false;
    private static boolean isRunning = false;

    public interface MuteCallback {
        void onMuteChanged(boolean muted);
    }
    private static MuteCallback muteCallback;

    public static void setMuteCallback(MuteCallback cb) { muteCallback = cb; }
    public static boolean isMuted() { return isMuted; }
    public static boolean isServiceRunning() { return isRunning; }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        NotificationHelper.createChannels(this);
        setupMuteReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_TOGGLE_MUTE.equals(intent.getAction())) {
            isMuted = !isMuted;
            notifyMuteChanged();
        }
        if (intent != null && intent.hasExtra(EXTRA_MUTED)) {
            isMuted = intent.getBooleanExtra(EXTRA_MUTED, false);
        }

        startForeground(NotificationHelper.ID_MEDIA, buildNotification());
        acquireWakeLock();
        return START_STICKY;
    }

    private Notification buildNotification() {
        PendingIntent contentPi = PendingIntent.getActivity(
                this, 0,
                new Intent(this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        PendingIntent mutePi = PendingIntent.getService(
                this, 1,
                new Intent(this, KeepAliveService.class)
                        .setAction(ACTION_TOGGLE_MUTE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        return NotificationHelper.buildMediaNotification(this, isMuted, contentPi, mutePi);
    }

    private void notifyMuteChanged() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NotificationHelper.ID_MEDIA, buildNotification());
        if (muteCallback != null) muteCallback.onMuteChanged(isMuted);
    }

    private void setupMuteReceiver() {
        muteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (ACTION_TOGGLE_MUTE.equals(intent.getAction())) {
                    isMuted = !isMuted;
                    notifyMuteChanged();
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
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "PokemonShowdown::KeepAlive"
            );
            wakeLock.acquire(60 * 60 * 1000L);
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (muteReceiver != null) unregisterReceiver(muteReceiver);
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
