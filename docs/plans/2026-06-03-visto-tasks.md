# Visto v0.1 Implementation Plan

Project name: **Visto**
Android package: `app.visto`
Project directory: `Visto/`


Date: 2026-06-03
Design: `docs/plans/2026-06-03-visto-design.md`
Status: Draft execution plan

## Execution Contract

This plan follows the Superpowers workflow: small tasks, test-first where practical, and verification after each task.

Hard constraints from the approved design:

- Android APK first.
- v0.1 is a read-only WebDAV gallery browser.
- No upload, no remote delete, no move/rename.
- No timeline view.
- No recursive full-library scan by default.
- Directory page layout: folders first, media grid below.
- Thumbnails are local-only and bounded by cache limit.
- Animated WebP/GIF grid thumbnails use first frame.

Working assumptions for v0.1 unless Vito changes them:

- Project root: `Visto/` under workspace.
- Package name: `app.visto`.
- minSdk: 26, targetSdk: current stable Android Gradle Plugin default.
- UI supports one WebDAV account, but database schema keeps `accountId` for future multi-account support.
- Root directory is a text field in v0.1; folder picker can be added later.
- Unsupported files are hidden by default.
- WebDAV implementation starts with OkHttp + custom minimal PROPFIND XML parser; Sardine is only used if the custom route proves painful.

## Verification Commands

Once project exists, use these gates:

```bash
cd Visto
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

If Android instrumentation tests are added later:

```bash
cd Visto
./gradlew connectedDebugAndroidTest
```

## Phase 0 — Project Skeleton and Guardrails

### Task 0.1 — Create Android project skeleton

Goal: create a minimal Kotlin Android project that builds.

Test first:

- Add a placeholder unit test `ProjectSmokeTest` expecting the app package/config object to exist.
- Run unit tests and see failure before adding the referenced object.

Implementation:

- Create `Visto/` Gradle project.
- Configure Kotlin Android, Compose, and basic app module.
- Add a tiny `AppInfo` object with package/name constants.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Commit/checkpoint note:

- Project skeleton builds.

### Task 0.2 — Add dependency baseline

Goal: add only dependencies needed for early tests and architecture.

Test first:

- Add a unit test importing MockWebServer and OkHttp types; it should fail before dependencies are added.

Implementation:

- Add dependencies:
  - OkHttp
  - MockWebServer for tests
  - Kotlin coroutines test
  - Room runtime/compiler/test dependencies
  - DataStore preferences
  - Coil Compose
  - Media3 ExoPlayer

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 0.3 — Establish package/module layout

Goal: create stable code organization.

Test first:

- Add tests referencing empty package-level marker classes for data/network/ui layers.

Implementation:

Suggested packages:

```text
app.visto
  core.model
  core.media
  core.sort
  data.account
  data.db
  data.webdav
  data.thumbnail
  ui.account
  ui.browser
  ui.viewer
  ui.settings
```

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

## Phase 1 — Core Models and Pure Logic

### Task 1.1 — Define media type model

Goal: classify remote files as folder/image/animated image/video/other.

Test first:

- Write tests for extensions:
  - `.jpg`, `.jpeg`, `.png` -> image
  - `.gif` -> animated image
  - `.webp` -> animated image candidate or image-like animated category
  - `.heic`, `.heif` -> image
  - `.mp4`, `.mov`, `.m4v`, `.webm` -> video
  - unknown extension -> other

Implementation:

- Add `MediaType` enum/sealed class.
- Add `MediaTypeDetector.detect(name, mimeType)`.
- MIME type wins when reliable; extension fallback otherwise.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 1.2 — Define remote entry model

Goal: model one WebDAV item independent of database and UI.

Test first:

- Test that constructing a folder entry and media entry normalizes paths and names.

Implementation:

- Add `RemoteEntry` domain model:
  - `accountId`
  - `parentPath`
  - `path`
  - `name`
  - `isDirectory`
  - `mediaType`
  - `mimeType`
  - `sizeBytes`
  - `etag`
  - `lastModified`

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 1.3 — Path normalization

Goal: handle messy WebDAV paths consistently.

Test first:

- Cases:
  - `""` -> `/`
  - `Photos` -> `/Photos`
  - `/Photos//Travel/` -> `/Photos/Travel`
  - encoded names remain safe for display after decode where appropriate
  - parent of `/Photos/Travel` -> `/Photos`
  - parent of `/` -> null

