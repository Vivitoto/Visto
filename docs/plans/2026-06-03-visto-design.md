# Visto v0.1 Design — WebDAV 相册浏览器

Project name: **Visto** (formerly working title "DavGallery")
Android package: `app.visto`
Project directory: `Visto/`


Date: 2026-06-03
Status: Draft for Vito review

## 1. Product Positioning

Visto v0.1 is an Android APK for browsing photo/video folders stored on WebDAV.

It is **not** a phone photo backup tool, not a timeline/photo-cloud replacement, and not a full file manager. Its first job is narrow:

> Turn WebDAV photo directories into a smooth gallery browsing experience.

### Core promise

- Connect to a WebDAV endpoint.
- Browse remote directories like albums.
- Show folders first, then photos/videos as a thumbnail grid.
- Cache thumbnails locally for smooth browsing.
- Open images, animated images, and videos comfortably.

### Non-goals for v0.1

- No phone photo backup.
- No upload flow.
- No timeline view.
- No full-library recursive indexing by default.
- No EXIF-based global organization.
- No AI classification or face recognition.
- No multi-user sharing.
- No service-side indexer.
- No cloud thumbnail directory written back to WebDAV.
- No complex file-management features such as batch move/rename.

## 2. Target Platform

v0.1 targets Android APK only.

Recommended stack:

- Language: Kotlin
- UI: Jetpack Compose
- Persistence: Room SQLite
- Settings: DataStore
- Networking: OkHttp
- WebDAV: lightweight custom WebDAV client or Sardine after spike
- Image loading: Coil
- Video playback: AndroidX Media3 / ExoPlayer
- Background work: Kotlin Coroutines; WorkManager only for longer thumbnail/cache jobs if needed

Rationale:

- Android-native APIs are better for local cache control, image decoding, WebP handling, video thumbnails, and APK distribution.
- v0.1 does not need cross-platform abstraction.

## 3. UX Structure

### First launch

1. Show WebDAV connection form.
2. User enters:
   - Server URL
   - Username
   - Password / app token
   - Root path, default `/`
3. User taps **Test Connection**.
4. If successful, app opens the configured root directory.

### Main browsing screen

The directory page is the core UI.

Layout:

```text
Top app bar:
  [Back] /Photos/Travel/Japan        [Sort] [Settings]

Folders section:
  [Tokyo] [Kyoto] [Hokkaido]

Media grid:
  [photo] [photo] [video]
  [webp ] [gif  ] [photo]
```

Rules:

- Folders appear first.
- Photos/videos appear below as a grid.
- Breadcrumb or compact path display should allow navigation context.
- Android system back goes to parent directory where possible.
- Pull-to-refresh or explicit refresh reloads the current directory.

### Viewer screen

When opening a media item:

- Images:
  - Show local thumbnail/preview immediately if available.
  - Download/load original as needed.
  - Support pinch zoom and swipe between media files in the current directory.
- Animated WebP/GIF:
  - Grid thumbnail uses first frame.
  - Detail view plays original animation if supported.
- Video:
  - Grid thumbnail uses a generated frame.
  - Detail view uses Media3/ExoPlayer.
- Info panel:
  - File name
  - Remote path
  - Size
  - Last modified
  - MIME/type if known

## 4. WebDAV Model

Visto treats WebDAV as the source of truth.

### Supported operations in v0.1

Required:

- `PROPFIND` current directory depth 1
- `GET` media files
- Optional `HEAD` if server supports it cleanly

Not required in v0.1:

- `DELETE`
- `PUT` upload
- `MOVE`
- `COPY`
- `MKCOL`
- WebDAV locking
- Recursive full-library crawl

### Directory scanning

v0.1 scans **on demand**:

```text
Open directory -> PROPFIND depth 1 -> update local directory cache -> render folders + media grid
```

It should not recursively scan the whole root by default. This keeps startup fast and avoids huge WebDAV libraries causing long first-run work.

### Server compatibility

The implementation should be tolerant of common WebDAV differences:

- Absolute vs relative hrefs.
- Encoded path names.
- Missing or weak ETags.
- Missing MIME type.
- Servers with slow PROPFIND responses.
- Servers that do not support HEAD consistently.

## 5. Data Model

Room database is used as a local cache, not as a separate truth source.

### `dav_account`

Fields:

- `id`
- `displayName`
- `baseUrl`
- `rootPath`
- `username`
- encrypted credential reference or encrypted token blob
- `createdAt`
- `updatedAt`

v0.1 can support one account in UI, but schema should not make multi-account impossible.

### `remote_entry`

Represents one item returned by WebDAV.

Fields:

- `id`
- `accountId`
- `parentPath`
- `path`
- `name`
- `isDirectory`
- `mediaType`:
  - `image`
  - `animated_image`
  - `video`
  - `other`
  - `unknown`
- `mimeType`
- `sizeBytes`
- `etag`
- `lastModified`
- `lastSeenAt`
- `sortName`

Indexes:

- `(accountId, parentPath)`
- `(accountId, path)` unique

### `thumbnail_cache`

Fields:

- `remoteEntryId`
- `cacheKey`
- `thumbPath`
- `width`
- `height`
- `sourceEtag`
- `sourceSizeBytes`
- `createdAt`
- `lastAccessedAt`
- `bytesOnDisk`
- `status`:
  - `ready`
  - `pending`
  - `failed`

Cache invalidation:

