package app.palm.core.reading

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.palm.core.db.AppDatabase
import app.palm.testing.seedReading
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The store over real Room, in memory and in a temp folder, so the debug install's readings are never touched (Pitfall 10). */
@RunWith(AndroidJUnit4::class)
class ReadingStoreRoomTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase
    private lateinit var dir: File
    private lateinit var store: ReadingStore

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder<AppDatabase>(context).setDriver(BundledSQLiteDriver()).build()
        dir = File(context.cacheDir, "test-${UUID.randomUUID()}")
        store = ReadingStore(dir, db.readings())
    }

    @After fun tearDown() {
        runCatching { db.close() }
        dir.deleteRecursively()
    }

    @Test fun saveThenDelete() = runBlocking {
        val id = seedReading(context, store)
        assertTrue(File(dir, "$id.jpg").isFile)
        assertEquals(id, store.latest().first()?.id)
        store.delete(id)
        assertNull(store.byId(id).first())
        assertFalse(File(dir, "$id.jpg").exists())
    }

    @Test fun saveOnAClosedDatabaseLeavesNoPhoto() = runBlocking {
        db.close()
        val failure = runCatching { seedReading(context, store) }.exceptionOrNull()
        assertTrue("save should fail, got $failure", failure != null)
        assertTrue(dir.listFiles().isNullOrEmpty())
    }
}
