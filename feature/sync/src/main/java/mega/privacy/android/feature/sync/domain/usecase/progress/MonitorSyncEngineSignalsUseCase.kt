package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEngineSignals
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/** Monitors the SDK's waiting and sync stall flags, polled while collected. */
internal class MonitorSyncEngineSignalsUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {
    operator fun invoke(): Flow<SyncEngineSignals> = syncRepository.monitorSyncEngineSignals()
}