Implementation:

- Add `DavPath` utility for normalize, join, parent, displayName.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 1.4 — Directory sorting rules

Goal: folders first, then media grid, with selectable sort modes.

Test first:

- Mixed entries sort with folders before files.
- Modified newest first default.
- Name A-Z / Z-A.
- Size largest/smallest.
- Type sort stable.

Implementation:

- Add `SortMode` enum.
- Add `DirectorySorter.sort(entries, mode)`.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

## Phase 2 — WebDAV Client

### Task 2.1 — Build PROPFIND request shape

Goal: send correct WebDAV depth-1 listing request.

Test first:

- With MockWebServer, assert request:
  - method `PROPFIND`
  - header `Depth: 1`
  - basic auth header present when credentials configured
  - body requests `resourcetype`, `getcontentlength`, `getcontenttype`, `getetag`, `getlastmodified`

Implementation:

- Add `WebDavClient.listDirectory(path)` shell.
- Use OkHttp.
- Do not implement parser yet; return empty on fixed response for now.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 2.2 — Parse simple WebDAV multistatus XML

Goal: convert WebDAV response XML into raw entries.

Test first:

- Fixture with one directory and one JPEG file.
- Assert href, content type, size, etag, last modified parsed.

Implementation:

- Add `WebDavMultistatusParser`.
- Keep parser tolerant of namespaces.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 2.3 — Handle href variants

Goal: tolerate common server path formats.

Test first:

- Fixture cases:
  - absolute URL href
  - root-relative href
  - encoded spaces/non-ASCII
  - current directory self entry should be excluded from children

Implementation:

- Normalize href against account base URL and requested path.
- Exclude self entry from directory children.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 2.4 — Map parsed WebDAV rows to `RemoteEntry`

Goal: produce domain entries with media classification.

Test first:

- Directory row -> `isDirectory=true`.
- File row with missing MIME but `.jpg` extension -> image.
- File row with `.mp4` -> video.
- Unknown file -> other.

Implementation:

- Add mapper from parsed XML row to `RemoteEntry`.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 2.5 — WebDAV error model

Goal: surface authentication/network/not-found/timeouts clearly.

Test first:

- Mock 401 -> `AuthFailed`.
- Mock 404 -> `NotFound`.
- Mock 500 -> `ServerError`.
- Timeout simulation -> `NetworkError` or `Timeout`.

Implementation:

- Add sealed `WebDavError`.
- Wrap client results in `Result`/custom outcome.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 2.6 — GET media request builder

Goal: provide authenticated GET URLs/requests for image/video loading.

Test first:

- Mock request path and auth for `GET /Photos/a.jpg`.

Implementation:

- Add method to build authenticated `Request` for a remote path.
- This can later feed Coil/Media3 loaders.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

## Phase 3 — Room Database Cache

### Task 3.1 — Define Room entities and DAO shell

Goal: create database schema for accounts, remote entries, thumbnails.

Test first:

- Room in-memory test tries to insert and query an account; fails until entities/DAO exist.

Implementation:

- Add `DavAccountEntity`.
- Add `RemoteEntryEntity`.
- Add `ThumbnailCacheEntity`.
- Add DAOs.
- Add indexes from design.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 3.2 — Save and load current directory listing

Goal: cache depth-1 listing for a directory.

Test first:

- Insert three entries under `/Photos`.
- Query by `(accountId, parentPath)` returns only those entries.

Implementation:

- DAO methods:
  - `upsertEntries(entries)`
  - `entriesForParent(accountId, parentPath)`

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 3.3 — Mark stale entries on refresh

