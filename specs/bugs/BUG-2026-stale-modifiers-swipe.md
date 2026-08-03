# BUG-2026-stale-modifiers: Modifier-then-swipe sends unmodified character, leaves modifier stuck

## Problem

When a modifier (Ctrl, Alt, Meta, Fn) is toggled via a swipe action and the next keypress is also a swipe, the character is sometimes sent unmodified and the modifier stays visually active forever.

- **Actual**: Swipe Ctrl toggle → swipe a letter → the letter appears literally (e.g., `c` instead of Ctrl+C). The modifier indicator remains on-screen and subsequent taps also ignore the modifier.
- **Expected**: The modifier-then-swipe chord sends the modified key event (Ctrl+C) and clears the one-shot modifier immediately.
- **Reproduce**: On any keyboard with swipe-activated modifier toggles (e.g., NOThumbKeyDataDrivenExpanded, where Ctrl is a topLeft swipe on the spacebar), toggle Ctrl via swipe, then immediately swipe a letter key. The issue is intermittent — it depends on whether Compose happens to recompose the gesture detector between the two swipes.

## Root Cause Analysis

The `pointerInput(detectDragGestures)` block in KeyboardKey is keyed by an `id` string that does NOT include `activeModifiers`. When a modifier toggle swipe updates `activeModifiers` in KeyboardScreen, all visible KeyboardKey composables recompose with the new value — but their `pointerInput` blocks are NOT re-created because the `id` key hasn't changed.

The `onDragEnd` callback inside `pointerInput` captures the `activeModifiers` Int parameter BY VALUE at the time the gesture detector was created. If the value was 0 when `pointerInput` first ran, it stays 0 inside that callback until something else changes `id` and forces a re-creation.

Impact chain:
1. `onDragEnd` dispatches `performKeyAction(..., activeModifiers = 0)` (stale)
2. In `performKeyAction`, the modifier-chord branch (`activeModifiers != 0 && ...`) is skipped
3. Falls through to `ic.commitText(text, 1)` — types the literal character
4. `onKeyEvent()` is never called (only the modifier-chord and single-char-no-IC branches call it)
5. The real `activeModifiers` state in KeyboardScreen remains set (e.g., `META_CTRL_ON`), so the indicator stays on and the modifier appears "stuck"

The `combinedClickable.onClick` handler (center tap) does NOT have this bug because it recomposes normally with the composable function, always capturing the latest `activeModifiers`. Only swipes are affected.

**Risk level**: Medium — intermittent, but when it happens the keyboard becomes unpredictable (modifier stuck on, subsequent chords fail).

## TDD Fix Plan

### 1. RED: Add `activeModifiers` to the pointerInput recomposition key

The fix is to include `activeModifiers` in the `id` computation that gates `pointerInput` recomposition. When `activeModifiers` changes, all gesture detectors will restart with the fresh value.

**GREEN**: Add `activeModifiers` (and `capsLock` for similar reason) to the `id` string in `KeyboardKey.kt`.

**verify**: `./gradlew lintKotlin lint assembleDebug`

### 2. RED: No code changes needed beyond the id fix — the existing test suite plus manual verification covers this.

**GREEN**: Verify via manual test: toggle Ctrl via swipe, immediately swipe 'c'. The modifier should send Ctrl+C and clear. Repeat with Alt, Meta, Fn.

**verify**: Manual verification on device

**REFACTOR**: None — this is a one-line fix.

## Acceptance Criteria

- [ ] Swipe-activating a modifier then swiping a letter key always sends the modified chord
- [ ] One-shot modifier clears after the chord is sent
- [ ] Locked modifiers (double-tap) persist across swipes correctly
- [ ] All existing modifier behavior (tap-to-toggle, tap-letter chords) still works
- [ ] Preflight green: `./gradlew lintKotlin lint assembleDebug`

## Resolution

Fixed in commit `ab35475` (merged to main).

**Change**: Added `activeModifiers` and `capsLock` to the `id` computation in `KeyboardKey.kt`'s `pointerInput` key. When modifiers change, the gesture detector is now re-created with the fresh value, so swipes always see the current modifier state.
