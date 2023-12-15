package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for broadcasting changes of cookie settings.
 */
class BroadcastCookieSettingsSavedUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {


    /**
     * Invoke.
     *
     * @param enabledCookieSettings set of enabled cookies of [CookieType]
     */
    suspend operator fun invoke(enabledCookieSettings: Set<CookieType>) =
        accountRepository.broadcastCookieSettings(enabledCookieSettings)
}