Goal: remove or hide entries no longer seen in latest PROPFIND.

Test first:

- Existing DB has `a.jpg`, `b.jpg`.
- Refresh returns only `a.jpg`.
- `b.jpg` is removed or marked absent after transaction.

Implementation:

- Add repository transaction `replaceDirectoryListing(accountId, parentPath, entries, refreshTime)`.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 3.4 — Persist one account settings record

Goal: support one account in UI while keeping schema future-proof.

Test first:

- Save account, update root path, retrieve active/default account.

Implementation:

- DAO/repository for `DavAccount`.
- For v0.1, latest/only account is active.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

## Phase 4 — Account Settings and Connection Test UI

### Task 4.1 — Account form state reducer

Goal: pure UI state logic for account form.

Test first:

- Invalid URL disables test button.
- Missing username/password disables test button.
- Root path normalizes to `/` when blank.

Implementation:

- Add `AccountFormState` and reducer/validator.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 4.2 — Compose account screen

Goal: render first-launch WebDAV form.

Test first:

- Add Compose UI test if feasible, or snapshot-free state test for ViewModel.

Implementation:

- Text fields:
  - server URL
  - username
  - password/token
  - root path
- Buttons:
  - Test Connection
  - Save

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

### Task 4.3 — Account ViewModel connection test

Goal: test connection without saving bad credentials.

Test first:

- Fake WebDAV repository returns success -> UI state success.
- Fake returns auth failure -> UI state error.

Implementation:

- Add `AccountViewModel`.
- Connect form to `WebDavClient.listDirectory(rootPath)`.
- Do not log credentials.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 4.4 — Save successful account and route to browser

Goal: after successful save, app opens root directory.

Test first:

- ViewModel test: save success emits navigation event with root path.

Implementation:

- Persist account.
- Navigate to browser screen.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Phase 5 — Directory Browser

### Task 5.1 — Browser ViewModel loads cached listing first

Goal: show cached directory entries immediately before network refresh.

Test first:

- Fake repository has cached entries.
- ViewModel exposes cached entries with `isRefreshing=false/true` as appropriate.

Implementation:

- Add `BrowserViewModel`.
- Expose state: path, folders, media, loading/error/refreshing.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 5.2 — Browser ViewModel refreshes WebDAV listing

Goal: `refresh()` pulls remote listing and updates cache.

Test first:

- Fake WebDAV returns new entries.
- Repository called to replace current directory.
- UI state updates.

Implementation:

- Wire WebDAV client + Room repository.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 5.3 — Browser error states

Goal: clear errors for auth/not-found/network.

Test first:

- Auth failure maps to account error message.
- Not found maps to directory error with go-root action.
- Network error maps to retry action.

Implementation:

- Add UI error model.
- Map WebDAV errors.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 5.4 — Compose browser screen layout

Goal: folders first, media grid below.

Test first:

- ViewModel/state test ensures folders and media are separated.
- Compose UI smoke test if feasible.

Implementation:

- Top app bar with back/path/sort/settings.
- Folder section.
- LazyVerticalGrid media section.
- Empty state.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

### Task 5.5 — Directory navigation

Goal: tap folder enters child directory; back goes parent.

Test first:

- Path navigation reducer test:
  - open `/Photos/Travel`
  - back -> `/Photos`
  - root back -> no parent

Implementation:

- Add navigation reducer/helper.
- Wire folder card click.
- Android back handler.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

### Task 5.6 — Sort menu

Goal: user can change sort mode for current directory.

Test first:

- Browser ViewModel applies sort mode and persists/defaults it.

Implementation:

- Sort menu in top bar.
- Persist selected sort mode in DataStore or simple settings repository.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Phase 6 — Thumbnail Cache

### Task 6.1 — Thumbnail cache key generation

Goal: stable unique key per remote file version.

Test first:

- Same path+etag -> same key.
- Same path changed etag -> different key.
- Missing etag uses size+lastModified.

Implementation:

