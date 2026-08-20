# Pokémon Showdown Android App

Una aplicación Android nativa que carga Pokémon Showdown directamente en un WebView.

## Características

- 🎮 Carga directa de https://play.pokemonshowdown.com/
- 📱 Pantalla completa inmersiva
- 🔄 Manejo de conexiones y errores
- 🎨 Barra de progreso personalizada con colores Pokémon
- ⚡ JavaScript y localStorage habilitados
- 🔒 Confirmación antes de salir
- 🎯 Icono de Pokéball adaptable

## Requisitos

- Android Studio Hedgehog (2023.1.1) o superior
- SDK Android 34
- Java 11+
- Gradle 8.6+

## Instrucciones de Compilación

### 1. Abrir en Android Studio

```bash
# Clona el repositorio y ábrelo en Android Studio
git clone https://github.com/TU_USUARIO/PokemonShowdownApp.git
# File > Open > Selecciona la carpeta del proyecto
```

### 2. Compilar y Ejecutar

```bash
# Opción 1: Desde Android Studio
# Run > Run 'app'

# Opción 2: Desde línea de comandos
./gradlew assembleDebug

# El APK se generará en:
# app/build/outputs/apk/debug/app-debug.apk
```

### 3. Instalar en tu dispositivo

```bash
# Con dispositivo conectado por USB (developer mode activado)
./gradlew installDebug

# O instala el APK manualmente
adb install app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Releases

### Crear una release

```bash
# 1. Añade y commit los cambios
git add .
git commit -m "feat: descripción del cambio"

# 2. Crea un tag de versión
git tag -a v1.0.0 -m "Release v1.0.0"

# 3. Push del tag (activa el workflow automáticamente)
git push origin v1.0.0
```

El workflow de GitHub Actions creará automáticamente una release con:
- **Release APK** (`app-release.apk`): Optimizado para distribución
- **Debug APK** (`app-debug.apk`): Para pruebas

### Releases manuales

También puedes crear releases ejecutando el workflow manualmente desde la pestaña **Actions > Build & Release APK > Run workflow**. Se te pedirá:
- **Versión** (ej: `v1.2.0`)

## Estructura del Proyecto

```
├── app/
│   ├── build.gradle            # Configuración de compilación
│   ├── proguard-rules.pro      # Reglas de ofuscación
│   └── src/main/
│       ├── AndroidManifest.xml # Permisos y actividad principal
│       ├── java/com/pokemonshowdown/app/
│       │   └── MainActivity.java  # WebView que carga el sitio
│       └── res/
│           ├── layout/
│           │   └── activity_main.xml
│           ├── drawable/
│           │   ├── ic_launcher_background.xml
│           │   ├── ic_launcher_foreground.xml
│           │   └── progress_bar_pokemon.xml
│           ├── mipmap-*/       # Iconos por densidad
│           └── values/
│               ├── strings.xml
│               ├── colors.xml
│               └── themes.xml
├── .github/workflows/
│   ├── ci.yml                 # CI en push/PR
│   └── release.yml            # Release automático
├── build.gradle
├── settings.gradle
└── gradle/wrapper/
    └── gradle-wrapper.properties
```

## Iconos de la App

La app incluye iconos adaptativos (Android 8.0+) con diseño de Pokéball:

| Densidad | Tamaño |
|----------|--------|
| mdpi | 48x48 px |
| hdpi | 72x72 px |
| xhdpi | 96x96 px |
| xxhdpi | 144x144 px |
| xxxhdpi | 192x192 px |

Los iconos se generan automáticamente desde vectores XML.

## Personalización

### Cambiar URL
Edita `MainActivity.java` y cambia la constante `URL`:
```java
private static final String URL = "https://tu-url-aqui.com/";
```

### Modificar colores
Edita `res/values/colors.xml` para cambiar los colores del tema.

### Cambiar orientación
En `AndroidManifest.xml`, cambia `android:screenOrientation`:
- `portrait` - Solo vertical (por defecto)
- `landscape` - Solo horizontal
- `unspecified` - Permite rotación

## Solución de Problemas

### El sitio no carga
- Verifica conexión a internet
- Asegúrate de que JavaScript esté habilitado en el navegador del dispositivo
- Revisa los logs en Android Studio (Logcat)

### El sitio se ve cortado
- Prueba cambiar `android:screenOrientation` a `landscape`
- Verifica que `useWideViewPort` esté habilitado

### WebSocket no conecta
Pokémon Showdown usa WebSockets. Asegúrate de:
1. Tener conexión estable a internet
2. No estar detrás de un firewall que bloquee WebSockets

## Workflows de GitHub

### CI (`ci.yml`)
- Se ejecuta en push a `main`/`develop` y en PRs
- Compila Debug y Release APK
- Guarda APKs como artifacts por 7 días

### Release (`release.yml`)
- Se ejecuta al hacer push de un tag `v*`
- Se puede ejecutar manualmente desde Actions (pide versión y resumen de cambios)
- Compila APKs
- Crea release en GitHub con los APKs adjuntos
- Genera notas de release automáticamente

## Licencia
Proyecto de uso personal. Pokémon Showdown es un proyecto open-source de Zarel.
