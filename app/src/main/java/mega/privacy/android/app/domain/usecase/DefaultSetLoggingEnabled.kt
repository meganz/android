package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

class DefaultSetLoggingEnabled @Inject constructor(private val settingsRepository: SettingsRepository) : SetLoggingEnabled {
    override fun invoke(enabled: Boolean) {
        settingsRepository.setLoggingEnabled(enabled)
    }
}