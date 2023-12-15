package mega.privacy.android.app.fragments.settingsFragments.cookie.usecase

import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.setting.BroadcastCookieSettingsSavedUseCase
import timber.log.Timber
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
    suspend operator fun invoke(enabledCookieSettings: Set<CookieType>) {
        runCatching {
            accountRepository.setCookieSettings(enabledCookieSettings)
            broadcastCookieSettingsSavedUseCase(enabledCookieSettings)
        }.onFailure {
            Timber.e(it, "Error updating cookie settings")
        }
    }
}