package app.palm.core.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
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
        } finally {
            db.close()
        }
    }
}
