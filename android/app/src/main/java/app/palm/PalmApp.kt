package app.palm

import android.app.Application
import app.palm.core.photo.ScanResultHolder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PalmApp : Application() {
    @Inject lateinit var scanResults: ScanResultHolder

    override fun onCreate() {
        super.onCreate()
        // The previous process's capture never survives a restart (D-12); one directory listing
        scanResults.clear()
    }
}
