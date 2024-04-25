package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Save to settings that the user should not be prompted to save destination after choosing it
 */
class SaveDoNotPromptToSaveDestinationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() = settingsRepository.setAskSetDownloadLocation(false)
}