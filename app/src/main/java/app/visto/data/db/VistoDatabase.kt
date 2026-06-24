package app.visto.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DavAccountEntity::class,
        RemoteEntryEntity::class,
        ThumbnailCacheEntity::class,
        AlbumSourceEntity::class,
        BookProgressEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class VistoDatabase : RoomDatabase() {
    abstract fun davAccountDao(): DavAccountDao
    abstract fun remoteEntryDao(): RemoteEntryDao
    abstract fun thumbnailCacheDao(): ThumbnailCacheDao
    abstract fun albumSourceDao(): AlbumSourceDao
    abstract fun bookProgressDao(): BookProgressDao

    companion object {
        private const val DB_NAME = "visto.db"

        fun create(context: Context): VistoDatabase = Room.databaseBuilder(
            context.applicationContext,
            VistoDatabase::class.java,
            DB_NAME,
        ).addMigrations(
            VistoMigrations.MIGRATION_1_2,
            VistoMigrations.MIGRATION_2_3,
            VistoMigrations.MIGRATION_3_4,
            VistoMigrations.MIGRATION_4_5,
            VistoMigrations.MIGRATION_5_6,
            VistoMigrations.MIGRATION_6_7,
        ).build()
    }
}
