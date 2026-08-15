# BUG-2026-clipboard-image-crash: Keyboard crashes when an image is copied to the clipboard

## Problem

Copying a non-text clip (e.g. an image) to the system clipboard crashes the keyboard (IME process dies) while it is visible.

- **Actual**: Copy an image in any app while thumb-key is shown → the keyboard process crashes / the keyboard disappears.
- **Expected**: Copying an image is ignored by clipboard history (images are not text and cannot be stored) and the keyboard keeps running. Pasting the image afterwards must still paste the image via the system clipboard.
- **Reproduce**: Enable clipboard history, show the keyboard, long-press an image in another app → "Copy image" → the keyboard crashes.

## Root Cause Analysis

`ThumbKeyClipboardManager.clipboardListener` (registered as `OnPrimaryClipChangedListener`, invoked on the IME main thread) coerced every clip unconditionally:

```kotlin
val text = clip.getItemAt(0).coerceToText(context).toString()
```

For an image clip the item has no inline text and carries a `content://` URI, so `ClipData.Item.coerceToText(context)` attempts `ContentResolver.openTypedAssetFileDescriptor(uri, "text/*", null)` against the source app's provider:

- On API 24–27 (minSdk 24) that call is not guarded in AOSP: a `SecurityException` (no read grant for the image URI in the IME process) or a `RuntimeException` (provider `RemoteException` wrap) propagates out of `coerceToText`, and because the listener runs on the IME main thread the uncaught exception kills the keyboard.
- On any version, OEM platform variants may return `null` from `coerceToText`, and the unconditional `.toString()` then throws `NullPointerException`.

Two additional latent defects in the same listener:

1. If coercion ever succeeded for an image (a provider serving binary data as `text/*`), the whole stream was read into a string and stored into clipboard history as garbage — contradicting the comment claiming images "are not added to the history".
2. On API 24–27 a `content://` URI string could be stored in history when coercion failed with `FileNotFoundException`.

## Fix

`app/src/main/java/com/dessalines/thumbkey/utils/ThumbKeyClipboardManager.kt`:

1. Gate on MIME type: only process clips that carry a `text/*` MIME type (`ClipDescription.MIMETYPE_TEXT_PREFIX`), scanning all MIME entries of the clip description. Image (and other non-text) clips skip coercion entirely.
2. Wrap `coerceToText` in try/catch (`Exception`) and null-check the result before use — belt and braces for provider errors, permission denials, and OEM `null` returns.
3. Keep `wasLastCopyOperationDoneViaSystem = true` for all clip types, so pasting a non-text clip still goes through the system clipboard (the documented private-clipboard behavior: internal history only holds text, so non-text pastes must use the system clipboard).

## Acceptance Criteria

- [ ] Copying an image while the keyboard is visible does not crash the keyboard
- [ ] Image clips never appear in clipboard history
- [ ] Copying text still adds to history (plain text and rich `text/html` copies)
- [ ] With private clipboard enabled, pasting right after copying an image pastes the image (system paste path)
- [ ] Preflight green: `./gradlew lintKotlin lint assembleDebug`

## Resolution

Fixed in commit (see git log). Root cause: unguarded `coerceToText` on non-text clips in `ThumbKeyClipboardManager.clipboardListener`.
