package mega.privacy.android.feature.sync.domain.usecase.solvedissue

import mega.privacy.android.feature.sync.domain.repository.SyncSolvedIssuesRepository
import javax.inject.Inject

internal class ClearSyncSolvedIssuesUseCase @Inject constructor(
    private val syncSolvedIssuesRepository: SyncSolvedIssuesRepository,
) {

    suspend operator fun invoke() {
        syncSolvedIssuesRepository.clear()
    }
}