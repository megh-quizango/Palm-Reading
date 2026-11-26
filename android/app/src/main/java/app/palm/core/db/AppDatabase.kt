package app.palm.core.db

import android.content.Context
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers

// A reading is one row holding the whole contract JSON. Phase 5 owns the final columns;
// nothing ships before then, so Phase 5 may redefine version 1.
@Entity(tableName = "reading")
data class ReadingEntity(
    @PrimaryKey val id: String,
    val schemaVersion: Int,
    val json: String,
    val createdAt: Long,
)

@Dao
interface ReadingDao {
    @Insert
    suspend fun insert(entity: ReadingEntity)

    @Query("SELECT * FROM reading WHERE id = :id")
    suspend fun byId(id: String): ReadingEntity?

    @Query("SELECT COUNT(*) FROM reading")
    suspend fun count(): Int
}

@Database(entities = [ReadingEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun readings(): ReadingDao
}

@Module
@InstallIn(SingletonComponent::class)
object DbModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder<AppDatabase>(context, context.getDatabasePath("palm.db").absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
}
