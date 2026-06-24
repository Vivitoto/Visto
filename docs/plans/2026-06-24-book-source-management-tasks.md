# Book Source Management Implementation Plan

> **For implementer:** Use TDD throughout where local tooling allows. Write/update tests before implementation and keep changes scoped to this plan.

**Goal:** Persist and manage user-selected Visto book directories, then scan/rescan them into the bookshelf while preserving reading progress.

**Architecture:** Add a Room `book_source` table/DAO and a Material 3 management screen. `book_source` stores configured roots and scan summaries; `book_progress` remains the bookshelf/reading-progress table. `BookshelfHost` coordinates picker, source CRUD, scanning, and UI state.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Room, coroutines, existing WebDAV and `BookDirectoryLoader` code.

---

### Task 1: Add persistent book source schema

**Files:**
- Create: `app/src/main/java/app/visto/data/db/BookSourceEntity.kt`
- Create: `app/src/main/java/app/visto/data/db/BookSourceDao.kt`
- Modify: `app/src/main/java/app/visto/data/db/VistoDatabase.kt`
- Modify: `app/src/main/java/app/visto/data/db/VistoMigrations.kt`
- Test: `app/src/test/java/app/visto/data/db/BookSourceDaoTest.kt`
- Test: `app/src/test/java/app/visto/data/db/VistoMigrationTest.kt`

**Implementation notes:**
- Mirror `AlbumSourceEntity` for core columns.
- Add nullable/zero-default scan metadata.
- Unique `(accountId, rootPath)`.
- Database version becomes 8.
- Register `MIGRATION_7_8`.

**Verify:**
- `./local-check.sh` at minimum.
- If Java is available, targeted DB tests.

### Task 2: Build book directory management UI

**Files:**
- Create: `app/src/main/java/app/visto/ui/bookshelf/BookDirectoryManagementScreen.kt`
- Modify: `app/src/main/java/app/visto/ui/Strings.kt`
- Test: update smoke tests as needed.

**Implementation notes:**
- Use `Scaffold`, `TopAppBar`, rounded cards, existing typography/colors.
- Show empty state, source cards, add/rescan/delete actions, scan status.
- Delete copy must clearly say it removes only local directory config, not WebDAV files or reading progress.

**Verify:**
- UI compiles in CI.
- `./local-check.sh` locally.

### Task 3: Wire management flow in BookshelfHost

**Files:**
- Modify: `app/src/main/java/app/visto/MainActivity.kt`
- Modify: `app/src/main/java/app/visto/ui/bookshelf/BookshelfScreen.kt` if header copy/content description needs updating.

**Implementation notes:**
- Bookshelf folder button opens management screen.
- Add source opens existing `FolderPickerScreen` starting at account root or current path.
- Selecting a folder inserts source if not duplicate and scans once.
- Rescan runs `BookDirectoryLoader` for that source.
- Delete only deletes source row.
- Preserve existing scan behavior into `book_progress`.
- Keep status messages specific to the selected source/path.

**Verify:**
- Duplicate root is handled with friendly Chinese error.
- Existing books preserve progress/settings.

### Task 4: Widen reader menu buttons

**Files:**
- Modify: `app/src/main/java/app/visto/ui/reader/ReaderScreen.kt`
- Modify: `app/src/test/java/app/visto/ui/reader/ReaderLayoutMetricsTest.kt` if existing expectations mention width.

**Implementation notes:**
- Increase `ReaderLayoutMetrics.ChromeIconButtonWidth` from current value to a modest wider value (for example 64.dp or 68.dp).
- Do not change button behavior or general bottom capsule style.

**Verify:**
- Layout metric tests/local checks.

### Task 5: Final validation and review

**Files:**
- Modify: `app/src/test/java/app/visto/ProjectSmokeTest.kt` if it enforces file/migration lists.
- Run `git diff --check`.
- Run `./local-check.sh`.
- If Gradle unavailable locally, state that CI is needed for unit/build validation.

**Commit guidance:**
- Keep all local only unless Vito explicitly approves push/release.
- Suggested final commit: `feat: manage book directory sources`.
