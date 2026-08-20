package com.pokemonshowdown.app;

import android.app.NotificationManager;
import android.content.Context;

import android.webkit.JavascriptInterface;

/**
 * JavaScript interface that receives game events from the WebView
 * and sends notifications when it's the player's turn.
 * Only notifies when the app is in the background.
 */
public class TurnNotifier {

    private final Context context;
    private static boolean enabled = true;
    private static boolean isAppInForeground = true;

    public TurnNotifier(Context context) {
        this.context = context;
        NotificationHelper.createChannels(context);
    }

    public static void setAppInForeground(boolean fg) {
        isAppInForeground = fg;
    }

    @JavascriptInterface
    public void onTurnDetected(String message) {
        if (enabled && !isAppInForeground) send(message);
    }

    @JavascriptInterface
    public void onOpponentJoin(String opponentName) {
        if (!enabled || isAppInForeground) return;
        send(context.getString(R.string.turn_notification_opponent_joined, opponentName));
    }

    @JavascriptInterface
    public void onBattleStart() {
        if (enabled && !isAppInForeground) {
            send(context.getString(R.string.turn_notification_battle_start));
        }
    }

    @JavascriptInterface
    public void setEnabled(boolean e) { enabled = e; }

    @JavascriptInterface
    public boolean isEnabled() { return enabled; }

    private void send(String message) {
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(
                    NotificationHelper.ID_TURNS,
                    NotificationHelper.buildTurnNotification(context, message)
            );
        }
    }
}
