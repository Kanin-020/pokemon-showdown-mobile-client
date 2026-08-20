package com.pokemonshowdown.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.webkit.WebView;

/**
 * Main entry point. Keeps a WebView open on Pokemon Showdown
 * and delegates all concerns to dedicated helpers.
 */
public class MainActivity extends AppCompatActivity {

    private static final String URL = "https://play.pokemonshowdown.com/";
    private static final String KEY_WEBVIEW_URL = "webview_url";

    private WebView webView;
    private AudioController audioController;
    private TurnNotifier turnNotifier;
    private boolean isMutedByService = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioController = new AudioController(audioManager);
        turnNotifier = new TurnNotifier(this);

        new WebViewConfigurator(webView, progressBar, turnNotifier).configure();
        setupBackButton();
        setupMuteCallback();
        requestNotificationPermission();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(URL);
        }
    }

    private void setupBackButton() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.dialog_exit_title)
                            .setMessage(R.string.dialog_exit_message)
                            .setPositiveButton(R.string.dialog_exit_button, (d, w) -> {
                                ServiceHelper.stop(MainActivity.this);
                                finish();
                            })
                            .setNegativeButton(R.string.dialog_cancel_button, null)
                            .show();
                }
            }
        });
    }

    private void setupMuteCallback() {
        KeepAliveService.setMuteCallback(muted -> runOnUiThread(() -> {
            isMutedByService = muted;
            audioController.applyState(muted);
        }));
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────

    @Override
    protected void onStart() {
        super.onStart();
        TurnNotifier.setAppInForeground(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        webView.postDelayed(() -> JavaScriptInjector.injectReconnection(webView), 500);
        if (isMutedByService) {
            webView.postDelayed(() -> audioController.mute(), 300);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Don't pause WebView — KeepAliveService preserves connections.
    }

    @Override
    protected void onStop() {
        super.onStop();
        TurnNotifier.setAppInForeground(false);
        ServiceHelper.start(this, isMutedByService);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (webView.getUrl() != null) out.putString(KEY_WEBVIEW_URL, webView.getUrl());
        webView.saveState(out);
    }

    @Override
    protected void onRestoreInstanceState(Bundle saved) {
        super.onRestoreInstanceState(saved);
        webView.restoreState(saved);
    }

    @Override
    protected void onDestroy() {
        KeepAliveService.setMuteCallback(null);
        ServiceHelper.stop(this);
        webView.destroy();
        super.onDestroy();
    }
}
