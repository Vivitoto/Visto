# Visto

Visto is a read-only Android WebDAV gallery browser.

- Package: `app.visto`
- v0.1 scope: browse WebDAV directories as albums
- No upload, no delete, no move/rename in v0.1

Design and implementation plan live in:

- `docs/plans/2026-06-03-visto-design.md`
- `docs/plans/2026-06-03-visto-tasks.md`

## Build

Requires Java 17, Android SDK, and Gradle/Gradle wrapper.

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```
