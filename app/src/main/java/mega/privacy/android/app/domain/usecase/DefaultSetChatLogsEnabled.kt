package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default set chat logs enabled
 *
 * @property settingsRepository
 */
class DefaultSetChatLogsEnabled @Inject constructor(private val settingsRepository: SettingsRepository) : SetChatLogsEnabled {
    override suspend fun invoke(enabled: Boolean) {
        settingsRepository.setChatLoggingEnabled(enabled)
    }
}