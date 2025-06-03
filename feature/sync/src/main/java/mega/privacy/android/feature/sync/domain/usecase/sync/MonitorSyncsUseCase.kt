package mega.privacy.android.feature.sync.domain.usecase.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.usecase.file.CanReadUriUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
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
) : MonitorSyncsUseCase {


    /**
     * Invoke.
     *
     * @return A [Flow] that emits the syncs
     */
    override operator fun invoke(): Flow<List<FolderPair>> = channelFlow {
        launch {
            syncRepository.monitorFolderPairChanges()
                .conflate()
                .collect { folderPairs ->
                    val (validSyncs, notResumedSyncs) = processFolderPairs(folderPairs)
                    send(validSyncs + notResumedSyncs)
                }
        }
    }

    private suspend fun processFolderPairs(folderPairs: List<FolderPair>): Pair<List<FolderPair>, List<FolderPair>> {
        val (validSyncs, invalidSyncs) = folderPairs.partition { it.syncError != SyncError.MISMATCH_OF_ROOT_FSID }
        // we need to retry resuming the syncs that have a mismatch of root fsId
        // fsId might change for an existing sync if the device is restarted and SD card will no longer point to the same fsId
        val notResumedSyncs = handleInvalidSyncs(invalidSyncs)
        return validSyncs to notResumedSyncs
    }

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
