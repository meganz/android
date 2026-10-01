package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Pass through use case to get storage default download location path
 */
class GetStorageDownloadDefaultPathUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     * @return Default Download Location Path as [String]
     */
    suspend operator fun invoke(): String =
        settingsRepository.buildDefaultDownloadDir().absolutePath
}