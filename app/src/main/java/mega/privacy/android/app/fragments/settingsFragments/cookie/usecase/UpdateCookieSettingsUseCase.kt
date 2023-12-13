package mega.privacy.android.app.fragments.settingsFragments.cookie.usecase

import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case to update cookie settings on SDK
 */
class UpdateCookieSettingsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(cookies: Set<CookieType>) {
        accountRepository.setCookieSettings(cookies)
        //To trigger BroadcastCookieSettingsSavedUseCase that will be invoked in AND-17883
    }

}