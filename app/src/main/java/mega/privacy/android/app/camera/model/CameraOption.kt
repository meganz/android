package mega.privacy.android.app.camera.model

import mega.privacy.android.app.camera.state.CaptureMode

internal enum class CameraOption(val mode: CaptureMode) {
    Photo(CaptureMode.Image),
    Video(CaptureMode.Video);
}
