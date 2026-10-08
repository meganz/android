package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunSnapshot
import mega.privacy.android.feature.sync.domain.repository.SyncProgressRepository
import javax.inject.Inject

/** Monitors the app-wide sync run snapshot; the first value may be NotSeeded. */
internal class MonitorSyncRunSnapshotUseCase @Inject constructor(
    private val syncProgressRepository: SyncProgressRepository,
) {
    operator fun invoke(): Flow<SyncRunSnapshot> = syncProgressRepository.monitorSyncRunSnapshot()
}
