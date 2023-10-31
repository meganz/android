package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.domain.usecase.file.DeleteFileUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import javax.inject.Inject

internal class ResolveStalledIssueUseCase @Inject constructor(
    private val deleteFileUseCase: DeleteFileUseCase,
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase
) {

    suspend operator fun invoke(
        stalledIssueResolutionAction: StalledIssueResolutionAction,
        megaNodeId: Long,
        localPath: String
    ) {
        when (stalledIssueResolutionAction.resolutionActionType) {
            StalledIssueResolutionActionType.RENAME_ALL_ITEMS -> {

            }

            StalledIssueResolutionActionType.REMOVE_DUPLICATES -> {

            }

            StalledIssueResolutionActionType.MERGE_FOLDERS -> {

            }

            StalledIssueResolutionActionType.REMOVE_DUPLICATES_AND_REMOVE_THE_REST -> {

            }

            StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE -> {
                moveNodesToRubbishUseCase(listOf(megaNodeId))
            }

            StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE -> {
                deleteFileUseCase(localPath)
            }

            StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME -> {

            }
        }
    }
}