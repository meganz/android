package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

class DefaultIsChatLoggingEnabled @Inject constructor(private val settingsRepository: SettingsRepository) : IsChatLoggingEnabled {
    override fun invoke(): Boolean {
        return settingsRepository.getAttributes()?.fileLoggerKarere.toBoolean()
    }
}