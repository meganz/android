package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

class DefaultMonitorStartScreenPreference @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : MonitorStartScreenPreference {
    override fun invoke(): Flow<StartScreen> {
        return settingsRepository.monitorPreferredStartScreen()
            .mapNotNull { it ?: StartScreen.Home }
    }
}