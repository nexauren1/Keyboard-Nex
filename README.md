# Keyboard Nex

**Nexauren — Building Digital Products**

Keyboard Nex is an Android keyboard with a Flutter companion interface and a native Android Input Method Service (IME).

## Technology

- Flutter Material SDK: companion dashboard, themes, clipboard manager and preferences.
- Kotlin: Android keyboard service, text input and system integration.
- Local storage: Android SharedPreferences for keyboard settings and clipboard history.
- Platform channel: connects the companion interface to the native keyboard.

The project uses the Flutter-compatible Material UI package for its companion interface; Android size reduction is handled by release shrinking, resource optimization and icon tree-shaking.

## Current foundation

- Native Android QWERTY keyboard with letters, numbers, symbols, shift, delete and enter.
- Companion dashboard with Android keyboard setup shortcuts.
- Four built-in themes: Neon Violet, Midnight Blue, Ocean Cyan and Minimal Light.
- Clipboard manager with search, filters, multi-selection, batch pin/unpin, edit, delete and clear-all.
- Configurable retention for unpinned clipboard entries: 5 minutes, 15 minutes, 30 minutes, 1 hour, 24 hours or 7 days.
- Pinned clips do not expire automatically.
- Preferences for automatic clipboard capture, vibration, key sounds and keyboard height.
- Local-first storage; no account or cloud sync is required.

## Privacy and clipboard behaviour

Clipboard history is stored in app-private Android preferences. The keyboard avoids capturing password fields and fields marked as not personalized. On Android 13 and later, it also checks the system clipboard's sensitive-content flag. Android may apply its own clipboard visibility and clearing rules; Keyboard Nex cannot override those operating-system controls.

The configured retention period applies to Keyboard Nex's internal history. Pinned entries are exempt from automatic expiry.

## Photo themes

The current build includes four built-in themes. Downloadable photo-theme packs are not included in this version.

## Optimized APK

The Android workflow creates one universal APK containing both:
- armeabi-v7a (ARM 32-bit)
- arm64-v8a (ARM 64-bit)

It does not generate an AAB or separate ABI APK files. Release code and Android resources are minified/shrunk, and unused Material icons are tree-shaken.

Build locally:

    flutter pub get
    flutter analyze
    flutter build apk --release --target-platform android-arm,android-arm64 --tree-shake-icons

Output:

    build/app/outputs/flutter-apk/app-release.apk

The CI build uses a temporary debug signing key for installation/testing. Replace it with Nexauren's private release signing key before publishing to an app store.

## Package and version

- Application ID: com.nexauren.keyboardnex
- Version name: 0.0.1
- Version code: 1
- Platform: Android
