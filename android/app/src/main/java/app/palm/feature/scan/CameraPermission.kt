package app.palm.feature.scan

/** Camera permission as the app knows it (D-04, research Pattern 5 "Permission reducer"). */
enum class CamPerm { Unknown, Granted, Denied, PermanentlyDenied }

/**
 * [requested] is true once Android has answered a real request in this screen; before that the
 * rationale flag is false for "never asked" too, so it means nothing and the state is Unknown.
 * After an answer, no rationale means the system will not show the dialog again.
 */
fun cameraPerm(granted: Boolean, requested: Boolean, showRationale: Boolean): CamPerm = when {
    granted -> CamPerm.Granted
    !requested -> CamPerm.Unknown
    showRationale -> CamPerm.Denied
    else -> CamPerm.PermanentlyDenied
}

/** What the Scan route shows: the live scanner, or a permission or camera fallback (D-04, SCAN-02). */
enum class GateScreen { Requesting, Scanner, Denied, PermanentlyDenied, NoCamera, CameraError }

fun gateScreen(perm: CamPerm, problem: CameraProblem?): GateScreen = when (perm) {
    CamPerm.Unknown -> GateScreen.Requesting
    CamPerm.Denied -> GateScreen.Denied
    CamPerm.PermanentlyDenied -> GateScreen.PermanentlyDenied
    CamPerm.Granted -> when (problem) {
        null -> GateScreen.Scanner
        CameraProblem.NoBackCamera -> GateScreen.NoCamera
        CameraProblem.OpenFailed -> GateScreen.CameraError
    }
}
