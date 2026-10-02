package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.repository.SyncTransferStoreRepository
import javax.inject.Inject

/** Monitors the sync transfer store; the first value may be NotSeeded. */
internal class MonitorSyncTransferStoreUseCase @Inject constructor(
    private val repository: SyncTransferStoreRepository,
) {
    operator fun invoke(): Flow<SyncTransferStoreState> = repository.monitorSyncTransferStore()
}
