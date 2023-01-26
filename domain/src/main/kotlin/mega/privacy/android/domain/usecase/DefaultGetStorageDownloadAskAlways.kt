package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Pass through use case to get Ask Always state
 */
class DefaultGetStorageDownloadAskAlways @Inject constructor(
    private val repository: SettingsRepository,
) : GetStorageDownloadAskAlways {
    /**
     * Invoke
     * @return storageAskAlways as [Boolean]
     */
    override suspend fun invoke(): Boolean {
        return repository.getStorageDownloadAskAlways()
    }
}