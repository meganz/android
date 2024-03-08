package mega.privacy.android.domain.usecase.camerauploads

import javax.inject.Inject

/**
 * Disable Media Upload Settings
 *
 */
class DisableMediaUploadsSettingsUseCase @Inject constructor(
    private val setupMediaUploadsSettingUseCase: SetupMediaUploadsSettingUseCase
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() {
        setupMediaUploadsSettingUseCase(isEnabled = false)
    }
}
