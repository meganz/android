package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Pass through use case to get storage download location path
 */
class GetStorageDownloadLocationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Invoke
     * @return Download Location Path as [String]
     */
    suspend operator fun invoke(): String? = settingsRepository.getStorageDownloadLocation()
}