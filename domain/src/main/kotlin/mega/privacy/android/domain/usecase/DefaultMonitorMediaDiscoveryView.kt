package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default monitor media discovery view setting
 */
class DefaultMonitorMediaDiscoveryView @Inject constructor(
    private val settingsRepository: SettingsRepository,
) :
    MonitorMediaDiscoveryView {
    override fun invoke(): Flow<Int?> =
        settingsRepository.monitorMediaDiscoveryView().map {
            it
        }
}