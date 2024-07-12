package mega.privacy.android.feature.sync.data.mapper.solvedissue

import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import javax.inject.Inject

internal class SolvedIssueEntityToSolvedIssueMapper @Inject constructor(
    private val listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
) {

    operator fun invoke(syncSolvedIssueEntity: SyncSolvedIssueEntity): SolvedIssue {
        return with(syncSolvedIssueEntity) {
            SolvedIssue(
                syncId = syncId,
                nodeIds = listToStringWithDelimitersMapper(nodeIds),
                localPaths = listToStringWithDelimitersMapper(localPaths),
                resolutionExplanation = resolutionExplanation
            )
        }
    }
}