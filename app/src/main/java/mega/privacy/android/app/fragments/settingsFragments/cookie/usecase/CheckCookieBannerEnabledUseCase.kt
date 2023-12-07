package mega.privacy.android.app.fragments.settingsFragments.cookie.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case to check if Cookie Banner is enabled on SDK
 */
class CheckCookieBannerEnabledUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() = accountRepository.isCookieBannerEnabled()

}
