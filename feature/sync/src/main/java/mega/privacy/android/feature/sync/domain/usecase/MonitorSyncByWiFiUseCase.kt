package mega.privacy.android.feature.sync.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import javax.inject.Inject

/**
 * Use case for monitoring if sync should be done only when connected to WiFi
 */
class MonitorSyncByWiFiUseCase @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository,
) {

    operator fun invoke(): StateFlow<Boolean> =
        syncPreferencesRepository.monitorSyncByWiFi()
}
