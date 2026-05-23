package org.acoustixaudio.opiqo.remotecontroloverssh.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SshProfile::class, RemoteProfile::class, RemoteCommand::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sshDao(): SshDao
    abstract fun remoteProfileDao(): RemoteProfileDao
    abstract fun remoteCommandDao(): RemoteCommandDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ssh_profiles ADD COLUMN hostKeyFingerprint TEXT")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ssh_remote_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
