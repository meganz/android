package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Pass through use case to set Ask Always state
 */
class DefaultSetStorageDownloadAskAlways @Inject constructor(
    private val repository: SettingsRepository
) : SetStorageDownloadAskAlways {
    /**
     * Invoke
     * @param isChecked as [Boolean]
     */
    override suspend fun invoke(isChecked: Boolean) {
        repository.setStorageAskAlways(isChecked)
    }
}