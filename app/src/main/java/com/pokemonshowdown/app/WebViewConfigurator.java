package com.pokemonshowdown.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * Configures the WebView for Pokemon Showdown with all required settings,
 * clients, and JavaScript interface bindings.
 */
public class WebViewConfigurator {

    private static final String TAG = "PokemonShowdown";
    private static final String ALLOWED_HOST = "pokemonshowdown.com";

    private final WebView webView;
    private final ProgressBar progressBar;
    private final TurnNotifier turnNotifier;

    public WebViewConfigurator(WebView webView, ProgressBar progressBar, TurnNotifier turnNotifier) {
        this.webView = webView;
        this.progressBar = progressBar;
        this.turnNotifier = turnNotifier;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void configure() {
        applySettings();
        webView.setWebViewClient(createWebViewClient());
        webView.setWebChromeClient(createChromeClient());
        webView.addJavascriptInterface(turnNotifier, "TurnNotifier");
        WebView.setWebContentsDebuggingEnabled(true);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void applySettings() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setDatabaseEnabled(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(false);
        s.setSaveFormData(true);
        s.setSavePassword(false);
    }

    private WebViewClient createWebViewClient() {
        return new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl() != null ? request.getUrl().toString() : "";
                if (url.contains(ALLOWED_HOST)) return false;

                try {
                    view.getContext().startActivity(
                            new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    );
                } catch (Exception e) {
                    Toast.makeText(view.getContext(), R.string.error_open_link, Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(android.view.View.GONE);
                JavaScriptInjector.injectReconnection(view);
                JavaScriptInjector.injectTurnDetection(view);
                JavaScriptInjector.injectViewport(view);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(view.getContext(), R.string.error_connection, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
            }
        };
    }

    private WebChromeClient createChromeClient() {
        return new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(android.view.View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(android.view.View.GONE);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView,
                                             android.webkit.ValueCallback<android.net.Uri[]> filePathCallback,
                                             android.webkit.WebChromeClient.FileChooserParams fileChooserParams) {
                return false;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message,
                                     android.webkit.JsResult result) {
                result.confirm();
                return true;
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage msg) {
                Log.d(TAG, "JS: " + msg.message());
                return true;
            }
        };
    }
}
