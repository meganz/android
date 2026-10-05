package mega.privacy.android.feature.sync.domain.usecase.sync

import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.solvedissue.ClearSolvedIssuesBySyncIdUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetUserPausedSyncUseCase
import javax.inject.Inject

/**
 * Use case to remove a sync, its related solved issues and its user pause mark.
 */
internal class RemoveFolderPairUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
    private val clearSolvedIssuesBySyncIdUseCase: ClearSolvedIssuesBySyncIdUseCase,
    private val setUserPausedSyncUseCase: SetUserPausedSyncUseCase,
) {

    /**
     * @param folderPairId [Long] id of the sync
     */
    suspend operator fun invoke(folderPairId: Long) {
        syncRepository.removeFolderPair(folderPairId)
        clearSolvedIssuesBySyncIdUseCase(folderPairId)
        setUserPausedSyncUseCase(folderPairId, paused = false)
    }
}
