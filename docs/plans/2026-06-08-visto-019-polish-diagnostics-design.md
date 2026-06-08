# Visto 0.1.9 Polish & Diagnostics Design

## Goal
Improve Visto's perceived polish without changing the read-only WebDAV boundary:

1. Album home should feel more like a photo app.
2. Viewer should be more immersive and self-explanatory.
3. Update/WebDAV failures should be diagnosable by normal users.

## Non-goals
- No upload/delete/move/rename remote files.
- No multi-account redesign.
- No EXIF timeline or indexing.
- No release/push in this implementation step without explicit confirmation.

## Existing constraints
- Repository currently has local uncommitted UI fixes in `MainActivity.kt`, `AlbumDetailScreen.kt`, `AlbumListScreen.kt`, and `BrowserScreen.kt`; preserve them.
- Visto is still read-only: album deletion means removing the local album source, not remote files.
- In-app update uses GitHub Release `latest` only.

## Feature A — Album home polish

### Behavior
- Keep the current album list model, but make each album row/card more album-like.
- Add a small summary above the album list: number of albums and available cover count when practical.
- Long-press or delete/action button opens a bottom sheet instead of immediately acting.
- Bottom sheet actions:
  - Open album
  - Refresh album (if wiring is cheap; otherwise leave as future action)
  - Copy path (if clipboard wiring is cheap)
  - Remove from Visto
  - Cancel
- Removal copy must clearly say it does not delete server files.

### UI notes
- Prefer Material3 modal bottom sheet.
- Keep thumbnail blur privacy behavior.
- Do not disturb existing staggered thumbnail animation imports/behavior.

## Feature B — Viewer polish

### Behavior
- Show current position, e.g. `23 / 142`, in viewer chrome.
- Tap toggles viewer chrome visibility.
- Keep double-tap zoom behavior for images.
- Add bottom filename/position overlay when chrome is visible.
- Keep video playback behavior unchanged.
- Error states should retain retry button.

### Risks
- Gesture conflicts with pager and pinch zoom. Use lightweight tap handling on page background/chrome, avoid rewriting the transform stack.

## Feature C — GitHub update error states

### Behavior
- `AppUpdateService` should classify common failures into clearer Chinese messages:
  - GitHub API timeout/network failure.
  - HTTP 403/rate limit.
  - HTTP 404 latest release missing.
  - Empty/non-JSON response.
  - Release has no APK asset.
  - APK download HTTP failure / size mismatch.
- Settings About card should show the clearer message and keep Retry/Open Release actions available.

### Boundary
- Do not add GitHub token configuration in this version.

## Feature D — WebDAV diagnostics

### Behavior
- Add shared diagnostic model/service for current WebDAV credentials.
- Account setup screen already has `测试连接`; upgrade result from a single message to step results.
- Settings account section also gets `测试连接` for the active/current account.
- Diagnostic steps:
  1. Server URL format.
  2. PROPFIND authentication and server response.
  3. Root path exists and is readable.
  4. Directory listing contains counts: folders/images/videos/other.
- Show results in a small card/list with status icons/text.

### Implementation approach
- Add `data/webdav/WebDavDiagnostic.kt` and `WebDavDiagnosticsService.kt`.
- Reuse `WebDavClient.listDirectory(rootPath)` so behavior matches real browsing.
- Map existing `WebDavError` to user-friendly messages.
- Thread through `AccountFormState` and `SettingsUiState` minimally.

## Validation
- `./local-check.sh` if available.
- `git diff --check`.
- Kotlin compile if Java/JAVA_HOME is available; otherwise note blocker and rely on CI later.
- Manual static checks for no read-write WebDAV operations.
