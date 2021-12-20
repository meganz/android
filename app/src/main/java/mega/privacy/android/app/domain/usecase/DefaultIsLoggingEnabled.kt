package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

class DefaultIsLoggingEnabled @Inject constructor(private val settingsRepository: SettingsRepository)  : IsLoggingEnabled {
    override fun invoke(): Boolean {
        return settingsRepository.isLoggingEnabled()
    }
}