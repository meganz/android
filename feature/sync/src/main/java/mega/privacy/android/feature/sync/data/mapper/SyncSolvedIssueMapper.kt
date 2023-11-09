package mega.privacy.android.feature.sync.data.mapper

import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import javax.inject.Inject

internal class SyncSolvedIssueMapper @Inject constructor(
    private val listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
) {

    operator fun invoke(solvedIssue: SolvedIssue): SyncSolvedIssueEntity =
        SyncSolvedIssueEntity(
            nodeIds = listToStringWithDelimitersMapper(solvedIssue.nodeIds),
            localPaths = listToStringWithDelimitersMapper(solvedIssue.localPaths),
            resolutionExplanation = solvedIssue.resolutionExplanation,
        )

    operator fun invoke(dbEntity: SyncSolvedIssueEntity): SolvedIssue =
        SolvedIssue(
            nodeIds = listToStringWithDelimitersMapper(dbEntity.nodeIds),
            localPaths = listToStringWithDelimitersMapper(dbEntity.localPaths),
            resolutionExplanation = dbEntity.resolutionExplanation,
        )
}