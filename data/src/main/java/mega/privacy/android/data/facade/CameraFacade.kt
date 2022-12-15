package mega.privacy.android.data.facade

import mega.privacy.android.data.gateway.CameraGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import org.webrtc.Camera1Enumerator
import org.webrtc.CameraEnumerator
import timber.log.Timber
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
        val frontCamera = getFrontCamera()
        if (frontCamera != null) {
            chatGateway.setChatVideoInDevice(frontCamera, null)
        } else {
            getBackCamera()?.let { backCamera ->
                chatGateway.setChatVideoInDevice(backCamera, null)
            }
        }
    }

    override fun getFrontCamera(): String? = getCameraDevice(true)

    override fun getBackCamera(): String? = getCameraDevice(false)

    /**
     * Get the front camera device.
     *
     * @return Front camera device.
     */
    private fun getCameraDevice(front: Boolean): String? {
        val enumerator: CameraEnumerator = Camera1Enumerator(true)
        val deviceList: Array<String> = deviceList()
        for (device in deviceList) {
            if (front && enumerator.isFrontFacing(device) || !front && enumerator.isBackFacing(
                    device)
            ) {
                return device
            }
        }
        return null
    }

    /**
     * Get the video capture devices list.
     *
     * @return The video capture devices list.
     */
    private fun deviceList(): Array<String> {
        Timber.d("DeviceList")
        val enumerator: CameraEnumerator = Camera1Enumerator(true)
        return enumerator.deviceNames
    }
}