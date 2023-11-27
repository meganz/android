package mega.privacy.android.feature.sync.data.mapper.solvedissue

import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import javax.inject.Inject

internal class SolvedIssueEntityToSolvedIssueMapper @Inject constructor(
    private val listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
    private val decryptData: DecryptData,
) {

    suspend operator fun invoke(syncSolvedIssueEntity: SyncSolvedIssueEntity): SolvedIssue {
        return with(syncSolvedIssueEntity) {
            SolvedIssue(
                nodeIds = decryptData(nodeIds)?.let { listToStringWithDelimitersMapper(it) }
                    ?: run { emptyList() },
                localPaths = decryptData(localPaths)?.let { listToStringWithDelimitersMapper(it) }
                    ?: run { emptyList() },
                resolutionExplanation = decryptData(resolutionExplanation) ?: ""
            )
        }
    }
}