package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseToggle
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorUserPausedSyncIdsUseCase
import javax.inject.Inject

/** Monitors the pause action to offer on the sync progress screen. */
internal class MonitorSyncPauseToggleUseCase @Inject constructor(
    private val monitorSyncRunSnapshotUseCase: MonitorSyncRunSnapshotUseCase,
    private val monitorUserPausedSyncIdsUseCase: MonitorUserPausedSyncIdsUseCase,
    private val getSyncPauseToggleUseCase: GetSyncPauseToggleUseCase,
) {

    /** Emits from the first seeded run snapshot on; distinct values only. */
    operator fun invoke(): Flow<SyncPauseToggle> = combine(
        monitorSyncRunSnapshotUseCase().filter { it.isSeeded },
        monitorUserPausedSyncIdsUseCase(),
    ) { runSnapshot, userPausedSyncIds ->
        getSyncPauseToggleUseCase(runSnapshot.syncs, userPausedSyncIds)
    }.distinctUntilChanged()
}
