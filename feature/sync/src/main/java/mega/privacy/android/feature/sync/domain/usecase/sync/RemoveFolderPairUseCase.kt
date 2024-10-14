package mega.privacy.android.feature.sync.domain.usecase.sync

import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.solvedissue.ClearSolvedIssuesBySyncIdUseCase
import javax.inject.Inject

/**
 * Use case to remove a sync and all its related solved issues.
 */
class RemoveFolderPairUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
    private val clearSolvedIssuesBySyncIdUseCase: ClearSolvedIssuesBySyncIdUseCase
) {

    /**
     * @param folderPairId [Long] id of the sync
     */
    suspend operator fun invoke(folderPairId: Long) {
        syncRepository.removeFolderPair(folderPairId)
        clearSolvedIssuesBySyncIdUseCase(folderPairId)
    }
}