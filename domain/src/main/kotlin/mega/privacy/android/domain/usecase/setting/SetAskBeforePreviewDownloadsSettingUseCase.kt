package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Pass through use case to set the setting to ask for confirmation before large preview downloads
 */
class SetAskBeforePreviewDownloadsSettingUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke
     * @param askForConfirmation true if the user wants to confirm large preview downloads, false if don't want to confirm anymore
     */
    suspend operator fun invoke(askForConfirmation: Boolean) {
        settingsRepository.setAskBeforePreviewDownloads(askForConfirmation)
    }
}
