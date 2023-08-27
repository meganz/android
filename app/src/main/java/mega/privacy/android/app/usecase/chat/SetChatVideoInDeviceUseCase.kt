package mega.privacy.android.app.usecase.chat

import mega.privacy.android.app.utils.wrapper.CameraEnumeratorWrapper
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Set chat video in device use case
 *
 * @property callRepository     [CallRepository]
 */
class SetChatVideoInDeviceUseCase @Inject constructor(
    private val callRepository: CallRepository,
    private val cameraEnumeratorWrapper: CameraEnumeratorWrapper,
) {

    /**
     * Set Chat Video In Device based on device camera devices
     *
     * @param isFrontCamera     True to use the front camera, false to use the back camera
     */
    suspend operator fun invoke(isFrontCamera: Boolean = true) {
        val cameraDevice = getCameraDevice(isFrontCamera)
            ?: getCameraDevice(!isFrontCamera) ?: error("Camera not found")
        callRepository.setChatVideoInDevice(cameraDevice)
    }

    /**
     * Get WebRTC camera device
     *
     * @param isFrontCamera     True to retrieve the front camera, false to use the back camera
     * @return                  Camera device name
     */
    private fun getCameraDevice(isFrontCamera: Boolean): String? =
        cameraEnumeratorWrapper().let { cameraEnumerator ->
            cameraEnumerator.deviceNames.firstOrNull { device ->
                (isFrontCamera && cameraEnumerator.isFrontFacing(device))
                        || (!isFrontCamera && cameraEnumerator.isBackFacing(device))
            }
        }
}
