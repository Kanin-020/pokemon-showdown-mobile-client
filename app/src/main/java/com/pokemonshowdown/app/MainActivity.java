package com.pokemonshowdown.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.media.AudioManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PokemonShowdown";
    private static final String URL = "https://play.pokemonshowdown.com/";
    private static final String KEY_WEBVIEW_URL = "webview_url";

    private WebView webView;
    private ProgressBar progressBar;
    private AudioManager audioManager;
    private int savedVolume = -1;
    private boolean isMutedByService = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        setupWebView();
        setupBackButton();
        setupMuteCallback();
        requestNotificationPermission();

        if (savedInstanceState != null) {
            // Restore WebView state from saved instance (process death, config change)
            webView.restoreState(savedInstanceState);
        } else {
            // Fresh start or update — load the URL.
            // WebView's on-disk storage (cookies, localStorage, cache) persists
            // across app updates because it lives in the app's data directory.
            webView.loadUrl(URL);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // Media settings
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Performance
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDatabaseEnabled(true);

        // Layout
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);

        // Enable web workers and WebSockets (needed for Pokemon Showdown)
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);

        // Force the WebView to use the existing on-disk storage (cookies, localStorage)
        // even after an app update. This is critical for preserving login sessions.
        settings.setSaveFormData(true);
        settings.setSavePassword(false); // Deprecated but harmless

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl() != null ? request.getUrl().toString() : "";

                // Allow Pokemon Showdown URLs
                if (url.contains("pokemonshowdown.com")) {
                    return false;
                }

                // Open external links in browser
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);

                // Inject reconnection script
                injectReconnectionScript(view);

                // Inject viewport meta tag
                view.evaluateJavascript(
                    "(function() {" +
                    "  var viewport = document.querySelector('meta[name=\"viewport\"]');" +
                    "  if (!viewport) {" +
                    "    viewport = document.createElement('meta');" +
                    "    viewport.name = 'viewport';" +
                    "    document.head.appendChild(viewport);" +
                    "  }" +
                    "  viewport.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';" +
                    "})();",
                    null
                );
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(
                        MainActivity.this,
                        "Error de conexion. Verifica tu internet.",
                        Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                return false;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                result.confirm();
                return true;
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d(TAG, "JS: " + consoleMessage.message());
                return true;
            }
        });

        WebView.setWebContentsDebuggingEnabled(true);
    }

    /**
     * Injects JavaScript that listens for visibility changes and reconnects
     * the Pokemon Showdown WebSocket when the app returns to the foreground.
     */
    private void injectReconnectionScript(WebView view) {
        String script =
            "(function() {" +
            "  if (window._pokemonShowdownReconnect) return;" +
            "  window._pokemonShowdownReconnect = true;" +
            "" +
            "  var OriginalWebSocket = window.WebSocket;" +
            "  window._psWebSocketRef = null;" +
            "" +
            "  window.WebSocket = function(url, protocols) {" +
            "    var ws = protocols ? new OriginalWebSocket(url, protocols) : new OriginalWebSocket(url);" +
            "    window._psWebSocketRef = ws;" +
            "    return ws;" +
            "  };" +
            "  window.WebSocket.prototype = OriginalWebSocket.prototype;" +
            "  window.WebSocket.CONNECTING = OriginalWebSocket.CONNECTING;" +
            "  window.WebSocket.OPEN = OriginalWebSocket.OPEN;" +
            "  window.WebSocket.CLOSING = OriginalWebSocket.CLOSING;" +
            "  window.WebSocket.CLOSED = OriginalWebSocket.CLOSED;" +
            "" +
            "  var reconnecting = false;" +
            "" +
            "  document.addEventListener('visibilitychange', function() {" +
            "    if (document.visibilityState === 'visible') {" +
            "      var ws = window._psWebSocketRef;" +
            "      if (ws && (ws.readyState === WebSocket.CLOSED || ws.readyState === WebSocket.CLOSING) && !reconnecting) {" +
            "        reconnecting = true;" +
            "        console.log('[PokemonShowdown] Connection lost, triggering reconnect...');" +
            "        setTimeout(function() {" +
            "          if (typeof BattleRoom !== 'undefined' && BattleRoom.reconnect) {" +
            "            BattleRoom.reconnect();" +
            "          } else if (typeof app !== 'undefined' && app.rooms) {" +
            "            for (var id in app.rooms) {" +
            "              if (app.rooms[id] && app.rooms[id].reconnect) {" +
            "                app.rooms[id].reconnect();" +
            "              }" +
            "            }" +
            "          } else {" +
            "            window.location.reload();" +
            "          }" +
            "          reconnecting = false;" +
            "        }, 1500);" +
            "      }" +
            "    }" +
            "  });" +
            "" +
            "  console.log('[PokemonShowdown] Reconnection handler installed');" +
            "})();";
        view.evaluateJavascript(script, null);
    }

    private void setupBackButton() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Salir")
                        .setMessage("Seguro que quieres salir de Pokemon Showdown?")
                        .setPositiveButton("Salir", (dialog, which) -> {
                            stopKeepAliveService();
                            finish();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
                }
            }
        });
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

    private void setupMuteCallback() {
        KeepAliveService.setMuteCallback(muted -> {
            runOnUiThread(() -> {
                isMutedByService = muted;
                if (muted) {
                    muteAudio();
                } else {
                    unmuteAudio();
                }
            });
        });
    }

    private void muteAudio() {
        if (audioManager != null) {
            if (savedVolume < 0) {
                savedVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            }
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        }
    }

    private void unmuteAudio() {
        if (audioManager != null) {
            if (savedVolume >= 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedVolume, 0);
                savedVolume = -1;
            } else {
                int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the current WebView URL so we can recover if needed
        if (webView.getUrl() != null) {
            outState.putString(KEY_WEBVIEW_URL, webView.getUrl());
        }
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // restoreState is already called in onCreate, but we ensure it here too
        webView.restoreState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();

        // Re-inject reconnection script
        webView.postDelayed(() -> injectReconnectionScript(webView), 500);

        // Reapply mute state if service requested it
        if (isMutedByService) {
            webView.postDelayed(() -> muteAudio(), 300);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Do NOT pause the WebView - this would kill WebSocket connections
        // The KeepAliveService will maintain the process
    }

    /**
     * When going to background: start the foreground service to keep the process alive.
     * Do NOT pause the WebView — Android will keep it alive while the service runs.
     */
    @Override
    protected void onStop() {
        super.onStop();
        // Pass current mute state to service
        Intent serviceIntent = new Intent(this, KeepAliveService.class);
        serviceIntent.putExtra(KeepAliveService.EXTRA_MUTED, isMutedByService);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    /**
     * When coming back to foreground: the service continues running (it was started
     * in onStop). We don't stop it here — it keeps the notification alive.
     */
    @Override
    protected void onStart() {
        super.onStart();
        // Service stays running — no need to restart or stop it.
    }

    @Override
    protected void onDestroy() {
        KeepAliveService.setMuteCallback(null);
        stopKeepAliveService();
        webView.destroy();
        super.onDestroy();
    }

    private void startKeepAliveService() {
        Intent serviceIntent = new Intent(this, KeepAliveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopKeepAliveService() {
        Intent serviceIntent = new Intent(this, KeepAliveService.class);
        stopService(serviceIntent);
    }
}
