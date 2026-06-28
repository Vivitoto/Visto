# Visto

Visto is a **read-only Android WebDAV media and reading browser**. It helps browse albums, photos, videos, folders, and book files stored on a WebDAV server, with local reading progress and reader appearance settings.

> Current version: `v1.1.18`
>
> Package: `app.visto`

[中文说明](README.md)

## Scope

Visto currently focuses on safe and lightweight remote content browsing:

- Add and manage WebDAV accounts
- Browse remote folders
- Use WebDAV folders as album sources
- View images, GIFs, and videos
- Browse thumbnails, GIF badges, and video badges
- Manually load original images to avoid unexpected bandwidth usage
- Cache thumbnails, directory metadata, and book text locally
- Clear local cache
- Browse previously opened books in the bookshelf
- Switch bookshelf layout between list, 3-column grid, and 5-column grid
- Generate local placeholder covers for books and show reading progress
- Read plain-text / Markdown-like text content
- Save per-book reading progress, font, font size, line spacing, text color, and background style
- Import local `.ttf` / `.otf` fonts for the reader

## Safety Boundary

Visto is designed as a read-only app. It does not provide these remote write operations:

- No upload
- No remote delete
- No move
- No rename
- No remote directory creation
- No modification of WebDAV server contents

In short, Visto only reads WebDAV content for browsing, viewing, and reading. Reading progress, display preferences, and cache data are stored locally on the device.

## Privacy and Local Data

This README does not include WebDAV URLs, usernames, passwords, tokens, keys, local machine paths, or other sensitive information. Account credentials, reading records, display preferences, and cache data are used only for local app functionality.

## Download

The recommended APK is available from GitHub Releases:

- https://github.com/Vivitoto/Visto/releases/tag/latest

Download the latest `visto-*.apk`.

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

Official release APKs are built by GitHub Actions and signed for release distribution.

## Project Docs

Design and implementation plans:

- `docs/plans/2026-06-03-visto-design.md`
- `docs/plans/2026-06-03-visto-tasks.md`
- `docs/plans/2026-06-04-visto-original-load-and-webdav-paths.md`