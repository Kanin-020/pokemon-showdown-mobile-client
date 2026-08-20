package com.pokemonshowdown.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationCompat;

/**
 * Centralized notification builder.
 * Single source of truth for creating channels and notifications (DRY).
 */
public final class NotificationHelper {

    public static final String CHANNEL_MEDIA = "pokemon_showdown_media";
    public static final String CHANNEL_TURNS = "pokemon_showdown_turn";
    public static final int ID_MEDIA = 1001;
    public static final int ID_TURNS = 2001;

    private NotificationHelper() {}

    /** Creates both notification channels (idempotent). */
    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel media = new NotificationChannel(
                CHANNEL_MEDIA,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        media.setDescription(context.getString(R.string.notification_channel_description));
        media.setShowBadge(false);
        media.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(media);

        NotificationChannel turns = new NotificationChannel(
                CHANNEL_TURNS,
                context.getString(R.string.turn_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        turns.setDescription(context.getString(R.string.turn_notification_channel_description));
        turns.enableVibration(true);
        turns.enableLights(true);
        nm.createNotificationChannel(turns);
    }

    /** Builds the persistent foreground-service notification (media-style with play/pause). */
    public static Notification buildMediaNotification(
            Context context,
            boolean isMuted,
            PendingIntent contentIntent,
            PendingIntent muteIntent
    ) {
        String muteText = context.getString(
                isMuted ? R.string.notification_action_enable_sound : R.string.notification_action_mute
        );
        int muteIcon = isMuted
                ? android.R.drawable.ic_media_play
                : android.R.drawable.ic_media_pause;

        return new NotificationCompat.Builder(context, CHANNEL_MEDIA)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(
                        isMuted ? R.string.notification_muted : R.string.notification_active
                ))
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(
                        context.getResources(), R.mipmap.ic_launcher
                ))
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(muteIcon, muteText, muteIntent)
                .build();
    }

    /** Builds a one-shot turn notification (high priority, auto-cancel). */
    public static Notification buildTurnNotification(
            Context context,
            String message
    ) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(context, CHANNEL_TURNS)
                .setContentTitle(context.getString(R.string.turn_notification_title))
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(
                        context.getResources(), R.mipmap.ic_launcher
                ))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
    }
}
