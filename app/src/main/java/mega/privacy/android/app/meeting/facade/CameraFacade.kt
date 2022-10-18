package mega.privacy.android.app.meeting.facade

import mega.privacy.android.app.meeting.gateway.CameraGateway
import mega.privacy.android.app.utils.VideoCaptureUtils
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import javax.inject.Inject

/**
 * Camera facade
 * implementation of [CameraGateway]
 *
 * @property chatGateway [MegaChatApiGateway]
 */
class CameraFacade @Inject constructor(
    private val chatGateway: MegaChatApiGateway,
) : CameraGateway {

    override fun setFrontCamera() {
        getFrontCamera().let { frontCamera ->
            chatGateway.setChatVideoInDevice(frontCamera, null)
        }
    }

    override fun getFrontCamera(): String =
        VideoCaptureUtils.getFrontCamera()

    override fun getBackCamera(): String =
        VideoCaptureUtils.getBackCamera()
}