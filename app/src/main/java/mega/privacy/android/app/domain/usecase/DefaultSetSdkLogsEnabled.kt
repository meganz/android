package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default set sdk logs enabled
 *
 * @property settingsRepository
 */
class DefaultSetSdkLogsEnabled @Inject constructor(private val settingsRepository: SettingsRepository) : SetSdkLogsEnabled {
    override suspend fun invoke(enabled: Boolean) {
        settingsRepository.setSdkLoggingEnabled(enabled)
    }
}