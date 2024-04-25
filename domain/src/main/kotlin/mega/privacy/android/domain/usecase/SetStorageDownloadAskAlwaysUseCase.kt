package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Pass through use case to set Ask Always state
 */
class SetStorageDownloadAskAlwaysUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     * @param isChecked as [Boolean]
     */
    suspend operator fun invoke(isChecked: Boolean) {
        settingsRepository.setStorageAskAlways(isChecked)
    }
}