# AUDIT-e01 — Floating Keyboard Mode

**Branch:** feat/floating-keyboard
**Audited:** 2026-08-02
**Result:** PASS (with advisory notes)
**Kotlin lines changed:** ~900

---

## Supply Chain & Security

✓ No new external dependencies — all changes use existing libraries
✓ No secrets in diff
✓ No external API calls
✓ SYSTEM_ALERT_WINDOW permission added to manifest (required for overlay)

## Provenance & Metadata

✓ Story specs include type: and context: metadata
✓ Implementation commits reference story IDs

## CONVENTIONS.md Compliance

✓ All specs in specs/ directory
✓ No gh commands used
✓ Kotlin only (no Java)
✓ ktlint formatting applied

## Scope

✓ Changes limited to floating keyboard feature
✓ No speculative features
✓ No files touched outside floating keyboard scope

## Boy Scout Rule

✓ Dead imports cleaned (Handler, Looper removed from IMEService.kt)
✓ No commented-out code
✓ No dead methods
✓ formatKotlin applied

## Types and Safety (Kotlin)

✓ No `any` types (N/A — Kotlin)
✓ Sealed class exhaustiveness enforced by compiler (ToggleFloatingMode added)
✓ No unchecked casts

## Test Coverage

✗ No test suite exists for this project (documented in CLAUDE.md)
  → Not blocking — Android IME testing requires device/emulator
  → Manual UAT performed and passing (9/9 steps verified)

## SOLID and Heuristics

✓ Single Responsibility: floating logic is cohesive within IMEService
✓ Open/Closed: ToggleFloatingMode extends KeyAction sealed class
✓ Dependency Inversion: FloatingModeRepository injected via ThumbkeyApplication

## Refactoring Smells — Advisory Only

| Smell | Severity | Details |
|-------|----------|---------|
| **Duplication** | Low | `disableFloatingForDocking()`, `enableFloatingAfterDocking()`, and `toggleFloatingMode()` share setting-update + view-swap patterns. Could extract common update method. |
| **Magic Numbers** | Low | Position defaults `100`/`200`, orientation `1`/`2`, fling threshold `1000f`, opacity range `30`–`100`, dp values `12`/`8`/`20`/`24`. Should be named constants. |
| **IMEService size** | Low | 489 lines. Consider extracting FloatingController if more features are added. |

## Code Style

✓ Functions generally under 25 lines
✓ Early returns over nested ifs
✓ Names are descriptive (`shouldRevertToFloating`, `pendingDockedMode`, `suppressLifecycle`)
✓ No more than 2 levels of indentation in most places
⚠ IMEService.kt at 489 lines — over the 300-line ideal, but within project norms

## Manual UAT Evidence

All 9 manual verification steps passed (recorded in `specs/verifications/e01-verify.yaml`):
1. Floating toggle ✓
2. Overlay appears on text field focus ✓
3. Drag to reposition ✓
4. Fling-down dismiss ✓
5. Text input works ✓
6. Focus loss hides ✓
7. Opacity slider ✓
8. Orientation persistence ✓
9. Emoji/clipboard docking ✓

## Verdict

**PASS** — no blocking issues. Advisory notes for potential refactoring (magic numbers, duplication) are low-severity and can be addressed in a future cleanup pass.
