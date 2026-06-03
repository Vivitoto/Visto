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
    ],
    version = 1,
    exportSchema = false,
)
abstract class VistoDatabase : RoomDatabase() {
    abstract fun davAccountDao(): DavAccountDao
    abstract fun remoteEntryDao(): RemoteEntryDao
    abstract fun thumbnailCacheDao(): ThumbnailCacheDao

    companion object {
        private const val DB_NAME = "visto.db"

        fun create(context: Context): VistoDatabase = Room.databaseBuilder(
            context.applicationContext,
            VistoDatabase::class.java,
            DB_NAME,
        ).fallbackToDestructiveMigration().build()
    }
}
