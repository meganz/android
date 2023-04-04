package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Pass through use case to get Ask Always state
 */
class GetStorageDownloadAskAlwaysUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Invoke
     * @return storageAskAlways as [Boolean]
     */
    suspend operator fun invoke(): Boolean = settingsRepository.getStorageDownloadAskAlways()
}