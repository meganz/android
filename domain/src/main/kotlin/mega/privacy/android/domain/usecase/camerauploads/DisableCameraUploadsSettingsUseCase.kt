package mega.privacy.android.domain.usecase.camerauploads

import javax.inject.Inject

/**
 * Disable the camera uploads and media uploads settings
 *
 */
class DisableCameraUploadsSettingsUseCase @Inject constructor(
    private val setupMediaUploadsSettingUseCase: SetupMediaUploadsSettingUseCase,
    private val setupCameraUploadsSettingUseCase: SetupCameraUploadsSettingUseCase,
) {

    /**
     * Disable the camera uploads settings
     */
    suspend operator fun invoke() {
        setupCameraUploadsSettingUseCase(isEnabled = false)
        setupMediaUploadsSettingUseCase(isEnabled = false)
    }
}
