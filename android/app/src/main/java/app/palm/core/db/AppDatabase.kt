package app.palm.core.db

import android.content.Context
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
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
import kotlinx.coroutines.flow.Flow

/** One saved reading (D-16): the whole contract JSON, its photo file name in filesDir/readings, hand and time. No landmarks. */
@Entity(tableName = "reading", indices = [Index("createdAt")])
data class ReadingEntity(
    @PrimaryKey val id: String,
    val schemaVersion: Int,
    val json: String,
    val photoFile: String,
    val handSide: String,
    val createdAt: Long,
)

@Dao
interface ReadingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: ReadingEntity)

    @Query("SELECT * FROM reading WHERE id = :id")
    fun byId(id: String): Flow<ReadingEntity?>

    @Query("SELECT * FROM reading ORDER BY createdAt DESC LIMIT 1")
    fun latest(): Flow<ReadingEntity?>

    @Query("DELETE FROM reading WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM reading")
    suspend fun all(): List<ReadingEntity>
}

@Database(entities = [ReadingEntity::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun readings(): ReadingDao
}

/** The production builder (DbModule and the v1 -> v2 test). */
fun buildDatabase(context: Context, path: String): AppDatabase =
    Room.databaseBuilder<AppDatabase>(context, path)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        // v1 only existed on debug installs and the app never wrote a row to it, so v1 is dropped
        .fallbackToDestructiveMigrationFrom(true, 1)
        .build()

@Module
@InstallIn(SingletonComponent::class)
object DbModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        buildDatabase(context, context.getDatabasePath("palm.db").absolutePath)
}
