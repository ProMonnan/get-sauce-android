package app.sahal.getsauce.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [DownloadJob::class, HistoryEntry::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var instance: AppDb? = null

        fun get(ctx: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                ctx.applicationContext, AppDb::class.java, "getsauce.db",
            ).build().also { instance = it }
        }
    }
}

class Converters {
    @TypeConverter fun statusToStr(s: JobStatus): String = s.name
    @TypeConverter fun statusFromStr(s: String): JobStatus = JobStatus.valueOf(s)
}
