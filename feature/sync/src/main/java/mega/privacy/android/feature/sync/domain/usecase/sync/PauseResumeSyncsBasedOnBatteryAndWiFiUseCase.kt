package mega.privacy.android.feature.sync.domain.usecase.sync

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.sync.option.IsSyncPausedByTheUserUseCase
import javax.inject.Inject

/**
 * Use case to pause/resume syncs based on the status of device battery and WiFi
 *
 * @param pauseSyncUseCase              [PauseSyncUseCase]
 * @param resumeSyncUseCase             [ResumeSyncUseCase]
 * @param monitorSyncsUseCase         [MonitorSyncsUseCase]
 * @param isSyncPausedByTheUserUseCase  [IsSyncPausedByTheUserUseCase]
 */
class PauseResumeSyncsBasedOnBatteryAndWiFiUseCase @Inject constructor(
    private val pauseSyncUseCase: PauseSyncUseCase,
    private val resumeSyncUseCase: ResumeSyncUseCase,
    private val monitorSyncsUseCase: MonitorSyncsUseCase,
    private val isSyncPausedByTheUserUseCase: IsSyncPausedByTheUserUseCase,
) {

    /**
     * Invoke
     *
     * @param shouldResumeSync       True if syncs should be resumed or False if they should be paused
     */
    suspend operator fun invoke(
        shouldResumeSync: Boolean,
    ) {
        if (shouldResumeSync) {
            val activeSyncs =
                monitorSyncsUseCase().first()
                    .filter {
                        (it.syncError == SyncError.NO_SYNC_ERROR || it.syncError == null)
                                && it.syncStatus == SyncStatus.PAUSED
                                && !isSyncPausedByTheUserUseCase(it.id)
                    }
            activeSyncs.forEach {
                resumeSyncUseCase(it.id)
            }
        } else {
            val activeSyncs =
                monitorSyncsUseCase().first()
                    .filter {
                        (it.syncError == SyncError.NO_SYNC_ERROR || it.syncError == null)
                                && (it.syncStatus == SyncStatus.SYNCED || it.syncStatus == SyncStatus.SYNCING)
                    }
            activeSyncs.forEach { pauseSyncUseCase(it.id) }
        }
    }
}
