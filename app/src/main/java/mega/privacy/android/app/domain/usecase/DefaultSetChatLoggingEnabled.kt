package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

class DefaultSetChatLoggingEnabled @Inject constructor(private val settingsRepository: SettingsRepository) : SetChatLoggingEnabled {
    override fun invoke(enabled: Boolean) {
        settingsRepository.setChatLoggingEnabled(enabled)
    }
}