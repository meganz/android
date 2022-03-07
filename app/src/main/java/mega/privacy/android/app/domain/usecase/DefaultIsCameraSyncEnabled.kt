package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default is camera sync enabled
 *
 * @property settingsRepository
 */
class DefaultIsCameraSyncEnabled @Inject constructor(private val settingsRepository: SettingsRepository) : IsCameraSyncEnabled {
    override fun invoke(): Boolean {
        return settingsRepository.getPreferences()?.camSyncEnabled.toBoolean()
    }
}