package mega.privacy.android.domain.usecase.auth

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to check if the master key has been exported or not
 */
class IsMasterKeyExportedUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Invocation function
     *
     * @return true if the Master Key has been exported
     */
    suspend operator fun invoke() = settingsRepository.isMasterKeyExported()
}