package com.pokemonshowdown.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PokemonShowdown";
    private static final String URL = "https://play.pokemonshowdown.com/";

    private WebView webView;
    private ProgressBar progressBar;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        setupWebView();
        setupBackButton();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();

        // Re-inject reconnection script
        webView.postDelayed(() -> injectReconnectionScript(webView), 500);
    }

    /**
     * When going to background: do NOT pause the WebView.
     * Instead, start the foreground service to keep the process alive.
     * The WebSocket connection stays active.
     */
    @Override
    protected void onStop() {
        super.onStop();
        startKeepAliveService();
    }

    /**
     * When coming back to foreground: stop the service (no longer needed).
     * Resume the WebView.
     */
    @Override
    protected void onStart() {
        super.onStart();
        stopKeepAliveService();
    }

    @Override
    protected void onDestroy() {
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
