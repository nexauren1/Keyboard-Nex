# Keyboard Nex

**Nexauren — Building Digital Products**

Keyboard Nex is an Android keyboard with a Flutter companion interface and a native Android Input Method Service (IME).

## Technology

- Flutter Material UI: companion dashboard, theme gallery, clipboard manager and preferences.
- Kotlin: Android keyboard service, text input and system integration.
- Android system spell checker: local word suggestions and spelling alternatives where a compatible dictionary is enabled on the device.
- Local storage: Android SharedPreferences for keyboard settings, recent emojis and clipboard history.
- Platform channel: connects the companion interface to the native keyboard.

The keyboard does not bundle a large dictionary, download language packs or send typed text to a Nexauren server.

## Keyboard features

- Three keyboard layouts: QWERTY, QWERTZ and AZERTY.
- Optional number row.
- Configurable emoji, language-switch, comma and period keys.
- Shift, double-tap Caps Lock, long-press Caps Lock, delete and enter.
- Long-press Q–P to type numbers.
- Double-space period.
- Swipe on the spacebar to move the text cursor.
- Sentence capitalization for fields that request it.
- Word suggestions and spelling alternatives from the Android system spell checker.
- Optional autocorrection, disabled by default.
- Suggestion language preference: device default, Portuguese or English. Actual availability depends on the spell checker and dictionaries installed on Android.
- Complete Unicode Emoji 18.0 catalogue: 3,972 fully-qualified emoji and components, grouped by the official categories.
- Emoji search with Portuguese CLDR names and keywords, plus English names.
- Recent emojis, local favourites and skin-tone variants, including long-press tone selection.
- NexaMoji: original Nexauren emoji combinations and a local creator for user-made emoji combinations.
- Four built-in themes: Neon Violet, Midnight Blue, Ocean Cyan and Minimal Light.

## Companion app and clipboard

- Companion dashboard with Android keyboard setup shortcuts.
- Clipboard manager with search, filters, multi-selection, batch pin/unpin, edit, delete and clear-all.
- Configurable retention for unpinned clipboard entries: 5 minutes, 15 minutes, 30 minutes, 1 hour, 24 hours or 7 days.
- Pinned clips do not expire automatically.
- Preferences for automatic clipboard capture, vibration, key sounds and keyboard height.
- Local-first storage; no account or cloud sync is required.

## Privacy

Clipboard history, recent emojis and keyboard preferences are stored locally in app-private Android preferences. The keyboard avoids capturing password fields and fields marked as not personalized. On Android 13 and later, it also checks the system clipboard's sensitive-content flag. Android may apply its own clipboard visibility and clearing rules; Keyboard Nex cannot override those operating-system controls.

Spell suggestions use the Android spell-checking service configured on the device. Results and language availability depend on that service. Autocorrection is optional and disabled by default.

## Photo themes

The current build includes four built-in themes. Downloadable photo-theme packs are not included in this version.

## Emoji data and rendering

The bundled catalogue is based on Unicode Emoji 18.0 and Portuguese CLDR annotations. It includes Unicode emoji characters and sequences, such as flags, families, professions and skin-tone variations. Search and selection work offline. The actual emoji artwork is drawn by the Android system font; older Android versions may show a placeholder for characters introduced after their emoji font was released. See THIRD_PARTY_NOTICES.md for data attribution and licensing.

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
