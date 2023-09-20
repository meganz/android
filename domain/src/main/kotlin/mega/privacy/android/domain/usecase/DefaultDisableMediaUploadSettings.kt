package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.usecase.camerauploads.SetupMediaUploadsSettingUseCase
import javax.inject.Inject

/**
 * Default Implementation of [DisableMediaUploadSettings]
 *
 */
class DefaultDisableMediaUploadSettings @Inject constructor(
    private val setupMediaUploadsSettingUseCase: SetupMediaUploadsSettingUseCase
) : DisableMediaUploadSettings {

    override suspend fun invoke() {
        setupMediaUploadsSettingUseCase(isEnabled = false)
    }
}
