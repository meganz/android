package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

class DefaultGetStartScreen @Inject constructor(private val settingsRepository: SettingsRepository) : GetStartScreen {
    override fun invoke(): Int {
        return settingsRepository.getStartScreen()
    }
}