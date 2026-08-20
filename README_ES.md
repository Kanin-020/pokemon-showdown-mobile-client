<p align="center">
  <a href="README.md">English</a> | <a href="README_ES.md">Espanol</a>
</p>

# Pokemon Showdown - Cliente Android

Un wrapper nativo de Android para [Pokemon Showdown](https://play.pokemonshowdown.com/), disenado para mantener conexiones activas en segundo plano.

## Caracteristicas

- Acceso directo a Pokemon Showdown via WebView
- Preservacion de conexion en segundo plano (servicio en primer plano)
- Notificaciones de turno cuando la app esta en segundo plano
- Control de silenciar/activar sonido desde la notificacion
- Reconexion automatica al desconectar WebSocket
- Soporte multi-idioma (Ingles / Espanol)
- Modo pantalla completa inmersiva

## Arquitectura

```
app/src/main/java/com/pokemonshowdown/app/
  MainActivity.java          # Punto de entrada, orquesta el ciclo de vida
  WebViewConfigurator.java   # Configuracion del WebView y callbacks
  JavaScriptInjector.java    # Scripts JS inyectados en el WebView
  AudioController.java       # Gestion de silenciar/activar audio
  ServiceHelper.java         # Inicio/parada del servicio en primer plano
  NotificationHelper.java    # Canales y constructores de notificaciones (DRY)
  KeepAliveService.java      # Servicio en primer plano para mantener conexion
  TurnNotifier.java          # Interfaz JS para notificaciones de turno
```

## Requisitos

- Android Studio Hedgehog (2023.1.1) o superior
- Android SDK 34
- Java 11+

## Compilacion

```bash
# Build de debug
./gradlew assembleDebug

# Instalar en dispositivo conectado
./gradlew installDebug
```

## Release

```bash
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

GitHub Actions construira y publicara automaticamente el release con los APKs de Debug y Release.

## Personalizacion

| Ajuste | Archivo | Descripcion |
|--------|---------|-------------|
| URL destino | `MainActivity.java` -> `URL` | Cambiar el sitio web cargado |
| Colores | `res/values/colors.xml` | Colores del tema de la app |
| Orientacion | `AndroidManifest.xml` -> `screenOrientation` | `portrait`, `landscape` o `unspecified` |

## Licencia

Proyecto de uso personal. Pokemon Showdown es un proyecto open-source de Zarel.
