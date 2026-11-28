package app.palm.core.db

import androidx.room3.Room
import java.io.File
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    @Test
    fun upsertThenReadById() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder<AppDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .build()
        try {
            val entity = ReadingEntity("r1", 1, """{"schema_version":1}""", "r1.jpg", "RIGHT", 1_700_000_000_000)
            db.readings().upsert(entity)
            assertEquals(entity, db.readings().byId("r1").first())
            assertEquals(listOf(entity), db.readings().all())
            assertEquals(entity, db.readings().latest().first())
            db.readings().delete("r1")
            assertEquals(emptyList<ReadingEntity>(), db.readings().all())
        } finally {
            db.close()
        }
    }

    /** A v1 file (schemas/1.json, written by hand without room3-testing) opens as v2 with its rows dropped (RESEARCH Pattern 5). */
    @Test
    fun version1FileOpensAsVersion2() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = context.getDatabasePath("v1-test.db")
        fun deleteFiles() = listOf("", "-wal", "-shm", "-journal").forEach { File(file.path + it).delete() }
        deleteFiles()
        file.parentFile?.mkdirs()
        val c = BundledSQLiteDriver().open(file.path)
        try {
            c.execSQL("CREATE TABLE IF NOT EXISTS `reading` (`id` TEXT NOT NULL, `schemaVersion` INTEGER NOT NULL, `json` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            c.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            c.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'd87abb872b81665c4e6776d2714c9889')")
            c.execSQL("INSERT INTO reading VALUES('old', 1, '{}', 1)")
            c.execSQL("PRAGMA user_version = 1")
        } finally {
            c.close()
        }
        val db = buildDatabase(context, file.path)
        try {
            assertEquals(emptyList<ReadingEntity>(), db.readings().all())
            val entity = ReadingEntity("r2", 1, "{}", "r2.jpg", "LEFT", 2)
            db.readings().upsert(entity)
            assertEquals(entity, db.readings().byId("r2").first())
        } finally {
            db.close()
            deleteFiles()
        }
    }
}
