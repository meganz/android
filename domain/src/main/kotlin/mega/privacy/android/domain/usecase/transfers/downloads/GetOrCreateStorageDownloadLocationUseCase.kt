package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Pass through use case to get storage download location path checking if it's not null, if it's null it first sets it to default location
 */
class GetOrCreateStorageDownloadLocationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     * @return Download Location Path as [String]
     */
    suspend operator fun invoke(): String? =
        settingsRepository.getStorageDownloadLocation() ?: run {
            settingsRepository.setDefaultStorageDownloadLocation()
            settingsRepository.getStorageDownloadLocation()
        }
}