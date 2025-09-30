package mega.privacy.android.domain.usecase.home

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

class DeleteWidgetConfigurationsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(identifier: String) {
        settingsRepository.deleteHomeScreenWidgetConfiguration(identifier)
    }
}