package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Pass through use case to get the setting to ask for confirmation before large preview downloads
 */
class ShouldAskBeforePreviewDownloadsSettingUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke
     * @return true if the user needs to confirm large preview downloads, false otherwise
     */
    suspend operator fun invoke() = settingsRepository.monitorAskBeforePreviewDownloads().first()
}
