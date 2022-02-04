package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

class DefaultShouldHideRecentActivity @Inject constructor(private val settingsRepository: SettingsRepository) : ShouldHideRecentActivity {
    override fun invoke(): Boolean {
        return settingsRepository.shouldHideRecentActivity()
    }
}