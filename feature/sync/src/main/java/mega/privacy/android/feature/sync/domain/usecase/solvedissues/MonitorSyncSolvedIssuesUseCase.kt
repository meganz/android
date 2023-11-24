package mega.privacy.android.feature.sync.domain.usecase.solvedissues

import mega.privacy.android.feature.sync.domain.repository.SyncSolvedIssuesRepository
import javax.inject.Inject

internal class MonitorSyncSolvedIssuesUseCase @Inject constructor(
    private val syncSolvedIssuesRepository: SyncSolvedIssuesRepository,
) {
    operator fun invoke() =
        syncSolvedIssuesRepository.monitorSolvedIssues()
}