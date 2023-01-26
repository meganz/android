package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject


/**
 * Pass through use case to get storage download location path
 */
class DefaultGetStorageDownloadLocation @Inject constructor(
    private val repository: SettingsRepository
) : GetStorageDownloadLocation {
    /**
     * Invoke
     * @return Download Location Path as [String]
     */
    override suspend fun invoke(): String? {
        return repository.getStorageDownloadLocation()
    }
}