package mega.privacy.android.feature.sync.domain.usecase.sync

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.entity.BatteryInfo
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
     * @param connectedToInternet   True if device is connected to Internet or False otherwise
     * @param syncOnlyByWifi        True if setting to sync only by WiFi is enabled or False otherwise
     * @param batteryInfo           The device [BatteryInfo]
     */
    suspend operator fun invoke(
        connectedToInternet: Boolean,
        isUserOnWifi: Boolean,
        syncOnlyByWifi: Boolean,
        batteryInfo: BatteryInfo,
    ) {
        val internetNotAvailable = !connectedToInternet
        val userNotOnWifi = !isUserOnWifi
        val isLowBatteryLevel =
            batteryInfo.level < LOW_BATTERY_LEVEL && !batteryInfo.isCharging

        if (internetNotAvailable || syncOnlyByWifi && userNotOnWifi || isLowBatteryLevel) {
            val activeSyncs =
                monitorSyncsUseCase().first()
                    .filter {
                        (it.syncError == SyncError.NO_SYNC_ERROR || it.syncError == null)
                                && (it.syncStatus == SyncStatus.SYNCED || it.syncStatus == SyncStatus.SYNCING)
                    }
            activeSyncs.forEach { pauseSyncUseCase(it.id) }
        } else {
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
        }
    }

    companion object {
        /**
         * Low battery level
         */
        const val LOW_BATTERY_LEVEL = 20
    }
}
