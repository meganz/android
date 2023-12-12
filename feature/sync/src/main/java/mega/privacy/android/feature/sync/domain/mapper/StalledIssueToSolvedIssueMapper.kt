package mega.privacy.android.feature.sync.domain.mapper

import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import javax.inject.Inject

internal class StalledIssueToSolvedIssueMapper @Inject constructor() {

    operator fun invoke(
        stalledIssue: StalledIssue,
        actionTaken: StalledIssueResolutionActionType
    ): SolvedIssue =
        SolvedIssue(
            nodeIds = stalledIssue.nodeIds,
            localPaths = stalledIssue.localPaths,
            resolutionExplanation = actionTaken.name
        )
}