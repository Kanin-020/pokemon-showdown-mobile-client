package com.pokemonshowdown.app;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Helper to start/stop the foreground KeepAliveService.
 * Encapsulates the API-level branching for startForegroundService.
 */
public final class ServiceHelper {

    private ServiceHelper() {}

    public static void start(Context context, boolean isMuted) {
        Intent intent = new Intent(context, KeepAliveService.class);
        intent.putExtra(KeepAliveService.EXTRA_MUTED, isMuted);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, KeepAliveService.class));
    }
}
