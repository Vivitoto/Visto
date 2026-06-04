package app.visto.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VistoMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        VistoDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migration1To2AddsAlbumSourceWithoutDroppingAccounts() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `dav_account` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `displayName` TEXT NOT NULL,
                    `baseUrl` TEXT NOT NULL,
                    `rootPath` TEXT NOT NULL,
                    `username` TEXT NOT NULL,
                    `credentialRef` TEXT NOT NULL,
                    `isActive` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_dav_account_baseUrl_username` ON `dav_account` (`baseUrl`, `username`)")
            execSQL(
                """
                INSERT INTO dav_account(id, displayName, baseUrl, rootPath, username, credentialRef, isActive, createdAt, updatedAt)
                VALUES (1, 'NAS', 'https://nas.example.com/dav', '/', 'vito', 'ref', 1, 1, 1)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, VistoMigrations.MIGRATION_1_2).apply {
            query("SELECT COUNT(*) FROM dav_account").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            execSQL(
                """
                INSERT INTO album_source(accountId, displayName, rootPath, createdAt, updatedAt)
                VALUES (1, '家庭照片', '/Photos/Family', 2, 2)
                """.trimIndent()
            )
            query("SELECT displayName, rootPath FROM album_source WHERE accountId = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("家庭照片", cursor.getString(0))
                assertEquals("/Photos/Family", cursor.getString(1))
            }
            close()
        }
    }

    companion object {
        private const val TEST_DB = "visto-migration-test"
    }
}
