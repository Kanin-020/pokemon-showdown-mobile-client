package com.pokemonshowdown.app;

import android.webkit.WebView;

/**
 * Holds all JavaScript snippets that get injected into the Pokemon Showdown WebView.
 * Keeps Java code clean and makes scripts easy to find and edit.
 */
public final class JavaScriptInjector {

    private JavaScriptInjector() {}

    /** Intercepts WebSocket creation and reconnects on visibility change. */
    public static void injectReconnection(WebView view) {
        view.evaluateJavascript(SCRIPT_RECONNECT, null);
    }

    /** Installs a MutationObserver that detects turn changes and battle events. */
    public static void injectTurnDetection(WebView view) {
        view.evaluateJavascript(SCRIPT_TURN_DETECTOR, null);
    }

    /** Sets the viewport meta tag for mobile layout. */
    public static void injectViewport(WebView view) {
        view.evaluateJavascript(SCRIPT_VIEWPORT, null);
    }

    private static final String SCRIPT_RECONNECT =
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

    private static final String SCRIPT_TURN_DETECTOR =
        "(function() {" +
        "  if (window._pokemonShowdownTurnDetector) return;" +
        "  window._pokemonShowdownTurnDetector = true;" +
        "" +
        "  var lastTurnValue = '';" +
        "  var lastBattleState = '';" +
        "" +
        "  var observer = new MutationObserver(function(mutations) {" +
        "    try {" +
        "      var turnElement = document.querySelector('.battle-controls');" +
        "      if (!turnElement) turnElement = document.querySelector('[class*=\"turn\"]');" +
        "      if (!turnElement) turnElement = document.querySelector('.controls');" +
        "" +
        "      if (turnElement) {" +
        "        var currentTurn = turnElement.textContent || '';" +
        "        if (currentTurn !== lastTurnValue && currentTurn.length > 0) {" +
        "          lastTurnValue = currentTurn;" +
        "          if (window.TurnNotifier) {" +
        "            window.TurnNotifier.onTurnDetected(currentTurn.trim());" +
        "          }" +
        "        }" +
        "      }" +
        "" +
        "      var battleRoom = document.querySelector('.pokemon-showdown .battle');" +
        "      if (battleRoom) {" +
        "        var battleState = battleRoom.className || '';" +
        "        if (battleState !== lastBattleState && lastBattleState === '') {" +
        "          lastBattleState = battleState;" +
        "          if (window.TurnNotifier) {" +
        "            window.TurnNotifier.onBattleStart();" +
        "          }" +
        "        }" +
        "      }" +
        "    } catch(e) {" +
        "      console.log('[TurnDetector] Error: ' + e.message);" +
        "    }" +
        "  });" +
        "" +
        "  var target = document.querySelector('.pokemon-showdown') || document.body;" +
        "  observer.observe(target, {" +
        "    childList: true," +
        "    subtree: true," +
        "    characterData: true," +
        "    attributes: true" +
        "  });" +
        "" +
        "  var chatObserver = new MutationObserver(function(mutations) {" +
        "    mutations.forEach(function(mutation) {" +
        "      mutation.addedNodes.forEach(function(node) {" +
        "        if (node.nodeType === 1 && node.classList && node.classList.contains('chat')) {" +
        "          var text = node.textContent || '';" +
        "          if (text.indexOf('Battle between') === 0 || text.indexOf('VS') >= 0) {" +
        "            if (window.TurnNotifier) {" +
        "              window.TurnNotifier.onTurnDetected(text.trim());" +
        "            }" +
        "          }" +
        "        }" +
        "      });" +
        "    });" +
        "  });" +
        "" +
        "  var chatArea = document.querySelector('.chatlog') || document.querySelector('.pokemon-showdown');" +
        "  if (chatArea) {" +
        "    chatObserver.observe(chatArea, { childList: true, subtree: true });" +
        "  }" +
        "" +
        "  console.log('[TurnDetector] Turn detection installed');" +
        "})();";

    private static final String SCRIPT_VIEWPORT =
        "(function() {" +
        "  var vp = document.querySelector('meta[name=\"viewport\"]');" +
        "  if (!vp) {" +
        "    vp = document.createElement('meta');" +
        "    vp.name = 'viewport';" +
        "    document.head.appendChild(vp);" +
        "  }" +
        "  vp.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';" +
        "})();";
}
