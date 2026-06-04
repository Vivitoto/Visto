# Visto

Visto is a **read-only Android WebDAV gallery browser** for browsing photos, videos, and folders stored on a WebDAV server.

> Current version: `v0.1.4`
> Package: `app.visto`

[中文说明](README.md)

## Scope

Visto v0.1 focuses on safe remote media browsing:

- Add one WebDAV account
- Browse remote folders
- Use WebDAV folders as album sources
- View images and videos
- Browse thumbnails, GIF badges, and video badges
- Manually load original images to avoid unexpected bandwidth usage
- Cache thumbnails and directory metadata locally
- Clear local cache

## Safety Boundary

Visto is currently read-only. v0.1 does not provide remote write operations:

- No upload
- No remote delete
- No move
- No rename
- No remote directory creation
- No modification of WebDAV server contents

In short, Visto only reads WebDAV content for browsing and viewing.

## Download

The recommended APK is available from GitHub Releases:

- https://github.com/Vivitoto/Visto/releases/tag/latest

Download `visto-v0_1_4.apk`.

## Build

Local builds require:

- Java 17
- Android SDK
- Gradle Wrapper

Common commands:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Official release APKs are built by GitHub Actions and signed with the Visto release key.

## Project Docs

Design and implementation plans:

- `docs/plans/2026-06-03-visto-design.md`
- `docs/plans/2026-06-03-visto-tasks.md`
- `docs/plans/2026-06-04-visto-original-load-and-webdav-paths.md`
