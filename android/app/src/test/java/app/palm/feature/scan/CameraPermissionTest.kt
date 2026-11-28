package app.palm.feature.scan

import org.junit.Assert.assertEquals
import org.junit.Test

/** D-04 / research Pattern 5: Denied vs PermanentlyDenied only after a real answer. */
class CameraPermissionTest {
    @Test fun grantedWinsOverEverything() {
        assertEquals(CamPerm.Granted, cameraPerm(granted = true, requested = false, showRationale = false))
        assertEquals(CamPerm.Granted, cameraPerm(granted = true, requested = true, showRationale = true))
    }

    @Test fun noAnswerYetIsUnknown() = assertEquals(CamPerm.Unknown, cameraPerm(false, requested = false, showRationale = false))

    @Test fun refusedWithRationaleIsDenied() = assertEquals(CamPerm.Denied, cameraPerm(false, requested = true, showRationale = true))

    @Test fun refusedWithoutRationaleIsBlocked() =
        assertEquals(CamPerm.PermanentlyDenied, cameraPerm(false, requested = true, showRationale = false))

    @Test fun unknownIsRequesting() = assertEquals(GateScreen.Requesting, gateScreen(CamPerm.Unknown, null))

    @Test fun grantedWithoutProblemIsScanner() = assertEquals(GateScreen.Scanner, gateScreen(CamPerm.Granted, null))

    @Test fun noBackCameraIsNoCamera() = assertEquals(GateScreen.NoCamera, gateScreen(CamPerm.Granted, CameraProblem.NoBackCamera))

    @Test fun openFailedIsCameraError() = assertEquals(GateScreen.CameraError, gateScreen(CamPerm.Granted, CameraProblem.OpenFailed))

    @Test fun deniedIgnoresProblem() {
        assertEquals(GateScreen.Denied, gateScreen(CamPerm.Denied, null))
        assertEquals(GateScreen.Denied, gateScreen(CamPerm.Denied, CameraProblem.OpenFailed))
    }

    @Test fun blockedIgnoresProblem() {
        assertEquals(GateScreen.PermanentlyDenied, gateScreen(CamPerm.PermanentlyDenied, null))
        assertEquals(GateScreen.PermanentlyDenied, gateScreen(CamPerm.PermanentlyDenied, CameraProblem.NoBackCamera))
    }
}
