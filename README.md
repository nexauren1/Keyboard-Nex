# Keyboard Nex

**Nexauren — Building Digital Products**

Keyboard Nex is a native Android keyboard project built with Kotlin and Android's InputMethodService. The repository started as an empty README and is being developed into a complete system keyboard plus a companion settings application.

## Current project status

The first native foundation is now in the repository:

- Android application module and Gradle configuration.
- Android input-method service registration.
- Initial QWERTY keyboard with letters, numbers, symbols, shift, delete and enter.
- Keyboard toolbar with clipboard, emoji, tools, themes and settings panels.
- Local text clipboard history.
- Clipboard items can be selected in batches, pinned/unpinned in batches, edited individually, pasted and deleted in batches.
- Unpinned clipboard history has configurable expiry: 5 minutes, 15 minutes, 30 minutes, 1 hour, 24 hours or 7 days.
- Pinned clipboard items do not expire automatically.
- Four initial color themes: Neon Violet, Midnight Blue, Ocean Cyan and Minimal Light.
- Setup screen to enable and select Keyboard Nex.
- Android GitHub Actions debug-build workflow.

**This is an early development build, not a release-ready keyboard.** Suggestions, autocorrection, swipe typing, voice typing, translation, GIF search, full language dictionaries and downloadable photo themes still require implementation and testing.

## Product direction

The goal is to compete in the Android keyboard category with a distinct Nexauren identity. Gboard is the functional benchmark, not a source-code or visual design template.

### Main screens

1. Welcome and keyboard activation.
2. Keyboard settings dashboard.
3. Languages and layouts.
4. Preferences and key behaviour.
5. Theme gallery and full keyboard preview.
6. Theme customization.
7. Clipboard history and batch management.
8. Emoji and symbols.
9. Corrections, suggestions and personal dictionary.
10. Text shortcuts and snippets.
11. Gesture and accessibility settings.
12. Privacy, diagnostics and about.

### Feature roadmap

**P0 — Native keyboard foundation**
- Stable IME lifecycle and text entry.
- Correct handling of selection, backspace, enter and editor actions.
- Layout sizing and theme contrast.
- Setup and default-keyboard guidance.

**P1 — Clipboard**
- Search and filters for recent/pinned clips.
- Multi-select, pin/unpin, edit, delete and clear-all.
- Per-user retention period for local clipboard history.
- Sensitive-content handling and password-field safeguards.
- Optional image and rich-content support after security review.

**P2 — Typing engine**
- Portuguese, English, Spanish, French and German language packs.
- Layout selection and simultaneous language support.
- Word suggestions, personal dictionary and autocorrection.
- Auto-capitalization, punctuation shortcuts and number row.
- Glide typing and cursor/delete gestures.

**P3 — Personalization**
- Downloadable theme packs and image-backed keyboard themes.
- Live theme preview, key shape, key border, opacity and background dimming.
- Key sounds, vibration strength, key height and one-handed mode.
- Floating and compact keyboard layouts.

**P4 — Productivity tools**
- Emoji search and GIF search.
- Translation while typing.
- Text snippets and quick replies.
- Text editing controls and advanced selection tools.
- Voice dictation integration where available and permitted.

## Clipboard privacy and expiry

Clipboard history is stored locally in app-private preferences. The first implementation captures text while Keyboard Nex is active and the user has enabled automatic saving. The keyboard skips clipboard data marked as sensitive on Android 13 and later.

The retention setting controls Keyboard Nex's own local history. Android can independently clear the system clipboard; the keyboard cannot override operating-system privacy limits. The current app does not request Internet access.

## Theme assets

Theme backgrounds should be delivered as complete visual compositions: image background, key surfaces, contrast overlay, key labels and controls. Do not ship or expose unmodified stock images as standalone downloads.

Pixabay's Content License allows many uses and adaptations but prohibits standalone redistribution. The Pixabay API also says not to permanently hotlink returned image URLs; assets intended for use must be hosted by the application owner. Review the live license and API rules before shipping any theme pack.

- Pixabay Content License: https://pixabay.com/service/license-summary/
- Pixabay Terms: https://pixabay.com/service/terms/
- Pixabay API documentation: https://pixabay.com/api/docs/

## Build

Requirements:
- JDK 17
- Android SDK 35
- Gradle 8.9

Build a debug APK from the repository root:
    gradle wrapper --gradle-version 8.9
    ./gradlew :app:assembleDebug

The APK is generated at:
    app/build/outputs/apk/debug/app-debug.apk

The GitHub Actions workflow also builds and uploads a debug APK artifact on pushes and pull requests to main.

## Package and version

- Application ID: com.nexauren.keyboardnex
- Version name: 0.0.1
- Version code: 1
- Platform: Android
- Implementation: Kotlin / native Android IME
