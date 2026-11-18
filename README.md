# Keyboard Nex

**Nexauren — Building Digital Products**

Keyboard Nex is a Flutter-first Android keyboard with a native Android Input Method Service (IME).

## Technology

- Flutter and Dart: companion app, home dashboard, theme gallery, clipboard manager and preferences.
- Kotlin: Android Input Method Service, text-input connection and system integration.
- Local storage: Android SharedPreferences for keyboard settings and clipboard history.
- Platform channel: a small method-channel bridge connects Flutter screens to the native keyboard data and actions.

## Current foundation

- Native Android QWERTY keyboard with letters, numbers, symbols, shift, delete and enter.
- Flutter home dashboard with Android keyboard setup shortcuts.
- Theme gallery with four built-in themes: Neon Violet, Midnight Blue, Ocean Cyan and Minimal Light.
- Clipboard manager with search, filters, multi-selection, batch pin/unpin, edit, delete and clear-all.
- Configurable retention for unpinned clipboard entries: 5 minutes, 15 minutes, 30 minutes, 1 hour, 24 hours or 7 days.
- Pinned clips do not expire automatically.
- Keyboard preferences for automatic clipboard capture, vibration, key sounds and keyboard height.
- Local-first storage; no account or cloud sync is required.

## Privacy and clipboard behaviour

Clipboard history is stored in app-private Android preferences. The keyboard avoids capturing password fields and fields marked as not personalized. On Android 13 and later, it also checks the system clipboard's sensitive-content flag. Android may apply its own clipboard visibility and clearing rules; Keyboard Nex cannot override those operating-system controls.

The configured retention period applies to Keyboard Nex's internal history. Pinned entries are exempt from automatic expiry.

## Photo themes

The first build includes four built-in themes. Photo-theme packs and downloads are planned for a later stage. Image packs must be licensed for the intended use and hosted by Nexauren; unmodified stock images should not be redistributed as standalone downloads. Review the current Pixabay licence and API conditions before adding assets.

- Pixabay Content License: https://pixabay.com/service/license-summary/
- Pixabay Terms: https://pixabay.com/service/terms/
- Pixabay API documentation: https://pixabay.com/api/docs/

## Build

Requirements:
- Flutter 3.50.0 stable
- Dart 3.14 (bundled with Flutter)
- JDK 17
- Android SDK provided by Flutter tooling

From the repository root, run:

    flutter pub get
    flutter analyze
    flutter build apk --debug

The debug APK is generated at:

    build/app/outputs/flutter-apk/app-debug.apk

The GitHub Actions workflow builds and uploads the debug APK artifact on pushes and pull requests to main.

## Package and version

- Application ID: com.nexauren.keyboardnex
- Version name: 0.0.1
- Version code: 1
- Platform: Android
- Main application UI: Flutter / Dart
- Input method service: Kotlin / Android
