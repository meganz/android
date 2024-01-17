package mega.privacy.android.feature.sync.ui.mapper.stalledissue

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose
import mega.privacy.android.feature.sync.domain.entity.StallIssueType.LocalAndRemotePreviouslyNotSyncedDifferUserMustChoose
import mega.privacy.android.feature.sync.domain.entity.StallIssueType.NamesWouldClashWhenSynced
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import javax.inject.Inject

internal class StalledIssueResolutionActionMapper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    operator fun invoke(
        issueType: StallIssueType,
        areAllNodesFolders: Boolean,
    ): List<StalledIssueResolutionAction> =
        when (issueType) {
            NamesWouldClashWhenSynced -> {
                if (areAllNodesFolders) {
                    listOf(
                        StalledIssueResolutionAction(
                            actionName = context.getString(R.string.sync_resolve_rename_all_items),
                            StalledIssueResolutionActionType.RENAME_ALL_ITEMS
                        ),
                        StalledIssueResolutionAction(
                            context.getString(R.string.sync_stalled_issue_merge_folders),
                            StalledIssueResolutionActionType.MERGE_FOLDERS
                        ),
                    )
                } else {
                    listOf(
                        StalledIssueResolutionAction(
                            context.getString(R.string.sync_resolve_rename_all_items),
                            StalledIssueResolutionActionType.RENAME_ALL_ITEMS
                        ),
                    )
                }
            }

            LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
            LocalAndRemotePreviouslyNotSyncedDifferUserMustChoose,
            -> {
                listOf(
                    StalledIssueResolutionAction(
                        context.getString(R.string.sync_stalled_issue_choose_local_file),
                        StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE
                    ),
                    StalledIssueResolutionAction(
                        context.getString(R.string.sync_stalled_issue_choose_remote_file),
                        StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE
                    ),
                    StalledIssueResolutionAction(
                        context.getString(R.string.sync_stalled_issue_choose_latest_modified_time),
                        StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME
                    ),
                )
            }

            else -> emptyList()
        }
}