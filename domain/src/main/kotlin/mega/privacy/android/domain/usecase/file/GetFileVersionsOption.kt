package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Get file versions option
 *
 */
class GetFileVersionsOption @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(forceRefresh: Boolean) =
        settingsRepository.getFileVersionsOption(forceRefresh)
}