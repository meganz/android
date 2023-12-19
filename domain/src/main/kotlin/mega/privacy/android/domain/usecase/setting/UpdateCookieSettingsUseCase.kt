package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case to update cookie settings on SDK
 */
class UpdateCookieSettingsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val broadcastCookieSettingsSavedUseCase: BroadcastCookieSettingsSavedUseCase,
) {

    /**
     * Updates cookie settings on SDK and broadcasts the event
     *
     * @param enabledCookieSettings Set of enabled cookie settings
     */
    suspend operator fun invoke(
        enabledCookieSettings: Set<CookieType>,
    ) {
        accountRepository.setCookieSettings(enabledCookieSettings)
        broadcastCookieSettingsSavedUseCase(enabledCookieSettings)
    }
}