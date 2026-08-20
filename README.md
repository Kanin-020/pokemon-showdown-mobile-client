<p align="center">
  <a href="README.md">English</a> | <a href="README_ES.md">Espanol</a>
</p>

# Pokemon Showdown - Android Client

A native Android wrapper for [Pokemon Showdown](https://play.pokemonshowdown.com/), designed to maintain active connections in the background.

## Features

- Direct access to Pokemon Showdown via WebView
- Background connection preservation (foreground service)
- Turn notifications when the app is in the background
- Mute/unmute control from the notification
- Auto-reconnection on WebSocket disconnect
- Multi-language support (English / Spanish)
- Immersive fullscreen mode

## Architecture

```
app/src/main/java/com/pokemonshowdown/app/
  MainActivity.java          # Entry point, orchestrates lifecycle
  WebViewConfigurator.java   # WebView settings and client callbacks
  JavaScriptInjector.java    # JS scripts injected into the WebView
  AudioController.java       # Mute/unmute audio management
  ServiceHelper.java         # Foreground service start/stop
  NotificationHelper.java    # Notification channels and builders (DRY)
  KeepAliveService.java      # Foreground service for background keep-alive
  TurnNotifier.java          # JS interface for turn detection notifications
```

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- Java 11+

## Build

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

## Release

```bash
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

GitHub Actions will automatically build and publish the release with both Debug and Release APKs.

## Customization

| Setting | File | Description |
|---------|------|-------------|
| Target URL | `MainActivity.java` -> `URL` | Change the loaded website |
| Colors | `res/values/colors.xml` | App theme colors |
| Orientation | `AndroidManifest.xml` -> `screenOrientation` | `portrait`, `landscape`, or `unspecified` |

## License

Personal use project. Pokemon Showdown is an open-source project by Zarel.
