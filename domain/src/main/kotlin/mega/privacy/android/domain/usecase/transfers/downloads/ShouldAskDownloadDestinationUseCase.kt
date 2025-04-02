package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to get if download location needs to be asked to the user or it's already defined in settings
 */
class ShouldAskDownloadDestinationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(): Boolean =
        settingsRepository.isStorageAskAlways() || settingsRepository.getStorageDownloadLocation() == null

}