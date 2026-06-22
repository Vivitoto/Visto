package app.visto.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migrations for Visto.
 *
 * v0.1 briefly used destructive migrations while the app was a skeleton. Now
 * that account/albums can contain real user setup, migrations preserve data.
 */
object VistoMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `album_source` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `accountId` INTEGER NOT NULL,
                    `displayName` TEXT NOT NULL,
                    `rootPath` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    FOREIGN KEY(`accountId`) REFERENCES `dav_account`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_source_accountId` ON `album_source` (`accountId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_album_source_accountId_rootPath` ON `album_source` (`accountId`, `rootPath`)")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
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
                    `addedAt` INTEGER NOT NULL,
                    FOREIGN KEY(`accountId`) REFERENCES `dav_account`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_book_progress_accountId_path` ON `book_progress` (`accountId`, `path`)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `book_progress` ADD COLUMN `fontChoice` TEXT NOT NULL DEFAULT 'system'")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `book_progress` ADD COLUMN `textColor` TEXT NOT NULL DEFAULT 'default'")
            db.execSQL("ALTER TABLE `book_progress` ADD COLUMN `backgroundStyle` TEXT NOT NULL DEFAULT 'default'")
        }
    }
}
