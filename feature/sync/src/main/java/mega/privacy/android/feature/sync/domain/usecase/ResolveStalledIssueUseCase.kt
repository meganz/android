package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.usecase.file.DeleteFileUseCase
import mega.privacy.android.domain.usecase.file.GetFileByPathUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.feature.sync.domain.mapper.StalledIssueToSolvedIssueMapper
import mega.privacy.android.feature.sync.domain.usecase.solvedissues.SetSyncSolvedIssueUseCase
import javax.inject.Inject

internal class ResolveStalledIssueUseCase @Inject constructor(
    private val deleteFileUseCase: DeleteFileUseCase,
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val getFileByPathUseCase: GetFileByPathUseCase,
    private val setSyncSolvedIssueUseCase: SetSyncSolvedIssueUseCase,
    private val stalledIssueToSolvedIssueMapper: StalledIssueToSolvedIssueMapper,
) {

    suspend operator fun invoke(
        stalledIssueResolutionAction: StalledIssueResolutionAction,
        stalledIssue: StalledIssue,
    ) {
        runCatching {
            resolveIssue(stalledIssueResolutionAction, stalledIssue)
        }.onSuccess {
            saveSolvedIssue(stalledIssue, stalledIssueResolutionAction)
        }
    }

    private suspend fun resolveIssue(
        stalledIssueResolutionAction: StalledIssueResolutionAction,
        stalledIssue: StalledIssue,
    ) {
        when (stalledIssueResolutionAction.resolutionActionType) {
            StalledIssueResolutionActionType.RENAME_ALL_ITEMS -> {
                // will be implemented in a separate MR
            }

            StalledIssueResolutionActionType.REMOVE_DUPLICATES -> {
                // will be implemented in a separate MR
            }

            StalledIssueResolutionActionType.MERGE_FOLDERS -> {
                // will be implemented in a separate MR
            }

            StalledIssueResolutionActionType.REMOVE_DUPLICATES_AND_REMOVE_THE_REST -> {
                // will be implemented in a separate MR
            }

            StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE -> {
                moveNodesToRubbishUseCase(listOf(stalledIssue.nodeIds.first().longValue))
            }

            StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE -> {
                deleteFileUseCase(stalledIssue.localPaths.first())
            }

            StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME -> {
                val localFile =
                    getFileByPathUseCase(stalledIssue.localPaths.first())
                val remoteNode = getNodeByHandleUseCase(stalledIssue.nodeIds.first().longValue)
                if (remoteNode is FileNode && localFile != null) {
                    val remoteModifiedDateInSeconds = remoteNode.modificationTime
                    val localModifiedDateInMilliseconds = localFile.lastModified()
                    if (localModifiedDateInMilliseconds / 1000 > remoteModifiedDateInSeconds) {
                        moveNodesToRubbishUseCase(listOf(stalledIssue.nodeIds.first().longValue))
                    } else {
                        deleteFileUseCase(stalledIssue.localPaths.first())
                    }
                }
            }
        }
    }

    private suspend fun saveSolvedIssue(
        stalledIssue: StalledIssue,
        stalledIssueResolutionAction: StalledIssueResolutionAction,
    ) {
        val solvedIssue = stalledIssueToSolvedIssueMapper(
            stalledIssue,
            stalledIssueResolutionAction.actionName
        )
        setSyncSolvedIssueUseCase(solvedIssue = solvedIssue)
    }
}