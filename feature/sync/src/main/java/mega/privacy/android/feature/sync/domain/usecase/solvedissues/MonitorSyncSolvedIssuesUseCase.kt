package mega.privacy.android.feature.sync.domain.usecase.solvedissues

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import mega.privacy.android.feature.sync.domain.repository.SyncSolvedIssuesRepository
import javax.inject.Inject

internal class MonitorSyncSolvedIssuesUseCase @Inject constructor(
    private val getSyncSolvedIssuesUseCase: GetSyncSolvedIssuesUseCase,
    private val syncSolvedIssuesRepository: SyncSolvedIssuesRepository,
) {

    suspend operator fun invoke(): Flow<List<SolvedIssue>> =
        syncSolvedIssuesRepository
            .monitorSolvedIssuesCountChanged()
            .map { getSyncSolvedIssuesUseCase() }
            .onStart { emit(getSyncSolvedIssuesUseCase()) }
}