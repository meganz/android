package mega.privacy.android.feature.sync.domain.usecase.sync

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.domain.usecase.environment.GetBatteryInfoUseCase
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase.Companion.LOW_BATTERY_LEVEL
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import javax.inject.Inject

/**
 * Establishes a pair between local and remote directories and starts the syncing process
 */
internal class SyncFolderPairUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
    private val getBatteryInfoUseCase: GetBatteryInfoUseCase,
    private val isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase,
    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase,
    private val pauseSyncUseCase: PauseSyncUseCase,
) {

    /**
     * Invoke method
     *
     * @param syncType Sync type of the folder pair
     * @param name Name of the folder pair
     * @param localPath Local path on the device
     * @param remotePath MEGA folder path
     * @return Boolean indicating whether the folder was set up successfully or not
     */
    suspend operator fun invoke(
        syncType: SyncType,
        name: String?,
        localPath: String,
        remotePath: RemoteFolder
    ): Boolean {
        val folderPairHandle = syncRepository.setupFolderPair(
            syncType = syncType,
            name = name,
            localPath = localPath,
            remoteFolderId = remotePath.id.longValue
        )

        // Pause the new sync if required due settings, network or battery level
        folderPairHandle?.let { handle ->
            val syncOnlyByWifi = monitorSyncByWiFiUseCase().first()
            val userNotOnWifi = !isOnWifiNetworkUseCase()
            val batteryInfo = getBatteryInfoUseCase()
            val isLowBatteryLevel = batteryInfo.level < LOW_BATTERY_LEVEL && !batteryInfo.isCharging

            if (syncOnlyByWifi && userNotOnWifi || isLowBatteryLevel) {
                pauseSyncUseCase(handle)
            }
        }

        return folderPairHandle != null
    }
}
