package mega.privacy.android.domain.usecase.home

import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

class MarkNewFeatureDisplayedUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val environmentRepository: EnvironmentRepository,
) {
    suspend operator fun invoke() {
        environmentRepository.getAppVersion()?.let {
            settingsRepository.setLastVersionNewFeatureShown(it)
        }
    }
}
