package mega.privacy.android.domain.usecase.auth

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use Case to enable multi-factor authentication for the account
 */
class EnableMultiFactorAuthUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Invocation function
     *
     * @param pin the valid pin code for multi-factor authentication
     * @return true if multi-factor authentication is successfully enabled
     */
    suspend operator fun invoke(pin: String): Boolean =
        settingsRepository.enableMultiFactorAuth(pin)
}