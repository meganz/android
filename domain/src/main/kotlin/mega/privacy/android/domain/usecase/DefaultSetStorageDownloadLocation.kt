package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Pass through use case to set storage download location path
 */
class DefaultSetStorageDownloadLocation @Inject constructor(
    private val repository: SettingsRepository
) : SetStorageDownloadLocation {
    /**
     * Invoke
     * @param location as folder path [String]
     */
    override suspend fun invoke(location: String) {
        repository.setStorageDownloadLocation(location)
    }
}