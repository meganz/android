package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Set initial CU Preferences
 */
class DefaultSetInitialCUPreferences @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val accountRepository: AccountRepository,
) : SetInitialCUPreferences {

    override suspend fun invoke() {
        accountRepository.setUserHasLoggedIn()
        settingsRepository.setStorageAskAlways(true)
        settingsRepository.setDefaultStorageDownloadLocation()
        settingsRepository.setPasscodeLockEnabled(false)
        settingsRepository.setPasscodeLockCode("")
        settingsRepository.setShowCopyright()
    }
}
