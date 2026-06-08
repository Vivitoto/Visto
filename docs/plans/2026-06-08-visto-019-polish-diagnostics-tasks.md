# Visto 0.1.9 Implementation Tasks

## Task 1 — Baseline checks
- Inspect current local diff and preserve existing uncommitted changes.
- Run syntax/static checks available locally.

## Task 2 — Diagnostic models/service
- Add WebDAV diagnostic data classes.
- Add a service that validates URL/root and calls `WebDavClient.listDirectory`.
- Map errors to Chinese user-facing messages.

## Task 3 — Account setup diagnostics UI
- Extend `AccountFormState` with diagnostic results.
- Wire `AccountSetup.onTestConnection` to run diagnostics.
- Render step results below the existing message area.

## Task 4 — Settings diagnostics UI
- Extend `SettingsUiState` for diagnostic state/results.
- Add active-account `测试连接` button in Accounts section.
- Wire `SettingsHost` to run diagnostics for the active account.

## Task 5 — GitHub update error classification
- Add update error mapping in `AppUpdateService`.
- Improve empty/non-JSON/rate-limit/not-found/APK-missing messages.
- Ensure Settings About card displays these messages unchanged.

## Task 6 — Album home bottom sheet/polish
- Replace immediate long-click delete with selected album bottom sheet.
- Sheet must clarify local-only removal.
- Keep existing thumbnail blur/animation behavior.

## Task 7 — Viewer polish
- Add chrome visibility state and tap toggle.
- Add `current / total` text.
- Keep double-tap zoom and pager behavior intact.

## Task 8 — Verification
- Run `./local-check.sh` if possible.
- Run `git diff --check`.
- Run Gradle tests if Java is available; otherwise record blocker.
- Summarize changed files and remaining risks.
