package app.palm.feature.scan

import java.util.concurrent.ExecutionException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraInitProblemTest {
    private val ok = {}
    private val failed = { throw ExecutionException(IllegalStateException("CameraValidator")) }

    @Test fun readyBackCamera() = assertNull(cameraInitProblem(ok) { true })

    @Test fun noBackCamera() = assertEquals(CameraProblem.NoBackCamera, cameraInitProblem(ok) { false })

    @Test fun failedInitNeverQueriesTheCamera() {
        var queried = false
        assertEquals(CameraProblem.OpenFailed, cameraInitProblem(failed) { queried = true; true })
        assertEquals(false, queried)
    }

    @Test fun hasCameraThrowing() =
        assertEquals(CameraProblem.OpenFailed, cameraInitProblem(ok) { throw IllegalStateException("Camera not initialized") })
}
