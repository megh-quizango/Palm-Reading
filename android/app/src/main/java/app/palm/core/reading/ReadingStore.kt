package app.palm.core.reading

import app.palm.contract.HandSide
import app.palm.contract.PalmReadingDto
import app.palm.core.db.ReadingDao
import app.palm.core.db.ReadingEntity
import app.palm.core.photo.ProcessedCapture
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** A stored reading; [photo] is null when its file name does not resolve inside the readings folder. */
data class SavedReading(val id: String, val json: String, val photo: File?, val hand: HandSide, val createdAt: Long)

/** Saved readings (D-16, D-17): the photo in [dir] as <id>.jpg plus one Room row. Never logs (D-18). */
class ReadingStore(
    private val dir: File,
    private val dao: ReadingDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /**
     * Moves the capture's photo into [dir] and writes the row; returns the reading id.
     * A completed scan is exactly one successful save (D-15), so Phase 6's free-scan counter increments here and nowhere else.
     */
    suspend fun save(reading: PalmReadingDto, capture: ProcessedCapture): String = withContext(io) {
        require(reading.verdict.ok) { "verdict not ok" }
        val id = reading.readingId
        requireSafeId(id) // the id becomes a file name (T-05-01): checked before any file work
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("cannot create the readings folder")
        val photo = File(dir, "$id.jpg")
        try {
            Files.move(capture.file.toPath(), photo.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.copy(capture.file.toPath(), photo.toPath(), StandardCopyOption.REPLACE_EXISTING)
            capture.file.delete()
        }
        // shortcut: a crash between the move and the insert leaves a private orphan photo; sweep at start once a history screen lists readings
        try {
            // capture.landmarks are never written (D-18)
            dao.upsert(ReadingEntity(id, reading.schemaVersion, ContractJson.encodeToString(reading), photo.name, reading.handSide.name, now()))
        } catch (e: Exception) {
            photo.delete()
            throw e
        }
        id
    }

    fun byId(id: String): Flow<SavedReading?> = dao.byId(id).map { it?.toSaved() }

    fun latest(): Flow<SavedReading?> = dao.latest().map { it?.toSaved() }

    suspend fun delete(id: String) = withContext(io) {
        requireSafeId(id)
        dao.delete(id)
        File(dir, "$id.jpg").delete()
        Unit
    }

    suspend fun deleteAll() = withContext(io) {
        // Rows straight through the DAO (a bad stored id must not stop the sweep); then every file, orphans included
        dao.all().forEach { dao.delete(it.id) }
        dir.listFiles()?.forEach { it.delete() }
        Unit
    }

    private fun ReadingEntity.toSaved(): SavedReading? {
        val hand = HandSide.entries.firstOrNull { it.name == handSide } ?: return null
        return SavedReading(id, json, photoInside(photoFile), hand, createdAt)
    }

    /** The photo only when it resolves inside [dir] (a stored "../x" never escapes). */
    private fun photoInside(name: String): File? {
        val file = File(dir, name)
        return file.takeIf { it.canonicalPath.startsWith(dir.canonicalPath + File.separator) }
    }

    /** Lower-case canonical UUIDs only (the schema's format: uuid), so "../x" or odd casings never reach a path. */
    private fun requireSafeId(id: String) {
        val canonical = try {
            UUID.fromString(id).toString()
        } catch (_: IllegalArgumentException) {
            null
        }
        require(canonical == id) { "reading id is not a lower-case UUID" }
    }
}
