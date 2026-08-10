package mega.privacy.android.domain.usecase.home

import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

class ShouldDisplayNewFeatureUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val environmentRepository: EnvironmentRepository,
) {
    suspend operator fun invoke(): Boolean {
        val current = environmentRepository.getAppVersion() ?: return false
        val last = settingsRepository.getLastVersionNewFeatureShown() ?: return true

        return current > last
    }
}