- Add `ThumbnailCacheKey` utility.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 6.2 — Thumbnail repository DB behavior

Goal: track thumbnail status and access time.

Test first:

- Insert pending, update ready, mark accessed.
- Changed source metadata invalidates old thumbnail.

Implementation:

- Add `ThumbnailRepository` around DAO.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 6.3 — Image thumbnail generator

Goal: generate small static thumbnails for JPEG/PNG/static image files.

Test first:

- Use local fixture bitmap/image input.
- Generate thumbnail <= target edge.
- Output file exists and size > 0.

Implementation:

- Add Android-compatible thumbnail generator.
- Prefer JVM-testable pure bitmap logic where possible; Android decoder may need Robolectric or instrumentation.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

If decoder cannot run in local JVM tests, add a small instrumentation test later.

### Task 6.4 — Animated image first-frame thumbnail

Goal: GIF/WebP grid thumbnail is static first frame.

Test first:

- Fixture animated image generates static small thumbnail.
- If animated fixture support is hard locally, test behavior routing: animated media calls first-frame path, not animated cache path.

Implementation:

- Use Android `ImageDecoder` where available.
- Fallback to placeholder on unsupported decoder errors.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

### Task 6.5 — Video frame thumbnail

Goal: generate one thumbnail frame for video entries.

Test first:

- Routing test: video media calls video thumbnail generator.
- Optional instrumentation/fixture test if feasible.

Implementation:

- Use `MediaMetadataRetriever`.
- Fallback to video placeholder if generation fails.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

### Task 6.6 — Visible-item thumbnail queue

Goal: only generate thumbnails for visible/near-visible media.

Test first:

- Given visible entry IDs, queue schedules only those not ready/failed recently.
- Duplicate visible events do not enqueue duplicates.

Implementation:

- Add `ThumbnailQueue` with limited concurrency.
- Browser grid reports visible media IDs.
- Do not scan/generate whole directory at once.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 6.7 — LRU cache cleanup

Goal: enforce default 1GB cache limit.

Test first:

- Fake thumbnails with sizes and access times.
- Cleanup deletes oldest until under limit.
- Ready thumbnails with missing files are removed from DB.

Implementation:

- Add `ThumbnailCacheCleaner`.
- Expose setting for cache limit, default 1GB.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

## Phase 7 — Media Loading and Viewer

### Task 7.1 — Viewer model selects current directory media only

Goal: swipe within current directory media items, not folders/other files.

Test first:

- Mixed entries produce viewer list of only image/animated/video.
- Opening an item sets correct index.

Implementation:

- Add `ViewerSession` model.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 7.2 — Image viewer Compose shell

Goal: show image media with loading/error states.

Test first:

- ViewModel state test for loading -> ready -> error.

Implementation:

- Viewer screen with top bar and info action.
- Use Coil for image display.
- Use authenticated request/model from WebDAV client.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

### Task 7.3 — Pinch zoom support

Goal: images support zoom/pan.

Test first:

- State reducer test for zoom bounds if custom transform state is written.

Implementation:

- Add Compose transform gestures or a proven zoomable image component if lightweight.

Verify:

```bash
cd Visto
./gradlew assembleDebug
```

### Task 7.4 — Swipe between media items

Goal: viewer can swipe left/right through current directory media.

Test first:

- Reducer test for next/previous index boundaries.

Implementation:

- Pager UI over viewer media list.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

### Task 7.5 — Video viewer

Goal: video entries play with Media3/ExoPlayer.

Test first:

- ViewModel/model test that video entries choose video rendering path.

Implementation:

- Add Media3 player screen.
- Feed authenticated URL/request headers if needed.
- Fallback to error if streaming unsupported by server.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

### Task 7.6 — Info panel

Goal: show file metadata.

Test first:

- Formatter tests:
  - bytes to human size
  - last modified display
  - remote path display

Implementation:

- Bottom sheet/dialog with name/path/size/modified/type.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Phase 8 — Settings and Cache Management

### Task 8.1 — Settings repository

