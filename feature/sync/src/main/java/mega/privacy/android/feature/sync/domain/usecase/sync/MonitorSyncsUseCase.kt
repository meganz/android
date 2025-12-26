package mega.privacy.android.feature.sync.domain.usecase.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.usecase.file.CanReadUriUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Use case for monitoring syncs
 */
class MonitorSyncsUseCaseImpl @Inject constructor(
    private val syncRepository: SyncRepository,
    private val changeSyncLocalRootUseCase: ChangeSyncLocalRootUseCase,
    private val resumeSyncUseCase: ResumeSyncUseCase,
    private val canReadUriUseCase: CanReadUriUseCase,
    private val setSyncWorkerForegroundPreferenceUseCase: SetSyncWorkerForegroundPreferenceUseCase,
) : MonitorSyncsUseCase {


    /**
     * Invoke.
     *
     * @return A [Flow] that emits the syncs
     */
    override operator fun invoke(): Flow<List<FolderPair>> = channelFlow {
        launch {
            syncRepository.monitorFolderPairChanges()
                .distinctUntilChanged()
                .map { pairs ->
                    pairs.partition { it.syncError != SyncError.MISMATCH_OF_ROOT_FSID }
                }
                .conflate()
                .collect { (validSyncs, invalidSyncs) ->
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
    }

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
