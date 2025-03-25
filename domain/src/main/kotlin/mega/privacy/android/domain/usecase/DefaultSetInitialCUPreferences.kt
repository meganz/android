package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.passcode.DisablePasscodeUseCase
import javax.inject.Inject

/**
 * Set initial CU Preferences
 */
class DefaultSetInitialCUPreferences @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val accountRepository: AccountRepository,
    private val disablePasscodeUseCase: DisablePasscodeUseCase
) : SetInitialCUPreferences {

    override suspend fun invoke() {
        accountRepository.setUserHasLoggedIn()
        settingsRepository.setAskSetDownloadLocation(true)
        settingsRepository.setDefaultStorageDownloadLocation()
        disablePasscodeUseCase()
        settingsRepository.setShowCopyright()
    }
}
