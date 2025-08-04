package mega.privacy.android.domain.usecase.preference

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

class MonitorStartScreenPreferenceDestinationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    operator fun invoke() =
        settingsRepository.monitorStartScreenPreferenceDestination()
}