package app.visto.data.db

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import app.visto.ui.reader.ReaderPageMargins
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class VistoMigrationTest {

    @Test
    fun currentSchemaContainsBookProgressCustomizationColumns() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VistoDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val sqlite = db.openHelper.writableDatabase
            sqlite.query("PRAGMA user_version").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(7, cursor.getInt(0))
            }

            sqlite.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'book_progress'").use { cursor ->
                assertTrue("book_progress table should exist", cursor.moveToFirst())
                assertEquals("book_progress", cursor.getString(0))
            }

            sqlite.query("PRAGMA index_list(`book_progress`)").use { cursor ->
                var foundUniqueAccountPathIndex = false
                while (cursor.moveToNext()) {
                    val indexName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    val isUnique = cursor.getInt(cursor.getColumnIndexOrThrow("unique")) == 1
                    if (indexName == "index_book_progress_accountId_path") {
                        foundUniqueAccountPathIndex = isUnique
                    }
                }
                assertTrue("book_progress should have a unique (accountId, path) index", foundUniqueAccountPathIndex)
            }

            sqlite.query("PRAGMA foreign_key_list(`book_progress`)").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("dav_account", cursor.getString(cursor.getColumnIndexOrThrow("table")))
                assertEquals("accountId", cursor.getString(cursor.getColumnIndexOrThrow("from")))
                assertEquals("id", cursor.getString(cursor.getColumnIndexOrThrow("to")))
                assertEquals("CASCADE", cursor.getString(cursor.getColumnIndexOrThrow("on_delete")))
            }

            sqlite.query("PRAGMA table_info(`book_progress`)").use { cursor ->
                val foundColumns = mutableSetOf<String>()
                val textColumns = setOf("fontChoice", "textColor", "backgroundStyle")
                val integerColumns = setOf(
                    "pageMarginTopDp",
                    "pageMarginBottomDp",
                    "pageMarginStartDp",
                    "pageMarginEndDp",
                    "pageStartChar",
                )
                while (cursor.moveToNext()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    if (name in textColumns) {
                        foundColumns += name
                        assertEquals("TEXT", cursor.getString(cursor.getColumnIndexOrThrow("type")))
                    }
                    if (name in integerColumns) {
                        foundColumns += name
                        assertEquals("INTEGER", cursor.getString(cursor.getColumnIndexOrThrow("type")))
                        if (name == "pageStartChar") {
                            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("notnull")))
                        }
                    }
                }
                assertEquals(textColumns + integerColumns, foundColumns)
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun migration2To3CreatesBookProgressTable() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "migration-2-3-${System.nanoTime()}.db"
        context.deleteDatabase(dbName)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(2) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
                        override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    }
                )
                .build()
        )

        try {
            val sqlite = helper.writableDatabase
            VistoMigrations.MIGRATION_2_3.migrate(sqlite)

            sqlite.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'book_progress'").use { cursor ->
                assertTrue("migration should create book_progress", cursor.moveToFirst())
                assertNotNull(cursor.getString(0))
            }
        } finally {
            helper.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun migration3To4AddsReaderFontChoiceDefault() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "migration-3-4-${System.nanoTime()}.db"
        context.deleteDatabase(dbName)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(3) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
                        override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    }
                )
                .build()
        )

        try {
            val sqlite = helper.writableDatabase
            sqlite.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `book_progress` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `accountId` INTEGER NOT NULL,
                    `path` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `sizeBytes` INTEGER,
                    `etag` TEXT,
                    `encoding` TEXT NOT NULL,
                    `chapterIndex` INTEGER NOT NULL,
                    `chapterTitle` TEXT,
                    `pageOffset` INTEGER NOT NULL,
                    `totalChapters` INTEGER NOT NULL,
                    `fontSizeSp` INTEGER NOT NULL,
                    `lineSpacing` REAL NOT NULL,
                    `theme` TEXT NOT NULL,
                    `lastReadAt` INTEGER NOT NULL,
                    `addedAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            sqlite.execSQL(
                """
                INSERT INTO `book_progress` (
                    `accountId`, `path`, `name`, `encoding`, `chapterIndex`, `pageOffset`,
                    `totalChapters`, `fontSizeSp`, `lineSpacing`, `theme`, `lastReadAt`, `addedAt`
                ) VALUES (1, '/Books/a.txt', 'a.txt', 'UTF-8', 0, 0, 1, 18, 1.5, 'light', 100, 50)
                """.trimIndent()
            )

            VistoMigrations.MIGRATION_3_4.migrate(sqlite)

            sqlite.query("SELECT fontChoice FROM book_progress WHERE path = '/Books/a.txt'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("system", cursor.getString(0))
            }
        } finally {
            helper.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun migration4To5AddsReaderColorAndBackgroundDefaults() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "migration-4-5-${System.nanoTime()}.db"
        context.deleteDatabase(dbName)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(4) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
                        override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    }
                )
                .build()
        )

        try {
            val sqlite = helper.writableDatabase
            sqlite.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `book_progress` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `accountId` INTEGER NOT NULL,
                    `path` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `sizeBytes` INTEGER,
                    `etag` TEXT,
                    `encoding` TEXT NOT NULL,
                    `chapterIndex` INTEGER NOT NULL,
                    `chapterTitle` TEXT,
                    `pageOffset` INTEGER NOT NULL,
                    `totalChapters` INTEGER NOT NULL,
                    `fontSizeSp` INTEGER NOT NULL,
                    `lineSpacing` REAL NOT NULL,
                    `theme` TEXT NOT NULL,
                    `fontChoice` TEXT NOT NULL DEFAULT 'system',
                    `lastReadAt` INTEGER NOT NULL,
                    `addedAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            sqlite.execSQL(
                """
                INSERT INTO `book_progress` (
                    `accountId`, `path`, `name`, `encoding`, `chapterIndex`, `pageOffset`,
                    `totalChapters`, `fontSizeSp`, `lineSpacing`, `theme`, `fontChoice`, `lastReadAt`, `addedAt`
                ) VALUES (1, '/Books/a.txt', 'a.txt', 'UTF-8', 0, 0, 1, 18, 1.5, 'light', 'serif', 100, 50)
                """.trimIndent()
            )

            VistoMigrations.MIGRATION_4_5.migrate(sqlite)

            sqlite.query("SELECT textColor, backgroundStyle FROM book_progress WHERE path = '/Books/a.txt'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("default", cursor.getString(0))
                assertEquals("default", cursor.getString(1))
            }
        } finally {
            helper.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun migration5To6AddsReaderMarginDefaults() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "migration-5-6-${System.nanoTime()}.db"
        context.deleteDatabase(dbName)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(5) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
                        override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    }
                )
                .build()
        )

        try {
            val sqlite = helper.writableDatabase
            sqlite.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `book_progress` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `accountId` INTEGER NOT NULL,
                    `path` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `sizeBytes` INTEGER,
                    `etag` TEXT,
                    `encoding` TEXT NOT NULL,
                    `chapterIndex` INTEGER NOT NULL,
                    `chapterTitle` TEXT,
                    `pageOffset` INTEGER NOT NULL,
                    `totalChapters` INTEGER NOT NULL,
                    `fontSizeSp` INTEGER NOT NULL,
                    `lineSpacing` REAL NOT NULL,
                    `theme` TEXT NOT NULL,
                    `fontChoice` TEXT NOT NULL DEFAULT 'system',
                    `textColor` TEXT NOT NULL DEFAULT 'default',
                    `backgroundStyle` TEXT NOT NULL DEFAULT 'default',
                    `lastReadAt` INTEGER NOT NULL,
                    `addedAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            sqlite.execSQL(
                """
                INSERT INTO `book_progress` (
                    `accountId`, `path`, `name`, `encoding`, `chapterIndex`, `pageOffset`,
                    `totalChapters`, `fontSizeSp`, `lineSpacing`, `theme`, `fontChoice`,
                    `textColor`, `backgroundStyle`, `lastReadAt`, `addedAt`
                ) VALUES (1, '/Books/a.txt', 'a.txt', 'UTF-8', 0, 0, 1, 18, 1.5, 'light', 'serif', 'ink', 'paper', 100, 50)
                """.trimIndent()
            )

            VistoMigrations.MIGRATION_5_6.migrate(sqlite)

            sqlite.query(
                """
                SELECT pageMarginTopDp, pageMarginBottomDp, pageMarginStartDp, pageMarginEndDp
                FROM book_progress
                WHERE path = '/Books/a.txt'
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(ReaderPageMargins.DEFAULT_TOP_DP, cursor.getInt(0))
                assertEquals(ReaderPageMargins.DEFAULT_BOTTOM_DP, cursor.getInt(1))
                assertEquals(ReaderPageMargins.DEFAULT_HORIZONTAL_DP, cursor.getInt(2))
                assertEquals(ReaderPageMargins.DEFAULT_HORIZONTAL_DP, cursor.getInt(3))
            }
        } finally {
            helper.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun migration6To7AddsNullablePageStartChar() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "migration-6-7-${System.nanoTime()}.db"
        context.deleteDatabase(dbName)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(6) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
                        override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    }
                )
                .build()
        )

        try {
            val sqlite = helper.writableDatabase
            sqlite.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `book_progress` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `accountId` INTEGER NOT NULL,
                    `path` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `sizeBytes` INTEGER,
                    `etag` TEXT,
                    `encoding` TEXT NOT NULL,
                    `chapterIndex` INTEGER NOT NULL,
                    `chapterTitle` TEXT,
                    `pageOffset` INTEGER NOT NULL,
                    `totalChapters` INTEGER NOT NULL,
                    `fontSizeSp` INTEGER NOT NULL,
                    `lineSpacing` REAL NOT NULL,
                    `theme` TEXT NOT NULL,
                    `fontChoice` TEXT NOT NULL DEFAULT 'system',
                    `textColor` TEXT NOT NULL DEFAULT 'default',
                    `backgroundStyle` TEXT NOT NULL DEFAULT 'default',
                    `pageMarginTopDp` INTEGER NOT NULL DEFAULT ${ReaderPageMargins.DEFAULT_TOP_DP},
                    `pageMarginBottomDp` INTEGER NOT NULL DEFAULT ${ReaderPageMargins.DEFAULT_BOTTOM_DP},
                    `pageMarginStartDp` INTEGER NOT NULL DEFAULT ${ReaderPageMargins.DEFAULT_HORIZONTAL_DP},
                    `pageMarginEndDp` INTEGER NOT NULL DEFAULT ${ReaderPageMargins.DEFAULT_HORIZONTAL_DP},
                    `lastReadAt` INTEGER NOT NULL,
                    `addedAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            sqlite.execSQL(
                """
                INSERT INTO `book_progress` (
                    `accountId`, `path`, `name`, `encoding`, `chapterIndex`, `pageOffset`,
                    `totalChapters`, `fontSizeSp`, `lineSpacing`, `theme`, `fontChoice`,
                    `textColor`, `backgroundStyle`, `pageMarginTopDp`, `pageMarginBottomDp`,
                    `pageMarginStartDp`, `pageMarginEndDp`, `lastReadAt`, `addedAt`
                ) VALUES (1, '/Books/a.txt', 'a.txt', 'UTF-8', 0, 4, 1, 18, 1.5, 'light', 'serif',
                    'ink', 'paper', 12, 52, 8, 8, 100, 50)
                """.trimIndent()
            )

            VistoMigrations.MIGRATION_6_7.migrate(sqlite)

            sqlite.query("PRAGMA table_info(`book_progress`)").use { cursor ->
                var found = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == "pageStartChar") {
                        found = true
                        assertEquals("INTEGER", cursor.getString(cursor.getColumnIndexOrThrow("type")))
                        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("notnull")))
                    }
                }
                assertTrue("migration should add nullable pageStartChar", found)
            }
            sqlite.query("SELECT pageStartChar FROM book_progress WHERE path = '/Books/a.txt'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(0))
            }
        } finally {
            helper.close()
            context.deleteDatabase(dbName)
        }
    }
}
