package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for monitoring changes of cookie settings.
 */
class MonitorCookieSettingsSavedUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {


    /**
     * Invoke.
     *
     * @return Flow of [Set] of [CookieType]
     */
    operator fun invoke() = accountRepository.monitorCookieSettingsSaved()
}