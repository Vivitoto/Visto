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
}
