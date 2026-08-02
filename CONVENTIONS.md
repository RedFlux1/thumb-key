# Thumb-Key — Agent Conventions

All AI agents working on this project MUST follow these rules.

## § Always Green / Shift Left

Every commit must pass Preflight locally. Red gates caught early cost 1× to fix; red gates caught in CI cost 10×; red gates in production cost 100×.

A gate is "green" when its exit code is 0 and stderr carries no unexpected errors. Reproducible failures are NOT pre-existing — they are new defects, regardless of when they were introduced.

Preflight: `./gradlew lintKotlin lint assembleDebug`

CI is Woodpecker (`.woodpecker.yml`): lintKotlin → lint → assembleDebug on pull_request.

Before any forward work, run Preflight locally. If it fails, fix before proceeding.

## § Discovered Defects

When you discover a reproducible defect during a development task (red test, lint failure, build break):

1. **Do not proceed** with the feature task.
2. Apply **fix-or-log**: invoke `quick-fix` for trivial data-only fixes, or `fix-bug` for defects requiring investigation and TDD.
3. Commit the fix **separately** from feature work.
4. Resume the feature task only after Preflight is green.

## Banned Dismissive Phrases

NEVER use these phrases to skip a red gate:

| Banned phrase | Why it is wrong |
|---------------|-----------------|
| "pre-existing failure" | A red gate is a defect regardless of origin |
| "unrelated to my changes" | If it is red now, it blocks forward progress |
| "not introduced by this session" | Discovery dates do not matter; only the red state |
| "out of scope" (for a red gate) | A red Preflight or CI is NEVER out of scope |
| "flaky — just re-run" | Flakiness is a defect to investigate, not ignore |

## § Stack Conventions

- **Language:** Kotlin exclusively — Java classes are forbidden
- **Formatting:** ktlint (Pinterest standard rules) via `./gradlew formatKotlin`
- **Markdown/YAML formatting:** prettier via `prettier --write "*.md" "*.yml"`
- **Architecture:** MVVM + Repositories (no domain layer), Jetpack Compose UI
- **Keyboard layouts:** named `[languages] [base] [qualifiers]? [version]?`, appended to end of `KeyboardLayout.kt`
- **Build:** `./gradlew assembleDebug`

## § Defensive Code Requirements

| Category | When required |
|----------|---------------|
| **Timeout** | All IME operations — InputConnection calls, key event dispatch. Cap at a reasonable duration. |
| **Graceful degradation** | When InputConnection is null or flaky, fall back gracefully rather than crashing. Never assume the IME connection is alive. |

## § Specs Output

All agent-generated planning, architecture, and investigation output MUST go to `specs/`:

- `specs/product/` — scope, vision, glossary
- `specs/epics/` — epic capsules with story tasks
- `specs/tech-architecture/` — tech stack, ADRs, test/security/design plans
- `specs/bugs/` — bug reports and registry
- `specs/verifications/` — verification evidence
- `specs/state.yaml` — session state and handoff
- `specs/release-plan.yaml` — release index
- `specs/execution-status.yaml` — per-story done/pending

## § Commit Messages

Follow Conventional Commits: `type(scope): description`. Types: feat, fix, chore, docs, refactor, test, ci.

## § Branch Strategy

- Work in git worktrees (use `kickoff-branch`)
- Feature branches: `feat/<slug>`
- Fix branches: `fix/<slug>`
- Default integrate mode: `solo-git` (land-branch.sh)

## § Code Quality

When making changes:
- Update all `when` branches in exhaustive dispatchers (especially `performKeyAction()`)
- Run `./gradlew formatKotlin` before committing
- Keep layout definitions consistent with the `KeyAction` sealed class
- No dead code, no commented-out blocks
