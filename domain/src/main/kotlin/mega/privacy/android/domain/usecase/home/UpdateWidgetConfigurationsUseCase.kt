package mega.privacy.android.domain.usecase.home

import mega.privacy.android.domain.entity.home.HomeWidgetConfiguration
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateWidgetConfigurationsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(configurations: List<HomeWidgetConfiguration>) {
        settingsRepository.updateHomeScreenWidgetConfiguration(configurations)
    }
}