- If remote ETag changes, regenerate.
- If no ETag, compare size + last modified.
- Failed thumbnail generation should be remembered briefly to avoid hot retry loops.

## 6. Media Type Detection

Detection priority:

1. MIME type from WebDAV response if reliable.
2. File extension fallback.
3. Lightweight content sniff only if needed and safe.

Initial supported extensions:

Images:

- `.jpg`
- `.jpeg`
- `.png`
- `.webp`
- `.gif`
- `.heic`
- `.heif`

Videos:

- `.mp4`
- `.mov`
- `.m4v`
- `.webm`

Animated image handling:

- `.gif` is treated as animated image.
- `.webp` may be static or animated. v0.1 may mark `.webp` as potentially animated and use first-frame thumbnail; detail view attempts normal animated playback.

## 7. Thumbnail Strategy

Thumbnails are local-only and never uploaded to WebDAV.

### Small thumbnails

- Target longest edge: around 240–320 px.
- Format: WebP or JPEG depending on Android support and implementation simplicity.
- Default cache limit: 1 GB.
- Eviction: LRU by `lastAccessedAt`.

### Generation policy

- Generate thumbnails only for visible or near-visible items.
- Do not generate thumbnails for an entire large directory immediately.
- Use placeholders while thumbnails are pending.
- Avoid repeated downloads for failed/unsupported files.

### Animated WebP / GIF

- Grid thumbnail: static first frame.
- Detail view: play original file if supported.
- Do not cache animated thumbnails for v0.1.

### Video

- Grid thumbnail: one frame, preferably near the start or a representative frame.
- Detail view: Media3/ExoPlayer.
- Do not create animated video previews in v0.1.

## 8. Sorting and Display

Default directory display:

1. Folders first.
2. Media grid below.
3. Default sort: last modified, newest first.

User-selectable sort modes:

- Name A-Z
- Name Z-A
- Modified newest first
- Modified oldest first
- Size largest first
- Size smallest first
- Type

Non-media files:

- Hidden by default in media grid.
- Setting can show unsupported files as generic file cards later.

## 9. Remote Deletion Policy

Decision: v0.1 is read-only. Remote delete is **not** included in the first version and is deferred to a later release.

Implications for v0.1:

- No `DELETE` requests against WebDAV.
- No delete UI in the viewer or grid.
- Long-press / multi-select actions, if any, are limited to safe local-only operations such as "clear local cache for this item".

When remote delete is added in a later version:

- Show exact remote path.
- Explain that this deletes from WebDAV, not just local cache.
- Require explicit confirmation.
- On success, remove entry from local DB and delete local thumbnail.
- On failure, preserve local entry and show error.

Suggested future confirmation copy:

> This will delete the file from the WebDAV server. This is not just clearing local cache. Continue?

## 10. Error Handling

Common errors and behavior:

- Authentication failed:
  - Return to account settings and ask user to update credentials.
- Directory not found:
  - Show error with option to go up/root.
- PROPFIND timeout:
  - Show retry.
- Thumbnail failed:
  - Show stable placeholder and avoid immediate repeat retries.
- Original image download failed:
  - Keep viewer open with retry action.
- TLS/certificate issue:
  - Show clear network/security error. Do not silently ignore certificate errors in v0.1.

## 11. Security / Privacy

- Credentials must not be logged.
- WebDAV passwords/tokens should be stored using Android Keystore-backed encryption if practical.
- Logs should redact URLs with credentials, Authorization headers, and tokens.
- Thumbnails stay local and should be clearable from settings.
- The app should not write hidden metadata directories to WebDAV in v0.1.

## 12. Implementation Phases

### Phase 0 — Spike

Goal: verify WebDAV access and Android media thumbnail feasibility.

- Test PROPFIND depth 1 against a sample WebDAV endpoint.
- Choose Sardine vs custom OkHttp XML parser.
- Verify thumbnail generation for JPEG/PNG/WebP/GIF/video on target Android.

### Phase 1 — Basic browsing skeleton

- Android project skeleton.
- Compose navigation.
- Account settings screen.
- WebDAV test connection.
- Directory listing with folders and media entries.

### Phase 2 — Local cache/index

- Room schema.
- Cache current directory listing.
- Sort modes.
- Refresh current directory.

### Phase 3 — Thumbnails

- Local thumbnail cache directory.
- Visible-item thumbnail queue.
- First-frame handling for GIF/WebP.
- Video frame thumbnail.
- LRU cache cleanup.

### Phase 4 — Viewer

- Image viewer with zoom.
- Swipe through current directory media items.
- Animated image detail handling.
- Video playback.
- Info panel.

### Phase 5 — Polish and safety

- Error states.
- Cache settings.
- APK build.
- (Remote delete intentionally excluded from v0.1; deferred to a later release.)

## 13. Open Questions

1. Should the app support multiple WebDAV accounts in schema only, or also in UI?
2. Should root directory selection be a text field only, or include a folder picker?
3. Should unsupported files be hidden completely or shown as file cards?
4. Should the first APK target a minimum Android version such as Android 8/9/10?

## 14. Current Decisions

- Android APK first.
- Browsing WebDAV photo directories is the priority.
- Phone photo backup is out of scope.
- Timeline is out of scope.
- Directory structure may be messy; app should browse raw folders directly.
- Directory page layout: folders first, media grid below.
- Thumbnails are local-only, generated on demand, with bounded cache.
- Animated WebP/GIF grid thumbnails use the first frame.
- v0.1 is read-only; no remote delete, no upload, no move/rename.
