package app.palm

import android.app.Application
import app.palm.core.photo.ScanResultHolder
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class PalmApp : Application() {
    @Inject lateinit var scanResults: ScanResultHolder

    override fun onCreate() {
        super.onCreate()
        // The previous process's capture never survives a restart (D-12); one directory listing
        scanResults.clear()
        // A stale share card never outlives the process (D-22, T-05-13); one more directory listing
        File(cacheDir, "share").listFiles()?.forEach { it.delete() }
    }
}
