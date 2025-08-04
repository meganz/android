package mega.privacy.android.domain.usecase.preference

import mega.privacy.android.domain.entity.preference.StartScreenDestinationPreference
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

class SetStartScreenPreferenceDestinationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(destination: StartScreenDestinationPreference) {
        settingsRepository.setStartScreenPreferenceDestination(destination)
    }
}