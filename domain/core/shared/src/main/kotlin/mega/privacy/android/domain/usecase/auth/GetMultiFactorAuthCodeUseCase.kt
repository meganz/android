package mega.privacy.android.domain.usecase.auth

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use Case that returns the multi factor authentication code
 */
class GetMultiFactorAuthCodeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Invocation function
     *
     * @return the secret code of the account to enable multi-factor authentication
     */
    suspend operator fun invoke() = settingsRepository.getMultiFactorAuthCode()
}