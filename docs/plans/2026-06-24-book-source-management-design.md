# Visto Book Source Management Design

## Goal
Turn the current one-shot bookshelf directory scan into a persistent book-directory management flow, while keeping UI consistent with Visto's existing Material 3 card/bottom-sheet style.

## Current problem
The bookshelf folder button opens a WebDAV folder picker and immediately scans the chosen directory. The selected directory itself is not persisted, so users cannot see previously configured book directories, rescan one, or remove a directory source.

## Recommended architecture
Add a persistent `BookSourceEntity`/DAO, similar to `AlbumSourceEntity`, but with scan-result metadata. `book_progress` remains the source of displayed bookshelf books and reading progress. `book_source` only stores configured WebDAV directory roots and their last scan summary.

## UX design
- The bookshelf top-right folder button opens a `BookDirectoryManagementScreen` instead of immediately opening the picker.
- The management screen uses the existing Visto style: `Scaffold`, top app bar, rounded `Card`s, concise Chinese copy, existing color scheme/typography.
- Empty state explains that adding a WebDAV directory scans TXT/Markdown/EPUB books into the bookshelf.
- Existing sources are shown as cards with:
  - display name / folder name
  - full path
  - last scan status/time/result
  - actions: rescan, delete
- Add-directory action opens the existing `FolderPickerScreen`.
- Selecting a folder inserts a source if not duplicate, then scans it once.
- Delete removes only the source configuration. It must not delete WebDAV files and must not delete `book_progress` reading records.

## Data model
New table `book_source`:
- `id: Long`
- `accountId: Long`
- `displayName: String`
- `rootPath: String`
- `createdAt: Long`
- `updatedAt: Long`
- `lastScannedAt: Long?`
- `lastImportedCount: Int`
- `lastUpdatedCount: Int`
- `lastFoldersVisited: Int`
- `lastFoldersFailed: Int`

Constraints:
- Foreign key to `dav_account(id)` with cascade delete.
- Unique index on `(accountId, rootPath)`.
- Regular index on `accountId`.

Database migration:
- Bump Room DB version `7 -> 8`.
- Add `MIGRATION_7_8` creating `book_source` and indices.

## Scan behavior
Reuse `BookDirectoryLoader`. Scanning a source:
- Insert new scanned books into `book_progress` with default reader settings.
- For existing books, update only `name`, `sizeBytes`, and `etag`.
- Preserve reading progress and per-book reader settings.
- Update source scan metadata after scan completes.

## Reader menu tweak
Increase `ReaderLayoutMetrics.ChromeIconButtonWidth` so the three bottom chrome buttons are wider while keeping the rounded capsule style. Do not change behavior.

## Testing strategy
Because the local machine may lack Java/Gradle, still add tests and run available local checks:
- DAO test for inserting/listing/deleting book sources and duplicate root rejection.
- Migration 7→8 test for `book_source` existence and constraints.
- Scan/source helper tests where practical.
- Project smoke test updated to include new files/migration.
- Reader layout metrics test updated for wider chrome buttons.

## Out of scope
- No auto/background periodic scans.
- No deleting bookshelf entries when a source is deleted.
- No publishing, pushing, or release upload without explicit confirmation.