Goal: persist sort/cache/display settings.

Test first:

- Default cache limit is 1GB.
- Saving sort mode persists and reloads.
- Unsupported files hidden default is true.

Implementation:

- DataStore settings repository.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 8.2 — Settings screen

Goal: expose safe user-configurable settings.

Test first:

- ViewModel tests for cache limit and clear-cache action state.

Implementation:

- Show:
  - account summary
  - root path
  - default sort
  - thumbnail cache limit
  - clear thumbnail cache
  - show unsupported files toggle, optional

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

### Task 8.3 — Clear local thumbnail cache

Goal: delete local thumbnails only, never remote files.

Test first:

- Fake cache directory with files.
- Clear action removes files and DB rows.
- Remote entries remain.

Implementation:

- Add local-only cache clear action.
- Label clearly as local cache.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

## Phase 9 — Security and Logging

### Task 9.1 — Credential storage abstraction

Goal: avoid plain logging and prepare for encrypted storage.

Test first:

- Credential redaction test: URL/user/password/token are not shown in logs/errors.

Implementation:

- Add credential model with redacted `toString()`.
- Store credential via encrypted preference/Keystore-backed mechanism if practical.
- If encryption dependency is delayed, document temporary storage and keep it local-only.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 9.2 — Network log redaction

Goal: ensure Authorization headers never reach logs.

Test first:

- Logger/redactor test removes `Authorization` and credentials in URLs.

Implementation:

- Add safe logging helper.
- Do not add verbose network logging by default.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 9.3 — Enforce read-only WebDAV operations in v0.1

Goal: prevent accidental mutation endpoints.

Test first:

- Unit test enumerates WebDAV client public operations and asserts no DELETE/PUT/MOVE/COPY methods are exposed for v0.1.
- MockWebServer test verifies browser/viewer flows only emit PROPFIND/GET/optional HEAD.

Implementation:

- Keep WebDAV client API read-only.
- No delete/upload UI.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

## Phase 10 — End-to-End Smoke and APK

### Task 10.1 — Fake WebDAV end-to-end browser test

Goal: prove account -> directory -> grid path against MockWebServer.

Test first:

- Build integration-style test with fake PROPFIND XML containing folders and media.
- Assert repository stores entries and BrowserViewModel exposes folders/media correctly.

Implementation:

- Wire real WebDAV parser/client with fake server and in-memory DB.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
```

### Task 10.2 — Manual local WebDAV smoke script

Goal: support manual testing with a local WebDAV container/server.

Test first:

- N/A for shell helper; inspect script behavior only.

Implementation:

- Add docs/manual-test-webdav.md with a simple local WebDAV setup.
- Avoid committing credentials.

Verify:

```bash
cd Visto
./gradlew assembleDebug
```

### Task 10.3 — APK build artifact

Goal: produce first debug APK.

Test first:

- Full unit test suite passes.

Implementation:

- Build debug APK.
- Record APK path.

Verify:

```bash
cd Visto
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Expected artifact:

```text
Visto/app/build/outputs/apk/debug/app-debug.apk
```

## Review Gates

After every phase:

1. Unit tests pass.
2. `assembleDebug` passes when UI/app code changed.
3. No WebDAV mutation methods added.
4. No credentials in logs or committed files.
5. Behavior remains directory-first, no timeline creep.

## Suggested Subagent Execution Order

If using subagents, dispatch one phase at a time:

1. Phase 0 skeleton.
2. Phase 1 pure logic.
3. Phase 2 WebDAV parser/client.
4. Phase 3 Room cache.
5. Phase 4 account UI.
6. Phase 5 browser UI.
7. Phase 6 thumbnails.
8. Phase 7 viewer.
9. Phase 8 settings/cache.
10. Phase 9 security/read-only guardrails.
11. Phase 10 smoke/APK.

Each subagent task should include:

- The design doc path.
- This task plan path.
- Exact phase/task scope.
- No scope creep beyond v0.1.
- Required verification command(s).
