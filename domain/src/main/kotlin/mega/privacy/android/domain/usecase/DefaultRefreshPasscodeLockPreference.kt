package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default refresh passcode lock preference
 *
 * @property settingsRepository
 */
class DefaultRefreshPasscodeLockPreference @Inject constructor(private val settingsRepository: SettingsRepository) :
    RefreshPasscodeLockPreference {
    override suspend fun invoke(): Boolean {
        if (settingsRepository.isPasscodeLockPreferenceEnabled() == null) {
            settingsRepository.setPasscodeLockEnabled(false)
        }
        return settingsRepository.isPasscodeLockPreferenceEnabled()
            ?: throw IllegalStateException("Passcode lock preference should not be null after setting it to false.")
    }
}