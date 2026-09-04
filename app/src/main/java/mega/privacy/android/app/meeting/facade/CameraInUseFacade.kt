package mega.privacy.android.app.meeting.facade

import mega.privacy.android.app.meeting.gateway.CameraInUseGateway
import mega.privacy.android.app.utils.VideoCaptureUtils
import javax.inject.Inject

/**
 * Implementation of [CameraInUseGateway] that delegates to the JNI-backed [VideoCaptureUtils] static.
 */
class CameraInUseFacade @Inject constructor() : CameraInUseGateway {

    override fun isFrontCameraInUse(): Boolean = VideoCaptureUtils.isFrontCameraInUse()
}
