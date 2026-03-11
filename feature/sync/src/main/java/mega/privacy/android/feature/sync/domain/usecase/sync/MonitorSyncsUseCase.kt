package mega.privacy.android.feature.sync.domain.usecase.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.backup.IsFolderUsedBySyncOrBackupAcrossDevicesUseCase
import mega.privacy.android.domain.usecase.file.CanReadUriUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.repository.SyncNotificationRepository
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetUserPausedSyncUseCase
import javax.inject.Inject

/**
 * Use case for monitoring syncs
 */
internal class MonitorSyncsUseCaseImpl @Inject constructor(
    private val syncRepository: SyncRepository,
    private val changeSyncLocalRootUseCase: ChangeSyncLocalRootUseCase,
    private val resumeSyncUseCase: ResumeSyncUseCase,
    private val canReadUriUseCase: CanReadUriUseCase,
    private val setSyncWorkerForegroundPreferenceUseCase: SetSyncWorkerForegroundPreferenceUseCase,
    private val isFolderUsedBySyncOrBackupAcrossDevicesUseCase: IsFolderUsedBySyncOrBackupAcrossDevicesUseCase,
    private val pauseSyncUseCase: PauseSyncUseCase,
    private val setUserPausedSyncUseCase: SetUserPausedSyncUseCase,
    private val syncNotificationRepository: SyncNotificationRepository,
    @ApplicationScope private val coroutineScope: CoroutineScope,
) : MonitorSyncsUseCase {

    // Shared flow - only one upstream subscription regardless of how many collectors
    private val sharedFlow: Flow<List<FolderPair>> = channelFlow {
        launch {
            syncRepository.monitorFolderPairChanges()
                .distinctUntilChanged()
                .map { pairs ->
                    pairs.partition { it.syncError != SyncError.MISMATCH_OF_ROOT_FSID }
                }
                .conflate()
                .collect { (validSyncs, invalidSyncs) ->
                    // Check for cross-device folder conflicts
                    val conflictingSyncs = validSyncs.filter {
                        val result = isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                            nodeId = it.remoteFolder.id,
                            shouldCheckCameraUploads = true,
                            shouldExcludeCurrentDevice = true,
                            useCache = true,
                        )
                        result != FolderUsageResult.NotUsed
                    }

                    // Pause conflicting syncs and mark as user-paused to prevent auto-resume
                    conflictingSyncs.forEach {
                        pauseSyncUseCase(it.id)
                        setUserPausedSyncUseCase(it.id, paused = true)
                    }

                    // Store notification for conflicting syncs
                    if (conflictingSyncs.isNotEmpty()) {
                        syncNotificationRepository.setPendingCrossDeviceConflictNotification(
                            conflictingSyncs
                        )
                    }

                    val pausedSyncs = invalidSyncs.map {
                        it.copy(
                            syncError = SyncError.NO_SYNC_ERROR,
                            syncStatus = SyncStatus.PAUSED
                        )
                    }
                    if (pausedSyncs.isNotEmpty()) {
                        send(validSyncs + pausedSyncs)
                        val notResumedSyncs = handleInvalidSyncs(invalidSyncs)
                        val totalSyncs =
                            validSyncs + pausedSyncs.minus(notResumedSyncs.toSet()) + notResumedSyncs
                        send(totalSyncs)
                    } else {
                        send(validSyncs)
                    }
                }
        }
    }.onEach {
        if (isSyncingCompleted(it)) {
            // this will ensure that when sync worker is not running when app is opened,
            // if the cancelled sync worker's syncs are completed then set the foreground preference to false
            setSyncWorkerForegroundPreferenceUseCase(false)
        }
    }.shareIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 1
    )

    /**
     * Invoke.
     *
     * @return A [Flow] that emits the syncs
     */
    override operator fun invoke(): Flow<List<FolderPair>> = sharedFlow

    private fun isSyncingCompleted(syncs: List<FolderPair>): Boolean =
        syncs.isNotEmpty() && syncs.all { it.syncStatus == SyncStatus.SYNCED || it.syncStatus == SyncStatus.PAUSED }

    private suspend fun handleInvalidSyncs(invalidSyncs: List<FolderPair>): List<FolderPair> {
        return invalidSyncs.mapNotNull { folderPair ->
            val localPath = folderPair.localFolderPath
            if (canReadUriUseCase(localPath)) {
                changeSyncLocalRootUseCase(folderPair.id, localPath)
                resumeSyncUseCase(folderPair.id)
                null // Successfully resumed, no need to add to notResumedSyncs
            } else {
                folderPair // Add to notResumedSyncs if it cannot be resumed
            }
        }
    }
}

/**
 * Use case for monitoring syncs
 */
interface MonitorSyncsUseCase {
    /**
     * Invoke.
     *
     * @return A [Flow] that emits the syncs
     */
    operator fun invoke(): Flow<List<FolderPair>>
}
