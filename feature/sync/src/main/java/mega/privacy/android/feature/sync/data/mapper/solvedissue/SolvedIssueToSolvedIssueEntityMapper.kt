package mega.privacy.android.feature.sync.data.mapper.solvedissue

import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import javax.inject.Inject

internal class SolvedIssueToSolvedIssueEntityMapper @Inject constructor(
    private val listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
    private val encryptData: EncryptData,
) {
    suspend operator fun invoke(solvedIssue: SolvedIssue): SyncSolvedIssueEntity {
        return with(solvedIssue) {
            SyncSolvedIssueEntity(
                nodeIds = encryptData(listToStringWithDelimitersMapper(nodeIds)) ?: "",
                localPaths = encryptData(listToStringWithDelimitersMapper(localPaths)) ?: "",
                resolutionExplanation = encryptData(resolutionExplanation) ?: ""
            )
        }
    }
}