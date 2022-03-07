package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default are sdk logs enabled
 *
 * @property settingsRepository
 */
class DefaultAreSdkLogsEnabled @Inject constructor(private val settingsRepository: SettingsRepository) : AreSdkLogsEnabled {
    override fun invoke(): Flow<Boolean> = settingsRepository.isSdkLoggingEnabled()
}