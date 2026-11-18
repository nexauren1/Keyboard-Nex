# Keyboard Nex — Flutter Architecture Plan

## Technology decision

Keyboard Nex will use Flutter and Dart as its primary application technology. A small native Android Kotlin layer is retained only where Android requires a native Input Method Service (IME).

## Architecture

- **Flutter application (Dart):** onboarding, setup guidance, settings dashboard, theme gallery, theme editor, language management, clipboard manager, emoji and symbol browser, personal dictionary, shortcuts, preferences, privacy and diagnostics.
- **Android IME host (Kotlin):** InputMethodService lifecycle, connection to the focused text field, key event dispatch, editor actions, selection and composing text, Android keyboard registration and system insets.
- **Shared local data:** define a versioned data contract for keyboard preferences, selected theme, language/layout, clipboard retention, pinned clips and user dictionary. Flutter and the IME host must read/write compatible data through a deliberate bridge; avoid two unsynchronised stores.
- **Flutter ↔ Android bridge:** use a platform channel for commands and events that need native Android APIs. Keep the bridge narrow and documented.

## Clipboard requirements

- Clipboard history is local to the device.
- Support search, filters, multi-select, pin/unpin, edit, delete selected and clear selected.
- Allow configurable expiry for unpinned entries.
- Pinned entries never expire automatically.
- Provide clear-all with confirmation.
- Respect Android sensitive clipboard flags and password fields. Never promise to bypass Android clipboard restrictions.
- Make retention behaviour explicit in the UI.

## Themes

- Ship several built-in themes first.
- Support image-backed themes with preview, crop/position, dim overlay, key surface opacity, key shape, border, label contrast and reset.
- Download theme packs only from assets Nexauren has permission to redistribute and host.
- Store downloaded assets locally and show download size/progress; allow deleting downloaded packs.
- Do not redistribute unmodified stock images. Review the relevant image licence and API terms before release.

## Typing and productivity roadmap

1. Reliable native text entry, delete, shift, enter, editor actions and keyboard lifecycle.
2. Keyboard layouts and language selection.
3. Suggestions, autocorrection, personal dictionary and text shortcuts.
4. Clipboard manager and expiry controls.
5. Theme gallery, image themes and visual effects.
6. Emoji/symbol search, number row, one-handed and floating modes.
7. Glide typing, GIF search, translation and dictation only after feasibility, privacy and quality review.

## Migration sequence

1. Audit the existing Kotlin Android IME and preserve its working input connection behaviour.
2. Add a Flutter module/application for the companion experience without removing the native IME.
3. Define the platform-channel contract and shared local storage format.
4. Rebuild the companion screens in Flutter.
5. Connect Flutter settings and clipboard management to the native IME.
6. Test on multiple Android versions, screen sizes, physical keyboards and text-field types.
7. Add advanced typing features incrementally; do not advertise features until implemented and tested.

## Product principles

- Gboard is a functional benchmark, not a source-code or visual-design template.
- Prioritise reliability, privacy, low resource usage and a polished interface.
- Use visible but brief processing feedback for navigation, downloads and settings changes; do not delay actual typing or key response with decorative animations.
- No network permission unless a feature genuinely requires it. Explain and obtain consent for online features.
