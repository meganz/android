package mega.privacy.android.feature.sync.ui.mapper

import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StallIssueType.NamesWouldClashWhenSynced
import mega.privacy.android.feature.sync.domain.entity.StallIssueType.LocalAndRemoteChangedSinceLastSyncedState_userMustChoose
import mega.privacy.android.feature.sync.domain.entity.StallIssueType.LocalAndRemotePreviouslyUnsyncedDiffer_userMustChoose
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import javax.inject.Inject

internal class StalledIssueResolutionActionMapper @Inject constructor() {

    operator fun invoke(issueType: StallIssueType): List<StalledIssueResolutionAction> =
        when (issueType) {
            NamesWouldClashWhenSynced -> {
                listOf(
                    StalledIssueResolutionAction(
                        "Rename all items",
                        StalledIssueResolutionActionType.RENAME_ALL_ITEMS
                    ),
                    StalledIssueResolutionAction(
                        "Remove duplicates",
                        StalledIssueResolutionActionType.REMOVE_DUPLICATES
                    ),
                    StalledIssueResolutionAction(
                        "Merge folders",
                        StalledIssueResolutionActionType.MERGE_FOLDERS
                    ),
                    StalledIssueResolutionAction(
                        "Remove duplicates and rename the rest",
                        StalledIssueResolutionActionType.REMOVE_DUPLICATES_AND_REMOVE_THE_REST
                    ),
                )
            }

            LocalAndRemoteChangedSinceLastSyncedState_userMustChoose,
            LocalAndRemotePreviouslyUnsyncedDiffer_userMustChoose,
            -> {
                listOf(
                    StalledIssueResolutionAction(
                        "Choose local file",
                        StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE
                    ),
                    StalledIssueResolutionAction(
                        "Choose remote file",
                        StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE
                    ),
                    StalledIssueResolutionAction(
                        "Choose the one with the latest modified time",
                        StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME
                    ),
                )
            }

            else -> emptyList()
        }
}