package mega.privacy.android.feature.sync.domain.usecase.solvedissue

import mega.privacy.android.feature.sync.domain.repository.SyncSolvedIssuesRepository
import javax.inject.Inject

/**
 * Use case to clear all the solved issues related to a specific sync id.
 *
 */
internal class ClearSolvedIssuesBySyncIdUseCase @Inject constructor(
    private val syncSolvedIssuesRepository: SyncSolvedIssuesRepository
) {

    /**
     * @param syncId [Long] id of the sync for which the issues were raised
     */
    suspend operator fun invoke(syncId: Long) {
        syncSolvedIssuesRepository.removeBySyncId(syncId)
    }
}