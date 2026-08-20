package com.pokemonshowdown.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import android.webkit.JavascriptInterface;

/**
 * JavaScript interface that receives game events from the WebView
 * and sends notifications when it's the player's turn.
 */
public class TurnNotifier {

    private static final String CHANNEL_ID = "pokemon_showdown_turn";
    private static final int NOTIFICATION_ID = 2001;
    private final Context context;
    private static boolean enabled = true;
    private static boolean isAppInForeground = true;

    public TurnNotifier(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    public static void setAppInForeground(boolean inForeground) {
        isAppInForeground = inForeground;
    }

    @JavascriptInterface
    public void onTurnDetected(String message) {
        if (!enabled || isAppInForeground) return;
        sendNotification(message);
    }

    @JavascriptInterface
    public void onOpponentJoin(String opponentName) {
        if (!enabled || isAppInForeground) return;
        String text = context.getString(R.string.turn_notification_opponent_joined, opponentName);
        sendNotification(text);
    }

    @JavascriptInterface
    public void onBattleStart() {
        if (!enabled || isAppInForeground) return;
        sendNotification(context.getString(R.string.turn_notification_battle_start));
    }

    @JavascriptInterface
    public void setEnabled(boolean enabled) {
        TurnNotifier.enabled = enabled;
    }

    @JavascriptInterface
    public boolean isEnabled() {
        return enabled;
    }

    private void sendNotification(String message) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.turn_notification_title))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.turn_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.turn_notification_channel_description));
            channel.enableVibration(true);
            channel.enableLights(true);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
