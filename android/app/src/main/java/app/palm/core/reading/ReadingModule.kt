package app.palm.core.reading

import android.content.Context
import app.palm.core.db.AppDatabase
import app.palm.core.photo.AndroidPhotoDecoder
import app.palm.core.photo.PhotoDecoder
import app.palm.core.photo.ScanResultHolder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReadingModule {
    /** The single Phase 6 swap point (D-01). */
    @Binds
    abstract fun repository(impl: FixturePalmReadingRepository): PalmReadingRepository

    @Binds
    abstract fun decoder(impl: AndroidPhotoDecoder): PhotoDecoder

    companion object {
        @Provides
        @Singleton
        fun store(@ApplicationContext context: Context, database: AppDatabase): ReadingStore =
            ReadingStore(File(context.filesDir, "readings"), database.readings())
    }
}

/** Instrumented tests seed and delete readings through the real graph. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReadingEntryPoint {
    fun readingStore(): ReadingStore
    fun scanResultHolder(): ScanResultHolder
}
