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
    version = 3,
    exportSchema = false,
)
abstract class VistoDatabase : RoomDatabase() {
    abstract fun davAccountDao(): DavAccountDao
    abstract fun remoteEntryDao(): RemoteEntryDao
    abstract fun thumbnailCacheDao(): ThumbnailCacheDao
    abstract fun albumSourceDao(): AlbumSourceDao

    companion object {
        private const val DB_NAME = "visto.db"

        fun create(context: Context): VistoDatabase = Room.databaseBuilder(
            context.applicationContext,
            VistoDatabase::class.java,
            DB_NAME,
        ).addMigrations(
            VistoMigrations.MIGRATION_1_2,
            VistoMigrations.MIGRATION_2_3,
        ).build()
    }
}
