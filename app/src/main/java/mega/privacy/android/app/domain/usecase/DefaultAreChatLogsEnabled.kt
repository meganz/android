package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default are chat logs enabled
 *
 * @property settingsRepository
 */
class DefaultAreChatLogsEnabled @Inject constructor(private val settingsRepository: SettingsRepository) : AreChatLogsEnabled {
    override fun invoke(): Flow<Boolean> {
        return settingsRepository.isChatLoggingEnabled()
    }
}