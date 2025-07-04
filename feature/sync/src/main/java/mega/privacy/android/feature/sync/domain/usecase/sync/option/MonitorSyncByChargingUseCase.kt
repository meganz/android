package mega.privacy.android.feature.sync.domain.usecase.sync.option

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import javax.inject.Inject

/**
 * Use case for monitoring if sync should be done only when charging
 */
class MonitorSyncByChargingUseCase @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository,
) {

    operator fun invoke(): Flow<Boolean> =
        syncPreferencesRepository
            .monitorSyncByCharging()
            .map { it ?: false }
}
