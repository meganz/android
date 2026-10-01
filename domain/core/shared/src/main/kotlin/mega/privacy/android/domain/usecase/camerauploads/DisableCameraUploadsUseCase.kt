package mega.privacy.android.domain.usecase.camerauploads

import javax.inject.Inject

/**
 * Disable camera uploads and clean up settings and resources
 */
class DisableCameraUploadsUseCase @Inject constructor(
    private val disableCameraUploadsSettingsUseCase: DisableCameraUploadsSettingsUseCase,
) {

    /**
     * Disable camera uploads and clean up settings and resources
     */
    suspend operator fun invoke() {
        disableCameraUploadsSettingsUseCase()
    }
}
