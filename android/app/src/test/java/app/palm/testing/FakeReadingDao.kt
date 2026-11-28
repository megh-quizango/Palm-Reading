package app.palm.testing

import app.palm.core.db.ReadingDao
import app.palm.core.db.ReadingEntity
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory ReadingDao; byId and latest re-emit on every change like Room's Flows. */
class FakeReadingDao : ReadingDao {
    val rows = MutableStateFlow<Map<String, ReadingEntity>>(emptyMap())

    /** When set, upsert throws IOException (a failed insert). */
    var failUpsert = false

    override suspend fun upsert(e: ReadingEntity) {
        if (failUpsert) throw IOException("upsert failed")
        rows.value = rows.value + (e.id to e)
    }

    override fun byId(id: String): Flow<ReadingEntity?> = rows.map { it[id] }

    override fun latest(): Flow<ReadingEntity?> = rows.map { r -> r.values.maxByOrNull { it.createdAt } }

    override suspend fun delete(id: String) {
        rows.value = rows.value - id
    }

    override suspend fun all(): List<ReadingEntity> = rows.value.values.toList()
}
