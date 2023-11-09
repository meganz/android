package mega.privacy.android.feature.sync.domain.usecase.solvedissues

import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import mega.privacy.android.feature.sync.domain.repository.SyncSolvedIssuesRepository
import javax.inject.Inject

internal class SetSyncSolvedIssueUseCase @Inject constructor(
    private val syncSolvedIssuesRepository: SyncSolvedIssuesRepository,
) {

    suspend operator fun invoke(solvedIssue: SolvedIssue) {
        syncSolvedIssuesRepository.set(solvedIssue)
    }
}