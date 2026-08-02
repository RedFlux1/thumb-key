# Thumb-Key — Claude Code

Read CONVENTIONS.md before any GitHub or git operation.

<!-- BEGIN bigpowers:project -->
## Project

An open-source Android keyboard IME with a 3×3 grid layout, built with Jetpack Compose and Kotlin.
Stack: Kotlin / Jetpack Compose / Android (Gradle, ktlint, prettier)

## Commands

| Action | Command |
|--------|---------|
| Build  | `./gradlew assembleDebug` |
| Test   | _No test suite yet_ |
| Lint   | `./gradlew lintKotlin && ./gradlew lint` |
| Format | `./gradlew formatKotlin` |
| Preflight | `./gradlew lintKotlin lint assembleDebug` |
| CI     | Woodpecker — lintKotlin + lint + assembleDebug on pull_request |

## Architecture

MVVM + Repositories (no domain layer). KeyboardDefinition holds mode layouts (main, shifted, numeric, ctrled, alted, emoji). KeyboardC is a 2D grid of KeyItemC. KeyItemC holds 8 swipe directions + center, each a KeyC wrapping a KeyAction sealed class. performKeyAction() in Utils.kt dispatches all actions. KeyboardScreen.kt owns mode state; KeyboardKey.kt renders individual keys. All layouts live in keyboards/.

## Conventions
- Full Kotlin — NEVER write Java classes
- ktlint formatting via `./gradlew formatKotlin` (Pinterest standard rules)
- prettier for markdown and YAML
- Layout naming: `[languages] [base] [qualifiers]? [version]?`
- 3×3 grid recommended for keyboard layouts
- New keyboards appended to end of KeyboardLayout.kt

## Never
- Never modify release.keystore, signing config, or gradle.properties credentials
- Never write Java classes — Kotlin only
- Never change the KeyAction sealed class hierarchy without updating ALL when branches in performKeyAction()
- Never modify .woodpecker.yml CI pipeline
- Never proceed on red Preflight or red CI — fix first
- Never dismiss reproducible gate failures as pre-existing or out of scope

## Agent Rules
- **Workflow Mandate:** You MUST use the bigpowers skills (e.g. `plan-work`, `develop-tdd`, `orchestrate-project`) to perform tasks. DO NOT write code directly in response to a user prompt like "build this feature".
- **Always Green:** Preflight and CI must be green before forward work. Reproducible gate failures require **fix-or-log** (quick-fix → fix-bug) per CONVENTIONS § Discovered Defects.
- Read specs/ before writing code.
- All planning and specifications MUST be written to `specs/` (`product/SCOPE_LATEST.yaml`, `release-plan.yaml`, `epics/`) before any code is generated.
- Write the minimum code that solves the stated problem. Nothing extra.
- Run tests after every change. Show evidence before declaring done.
- One clarifying question beats a wrong assumption baked into 200 lines.
<!-- END bigpowers:project -->

<!-- BEGIN bigpowers:context-routing -->
<!-- No sub-AGENTS.md routing configured -->
<!-- END bigpowers:context-routing -->

<!-- BEGIN bigpowers:learned-preferences -->
## Learned User Preferences
- None yet

## Workspace Facts
- Project root: thumb-key/
- Plan document: ../thumb-key-modifier-plan.md (Ctrl/Alt/Meta modifier implementation)
<!-- END bigpowers:learned-preferences -->
