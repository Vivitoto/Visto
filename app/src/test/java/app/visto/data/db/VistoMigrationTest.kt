package app.visto.data.db

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
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
    fun version3SchemaContainsBookProgressTableAndUniquePathIndex() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VistoDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val sqlite = db.openHelper.writableDatabase
            sqlite.query("PRAGMA user_version").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(3, cursor.getInt(0))
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
}
