package mega.privacy.android.domain.usecase.home

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

class MonitorHomeWidgetConfigurationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke() = settingsRepository.monitorHomeScreenWidgetConfiguration()
}